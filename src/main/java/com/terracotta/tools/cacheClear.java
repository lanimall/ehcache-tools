package com.terracotta.tools;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

public class cacheClear {
    public static void main(String args[]) {
        String name = null;
        try {
            if (args.length > 0) {
                name = args[0];
            }

            if (name == null) {
                System.out.println("No cache name defined. Doing nothing.");
            } else {
                CacheManager cmgr = CacheFactory.getInstance().getCacheManager();
                if ("all".equalsIgnoreCase(name)) {
                    System.out.println("Requested to clear all caches...");
                    String[] cname = cmgr.getCacheNames();
                    for (int i = 0; i < cname.length; i++) {
                        Cache cache = cmgr.getCache(cname[i]);
                        clearCache(cache);
                    }
                } else {
                    Cache cache = cmgr.getCache(name);
                    clearCache(cache);
                }

                System.out.println("Checking cache sizes now...");
                cacheSize.main(new String[]{"5000"});
            }
        } catch (Exception ex) {
            System.out.println(ex);
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
