package com.terracotta.tools;

import com.terracotta.tools.utils.AppConstants;
import com.terracotta.tools.utils.CacheFactory;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class cacheKeyValuePrint {
    private static Logger log = LoggerFactory.getLogger(cacheKeyValuePrint.class);

    private final String cacheName;
    private final String key;

    public cacheKeyValuePrint(String cacheName, String key) {
        this.cacheName = cacheName;
        this.key = key;
    }

    public static void main(String args[]) {
        String cacheName = null;
        String key = null;
        try {
            if (args.length == 0) {
                System.out.println("Wrong arguments.");
                System.out.println("Usage " + cacheKeyValuePrint.class.getSimpleName() + " <cache name> <key>");
                System.exit(1);
            }

            if (args.length > 0) {
                cacheName = args[0];
                if (args.length > 1) {
                    key = args[1];
                }
            }

            new cacheKeyValuePrint(cacheName, key).run();

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
            if (key == null || "".equals(key)) {
                throw new Exception("No cache key specified. Doing nothing.");
            } else {
                Cache cache = CacheFactory.getInstance().getCacheManager().getCache(cacheName);
                if (cache == null) {
                    throw new Exception("Cache " + cacheName + "not found.");
                }

                if (AppConstants.PARAMS_ALL.equalsIgnoreCase(key)) {
                    printAllValues(cache);
                } else {
                    printKeyValue(cache, key);
                }
            }
        }
    }

    private void printAllValues(Cache cache) throws Exception {
        if (null != cache) {
            System.out.println("-----------------------------------------------------------------");
            System.out.println("Start Cache Keys/Values Print" + new Date());

            List cacheKeyList = (AppConstants.useKeyWithExpiryCheck) ? cache.getKeysWithExpiryCheck() : cache.getKeys();
            System.out.println("Listing Keys and Values for cache " + cache.getName() + " Size = " + cacheKeyList.size());
            Iterator iterator = cacheKeyList.iterator();
            while (iterator.hasNext()) {
                printKeyValue(cache, iterator.next());
            }

            System.out.println("End Cache Keys/Values Print" + new Date());
        } else {
            throw new Exception("Cache is null...doing nothing.");
        }
    }

    private void printKeyValue(Cache cache, Object key) throws Exception {
        if (null != cache && key != null) {
            Element e = cache.get(key);
            if (e != null) {
                printValue(e);
            } else {
                System.out.println(String.format("Cache key %s is not in cache.", (null != key) ? key.toString() : "null"));
            }
        } else {
            throw new Exception("Cache or key is null...doing nothing.");
        }
    }

    private void printValue(Element e) {
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
