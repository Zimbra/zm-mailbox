#!/bin/bash
# Clone Network extension repos, build jars
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

EXT_DIR="./extension-network-core"
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
  if [ $line = "zm-sync-client" ] || [ $line = "zm-ews-stub" ]; then
     ant clean publish-local -Dzimbra.buildinfo.version=8.9.0_GA_1012 -Dzimbra.buildinfo.release=`date +'%Y%m%d%H%M%S'` -Dzimbra.buildinfo.date=`date +'%Y%m%d%H%M%S'`
  elif [ $line = "zm-openoffice-store" ]; then
     ant clean publish-local build-dist -Dzimbra.buildinfo.version=8.9.0_GA_1012 -Dzimbra.buildinfo.release=`date +'%Y%m%d%H%M%S'` -Dzimbra.buildinfo.date=`date +'%Y%m%d%H%M%S'`
  else
     ant clean publish-local dist -Dzimbra.buildinfo.version=8.9.0_GA_1012 -Dzimbra.buildinfo.release=`date +'%Y%m%d%H%M%S'` -Dzimbra.buildinfo.date=`date +'%Y%m%d%H%M%S'`
  fi
  cd ../../..
done < "$repoFile"

cd $EXT_DIR
docker build -t dev-registry.zimbra-docker-registry.tk/zms-core-network-extension:${TAG} .
#docker push dev-registry.zimbra-docker-registry.tk/zms-core-network-extension:${TAG}
cd ../

rm -rf $EXT_DIR/build
