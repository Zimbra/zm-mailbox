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
  
