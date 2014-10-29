package com.terracotta.tools;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import com.terracotta.tools.utils.AppConstants;
import com.terracotta.tools.utils.CacheFactory;
import net.sf.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class cacheSize {
    private static Logger log = LoggerFactory.getLogger(cacheSize.class);

    private static final int SLEEP_DEFAULT = 1000;
    private static final int ITERATION_LIMIT_DEFAULT = 2;

    private final AppParams runParams;

    public cacheSize(final AppParams params) {
        this.runParams = params;
    }

    public cacheSize(String cacheName, int sleep, int iterationLimit) {
        this.runParams = new AppParams(cacheName, sleep, iterationLimit);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Wrong arguments.");
            System.out.println("Usage " + cacheSize.class.getSimpleName() + " <cache name|all> <sleep interval in ms> <iteration max count>");
            System.exit(1);
        }

        try {
            AppParams params = CliFactory.parseArgumentsUsingInstance(new AppParams(), args);

            cacheSize launcher = new cacheSize(params);

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
            System.out.println("-----------------------------------------------------------------");
            System.out.println("Start Cache Sizes at " + new Date() + "\n");

            String[] cname;
            if (AppConstants.PARAMS_ALL.equalsIgnoreCase(runParams.getCacheNames())) {
                System.out.println("Requested to get size for all caches...");
                cname = CacheFactory.getInstance().getCacheManager().getCacheNames();
            } else {
                cname = new String[]{runParams.getCacheNames()};
            }

            //perform operation
            int it = 0;
            while (it < runParams.getIterationLimit()) {
                System.out.println(String.format("---------------- Iteration %d ----------------", it + 1));
                for (int i = 0; i < cname.length; i++) {
                    Cache cache = CacheFactory.getInstance().getCacheManager().getCache(cname[i]);
                    if (null != cache) {
                        printSize(cache);
                    } else {
                        System.out.println(String.format("Cache %s not found.", cname[i]));
                    }
                }
                Thread.sleep(runParams.getSleep());
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

    public static class AppParams {
        private String cacheNames;
        int sleep;
        int iterationLimit;

        public AppParams() {
        }

        public AppParams(String cacheNames, int sleep, int iterationLimit) {
            this.cacheNames = cacheNames;
            this.sleep = sleep;
            this.iterationLimit = iterationLimit;
        }

        public String getCacheNames() {
            return cacheNames;
        }

        @Option(defaultValue = "", longName = "caches")
        public void setCacheNames(String cacheNames) {
            this.cacheNames = cacheNames;
        }

        public int getSleep() {
            return sleep;
        }

        @Option(defaultValue = "" + SLEEP_DEFAULT, longName = "sleep")
        public void setSleep(int sleep) {
            this.sleep = sleep;
        }

        public int getIterationLimit() {
            return iterationLimit;
        }

        @Option(defaultValue = "" + ITERATION_LIMIT_DEFAULT, longName = "iterations")
        public void setIterationLimit(int iterationLimit) {
            this.iterationLimit = iterationLimit;
        }
    }
}