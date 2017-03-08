## Inputs from Perforce

- `ZimbraServer/src/java/com/zimbra/cs/milter`
- `ZimbraServer/src/java-test/com/zimbra/cs/milter`
- `ZimbraServer/src/java-test/localconfig-test.xml`
- `ZimbraServer/src/java-test/log4j-test.properties`
- `ZimbraServer/conf/milter.log4j.properties`
- `ZimbraServer/conf/mta_milter_options.in`
- `ZimbraServer/build.xml`

## Dependencies

- `zm-thirdparty-jars`
- `zm-common`
- `zm-client`
- `zm-store`

## Artifacts

- `zm-milter-<version>.jar`

## Build Pre-requisite

- create .zcs-deps folder in home directory
- copy following jars in the .zcs-deps folder:
     - `ant-contrib-1.0b1.jar`
     - `zm-common.jar`
     - `zm-client.jar`
     - `zm-store.jar`
