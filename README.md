This repository has the core platform code for Zimbra Collaboration Suite.

Branch | Status
------ | ------
feature/ha   | [![CircleCI](https://circleci.com/gh/Zimbra/zm-mailbox/tree/feature%2Fha.svg?style=svg)](https://circleci.com/gh/Zimbra/zm-mailbox/tree/feature%2Fha)

## Build Mailbox as Service Image

* Currently Mailbox image is a multi layered docker image where the required file  systems from different Github repository are created as small busybox docker image which are integrated into mailbox image.
* Currently all other docker images which are integrated into mailbox docker image has tag `1.0`.
* Below are the list of docker images currently integrated into Mailbox docker image.

Github Repository Name | Image Name | Tag
------ | ------ | ------
zm-docker | zms-base | 1.0
zm-core-utils | zms-core-utils | 1.0
zm-zcs-lib | zms-zcs-lib | 1.0
zm-jetty-conf | zms-jetty-conf | 1.0
zm-jython | zms-jython | 1.0
zm-build | zms-perl | 1.0
zm-db-conf | zms-db-conf | 1.0
zm-admin-console | zms-admin-consol | 1.0
zm-ldap-utilities | zms-ldap-utilities | 1.0
zm-timezones | zms-timezones | 1.0

N.B : **`zms-extension:1.0`** image is also used currently for all required extensions. But its build process needs to be finalized.

* Below is the ant target needs to be invoked to build and push mailbox docker image to Zimbra dev docker registry.

```
ant clean push-image -Dzimbra.buildinfo.version=<build version> -Dzimbra.buildinfo.release=<timestamp>  -Dzimbra.buildinfo.date=<timestamp> -DDOCKER_REPO_PUSH_NS=<registry name> -DDOCKER_MAILBOX_BUILD_TAG=<tag number>
```
e.g
```
ant clean push-image -Dzimbra.buildinfo.version=8.9.0_GA -Dzimbra.buildinfo.release=`date +'%Y%m%d%H%M%S'` -Dzimbra.buildinfo.date=`date +'%Y%m%d%H%M%S'` -DDOCKER_REPO_PUSH_NS=dev-registry.zimbra-docker-registry.tk -DDOCKER_MAILBOX_BUILD_TAG=1.2.1
```

* If this argument `-DDOCKER_REPO_PUSH_NS=dev-registry.zimbra-docker-registry.tk` and `-DDOCKER_MAILBOX_BUILD_TAG=1.2.1` would not be provided from the command line
  to the ant target then the docker image will be created  with default value `zms/zms-base:1.0` and the `docker push` might fail. So to push the docker image to the zimbra dev
  docker registry this argument `-DDOCKER_REPO_PUSH_NS=dev-registry.zimbra-docker-registry.tk` must be passed.
* If any dependent image tag would change then it needs to be updated in the mailbox `Dockerfile` before image build.
