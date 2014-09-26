package com.terracotta.tools;

import com.terracotta.tools.utils.CacheFactory;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class cachePut {
    private static Logger log = LoggerFactory.getLogger(cachePut.class);

    public static void main(String args[]) {
        CacheManager cacheManager = CacheFactory.getInstance().getCacheManager();

        Cache myCache = cacheManager.getCache("knowledgeBases");
        for (int i = 0; i < 1000; i++) {
            String key = new Integer(i).toString();

            myCache.put(new Element(key, "Valueweroiuwewejoiwqeurwqwljefwuer0ldnhwqierufweoriwef"));
        }

        CacheFactory.getInstance().getCacheManager().shutdown();
        System.exit(0);
    }
}