package com.terracotta.tools;

import com.terracotta.tools.utils.CacheFactory;
import com.terracotta.tools.utils.CacheSizeStats;
import com.terracotta.tools.utils.NamedThreadFactory;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.pool.sizeof.SizeOf;
import net.sf.ehcache.pool.sizeof.filter.PassThroughFilter;
import org.apache.commons.cli.*;
import org.apache.commons.lang.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class cacheObjectsInspect {
    private static Logger log = LoggerFactory.getLogger(cacheObjectsInspect.class);
    private static final boolean isDebug = log.isDebugEnabled();

    private static final int KEYSTAT_INDEX = 0;
    private static final int VALUESTAT_INDEX = 1;

    private static final int DEFAULT_SAMPLEDSIZE = 1000;
    private static final int DEFAULT_CACHEPOOLSIZE = 2;
    private static final int DEFAULT_CACHEGETSPOOLSIZE = 4;

    public static final String CONFIG_CALCULATESERIALIZEDSIZE = "serializedSize";
    public static final String CONFIG_USETHREADING = "useThreading";
    public static final String CONFIG_CACHEPOOLSIZE = "cachePoolSize";
    public static final String CONFIG_CACHEGETSPOOLSIZE = "cacheGetsPoolSize";
    public static final String CONFIG_SIZEOFTYPE = "sizeOfType";

    private final boolean useThreading = System.getProperties().containsKey(CONFIG_USETHREADING);
    private final boolean serializedSizeCalculation = System.getProperties().containsKey(CONFIG_CALCULATESERIALIZEDSIZE);

    private final String sCachePoolSize = System.getProperty(CONFIG_CACHEPOOLSIZE, new Integer(DEFAULT_CACHEPOOLSIZE).toString());
    private final String sCacheGetsPoolSize = System.getProperty(CONFIG_CACHEGETSPOOLSIZE, new Integer(DEFAULT_CACHEGETSPOOLSIZE).toString());

    private final String[] myCacheNames;
    private final int sampledSize;

    private final ExecutorService cacheFetchService;
    private final ExecutorService cacheGetService;

    //agent (DEFAULT), reflection, unsafe
    private static final SizeOf sizeOf;

    static {
        String sizeOfType = System.getProperty(CONFIG_SIZEOFTYPE);
        if (null == sizeOfType) {
            sizeOf = new net.sf.ehcache.pool.sizeof.AgentSizeOf(new PassThroughFilter(), true);
        } else {
            sizeOfType = sizeOfType.toLowerCase();
            if ("reflection".equals(sizeOfType)) {
                sizeOf = new net.sf.ehcache.pool.sizeof.ReflectionSizeOf(new PassThroughFilter(), true);
            } else if ("agent".equals(sizeOfType)) {
                sizeOf = new net.sf.ehcache.pool.sizeof.AgentSizeOf(new PassThroughFilter(), true);
            } else if ("unsafe".equals(sizeOfType)) {
                sizeOf = new net.sf.ehcache.pool.sizeof.UnsafeSizeOf(new PassThroughFilter(), true);
            } else {
                sizeOf = new net.sf.ehcache.pool.sizeof.AgentSizeOf(new PassThroughFilter(), true);
            }
        }
    }

    public cacheObjectsInspect(final String cacheName) {
        this(cacheName, -1);
    }

    public cacheObjectsInspect(final String cacheName, int sampledSize) {
        CacheManager cacheManager = CacheFactory.getInstance().getCacheManager();

        if (sampledSize <= 0)
            throw new IllegalArgumentException("Sampled size should be > 0");

        this.sampledSize = sampledSize;

        if (cacheName == null || !cacheManager.cacheExists(cacheName)) {
            this.myCacheNames = cacheManager.getCacheNames();
        } else {
            this.myCacheNames = new String[]{cacheName};
        }

        if (myCacheNames.length == 0)
            throw new IllegalArgumentException("No cache defined...verify that ehcache.xml is specified.");

        int cachePoolSize;
        try {
            cachePoolSize = Integer.parseInt(sCachePoolSize);
        } catch (NumberFormatException e) {
            cachePoolSize = DEFAULT_CACHEPOOLSIZE;
        }

        int cacheGetsPoolSize;
        try {
            cacheGetsPoolSize = Integer.parseInt(sCacheGetsPoolSize);
        } catch (NumberFormatException e) {
            cacheGetsPoolSize = DEFAULT_CACHEGETSPOOLSIZE;
        }

        this.cacheFetchService = Executors.newFixedThreadPool((myCacheNames.length > cachePoolSize) ? cachePoolSize : myCacheNames.length, new NamedThreadFactory("Cache Sizing Pool"));
        this.cacheGetService = Executors.newFixedThreadPool(cacheGetsPoolSize, new NamedThreadFactory("Cache Gets Pool"));
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        shutdownAndAwaitTermination(cacheFetchService);
    }

    public void findObjectSizesInCache() {
        if (useThreading) {
            findThreadingObjectSizesInCache();
        } else {
            findSerialObjectSizesInCache();
        }
    }

    private void findThreadingObjectSizesInCache() {
        Future<CacheSizeStats[]> futs[] = new Future[myCacheNames.length];
        int cachecount = 0;
        for (String cacheName : myCacheNames) {
            Cache myCache = CacheFactory.getInstance().getCache(cacheName);
            futs[cachecount++] = cacheFetchService.submit(new CacheFetchOp(myCache));
        }

        for (int i = 0; i < cachecount; i++) {
            try {
                while (!futs[i].isDone()) {
                    System.out.print(".");
                    Thread.sleep(5000);
                }

                CacheSizeStats[] stats = futs[i].get();
                if (null != stats) {
                    if (stats.length > KEYSTAT_INDEX && null != stats[KEYSTAT_INDEX]) {
                        System.out.println("Keys Inspector--" + stats[KEYSTAT_INDEX].toString());
                    }

                    if (stats.length > VALUESTAT_INDEX && null != stats[VALUESTAT_INDEX]) {
                        System.out.println("Values Inspector--" + stats[VALUESTAT_INDEX].toString());
                    }
                }
            } catch (InterruptedException e) {
                log.error("", e);
            } catch (ExecutionException e) {
                log.error("", e);
            }
        }
    }

    private void findSerialObjectSizesInCache() {
        CacheSizeStats[] stats = new CacheSizeStats[myCacheNames.length];
        for (int j = 0; j < myCacheNames.length; j++) {
            Cache myCache = CacheFactory.getInstance().getCache(myCacheNames[j]);
            stats[j] = new CacheSizeStats(myCache.getName());

            List<Object> keys = myCache.getKeys();
            int iterationLimit = 0;
            for (Object key : keys) {
                if (iterationLimit >= sampledSize) break;

                Element e = myCache.getQuiet(key);
                if (e != null) {
                    long size = (serializedSizeCalculation) ? e.getSerializedSize() : sizeOf.deepSizeOf(Integer.MAX_VALUE, true, e.getObjectValue()).getCalculated();
                    stats[j].add(size, getObjectType(e.getObjectValue()));
                    if (isDebug)
                        log.debug("Cache=" + myCache.getName() + ":\t" + "Key = " + e.getObjectKey() + " size of element = " + size);
                }
                iterationLimit++;
            }
        }
        System.out.println("");

        for (CacheSizeStats stat : stats) {
            System.out.println(stat.toString());
        }
    }

    private class CacheFetchOp implements Callable<CacheSizeStats[]> {
        private final Cache myCache;

        public CacheFetchOp(Cache myCache) {
            this.myCache = myCache;
        }

        @Override
        public CacheSizeStats[] call() throws Exception {
            CacheSizeStats[] cacheStats = null;
            try {
                cacheStats = new CacheSizeStats[]{new CacheSizeStats(myCache.getName()), new CacheSizeStats(myCache.getName())};

                List<Object> keys = myCache.getKeys();
                int iterationLimit = 0;
                for (Object key : keys) {
                    if (iterationLimit >= sampledSize) break;

                    cacheGetService.submit(new CacheGetOp(myCache, key, cacheStats));

                    iterationLimit++;
                }


            } catch (IllegalStateException e1) {
                log.error("", e1);
            } catch (CacheException e1) {
                log.error("", e1);
            }
            return cacheStats;
        }
    }

    private class CacheGetOp implements Runnable {
        private final Cache myCache;
        private final Object key;
        private final CacheSizeStats[] cacheStats;

        public CacheGetOp(Cache myCache, Object key, CacheSizeStats[] cacheStats) {
            this.myCache = myCache;
            this.cacheStats = cacheStats;
            this.key = key;
        }

        @Override
        public void run() {
            try {
                Element e = myCache.getQuiet(key);
                if (e != null) {
                    if (e.getKey() != null) {
                        //TODO
                        byte[] serialized = SerializationUtils.serialize(e.getKey());

                        long heapSize = sizeOf.deepSizeOf(Integer.MAX_VALUE, true, e.getKey()).getCalculated();

                        cacheStats[KEYSTAT_INDEX].add(heapSize, getObjectType(e.getKey()));
                        if (isDebug) {
                            log.debug(String.format("Cache=%s:\t-Deserialized Size=%d,Serialized size=%d" + heapSize, myCache.getName(), heapSize, serialized.length));
                        }
                    }

                    if (e.getValue() != null) {
                        //TODO
                        byte[] serialized = SerializationUtils.serialize(e.getValue());
                        long heapSize = sizeOf.deepSizeOf(Integer.MAX_VALUE, true, e.getValue()).getCalculated();

                        cacheStats[VALUESTAT_INDEX].add(heapSize, getObjectType(e.getValue()));
                        if (isDebug)
                            log.debug("Cache=" + myCache.getName() + ":\t" + "Size of value object = " + heapSize);
                    }
                }
            } catch (IllegalStateException e1) {
                log.error("", e1);
            } catch (CacheException e1) {
                log.error("", e1);
            }
        }
    }

    /*
     * thread executor shutdown
     */
    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted

        try {
            // Wait until existing tasks to terminate
            while (!pool.awaitTermination(5, TimeUnit.SECONDS)) ;

            pool.shutdownNow(); // Cancel currently executing tasks

            // Wait a while for tasks to respond to being canceled
            if (!pool.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS))
                log.error("Pool did not terminate");
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();

            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    public String getObjectType(Object obj) {
        String objectType;
        if (obj instanceof Collection) {
            Collection objBag = (Collection) obj;

            objectType = objBag.getClass().getCanonicalName();
            for (Object innerObj : objBag) {
                objectType += "->" + innerObj.getClass().getCanonicalName();
                break;
            }
        } else {
            objectType = obj.getClass().getCanonicalName();
        }
        return objectType;
    }

    public static void main(String[] args) {
        String cacheNames;
        int samplingSize;

        try {
            // create Options object
            Options options = new Options();
            options.addOption(new Option("help", "this message..."));
            options.addOption("cacheNames", true, "Cache names to inspect.");
            options.addOption("samplingSize", true, "Number of items to sample for size calculation");

            // create the parser
            CommandLineParser parser = new GnuParser();
            try {
                // parse the command line arguments
                CommandLine line = parser.parse(options, args);
                if (line.hasOption("help")) {
                    HelpFormatter formatter = new HelpFormatter();
                    formatter.printHelp("SizeIteratorLauncher", options);
                    System.exit(0);
                }
                cacheNames = line.getOptionValue("cacheNames", "");
                samplingSize = Integer.parseInt(line.getOptionValue("samplingSize", new Integer(DEFAULT_SAMPLEDSIZE).toString()));
            } catch (ParseException exp) {
                // oops, something went wrong
                System.err.println("Parsing failed.  Reason: " + exp.getMessage());
                return;
            }

            cacheObjectsInspect launcher = new cacheObjectsInspect(cacheNames, samplingSize);
            launcher.findObjectSizesInCache();

            CacheFactory.getInstance().getCacheManager().shutdown();
            System.exit(0);
        } catch (Exception ex) {
            log.error("", ex);
            System.exit(1);
        }
    }
}