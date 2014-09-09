#!/bin/sh

#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
#

case "$1" in
  ""|"--help"|"-h"|"-?")
    echo "Syntax: $0 [terracotta_connect_url]"
    echo "terracotta_connect_url : comma-separated entry of net_address:port for all the terracotta nodes to test against"
    exit
    ;;
esac

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

PRGDIR=`dirname "$PRG"`
BASEDIR=`cd "$PRGDIR/.." > /dev/null; pwd`

#add terracotta url here
TC_CONNECT_URL=$1

if [ "x$TC_CONNECT_URL" == "x" ]; then
  echo "Error: terracotta_connect_url is not defined correctly." 1>&2
  echo "  We cannot execute $JAVACMD" 1>&2
  exit 1
fi

#path to ehcache config
EHCACHE_CONFIG_PATH=${BASEDIR}/config/ehcache-ping.xml

#add path to license key here
TC_LICENSEKEY_PATH=${HOME}/terracotta-license.key

if [ ! -f ${TC_LICENSEKEY_PATH} ]; then
  TC_LICENSEKEY_PATH=${BASEDIR}/config/terracotta-license.key
fi

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true
           if [ -z "$JAVA_VERSION" ] ; then
             JAVA_VERSION="CurrentJDK"
           else
             echo "Using Java version: $JAVA_VERSION"
           fi
           if [ -z "$JAVA_HOME" ] ; then
             JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/${JAVA_VERSION}/Home
           fi
           ;;
esac

if [ -z "$JAVA_HOME" ] ; then
  if [ -r /etc/gentoo-release ] ; then
    JAVA_HOME=`java-config --jre-home`
  fi
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$CLASSPATH" ] && CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

# If a specific java binary isn't specified search for the standard 'java' binary
if [ -z "$JAVACMD" ] ; then
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD=`which java`
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly." 1>&2
  echo "  We cannot execute $JAVACMD" 1>&2
  exit 1
fi

if [ -z "$LIBS" ]
then
  LIBS="$BASEDIR"/libs
fi

if [ -z "$CONFIG" ]
then
  CONFIG="$BASEDIR"/config
fi

CLASSPATH=$CLASSPATH_PREFIX:"$CONFIG":"$LIBS"/*

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  [ -n "$CLASSPATH" ] && CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
  [ -n "$HOME" ] && HOME=`cygpath --path --windows "$HOME"`
  [ -n "$BASEDIR" ] && BASEDIR=`cygpath --path --windows "$BASEDIR"`
  [ -n "$LIBS" ] && LIBS=`cygpath --path --windows "$LIBS"`
  [ -n "$CONFIG" ] && CONFIG=`cygpath --path --windows "$CONFIG"`
fi

JAVA_OPTS="${JAVA_OPTS} -Xms64m -Xmx128m -Dcom.tc.productkey.path=${TC_LICENSEKEY_PATH} -Dtc.connect.servers=${TC_CONNECT_URL} -Dehcache.config.path=${EHCACHE_CONFIG_PATH}"
dt=`date +%Y%m%d_%H%M%S`

echo
echo "####### Executing Step 1 of Ping: Putting objects in cache"
echo

$JAVACMD $JAVA_OPTS \
   -classpath "$CLASSPATH" \
   com.terracotta.tools.tcPing 1 $dt 10000

return_code=$?
if [ $return_code == 0 ] ; then
    echo "####### Executing Step 2 of Ping: Retrieving all objects in cache"
    echo

    $JAVACMD $JAVA_OPTS \
       -classpath "$CLASSPATH" \
       com.terracotta.tools.tcPing 2 $dt 10000

    return_code=$?
    if [ $return_code == 0 ] ; then
       echo
       echo "####### Terracotta Ping Successful!!"
       echo
       exit 0
    else # Step 2 failed
       echo
       echo "####### Terracotta Ping failed!!"
       echo
       exit 1
    fi
else
    echo
    echo "####### Terracotta Ping failed!!"
    echo
    exit 1
fi