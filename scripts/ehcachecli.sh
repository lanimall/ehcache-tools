#!/bin/sh

#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
#
case "$1" in
  "--help"|"-h"|"-?")
    echo "Syntax: $0 [cacheKeyValuePrint|cacheKeysPrint|cacheSize] [arguments.....]"
    echo "cacheKeyValuePrint - Prints the Keys and values (only string or list/string) for a given cache."  
    echo "cacheKeysPrint - Prints the Keys in a cache or all caches."
    echo "cacheSize - Prints the total number of cache entries in each cache in a continous loop."
    echo "cacheClear - Clear all cache entries in specified cache or all caches."
    echo "tcPing - Health Check of the cluster"
    exit
    ;;
esac

BASE_DIR=`dirname "$0"`/..
echo $BASE_DIR

#path to ehcache config
EHCACHE_CONFIG_PATH=${BASE_DIR}/config/ehcache.xml

#add path to license key here
TC_CONNECT_URL="localhost:9510"

#add path to license key here
TC_LICENSEKEY_PATH=${HOME}/terracotta-license.key

if [ ! -f ${TC_LICENSEKEY_PATH} ]; then
  TC_LICENSEKEY_PATH=${BASE_DIR}/config/terracotta-license.key
fi

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

if test \! -d "${JAVA_HOME}"; then
  echo "$0: the JAVA_HOME environment variable is not defined correctly"
  exit 2
fi

# For Cygwin, convert paths to Windows before invoking java
if $cygwin; then
  [ -n "$BASE_DIR" ] && BASE_DIR=`cygpath -d "$BASE_DIR"`
fi

PERF_CLASSPATH=$(echo ${BASE_DIR}/lib/*.jar | tr ' ' ':')
echo $PERF_CLASSPATH

JAVA_OPTS="${JAVA_OPTS} -Xms128m -Xmx256m -XX:MaxDirectMemorySize=10G -Dcom.tc.productkey.path=${TC_LICENSEKEY_PATH} -Dtc.connect.servers=${TC_CONNECT_URL} -Dehcache.config.path=${EHCACHE_CONFIG_PATH}"

${JAVA_HOME}/bin/java ${JAVA_OPTS} -cp ${PERF_CLASSPATH} com.terracotta.tools.$@