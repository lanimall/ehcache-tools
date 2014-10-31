package com.terracotta.tools.utils;

import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Date;

/**
 * Created by FabienSanglier on 9/25/14.
 */
public class AppConstants {
    private static Logger log = LoggerFactory.getLogger(AppConstants.class);
    private static final boolean isDebug = log.isDebugEnabled();

    public static final String CONFIG_USEKEYWITHEXPIRYCHECK = "useKeyWithExpiryCheck";

    public static final boolean useKeyWithExpiryCheck = System.getProperties().containsKey(CONFIG_USEKEYWITHEXPIRYCHECK);
    public static final String PARAMS_ALL = "all";

    public enum CacheElementDateType {
        created {
            @Override
            public long getCacheElementDate(Element e) {
                return e.getCreationTime();
            }
        }, lastUpdated {
            @Override
            public long getCacheElementDate(Element e) {
                return e.getLastUpdateTime();
            }
        }, lastAccessed {
            @Override
            public long getCacheElementDate(Element e) {
                return e.getLastAccessTime();
            }
        };

        public abstract long getCacheElementDate(Element e);

        public static String print() {
            StringWriter sw = new StringWriter();
            for (CacheElementDateType type : CacheElementDateType.values()) {
                if (sw.getBuffer().length() > 0)
                    sw.append(",");
                sw.append(type.name());
            }
            return sw.toString();
        }
    }

    ;

    public enum DateCompareOperation {
        before {
            @Override
            public boolean compare(long time1, long time2) {
                boolean matched = (time1 < time2);

                if (isDebug)
                    log.debug(String.format("Time1(%s) < Time2(%s) --> %s", new Date(time1).toString(), new Date(time2).toString(), matched));

                return matched;
            }
        }, after {
            @Override
            public boolean compare(long time1, long time2) {
                boolean matched = (time1 > time2);

                if (isDebug)
                    log.debug(String.format("Time1(%s) > Time2(%s) --> %s", new Date(time1).toString(), new Date(time2).toString(), matched));

                return matched;
            }
        }, equal {
            @Override
            public boolean compare(long time1, long time2) {
                boolean matched = (time1 == time2);

                if (isDebug)
                    log.debug(String.format("Time1(%s) == Time2(%s) --> %s", new Date(time1).toString(), new Date(time2).toString(), matched));

                return matched;
            }
        };

        public abstract boolean compare(long time1, long time2);

        public static String print() {
            StringWriter sw = new StringWriter();
            for (DateCompareOperation type : DateCompareOperation.values()) {
                if (sw.getBuffer().length() > 0)
                    sw.append(",");
                sw.append(type.name());
            }
            return sw.toString();
        }
    }

    ;

    public enum KeyPrimitiveType {
        StringType("String") {
            @Override
            public Object[] createArray(int length) {
                return new String[length];
            }

            @Override
            public Object castToObjectFromString(String obj) {
                return obj;
            }
        }, LongType("Long") {
            @Override
            public Object[] createArray(int length) {
                return new Long[length];
            }

            @Override
            public Object castToObjectFromString(String obj) {
                return new Long(Long.parseLong(obj));
            }
        }, IntegerType("Integer") {
            @Override
            public Object[] createArray(int length) {
                return new Integer[length];
            }

            @Override
            public Object castToObjectFromString(String obj) {
                return new Integer(Integer.parseInt(obj));
            }
        }, FloatType("Float") {
            @Override
            public Object[] createArray(int length) {
                return new Float[length];
            }

            @Override
            public Object castToObjectFromString(String obj) {
                return new Float(Float.parseFloat(obj));
            }
        }, DoubleType("Double") {
            @Override
            public Object[] createArray(int length) {
                return new Double[length];
            }

            @Override
            public Object castToObjectFromString(String obj) {
                return new Double(Double.parseDouble(obj));
            }
        };

        private final String typeString;

        KeyPrimitiveType(String type) {
            this.typeString = type;
        }

        public String getTypeString() {
            return typeString;
        }

        public abstract Object[] createArray(int length);

        public abstract Object castToObjectFromString(String obj);

        public static KeyPrimitiveType getPrimitiveType(String name) {
            for (KeyPrimitiveType type : KeyPrimitiveType.values()) {
                if (type.typeString.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Value " + name + " not found in the list of KeyPrimitiveType");
        }

        public static String print() {
            StringWriter sw = new StringWriter();
            for (KeyPrimitiveType type : KeyPrimitiveType.values()) {
                if (sw.getBuffer().length() > 0)
                    sw.append(",");
                sw.append(type.typeString);
            }
            return sw.toString();
        }
    }

    ;
}
