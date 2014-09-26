package com.terracotta.tools;

import com.terracotta.tools.utils.AppConstants;
import com.terracotta.tools.utils.CacheFactory;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class cacheKeysPrint {
    private static Logger log = LoggerFactory.getLogger(cacheKeysPrint.class);

    public static void main(String args[]) {
        int sleep = 1000;
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
                    System.out.println("Requested to print keys for all caches...");
                    String[] cname = cacheManager.getCacheNames();
                    for (int i = 0; i < cname.length; i++) {
                        Cache cache = cacheManager.getCache(cname[i]);
                        printKeys(cache);
                    }
                } else {
                    Cache cache = cacheManager.getCache(name);
                    printKeys(cache);
                }
            }


            CacheFactory.getInstance().getCacheManager().shutdown();
            System.exit(0);
        } catch (Exception ex) {
            log.error("", ex);
            System.exit(1);
        }
    }

    public static void printKeys(Cache cache) throws Exception {
        System.out.println("-----------------------------------------------------------------");
        System.out.println("Start Cache Keys Print" + new Date());

        List cacheKeyList = (AppConstants.useKeyWithExpiryCheck) ? cache.getKeysWithExpiryCheck() : cache.getKeys();
        System.out.println("Listing Keys for cache " + cache.getName() + " Size = " + cacheKeyList.size());
        Iterator iterator = cacheKeyList.iterator();
        while (iterator.hasNext()) {
            Object key = iterator.next();
            if (!AppConstants.useKeyWithExpiryCheck) {
                if (null != cache.get(key))
                    System.out.println(key.toString());
            } else {
                System.out.println(key.toString());
            }
        }

        System.out.println("End Cache Keys Print" + new Date());
    }
}
