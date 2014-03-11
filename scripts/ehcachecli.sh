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
    echo "tcPing - Health Check of the cluster"
    exit
    ;;
esac

BASE_DIR=`dirname "$0"`/..
echo $BASE_DIR


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
PERF_CLASSPATH=$PERF_CLASSPATH:${BASE_DIR}/config/
JAVA_OPTS="${JAVA_OPTS} -Xms128m -Xmx512m -XX:MaxDirectMemorySize=10G -Dcom.tc.productkey.path=${BASE_DIR}/config/terracotta-license.key"

${JAVA_HOME}/bin/java ${JAVA_OPTS} -cp ${PERF_CLASSPATH} com.terracotta.tools.$@ 
