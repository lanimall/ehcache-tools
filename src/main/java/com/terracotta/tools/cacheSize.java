package com.terracotta.tools;

import java.util.Date;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

public class cacheSize {

	public static void main(String args[]){
		int sleep = 1000;
		try{
			if ( args.length > 0 ){
				sleep =  Integer.parseInt(args[0]);
				System.out.println("Print stats with wait interval of "+sleep);
			}

			CacheManager cmgr = new CacheManager();
			boolean loop=true;
			while(loop){

				printStats(cmgr,sleep);

				Thread.sleep(sleep);

			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	public static void printStats(CacheManager cmgr,  int wait) throws InterruptedException{

		//Get All caches in a loop
		String[] name= cmgr.getCacheNames();
		System.out.println("-----------------------------------------------------------------");
		System.out.println("Start Cache Sizes" +new Date() );
		for (int i=0; i< name.length ; i++){
			Cache cache = cmgr.getCache(name[i]);
			System.out.println(cache.getName() + "  "+cache.getKeysNoDuplicateCheck().size() );
		}
		System.out.println("End Cache Sizes" +new Date() );
	}

}
