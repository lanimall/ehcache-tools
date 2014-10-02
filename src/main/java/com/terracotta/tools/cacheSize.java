package com.terracotta.tools;

import com.terracotta.tools.utils.AppConstants;
import com.terracotta.tools.utils.CacheFactory;
import net.sf.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class cacheSize {
    private static Logger log = LoggerFactory.getLogger(cacheSize.class);

    private static final int SLEEP_DEFAULT = 1000;
    private static final int ITERATION_LIMIT_DEFAULT = 10;

    private final int sleep;
    private final String cacheName;
    private final int iterationLimit;

    public cacheSize(String cacheName, int sleep, int iterationLimit) {
        this.sleep = sleep;
        this.cacheName = cacheName;
        this.iterationLimit = iterationLimit;
    }

    public static void main(String args[]) {
        int sleep = SLEEP_DEFAULT;
        String cacheName = null;
        int iterationLimit = ITERATION_LIMIT_DEFAULT;

        try {
            if (args.length == 0) {
                System.out.println("Wrong arguments.");
                System.out.println("Usage " + cacheSize.class.getSimpleName() + " <cache name|all> <sleep interval in ms> <iteration max count>");
                System.exit(1);
            }

            if (args.length > 0) {
                cacheName = args[0];
                if (args.length > 1) {
                    try {
                        sleep = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        log.warn("Sleep value not formatted right. Default to " + SLEEP_DEFAULT);
                        sleep = SLEEP_DEFAULT;
                    }

                    if (args.length > 2) {
                        try {
                            iterationLimit = Integer.parseInt(args[2]);
                        } catch (NumberFormatException e) {
                            log.warn("Iteration limit not formatted right. Default to " + ITERATION_LIMIT_DEFAULT);
                            iterationLimit = ITERATION_LIMIT_DEFAULT;
                        }
                    }
                }
            }

            new cacheSize(cacheName, sleep, iterationLimit).run();

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
            System.out.println("No cache name defined. Doing nothing.");
        } else {
            System.out.println("-----------------------------------------------------------------");
            System.out.println("Start Cache Sizes at " + new Date() + "\n");

            String[] cname;
            if (AppConstants.PARAMS_ALL.equalsIgnoreCase(cacheName)) {
                System.out.println("Requested to get size for all caches...");
                cname = CacheFactory.getInstance().getCacheManager().getCacheNames();
            } else {
                cname = new String[]{cacheName};
            }

            //perform operation
            int it = 0;
            while (it < iterationLimit) {
                System.out.println(String.format("---------------- Iteration %d ----------------", it + 1));
                for (int i = 0; i < cname.length; i++) {
                    Cache cache = CacheFactory.getInstance().getCacheManager().getCache(cname[i]);
                    if (null != cache) {
                        printSize(cache);
                    } else {
                        System.out.println(String.format("Cache %s not found.", cname[i]));
                    }
                }
                Thread.sleep(sleep);
                it++;
            }

            System.out.println("End Cache Sizes " + new Date());
        }
    }

    private void printSize(Cache cache) throws Exception {
        if (null != cache) {
            if (AppConstants.useKeyWithExpiryCheck) {
                System.out.println(String.format("%s %d", cache.getName(), cache.getKeysWithExpiryCheck().size()));
            } else {
                System.out.println(String.format("%s %d", cache.getName(), cache.getSize()));
            }
        } else {
            throw new Exception("Cache is null...doing nothing.");
        }
    }
}