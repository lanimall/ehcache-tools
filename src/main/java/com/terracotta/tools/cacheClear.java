package com.terracotta.tools;

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

    private final String cacheName;
    private final String commaSeparatedKeys;

    public cacheClear(String cacheName, String commaSeparatedKeys) {
        this.cacheName = cacheName;
        this.commaSeparatedKeys = commaSeparatedKeys;
    }

    public static void main(String args[]) {
        String cacheName = null;
        String commaSeparatedKeys = null;

        try {
            if (args.length == 0) {
                System.out.println("Wrong arguments.");
                System.out.println("Usage " + cacheClear.class.getSimpleName() + " <cache name|all> <key1,key2,...,keyN|all>");
                System.exit(1);
            }

            if (args.length > 0) {
                cacheName = args[0];
                if (args.length > 1) {
                    commaSeparatedKeys = args[1];
                }
            }

            new cacheClear(cacheName, commaSeparatedKeys).run();

            System.exit(0);
        } catch (Exception ex) {
            log.error("", ex);
            System.exit(1);
        } finally {
            CacheFactory.getInstance().getCacheManager().shutdown();
        }
    }

    public void run() throws Exception {
        if (cacheName == null || "".equals(cacheName)) {
            throw new Exception("No cache name defined. Doing nothing.");
        } else {
            if (commaSeparatedKeys == null || "".equals(commaSeparatedKeys)) {
                throw new Exception("No cache key(s) specified. Doing nothing.");
            } else {
                String[] cname;
                if (AppConstants.PARAMS_ALL.equalsIgnoreCase(cacheName)) {
                    System.out.println("Requested to clear all caches...");
                    cname = CacheFactory.getInstance().getCacheManager().getCacheNames();
                } else {
                    cname = new String[]{cacheName};
                }

                //perform operation
                for (int i = 0; i < cname.length; i++) {
                    Cache cache = CacheFactory.getInstance().getCacheManager().getCache(cname[i]);

                    if (AppConstants.PARAMS_ALL.equalsIgnoreCase(commaSeparatedKeys)) {
                        clearCache(cache);

                        System.out.println("Checking cache sizes now...");
                        new cacheSize(cacheName, 1000, CHECK_ITERATION_LIMIT_DEFAULT).run();
                    } else {
                        String[] keys = null;
                        if (null != commaSeparatedKeys) {
                            keys = commaSeparatedKeys.split(",");
                        }

                        clearCache(cache, keys);

                        System.out.println("Now, checking if requested keys are in cache...");
                        int it = 0;
                        while (it < CHECK_ITERATION_LIMIT_DEFAULT) {
                            System.out.println(String.format("---------------- Iteration %d ----------------", it + 1));
                            for (String key : keys) {
                                new cacheKeysPrint(cacheName, key).run();
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
}
