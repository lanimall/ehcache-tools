package com.terracotta.tools;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import com.terracotta.tools.utils.AppConstants;
import com.terracotta.tools.utils.CacheFactory;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class cacheKeysPrint {
    private static Logger log = LoggerFactory.getLogger(cacheKeysPrint.class);

    SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyyMMdd HHmmss");
    private final AppParams runParams;

    public cacheKeysPrint(final String cacheNames, final String cacheKeys) {
        this.runParams = new AppParams();
        runParams.setCacheNames(cacheNames);
        runParams.setCacheKeys(cacheKeys);
    }

    public cacheKeysPrint(final AppParams params) {
        this.runParams = params;
    }

    public static void main(String[] args) {
        try {
            AppParams params = CliFactory.parseArgumentsUsingInstance(new AppParams(), args);

            cacheKeysPrint launcher = new cacheKeysPrint(params);

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

                System.out.println("-----------------------------------------------------------------");
                System.out.println(String.format("Start Cache Keys Print - %s", dateTimeFormatter.format(new Date())));

                int keyInCacheCount = 0;
                if (AppConstants.PARAMS_ALL.equalsIgnoreCase(runParams.getCacheKeys())) {
                    keyInCacheCount = printAllKeys(cache);
                } else {
                    keyInCacheCount = printKey(cache, runParams.getCacheKeys());
                }

                System.out.println(String.format("Keys found in cache = %d", keyInCacheCount));
                System.out.println(String.format("End Cache Keys Print - %s", dateTimeFormatter.format(new Date())));
            }
        }
    }

    private int printAllKeys(Cache cache) throws Exception {
        int keyInCacheCount = 0;
        if (null != cache) {
            List cacheKeyList = (AppConstants.useKeyWithExpiryCheck) ? cache.getKeysWithExpiryCheck() : cache.getKeys();
            System.out.println("Listing Keys for cache " + cache.getName() + " Size = " + cacheKeyList.size());
            Iterator iterator = cacheKeyList.iterator();
            while (iterator.hasNext()) {
                keyInCacheCount += printKey(cache, iterator.next());
            }
        } else {
            throw new Exception("Cache is null...doing nothing.");
        }
        return keyInCacheCount;
    }

    private int printKey(Cache cache, Object key) throws Exception {
        int keyInCacheCount = 0;
        if (null != cache && null != key) {
            Element e = cache.getQuiet(key);
            if (e != null) {
                System.out.println(String.format("key=%s,created=%s,updated=%s,last accessed=%s,expire=%s",
                                e.getObjectKey().toString(),
                                dateTimeFormatter.format(new Date(e.getCreationTime())),
                                dateTimeFormatter.format(new Date(e.getLastUpdateTime())),
                                dateTimeFormatter.format(new Date(e.getLastAccessTime())),
                                dateTimeFormatter.format(new Date(e.getExpirationTime()))
                        )
                );
                keyInCacheCount++;
            } else {
                System.out.println(String.format("key %s is not in cache.", (null != key) ? key.toString() : "null"));
            }
        } else {
            throw new Exception("Cache or key is null...doing nothing.");
        }
        return keyInCacheCount;
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