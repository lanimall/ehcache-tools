package com.terracotta.tools;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

public class tcPing {

	private static void usage() {
		System.out
				.println("Usage tcPing  [1/2] [unique id] [number of elements]");
		System.out
				.println("tcPing needs to be executed in a 2 step process. The first argument 1 is to load and 2 is retrieve and avalidate");

	}

	public static void main(String args[]) {
		int step = 1;
		String uid = "";
		int count = 1000;
		try {
			// Parse Input Arguments
			if (args.length < 3) {
				usage();
				System.exit(1);
			} else {
				step = Integer.parseInt(args[0]);
				uid = args[1];
				count = Integer.parseInt(args[2]);
			}

			CacheManager cacheManager = CacheManager.getInstance();
			if (cacheManager == null) {
				System.out
						.println("Unable to create cache manager. Check if ehcache.xml is in path.");
				System.exit(1);
			}

			Cache pingCache = cacheManager.getCache("terracottaPing");
			if (pingCache == null) {
				System.out
						.println("Unable to create cache 'terracottaPing'. Check if ehcache.xml has this cache defined.");
				System.exit(1);
			}

			if (step == 1) {
				System.out.println("Starting Step 1 of Ping..");
				step1(pingCache, uid, count);
				Thread.sleep(1000);
			} else {
				System.out.println("Starting Step 2 of Ping..");
				step2(pingCache, uid, count);
				System.out.println("Terracotta Ping Succesful");
			}
			
			cacheManager.shutdown();

		} catch (Exception ex) {
			System.out.println(ex);
			System.exit(1);
		}
	}
	
	

	private static void step1(Cache pingCache, String uid, int count)
			throws Exception {
		//Clear the cache to remove stale data
		pingCache.removeAll();
		if (pingCache.getSize() > 0){
			throw new Exception("Unable to clear cache before Starting ping.");
		}
		SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
		Date date = new Date();
		for (int i = 0; i < count; i++) {
			String key = uid + "_" + new Integer(i).toString();
			pingCache.put(new Element(key, sdf.format(date)));
		}
		System.out.println("Loaded cache "+pingCache.getName()+". Size =  "+pingCache.getSize());
	}

	private static void step2(Cache pingCache, String uid, int count)
			throws Exception {
		
		System.out.println("Found cache "+pingCache.getName()+" Size "+pingCache.getSize());

		for (int i = 0; i < count; i++) {
			String key = uid + "_" + new Integer(i).toString();
			Element val = pingCache.get(key);
			if (val == null){
				throw new Exception("Cache elements not found. Failed to validate key="+key);
			}
		}
		pingCache.removeAll();
		System.out.println("Cleared  "+pingCache.getName()+" Size "+pingCache.getSize());
		
		if (pingCache.getSize() > 0){
			throw new Exception("Unable to clear cache.");
		}
		
	}
}
