package com.terracotta.tools;

import com.terracotta.tools.utils.AppConstants;
import com.terracotta.tools.utils.CacheFactory;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class cacheKeyValuePrint {
    private static Logger log = LoggerFactory.getLogger(cacheKeyValuePrint.class);

    public static void main(String args[]) {

        String cacheName = null;
        String key = null;
        try {

            if (args.length == 0) {
                System.out.println("Usage <cache name> [key]");
                System.exit(1);
            }
            if (args.length == 1) {
                cacheName = args[0];

            } else if (args.length == 2) {
                cacheName = args[0];
                key = args[1];
            } else {

            }

            CacheManager cacheManager = CacheFactory.getInstance().getCacheManager();
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                System.out.println("Cache " + cacheName + "not found.");
                System.exit(1);
            }

            if (key == null) {
                printAllValues(cache);
            } else {
                printKeyValue(cache, key);
            }

            CacheFactory.getInstance().getCacheManager().shutdown();
            System.exit(0);
        } catch (Exception ex) {
            log.error("", ex);
            System.exit(1);
        }
    }

    private static void printAllValues(Cache cache) {
        System.out.println("-----------------------------------------------------------------");
        System.out.println("Start Cache Keys/Values Print" + new Date());

        List cacheKeyList = (AppConstants.useKeyWithExpiryCheck) ? cache.getKeysWithExpiryCheck() : cache.getKeys();
        System.out.println("Listing Keys and Values for cache " + cache.getName() + " Size = " + cacheKeyList.size());
        Iterator iterator = cacheKeyList.iterator();
        while (iterator.hasNext()) {
            printKeyValue(cache, iterator.next());
        }

        System.out.println("End Cache Keys/Values Print" + new Date());
    }

    private static void printKeyValue(Cache cache, Object key) {
        Element e = cache.get(key);
        if (e != null) {
            printValue(e);
        } else {
            System.out.println(String.format("Key %s not found. May be expired.", key.toString()));
        }
    }

    private static void printValue(Element e) {
        if (e != null) {
            if (e.getObjectValue() == null) {
                System.out.println(String.format("Key=%s / Value=%s", e.getObjectKey().toString(), "null"));
            } else {
                if (e.getObjectValue() instanceof List) {
                    System.out.println(String.format("Key=%s", e.getObjectKey().toString()));
                    List cacheValueList = (List) e.getObjectValue();
                    Iterator iterator = cacheValueList.iterator();
                    while (iterator.hasNext()) {
                        System.out.println(iterator.next().toString());
                    }
                } else {
                    System.out.println(String.format("Key=%s / Value=%s", e.getObjectKey().toString(), e.getObjectValue().toString()));
                }
            }
        }
    }
}
