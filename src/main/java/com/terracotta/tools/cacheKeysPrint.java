package com.terracotta.tools;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import java.util.Iterator;
import java.util.List;

public class cacheKeysPrint {

	public static void main(String args[]){
		int sleep = 1000;
		String name=null;
		try{
			if ( args.length > 0 ){
				name=args[0];
			}

			if (name == null){
				System.out.println("No cache name defined. Doing nothing.");
			} else {
                CacheManager cmgr = CacheFactory.getInstance().getCacheManager();
                if ("all".equalsIgnoreCase(name)) {
                    System.out.println("Requested to clear all caches...");
					String[] cname= cmgr.getCacheNames();
					for (int i=0; i< cname.length ; i++){
						Cache cache = cmgr.getCache(cname[i]);
						printKeys(cache);
					}
				} else {
					Cache cache = cmgr.getCache(name);
					printKeys(cache);
				}
			}
		}catch(Exception ex){
			System.out.println(ex);
		}
	}

	public  static void printKeys(Cache cache) throws Exception{
		List<String> cacheKeyList = cache.getKeysWithExpiryCheck();
		System.out.println("Listing Keys for cache "+cache.getName() +" Size = "+cacheKeyList.size() );
		Iterator<String> iterator = cacheKeyList.iterator();
		while (iterator.hasNext()) {
			System.out.println(iterator.next());
		}
		System.out.println("------------------------------------------------");
	}
}
