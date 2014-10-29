package com.terracotta.tools;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import com.terracotta.tools.utils.CacheFactory;
import com.terracotta.tools.utils.NamedThreadFactory;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

public class cacheDateClear {
    private static Logger log = LoggerFactory.getLogger(cacheDateClear.class);
    private static final boolean isDebug = log.isDebugEnabled();
    private final ExecutorService cacheFetchService;
    private final AppParams runParams;

    public cacheDateClear(final AppParams params) {
        this.runParams = params;

        if (null == runParams.getCacheNames() || runParams.getCacheNames().size() == 0) {
            throw new IllegalArgumentException("No cache defined...verify that ehcache.xml is specified.");
        }

        this.cacheFetchService = Executors.newCachedThreadPool(new NamedThreadFactory("Concurrent Cache Operations"));
    }

    public void run() {
        if (runParams.isUseThreading()) {
            selectiveRemoveInCache();
        } else {
            selectiveRemoveInCacheSerial();
        }
    }

    public void postRun() {
        CacheFactory.getInstance().getCacheManager().shutdown();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        shutdownAndAwaitTermination(cacheFetchService);
    }

    private void selectiveRemoveInCache() {
        Future futs[] = new Future[runParams.getCacheNames().size()];
        int cacheCount = 0;
        for (String cacheName : runParams.getCacheNames()) {
            System.out.println(String.format("Working on cache %s", cacheName));
            Cache myCache = CacheFactory.getInstance().getCache(cacheName);
            futs[cacheCount++] = cacheFetchService.submit(
                    new CacheFetchAndRemoveOp(
                            myCache,
                            runParams.getDateCompareType(),
                            runParams.getDateCompareOperation(),
                            runParams.getDate().getTime()));
        }

        for (int i = 0; i < cacheCount; i++) {
            try {
                while (!futs[i].isDone()) {
                    System.out.print(".");
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                log.error("", e);
            }
        }
    }

    private void selectiveRemoveInCacheSerial() {
        for (String cacheName : runParams.getCacheNames()) {
            System.out.println(String.format("Working on cache %s", cacheName));
            Cache myCache = CacheFactory.getInstance().getCache(cacheName);
            if (null != myCache) {
                new CacheFetchAndRemoveOp(
                        myCache,
                        runParams.getDateCompareType(),
                        runParams.getDateCompareOperation(),
                        runParams.getDate().getTime()).run();
            }
        }
    }

    public enum DateCompareType {created, lastUpdated, lastAccessed}

    ;

    public enum DateCompareOperation {before, after, equal}

    ;

    public class CacheFetchAndRemoveOp implements Runnable {
        private final Cache myCache;
        private final long dateTimeTocompare;
        private final DateCompareType dateCompareType;
        private final DateCompareOperation dateCompareOperation;

        private final ExecutorService cacheGetsPool;
        private final CompletionService<Integer> cacheGetCompletionService;

        public CacheFetchAndRemoveOp(Cache myCache, DateCompareType dateCompareType, DateCompareOperation dateCompareOperation, long dateTimeTocompare) {
            this.myCache = myCache;
            this.dateTimeTocompare = dateTimeTocompare;
            this.dateCompareType = dateCompareType;
            this.dateCompareOperation = dateCompareOperation;

            this.cacheGetsPool = Executors.newFixedThreadPool(runParams.cacheThreadCounts, new NamedThreadFactory(String.format("Cache %s Gets Pool", myCache.getName())));
            this.cacheGetCompletionService = new ExecutorCompletionService<Integer>(cacheGetsPool);
        }

        @Override
        public void run() {
            if (null != myCache) {
                List<Object> keys = myCache.getKeys();
                try {
                    for (final Object key : keys) {
                        cacheGetCompletionService.submit(
                                new Callable<Integer>() {
                                    @Override
                                    public Integer call() throws Exception {
                                        return getKeyAndRemove(key);
                                    }
                                }
                        );
                    }

                    int removeCount = 0;
                    for (int i = 0; i < keys.size(); i++) {
                        removeCount += cacheGetCompletionService.take().get();
                    }
                    log.info(String.format("Final Removal Summary: %s entries removed", removeCount));
                } catch (InterruptedException e) {
                    log.error("", e);
                } catch (ExecutionException e) {
                    log.error("", e);
                } finally {
                    shutdownAndAwaitTermination(cacheGetsPool);
                }
            } else {
                throw new IllegalArgumentException("cache is null...not able to perform any operation.");
            }
        }

        private int getKeyAndRemove(Object key) {
            int removeCount = 0;
            Element e = myCache.getQuiet(key);
            if (e != null) {
                long elementDate;
                switch (dateCompareType) {
                    case created:
                        elementDate = e.getCreationTime();
                        break;
                    case lastUpdated:
                        elementDate = e.getLastUpdateTime();
                        break;
                    case lastAccessed:
                        elementDate = e.getLastAccessTime();
                        break;
                    default:
                        elementDate = e.getCreationTime();
                }

                boolean removeEntry = false;
                switch (dateCompareOperation) {
                    case after: //remove if element's date is after provided date
                        removeEntry = (elementDate > dateTimeTocompare);

                        if (isDebug)
                            log.debug(String.format("Element Date(%s) > Date Time to compare(%s) --> %s", new Date(elementDate).toString(), new Date(dateTimeTocompare).toString(), removeEntry));

                        break;
                    case before: //remove if element's date is before provided date
                        removeEntry = (elementDate < dateTimeTocompare);

                        if (isDebug)
                            log.debug(String.format("Element Date(%s) < Date Time to compare(%s) --> %s", new Date(elementDate).toString(), new Date(dateTimeTocompare).toString(), removeEntry));

                        break;
                    case equal: //remove if element's date is equal provided date
                        removeEntry = (elementDate == dateTimeTocompare);

                        if (isDebug)
                            log.debug(String.format("Element Date(%s) == Date Time to compare(%s) --> %s", new Date(elementDate).toString(), new Date(dateTimeTocompare).toString(), removeEntry));

                        break;
                }

                if (removeEntry) {
                    log.info(String.format("Removing entry with key %s", key.toString()));
                    myCache.remove(key);
                    removeCount++;
                }
            }
            return removeCount;
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

    public static void main(String[] args) {
        try {
            AppParams params = CliFactory.parseArgumentsUsingInstance(new AppParams(), args);

            cacheDateClear launcher = new cacheDateClear(params);

            launcher.run();

            launcher.postRun();

            System.exit(0);
        } catch (Exception e) {
            log.error("", e);
            System.exit(1);
        }
    }

    public static class AppParams {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyyMMdd HHmmss");

        List<String> cacheNames;
        DateCompareType dateCompareType;
        DateCompareOperation dateCompareOperation;
        Date date;
        boolean useThreading;
        int cacheThreadCounts;

        public AppParams() {
        }

        public List<String> getCacheNames() {
            return cacheNames;
        }

        @Option(shortName = "c")
        public void setCacheNames(List<String> cacheNames) {
            this.cacheNames = cacheNames;
        }

        public DateCompareType getDateCompareType() {
            return dateCompareType;
        }

        @Option(shortName = "t")
        public void setDateCompareType(DateCompareType dateCompareType) {
            this.dateCompareType = dateCompareType;
        }

        public DateCompareOperation getDateCompareOperation() {
            return dateCompareOperation;
        }

        @Option(shortName = "o")
        public void setDateCompareOperation(DateCompareOperation dateCompareOperation) {
            this.dateCompareOperation = dateCompareOperation;
        }

        public boolean isUseThreading() {
            return useThreading;
        }

        @Option(defaultValue = "true", longName = "parallel")
        public void setUseThreading(boolean useThreading) {
            this.useThreading = useThreading;
        }

        public int getCacheThreadCounts() {
            return cacheThreadCounts;
        }

        @Option(defaultValue = "4", longName = "cachethreads")
        public void setCacheThreadCounts(int cacheThreadCounts) {
            this.cacheThreadCounts = cacheThreadCounts;
        }

        public Date getDate() {
            return date;
        }

        @Option(shortName = "d")
        public void setDate(String dateInString) {
            if (null != dateInString && !"".equals(dateInString)) {
                if ("now".equalsIgnoreCase(dateInString))
                    this.date = new Date();
                else {
                    try {
                        if (dateInString.length() == dateFormatter.toPattern().length())
                            this.date = dateFormatter.parse(dateInString);
                        else if (dateInString.length() == dateTimeFormatter.toPattern().length())
                            this.date = dateTimeFormatter.parse(dateInString);
                        else
                            throw new ArgumentValidationException("Date string does not match valid date patterns.");
                    } catch (ParseException e) {
                        throw new ArgumentValidationException("Date string cannot be parsed using valid date patterns.", e);
                    }
                }
            } else {
                throw new ArgumentValidationException("Date string cannot be null");
            }
        }
    }
}