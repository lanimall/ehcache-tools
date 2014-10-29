package com.terracotta.tools;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
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

    private final AppParams runParams;

    public cacheKeyValuePrint(final String cacheNames, final String cacheKeys) {
        this.runParams = new AppParams();
        runParams.setCacheNames(cacheNames);
        runParams.setCacheKeys(cacheKeys);
    }

    public cacheKeyValuePrint(final AppParams params) {
        this.runParams = params;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Wrong arguments.");
            System.out.println("Usage " + cacheKeyValuePrint.class.getSimpleName() + " <cache name> <key>");
            System.exit(1);
        }

        try {
            AppParams params = CliFactory.parseArgumentsUsingInstance(new AppParams(), args);

            cacheKeyValuePrint launcher = new cacheKeyValuePrint(params);

            launcher.run();

            System.exit(0);
        } catch (Exception e) {
            log.error("", e);
            System.exit(1);
        } finally {
            CacheFactory.getInstance().getCacheManager().shutdown();
        }
    }

    public void run() throws Exception {
        if (runParams.getCacheNames() == null || "".equals(runParams.getCacheNames())) {
            throw new Exception("No cache name defined. Doing nothing.");
        } else {
            if (runParams.getCacheKeys() == null || "".equals(runParams.getCacheKeys())) {
                throw new Exception("No cache key specified. Doing nothing.");
            } else {
                Cache cache = CacheFactory.getInstance().getCacheManager().getCache(runParams.getCacheNames());
                if (cache == null) {
                    throw new Exception("Cache " + runParams.getCacheNames() + "not found.");
                }

                if (AppConstants.PARAMS_ALL.equalsIgnoreCase(runParams.getCacheKeys())) {
                    printAllValues(cache);
                } else {
                    printKeyValue(cache, runParams.getCacheKeys());
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
