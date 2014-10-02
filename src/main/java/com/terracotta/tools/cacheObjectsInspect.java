package com.terracotta.tools;

import com.terracotta.tools.utils.CacheFactory;
import com.terracotta.tools.utils.CacheSizeStats;
import com.terracotta.tools.utils.CacheStatsDefinition;
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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class cacheObjectsInspect {
    private static Logger log = LoggerFactory.getLogger(cacheObjectsInspect.class);
    private static final boolean isDebug = log.isDebugEnabled();

    private static final int DEFAULT_SAMPLEDSIZE = 1000;
    private static final int DEFAULT_CACHEPOOLSIZE = 2;

    public static final String CONFIG_NO_UNSERIALIZED_SIZE = "noUnSerializedSize";
    public static final String CONFIG_NO_SERIALIZED_SIZE = "noSerializedSize";

    public static final String CONFIG_USETHREADING = "useThreading";
    public static final String CONFIG_CACHEPOOLSIZE = "cachePoolSize";
    public static final String CONFIG_SIZEOFTYPE = "sizeOfType";

    private final boolean useThreading = System.getProperties().containsKey(CONFIG_USETHREADING);

    private final boolean noSerializedSize = System.getProperties().containsKey(CONFIG_NO_SERIALIZED_SIZE);
    private final boolean noUnSerializedSize = System.getProperties().containsKey(CONFIG_NO_UNSERIALIZED_SIZE);

    private final String sCachePoolSize = System.getProperty(CONFIG_CACHEPOOLSIZE, new Integer(DEFAULT_CACHEPOOLSIZE).toString());

    private final String[] myCacheNames;
    private final int sampledSize;

    private final ExecutorService cacheFetchService;
    //private final ExecutorService cacheGetService;

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

        this.cacheFetchService = Executors.newFixedThreadPool((myCacheNames.length > cachePoolSize) ? cachePoolSize : myCacheNames.length, new NamedThreadFactory("Cache Sizing Pool"));
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
        Future<Map<CacheStatsDefinition, CacheSizeStats>> futs[] = new Future[myCacheNames.length];
        int cacheCount = 0;
        for (String cacheName : myCacheNames) {
            Cache myCache = CacheFactory.getInstance().getCache(cacheName);
            futs[cacheCount++] = cacheFetchService.submit(new CacheFetchOp(myCache));
        }

        for (int i = 0; i < cacheCount; i++) {
            try {
                while (!futs[i].isDone()) {
                    System.out.print(".");
                    Thread.sleep(5000);
                }

                Map<CacheStatsDefinition, CacheSizeStats> cacheStats = futs[i].get();
                if (null != cacheStats) {
                    for (CacheStatsDefinition statsDef : cacheStats.keySet()) {
                        CacheSizeStats stats = cacheStats.get(statsDef);

                        System.out.println(statsDef.toString() + " " + ((null != stats) ? stats.toString() : "null"));
                    }
                }
                System.out.println("");
            } catch (InterruptedException e) {
                log.error("", e);
            } catch (ExecutionException e) {
                log.error("", e);
            }
        }
    }

    private void findSerialObjectSizesInCache() {

        Map<CacheStatsDefinition, CacheSizeStats> cacheStats = null;
        for (String cacheName : myCacheNames) {
            Cache myCache = CacheFactory.getInstance().getCache(cacheName);
            cacheStats = new CacheFetchOp(myCache).call();

            if (null != cacheStats) {
                for (CacheStatsDefinition statsDef : cacheStats.keySet()) {
                    CacheSizeStats stats = cacheStats.get(statsDef);

                    System.out.println(statsDef.toString() + " " + ((null != stats) ? stats.toString() : "null"));
                }
            }
            System.out.println("");
        }
    }

    private class CacheFetchOp implements Callable<Map<CacheStatsDefinition, CacheSizeStats>> {
        private final Cache myCache;

        public CacheFetchOp(Cache myCache) {
            this.myCache = myCache;
        }

        @Override
        public Map<CacheStatsDefinition, CacheSizeStats> call() {
            Map<CacheStatsDefinition, CacheSizeStats> cacheStats = new HashMap<CacheStatsDefinition, CacheSizeStats>();
            try {
                List<Object> keys = myCache.getKeys();
                int iterationLimit = 0;
                for (Object key : keys) {
                    if (iterationLimit >= sampledSize) break;

                    doGets(key, cacheStats);

                    iterationLimit++;
                }
            } catch (IllegalStateException e1) {
                log.error("", e1);
            } catch (CacheException e1) {
                log.error("", e1);
            }
            return cacheStats;
        }

        private void doGets(Object key, Map<CacheStatsDefinition, CacheSizeStats> cacheStats) {
            try {
                Element e = myCache.getQuiet(key);
                if (e != null) {
                    Object objKey = e.getObjectKey();
                    Object objValue = e.getObjectValue();

                    if (objKey != null) {
                        String objType = getObjectType(objKey);

                        //serialized case
                        if (!noSerializedSize) {
                            byte[] serializedSize = SerializationUtils.serialize((Serializable) objKey);
                            add(CacheStatsDefinition.key_serialized(myCache.getName()), cacheStats, serializedSize.length, objType);

                            if (isDebug) {
                                log.debug(String.format("Cache=%s - Target: KEY - Serialized size=%d", myCache.getName(), serializedSize.length));
                            }
                        }

                        //unserialized case
                        if (!noUnSerializedSize) {
                            long objectOnHeapSize = sizeOf.deepSizeOf(Integer.MAX_VALUE, true, objKey).getCalculated();
                            add(CacheStatsDefinition.key_unserialized(myCache.getName()), cacheStats, objectOnHeapSize, objType);

                            if (isDebug) {
                                log.debug(String.format("Cache=%s - Target: KEY - Unserialized Size=%d", myCache.getName(), objectOnHeapSize));
                            }
                        }
                    }

                    if (objValue != null) {
                        String objType = getObjectType(objValue);

                        //serialized case
                        if (!noSerializedSize) {
                            byte[] serializedSize = SerializationUtils.serialize((Serializable) objValue);
                            add(CacheStatsDefinition.value_serialized(myCache.getName()), cacheStats, serializedSize.length, objType);

                            if (isDebug) {
                                log.debug(String.format("Cache=%s - Target: VALUE - Serialized size=%d", myCache.getName(), serializedSize.length));
                            }
                        }

                        //unserialized case
                        if (!noUnSerializedSize) {
                            long objectOnHeapSize = sizeOf.deepSizeOf(Integer.MAX_VALUE, true, objValue).getCalculated();
                            add(CacheStatsDefinition.value_unserialized(myCache.getName()), cacheStats, objectOnHeapSize, objType);

                            if (isDebug) {
                                log.debug(String.format("Cache=%s - Target: VALUE - Serialized size=%d", myCache.getName(), objectOnHeapSize));
                            }
                        }
                    }
                }
            } catch (IllegalStateException e1) {
                log.error("", e1);
            } catch (CacheException e1) {
                log.error("", e1);
            }
        }

        private void add(CacheStatsDefinition cacheDef, Map<CacheStatsDefinition, CacheSizeStats> cacheStats, long value, String type) {
            CacheSizeStats stats = null;
            if (null == (stats = cacheStats.get(cacheDef))) {
                stats = new CacheSizeStats();
            }
            stats.add(value, type);
            cacheStats.put(cacheDef, stats);
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
            options.addOption("cachename", true, "Cache name to inspect.");
            options.addOption("samplingsize", true, "Number of items to sample for size calculation");

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
                cacheNames = line.getOptionValue("cachename", "");
                samplingSize = Integer.parseInt(line.getOptionValue("samplingsize", new Integer(DEFAULT_SAMPLEDSIZE).toString()));
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