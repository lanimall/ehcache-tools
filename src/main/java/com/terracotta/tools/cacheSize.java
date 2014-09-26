package com.terracotta.tools;

import com.terracotta.tools.utils.AppConstants;
import com.terracotta.tools.utils.CacheFactory;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class cacheSize {
    private static Logger log = LoggerFactory.getLogger(cacheSize.class);

    private static final int ITERATION_LIMIT = 10;

    public static void main(String args[]) {
        int sleep = 1000;
        try {
            if (args.length > 0) {
                sleep = Integer.parseInt(args[0]);
                System.out.println("Print stats with wait interval of " + sleep);
            }

            CacheManager cmgr = CacheFactory.getInstance().getCacheManager();

            int it = 0;
            while (++it < ITERATION_LIMIT) {
                printStats(cmgr, sleep);
                Thread.sleep(sleep);
            }

            CacheFactory.getInstance().getCacheManager().shutdown();
            System.exit(0);
        } catch (Exception ex) {
            log.error("", ex);
            System.exit(1);
        }
    }

    public static void printStats(CacheManager cmgr, int wait) throws Exception {
        //Get All caches in a loop
        String[] name = cmgr.getCacheNames();
        System.out.println("-----------------------------------------------------------------");
        System.out.println("Start Cache Sizes" + new Date());

        for (int i = 0; i < name.length; i++) {
            Cache cache = cmgr.getCache(name[i]);
            if (AppConstants.useKeyWithExpiryCheck) {
                System.out.println(String.format("%s %d", cache.getName(), cache.getKeysWithExpiryCheck().size()));
            } else {
                System.out.println(String.format("%s %d", cache.getName(), cache.getSize()));
            }
        }

        System.out.println("End Cache Sizes" + new Date());
    }
}
