#!/bin/bash
# Clone FOSS extension repos, build jars and copy to appropriate location for assembly
#
# ***** BEGIN LICENSE BLOCK *****
# Zimbra Collaboration Suite Server
# Copyright (C) 2019 Synacor, Inc.
#
# This program is free software: you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free Software Foundation,
# version 2 of the License.
#
# This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
# without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
# See the GNU General Public License for more details.
# You should have received a copy of the GNU General Public License along with this program.
# If not, see <https://www.gnu.org/licenses/>.
# ***** END LICENSE BLOCK *****
#

EXT_DIR="./extension-common-core"
FILE_NAME=$EXT_DIR/repo-list

TAG=$1
if [ -z $TAG ] || [ $TAG = " " ];
then
   echo "BUILD_TAG is missing, please provide BUILD_TAG. e.g; 1, 1.0, 1.0.1, 1.1"
   exit 1
fi
repoFile=$FILE_NAME
ZM_BUILD_BRANCH="develop"
if [ -z "${GITHUB_ACCESS_TOKEN}" ]
then
  echo "GITHUB_ACCESS_TOKEN is undefined"
  exit 1
fi;
rm -rf $EXT_DIR/build
mkdir -p $EXT_DIR/build
cd $EXT_DIR/build
git clone --branch ${ZM_BUILD_BRANCH} https://${GITHUB_ACCESS_TOKEN}@github.com/ZimbraOS/zm-zcs.git
git clone --branch ${ZM_BUILD_BRANCH} https://${GITHUB_ACCESS_TOKEN}@github.com/ZimbraOS/zimbra-package-stub.git
cd ../..
while IFS= read -r line
do
  cd $EXT_DIR/build
  git clone --branch ${ZM_BUILD_BRANCH} https://${GITHUB_ACCESS_TOKEN}@github.com/ZimbraOS/${line}.git
  cd ${line}
  if [ $line = "zm-license-tools" ] || [ $line = "zm-license-store" ]
  then
     ant clean publish-local dist -Dis-production=1 -Dzimbra.buildinfo.version=8.9.0_GA_1012 -Dzimbra.buildinfo.release=`date +'%Y%m%d%H%M%S'` -Dzimbra.buildinfo.date=`date +'%Y%m%d%H%M%S'`
  else
     ant clean publish-local -Dzimbra.buildinfo.version=8.9.0_GA_1012 -Dzimbra.buildinfo.release=`date +'%Y%m%d%H%M%S'` -Dzimbra.buildinfo.date=`date +'%Y%m%d%H%M%S'`
  fi
  cd ../../..

done < "$repoFile"


# OopenId dist-package for extension-extra
cd $EXT_DIR/build/zm-openid-consumer-store
ant clean dist-package
rm -rf build/dist/extensions-extra
cd ../../..

echo "cloning saml2"
mkdir $EXT_DIR/build/saml2sp
cd $EXT_DIR/build/saml2sp
curl -s -k -o saml2sp.jar 'https://s3.amazonaws.com/docker.zimbra.com/assets/saml2sp_zimbra8.jar'
cd ../../..

mkdir $EXT_DIR/build/zimberg
cd $EXT_DIR/build/zimberg
curl -k -L -o  zimberg_store_manager-0.3.0.jar  'https://s3.amazonaws.com/docker.zimbra.com/assets/zimberg_store_manager-0.3.0.jar'
cd ../../..

cd $EXT_DIR/build/zm-bulkprovision-store
curl  -k -L -o  commons-csv-1.2.jar 'https://repo1.maven.org/maven2/org/apache/commons/commons-csv/1.2/commons-csv-1.2.jar'
cd ../../..

cd $EXT_DIR/build/zm-gql
curl  -k -L -o  java-jwt-3.2.0.jar 'https://repo1.maven.org/maven2/com/auth0/java-jwt/3.2.0/java-jwt-3.2.0.jar'
cd ../../..

cd $EXT_DIR
docker build -t dev-registry.zimbra-docker-registry.tk/zms-core-extension:${TAG} .
#docker push dev-registry.zimbra-docker-registry.tk/zms-core-extension:${TAG}
rm -rf build
