package com.terracotta.tools;

import com.terracotta.tools.utils.CacheFactory;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class cacheClear {
    private static Logger log = LoggerFactory.getLogger(cacheClear.class);

    public static void main(String args[]) {
        String name = null;
        try {
            if (args.length > 0) {
                name = args[0];
            }

            CacheManager cacheManager = CacheFactory.getInstance().getCacheManager();
            if (name == null) {
                System.out.println("No cache name defined. Doing nothing.");
            } else {
                if ("all".equalsIgnoreCase(name)) {
                    System.out.println("Requested to clear all caches...");
                    String[] cname = cacheManager.getCacheNames();
                    for (int i = 0; i < cname.length; i++) {
                        Cache cache = cacheManager.getCache(cname[i]);
                        clearCache(cache);
                    }
                } else {
                    Cache cache = cacheManager.getCache(name);
                    clearCache(cache);
                }

                System.out.println("Checking cache sizes now...");
                cacheSize.main(new String[]{"5000"});
            }

            CacheFactory.getInstance().getCacheManager().shutdown();
            System.exit(0);
        } catch (Exception ex) {
            log.error("", ex);
            System.exit(1);
        }
    }

    public static void clearCache(Cache cache) throws Exception {
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
    }
}
