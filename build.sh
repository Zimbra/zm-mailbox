#!/bin/bash

DOCKER_REPO_NS="$1"
DOCKER_BUILD_TAG="$2"
LIBS_REPO_BRANCH="$3"

if [ -z "$1" ]
  then
    DOCKER_REPO_NS="zms"
fi

if [ -z "$2" ]
  then
    DOCKER_BUILD_TAG="1.0"
fi

if [ -z "$3" ]
  then
    LIBS_REPO_BRANCH="develop"
fi

# Generate store/build/dist/conf/attrs/zimbra-attrs-schema
ant create-version-ldap -f store/build.xml

# Generate service webapp at store/build/service 
ant war -f store/build.xml -Dzimbra.buildinfo.version=8.9.0
mkdir store/build/service
cp store/build/service.war store/build/service/
cd store/build/service/ && jar -xf service.war && rm service.war
cd ../../..

# Generate store-conf/build/zms/mailbox-conf
ant build-mailbox-conf -f store-conf/build.xml

# Generate core jars
rm -rf native/build/*.jar
rm -rf common/build/*.jar
rm -rf soap/build/*.jar
rm -rf client/build/*.jar
rm -rf store/build/*.jar
ant publish-local -f native/build.xml -Dzimbra.buildinfo.version=8.9.0
ant publish-local -f common/build.xml -Dzimbra.buildinfo.version=8.9.0
ant publish-local -f soap/build.xml -Dzimbra.buildinfo.version=8.9.0
ant publish-local -f client/build.xml -Dzimbra.buildinfo.version=8.9.0
ant publish-local -f store/build.xml -Dzimbra.buildinfo.version=8.9.0
mv `ls native/build/*.jar` native/build/zimbra-native.jar
mv `ls common/build/*.jar` common/build/zimbracommon.jar
mv `ls soap/build/*.jar` soap/build/zimbrasoap.jar
mv `ls client/build/*.jar` client/build/zimbraclient.jar
mv `ls store/build/*.jar` store/build/zimbrastore.jar

# Generate native lib
ant generate-native-headers -f native/build.xml -Dzimbra.buildinfo.version=8.9.0
ant generate-native-lib

# Build mailbox components docker image
docker build -t ${DOCKER_REPO_NS}/zms-mailbox:${DOCKER_BUILD_TAG} .