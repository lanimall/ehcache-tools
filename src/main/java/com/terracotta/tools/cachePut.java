package com.terracotta.tools;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

public class cachePut {
	public static void main(String args[]){
		CacheManager cacheManager = CacheManager.getInstance();

		Cache myCache = cacheManager.getCache("knowledgeBases");
		for (int i = 0; i < 1000; i++) {
			String key = new Integer(i).toString();

			myCache.put(new Element(key, "Valueweroiuwewejoiwqeurwqwljefwuer0ldnhwqierufweoriwef"));
		}
	}
}