#!/bin/sh

#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
#


############ custom section based on what needs to be looked into #####################
# extra custom classpath dependencies to add
CLASSPATH_PREFIX=

# path to ehcache config
EHCACHE_CONFIG_PATH=classpath:ehcache.xml

# If terracotta-license.key file is in the config folder, it's loaded automatically
# But otherwise, specify the path here
#TC_LICENSEKEY=terracotta-license.key

# load the environment variables that may be used i nthe ehcache config file
#JAVA_OPTS="${JAVA_OPTS} -Dtc.connect.servers=${TC_CONNECT_URL}"

############ custom section based on what needs to be looked into #####################
