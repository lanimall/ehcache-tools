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

public class cacheKeysPrint {
    private static Logger log = LoggerFactory.getLogger(cacheKeysPrint.class);

    private final String cacheName;
    private final String key;

    public cacheKeysPrint(String cacheName, String key) {
        this.cacheName = cacheName;
        this.key = key;
    }

    public static void main(String args[]) {
        String cacheName = null;
        String key = null;
        try {
            if (args.length == 0) {
                System.out.println("Wrong arguments.");
                System.out.println("Usage " + cacheKeysPrint.class.getSimpleName() + " <cache name> <key>");
                System.exit(1);
            }

            if (args.length > 0) {
                cacheName = args[0];
                if (args.length > 1) {
                    key = args[1];
                }
            }

            new cacheKeysPrint(cacheName, key).run();

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
                    printAllKeys(cache);
                } else {
                    printKey(cache, key);
                }
            }
        }
    }

    private void printAllKeys(Cache cache) throws Exception {
        if (null != cache) {
            System.out.println("-----------------------------------------------------------------");
            System.out.println("Start Cache Keys Print" + new Date());

            List cacheKeyList = (AppConstants.useKeyWithExpiryCheck) ? cache.getKeysWithExpiryCheck() : cache.getKeys();
            System.out.println("Listing Keys for cache " + cache.getName() + " Size = " + cacheKeyList.size());
            Iterator iterator = cacheKeyList.iterator();
            while (iterator.hasNext()) {
                printKey(cache, iterator.next());
            }

            System.out.println("End Cache Keys Print" + new Date());
        } else {
            throw new Exception("Cache is null...doing nothing.");
        }
    }

    private void printKey(Cache cache, Object key) throws Exception {
        if (null != cache && null != key) {
            Element e = cache.getQuiet(key);
            if (e != null) {
                System.out.println(String.format("Cache key %s is in cache.", e.getObjectKey().toString()));
            } else {
                System.out.println(String.format("Cache key %s is not in cache.", (null != key) ? key.toString() : "null"));
            }
        } else {
            throw new Exception("Cache or key is null...doing nothing.");
        }
    }
}