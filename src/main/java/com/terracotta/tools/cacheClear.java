package com.terracotta.tools;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import com.terracotta.tools.utils.AppConstants;
import com.terracotta.tools.utils.CacheFactory;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class cacheClear {
    private static Logger log = LoggerFactory.getLogger(cacheClear.class);

    private static final int CHECK_ITERATION_LIMIT_DEFAULT = 5;

    private final AppParams runParams;

    public cacheClear(final AppParams params) {
        this.runParams = params;
    }

    public void run() throws Exception {
        if (runParams.getCacheNames() == null || "".equals(runParams.getCacheNames())) {
            throw new Exception("No cache name defined. Doing nothing.");
        } else {
            if (runParams.getCacheKeys() == null || "".equals(runParams.getCacheKeys())) {
                throw new Exception("No cache key(s) specified. Doing nothing.");
            } else {
                String[] cname;
                if (AppConstants.PARAMS_ALL.equalsIgnoreCase(runParams.getCacheNames())) {
                    System.out.println("Requested to clear all caches...");
                    cname = CacheFactory.getInstance().getCacheManager().getCacheNames();
                } else {
                    cname = new String[]{runParams.getCacheNames()};
                }

                //perform operation
                for (int i = 0; i < cname.length; i++) {
                    Cache cache = CacheFactory.getInstance().getCacheManager().getCache(cname[i]);

                    if (AppConstants.PARAMS_ALL.equalsIgnoreCase(runParams.getCacheKeys())) {
                        clearCache(cache);

                        System.out.println("Checking cache sizes now...");
                        new cacheSize(runParams.getCacheNames(), 1000, CHECK_ITERATION_LIMIT_DEFAULT).run();
                    } else {
                        String[] keys = null;
                        if (null != runParams.getCacheKeys()) {
                            keys = runParams.getCacheKeys().split(",");
                        }

                        clearCache(cache, keys);

                        System.out.println("Now, checking if requested keys are in cache...");
                        int it = 0;
                        while (it < CHECK_ITERATION_LIMIT_DEFAULT) {
                            System.out.println(String.format("---------------- Iteration %d ----------------", it + 1));
                            for (String key : keys) {
                                new cacheKeysPrint(runParams.getCacheNames(), key).run();
                            }
                            it++;
                        }
                    }
                }
            }
        }
    }

    private void clearCache(Cache cache, Object[] keys) throws Exception {
        if (null != cache && null != keys && keys.length > 0) {
            int beforeRemoveSize = cache.getSize();
            System.out.println(String.format("Cache %s -- Current Size = %d", cache.getName(), beforeRemoveSize));
            System.out.println(String.format("Cache %s -- About to clear keys %s", cache.getName(), Arrays.deepToString(keys)));

            List keyList = Arrays.asList(keys);

            //check first if requested keys are in cache currently
            List requestedKeysInCache = getKeysInCache(cache, keyList);
            if (null == requestedKeysInCache || requestedKeysInCache.size() == 0) {
                System.out.println(String.format("Cache %s -- Requested keys are not in cache -- doing nothing. ", cache.getName(), Arrays.deepToString(keys)));
                return;
            }

            //remove stage
            cache.removeAll(keyList);

            //verification stage
            int loop = 0;
            int allRemovedCount = 0;
            while (allRemovedCount < keys.length && loop++ <= 20) {
                allRemovedCount = 0;
                Map<Object, Element> inCache = cache.getAll(keyList);
                for (Object inCacheKey : inCache.keySet()) {
                    if (null == inCache.get(inCacheKey)) {
                        allRemovedCount++;
                    }
                }

                Thread.sleep(1000);
            }

            //if after 20+ seconds we still get the key, something must be wrong...
            if (allRemovedCount < keys.length && loop >= 20) {
                List unclearedKeys = getKeysInCache(cache, keyList);
                if (unclearedKeys.size() > 0) {
                    throw new Exception(String.format("Cache %s - Unable to clear keys %s", cache.getName(), Arrays.deepToString(unclearedKeys.toArray())));
                }
            }

            System.out.println(String.format("Cache %s: Clearing success -- Following keys removed: %s", cache.getName(), Arrays.deepToString(keys)));
            System.out.println("------------------------------------------------");
        } else {
            throw new Exception("Cache or key is null...doing nothing.");
        }
    }

    private List getKeysInCache(Cache cache, List keyList) {
        List unclearedKeys = new ArrayList();
        Map<Object, Element> inCache = cache.getAll(keyList);
        for (Object inCacheKey : inCache.keySet()) {
            if (null != inCache.get(inCacheKey)) {
                unclearedKeys.add(inCacheKey);
            }
        }
        return unclearedKeys;
    }

    private void clearCache(Cache cache) throws Exception {
        if (null != cache) {
            int beforeRemoveSize = cache.getSize();
            System.out.println("Clearing cache " + cache.getName() + " - Current size:" + beforeRemoveSize);

            if (beforeRemoveSize == 0) {
                System.out.println(cache.getName() + " is already empty...");
                return;
            }

            cache.removeAll();

            int afterRemoveSize;
            int loop = 0;
            while (beforeRemoveSize == (afterRemoveSize = cache.getSize()) && loop++ <= 20) {
                Thread.sleep(1000);
            }

            //if after 20+ seconds, the cache size hasn't changed, something must be wrong...
            if (beforeRemoveSize == afterRemoveSize) {
                throw new Exception("Unable to clear cache.");
            }

            if (afterRemoveSize > 0) {
                System.out.println("Cleared some entries in " + cache.getName() + "... but cache is not empty. Probably new entries were added at the same time.");
            }

            System.out.println(cache.getName() + ": Final Size " + cache.getSize());
            System.out.println("------------------------------------------------");
        } else {
            throw new Exception("Cache is null...doing nothing.");
        }
    }

    public static void main(String[] args) {
        try {
            AppParams params = CliFactory.parseArgumentsUsingInstance(new AppParams(), args);

            cacheClear launcher = new cacheClear(params);

            launcher.run();

            System.exit(0);
        } catch (Exception e) {
            log.error("", e);
            System.exit(1);
        } finally {
            CacheFactory.getInstance().getCacheManager().shutdown();
        }
    }

    public static class AppParams {
        private String cacheNames;
        private String cacheKeys;

        public AppParams() {
        }

        public String getCacheNames() {
            return cacheNames;
        }

        @Option(defaultValue = "", longName = "caches")
        public void setCacheNames(String cacheNames) {
            this.cacheNames = cacheNames;
        }

        public String getCacheKeys() {
            return cacheKeys;
        }

        @Option(defaultValue = "", longName = "keys")
        public void setCacheKeys(String cacheKeys) {
            this.cacheKeys = cacheKeys;
        }
    }
}
