## Inputs from Perforce

- `ZimbraServer/src/java`
- `ZimbraServer/src/java-test`
- `ZimbraServer/WebRoot/*`
- `ZimbraServer/build.xml`
- `ZimbraServer/data`
- `ZimbraServer/conf/attrs`
- `ZimbraServer/conf/cxf.xml`
- `ZimbraServer/docs`
- `ZimbraServer/conf/web.xml`
- `ZimbraServer/conf/web.xml.production`

## Dependencies

- `zm-common`
- `zm-thirdparty-jars`
- `zm-client`
- `zm-store`
- `zm-soap`
- `zm-native`

## Artifacts

- `zimbrastore.jar`
- `service.war`
- `zimbrastore-test.jar`

## Setting up dev environment

1. install latest version of ZCS
2. create `/home/zimbra` and make `zimbra` user the owner
3. install `git`, `ant`, `ant-contrib`
4. configure `/opt/zimbra/.ssh/config` to use your ssh key for git host
5. edit `/opt/zimbra/.bash_profile`
        - comment out `export LANG=C` and `export LC_ALL=C` 
        - add `export LANG=en_US.UTF-8` 
        - add `export ANT_OPTS=-Ddev.home=/home/zimbra`
6. Change permissions on files and folders that you will be updating. E.g.:

        chmod -R o+w /opt/zimbra/lib/
        chmod -R o+w /opt/zimbra/jetty/
        
    (note, if you run `zmfixperms`, these permission changes will get overwritten)


7. `mkdir /home/zimbra/git`
   - clone any repos you need
   - set your user.name and user.email options for git repositories

8. if you want email delivery to work, set up a DNS server on your host machine or another VM and configure zimbraDNSMasterIP to point to it. E.g.:

        zmprov ms ubuntu2.local zimbraDNSMasterIP 172.16.150.1

9. You are ready to build zimbra code and test your changes

## Adding LDAP attributes

1. Clone zm-ldap-utilities and zm-common repositories
2. Add new XML to zm-store/conf/attrs/zimbra-attrs.xml
3. Invoke `ant generate-getters` from zm-store/build.xml
4. Invoke `ant clean compile test` to make sure everything went OK
5. `chmod -R o+w /opt/zimbra/common/etc/openldap/schema`
   `chmod o+w /opt/zimbra/conf/zimbra.ldif`
6. Invoke `ant deploy publish-local` from zm-common/build.xml
   
   Note 1: until zm-common compiles with Java8, you need to switch to Java7 and run `ant jar` from zm-common/build.xml before step 6 and then switch back to Java8.
   e.g.: `export PATH=/home/zimbra/jdk1.7.0_79/bin/:$PATH` and `export JAVA_HOME=/home/zimbra/jdk1.7.0_79/`
   
   Note 2: until all ivy.xml files are using zm-common instead of zimbracommon as a dependency, you will need to clear your ivy cache before step 7. 
   e.g.: `rm -r /home/zimbra/.ivy2/cache/zimbracommon/`
     
7. Invoke `ant deploy update-ldap-schema` from zm-store/build.xml
8. At this point your zimbra server should be running and working with the new LDAP attributes that you added to zimbra-attrs.xml. Check that new LDAP attributes work by modifying them with `zmprov`    
9. Commit java changes in zm-store and zm-common as well as changes to zimbra-attrs.xml. You should split this into 3 commits
   - commit changes to java files in zm-common
   - commit changes to java files in zm-store
   - commit changes to zimbra-attrs.xml in zm-store
10. Create 2 pull requests: one pull request for your changes to zm-common and another pull request for your changes in zm-store