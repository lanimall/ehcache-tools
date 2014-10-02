package com.terracotta.tools.utils;

/**
 * Created by FabienSanglier on 9/25/14.
 */
public class AppConstants {
    public static final String CONFIG_USEKEYWITHEXPIRYCHECK = "useKeyWithExpiryCheck";

    public static final boolean useKeyWithExpiryCheck = System.getProperties().containsKey(CONFIG_USEKEYWITHEXPIRYCHECK);
    public static final String PARAMS_ALL = "all";
}
