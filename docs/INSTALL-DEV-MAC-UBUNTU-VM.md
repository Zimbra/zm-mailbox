Setup Development Environment for Mac with a Ubuntu Server VM
=============================================================
These instructions describe how to set up a development environment on an OSX machine with VMWare Fusion. 
Please note, that Zimbra does not run on OSX natively. Therefore, in order to run Zimbra, you need to have a Linux VM. However, you can still
use Eclipse and p4v on your Mac.

## Create a Fusion VM with Ubuntu 14.04

1. Download Ubuntu 14.04 Server ISO file
2. Create a VMWare Fusion virtual machine with Ubuntu 14.04 Server guest OS. DO NOT USE "Easy Install" OPTION - uncheck the "Easy Install" checkbox". 
Make sure to assign enough RAM and disk space to the VM (4GB RAM and 20GB disk is sufficient). Default (shared) network option is sufficient. Assigning 2 virtual CPUs may also speed things up. You can choose all default options during installation and you should also install OpenSSH Server when prompted to select additional packages - DO NOT INSTALL ANY OTHER ADDITIONAL PACKAGES SUGGESTED BY THE INSTALLER. 
During installation create "zimbra" user account.

## Install and update required packages on the VM
1. update apt

````
$ sudo apt-get update
$ sudo apt-get install build-essential ant python-pip
````

When MariaDB installer prompts for root password type test123

2. add and configure zimbra repositories

To enable the repository on UBUNTU12, create the following file:
````
/etc/apt/sources.list.d/zimbra.list
````
With the following contents:

````
deb     [arch=amd64] https://repo.zimbra.com/apt/87 trusty zimbra
deb-src [arch=amd64] https://repo.zimbra.com/apt/87 trusty zimbra
````
3. Run the following commands
````
$ sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 9BE6ED79
$ sudo apt-get install -y apt-transport-https
$ sudo apt-get update
````
4. Install zimbra packages
````
$ sudo apt-get install zimbra-openldap-server zimbra-openldap-client zimbra-rsync zimbra-openssl zimbra-openjdk zimbra-openjdk-cacerts zimbra-mariadb zimbra-tcmalloc-lib zimbra-openjdk
````

5. Change owner of /opt/zimbra to "zimbra"

````
$ sudo chown -R zimbra /opt/zimbra
````

## Configure your workspace environment on the VM

1. Add Helix (former Perforce) repository following instructions on this page: https://www.perforce.com/perforce-packages/helix-versioning-engine
2. Install helix-cli via apt
````
$ sudo apt-get install helix-cli
````
3. Copy your SSH keys (public and private) to $HOME/.ssh/ folder on the VM
4. Set up your environment variables (may be different depending on your office location and SSH set up)

Add the following content to $HOME/.profile
````
export P4PORT=1066
export P4HOST=p4proxy.eng.zimbra.com
export P4USER={your p4 username}
export P4CONFIG=.p4config
export P4EDITOR=/usr/bin/vi
export PATH=$PATH:/opt/zimbra/bin:$HOME/bin
export ZIMBRA_HOSTNAME={your computer name}.local
alias ssh_p4='ssh -f -N p4'
alias ssh_web='ssh -f -N web'
alias ssh_rb='ssh -f -N rb'
alias ssh_all='ssh_p4; ssh_web; ssh_rb'
````
5. set up SSH configuration for accessing servers behind the firewall
Add the following content to $HOME/.ssh/config
````
Host *
User {your user name on fence-new}
IdentityFile ~/.ssh/id_rsa
ForwardAgent yes
ServerAliveInterval 30
ServerAliveCountMax 120

Host p4
Hostname fence-new.zimbra.com
LocalForward 1066 perforce.zimbra.com:1066

Host web
Hostname fence-new.zimbra.com
DynamicForward 8787

Host rb
Hostname fence-new.zimbra.com
LocalForward 8080 reviewboard.eng.zimbra.com:80
````
6. load new environment settings and start SSH tunnel to perforce:
````
$ source ~/.profile
$ ssh_all
````
## Install VMWare tools
If you want to be able to edit files using an IDE on your Mac, you have to set up a shared folder on your Mac with read/write access for your Ubuntu VM. Follow instructions on VMWare website: http://kb.vmware.com/selfservice/microsites/search.do?language=en_US&cmd=displayKC&externalId=1022525

Once VMWare tools are installed, your Mac's shared folder will be mounted at /mnt/hgfs/. You can use this path as your workspace or you can map it to another folder on the VM.

## Download Sun JDK 1.7
Even though JUDASPRIEST branch runs on Java8, you need JDK 1.7 to compil JUDASPRIEST branch.
If you are installing Sun JDK. Download and unpack it to your home folder on Ubuntu. Then, add jdk's bin folder to your $PATH environment variable.
If you are using zimbra-openjdk for compiling Zimbra Java code, add /opt/zimbra/java/bin to your $PATH. You may also want to set JAVA_HOME for other Java tools to work properly.
If using Sun JDK 1.7.0 rev 79, add the following to $HOME/.profile
````
export JAVA_HOME="$HOME/jdk1.7.0_79"
````

## Get the source code
You have to log in to perforce on the guest Ubuntu VM.

````
$ p4 login
* enter your password
$ p4 client
````
If you are configuring perforce client on your host (Mac), then make sure your workspace is mapped to the folder that you are sharing between the Ubuntu guest VM and Mac host. E.g.: /mnt/hgfs/ubuntuhome/p4

````
Root:   /mnt/hgfs/ubuntuhome/p4
````

Enter the following as the view contents:

````
//depot/zimbra/JUDASPRIEST/ant-global.xml //{your-workspace-name}/JUDASPRIEST/ant-global.xml
//depot/zimbra/JUDASPRIEST/pom.xml //{your-workspace-name}/JUDASPRIEST/pom.xml
//depot/zimbra/JUDASPRIEST/zimbra.l10n //{your-workspace-name}/JUDASPRIEST/zimbra.l10n
//depot/zimbra/JUDASPRIEST/mvn-local-jars.sh //{your-workspace-name}/JUDASPRIEST/mvn-local-jars.sh
//depot/zimbra/JUDASPRIEST/OpenID/... //{your-workspace-name}/JUDASPRIEST/OpenID/...
//depot/zimbra/JUDASPRIEST/ZimbraMezeoExtension/... //{your-workspace-name}/JUDASPRIEST/ZimbraMezeoExtension/...
//depot/zimbra/JUDASPRIEST/ZimbraHSM/... //{your-workspace-name}/JUDASPRIEST/ZimbraHSM/...
//depot/zimbra/JUDASPRIEST/ZimbraHtmlExtras/... //{your-workspace-name}/JUDASPRIEST/ZimbraHtmlExtras/...
//depot/zimbra/JUDASPRIEST/ZimbraFreeBusyProvider/... //{your-workspace-name}/JUDASPRIEST/ZimbraFreeBusyProvider/...
//depot/zimbra/JUDASPRIEST/ZimbraAdminVersionCheck/... //{your-workspace-name}/JUDASPRIEST/ZimbraAdminVersionCheck/...
//depot/zimbra/JUDASPRIEST/ZimbraEws/... //{your-workspace-name}/JUDASPRIEST/ZimbraEws/...
//depot/zimbra/JUDASPRIEST/ZimbraLicensePortal/... //{your-workspace-name}/JUDASPRIEST/ZimbraLicensePortal/...
//depot/zimbra/JUDASPRIEST/ZimbraEwsCommon/... //{your-workspace-name}/JUDASPRIEST/ZimbraEwsCommon/...
//depot/zimbra/JUDASPRIEST/ZimbraLicenseExtension/... //{your-workspace-name}/JUDASPRIEST/ZimbraLicenseExtension/...
//depot/zimbra/JUDASPRIEST/ZimbraLicenseTools/... //{your-workspace-name}/JUDASPRIEST/ZimbraLicenseTools/...
//depot/zimbra/JUDASPRIEST/ZimbraLicenseHtmlExtras/... //{your-workspace-name}/JUDASPRIEST/ZimbraLicenseHtmlExtras/...
//depot/zimbra/JUDASPRIEST/ZimbraLDAPUtilsExtension/... //{your-workspace-name}/JUDASPRIEST/ZimbraLDAPUtilsExtension/...
//depot/zimbra/JUDASPRIEST/ZimbraNginxLookup/... //{your-workspace-name}/JUDASPRIEST/ZimbraNginxLookup/...
//depot/zimbra/JUDASPRIEST/ZimbraOpenOfficeExt/... //{your-workspace-name}/JUDASPRIEST/ZimbraOpenOfficeExt/...
//depot/zimbra/JUDASPRIEST/ZimbraPosixAccountsExtension/... //{your-workspace-name}/JUDASPRIEST/ZimbraPosixAccountsExtension/...
//depot/zimbra/JUDASPRIEST/ZimbraCharset/... //{your-workspace-name}/JUDASPRIEST/ZimbraCharset/...
//depot/zimbra/JUDASPRIEST/ThirdParty/jetty/... //{your-workspace-name}/JUDASPRIEST/ThirdParty/jetty/...
//depot/zimbra/JUDASPRIEST/ThirdPartyBuilds/... //{your-workspace-name}/JUDASPRIEST/ThirdPartyBuilds/...
//depot/zimbra/JUDASPRIEST/ZimbraServer/... //{your-workspace-name}/JUDASPRIEST/ZimbraServer/...
//depot/zimbra/JUDASPRIEST/ZimbraPerf/... //{your-workspace-name}/JUDASPRIEST/ZimbraPerf/...
//depot/zimbra/JUDASPRIEST/ZimbraPerf/data/dbrawdata/... //{your-workspace-name}/JUDASPRIEST/ZimbraPerf/data/dbrawdata/...
//depot/zimbra/JUDASPRIEST/ZimbraXMbxSearch/... //{your-workspace-name}/JUDASPRIEST/ZimbraXMbxSearch/...
//depot/zimbra/JUDASPRIEST/ZimbraCommon/... //{your-workspace-name}/JUDASPRIEST/ZimbraCommon/...
//depot/zimbra/JUDASPRIEST/ZimbraClient/... //{your-workspace-name}/JUDASPRIEST/ZimbraClient/...
//depot/zimbra/JUDASPRIEST/ZimbraCluster/... //{your-workspace-name}/JUDASPRIEST/ZimbraCluster/...
//depot/zimbra/JUDASPRIEST/ZimbraBackup/... //{your-workspace-name}/JUDASPRIEST/ZimbraBackup/...
//depot/zimbra/JUDASPRIEST/ZimbraBuild/... //{your-workspace-name}/JUDASPRIEST/ZimbraBuild/...
//depot/zimbra/JUDASPRIEST/ZimbraArchive/... //{your-workspace-name}/JUDASPRIEST/ZimbraArchive/...
//depot/zimbra/JUDASPRIEST/ZimbraAppliance/... //{your-workspace-name}/JUDASPRIEST/ZimbraAppliance/...
//depot/zimbra/JUDASPRIEST/ZimbraAdminExt/... //{your-workspace-name}/JUDASPRIEST/ZimbraAdminExt/...
//depot/zimbra/JUDASPRIEST/ZimbraConvertd/... //{your-workspace-name}/JUDASPRIEST/ZimbraConvertd/...
//depot/zimbra/JUDASPRIEST/ZimbraEvolution/... //{your-workspace-name}/JUDASPRIEST/ZimbraEvolution/...
//depot/zimbra/JUDASPRIEST/ZimbraNative/... //{your-workspace-name}/JUDASPRIEST/ZimbraNative/...
//depot/zimbra/JUDASPRIEST/ZimbraNetwork/... //{your-workspace-name}/JUDASPRIEST/ZimbraNetwork/...
//depot/zimbra/JUDASPRIEST/ZimbraOffline/... //{your-workspace-name}/JUDASPRIEST/ZimbraOffline/...
//depot/zimbra/JUDASPRIEST/ZimbraOfflineExt/... //{your-workspace-name}/JUDASPRIEST/ZimbraOfflineExt/...
//depot/zimbra/JUDASPRIEST/ZimbraSoap/... //{your-workspace-name}/JUDASPRIEST/ZimbraSoap/...
//depot/zimbra/JUDASPRIEST/ZimbraSync/... //{your-workspace-name}/JUDASPRIEST/ZimbraSync/...
//depot/zimbra/JUDASPRIEST/ZimbraSyncClient/... //{your-workspace-name}/JUDASPRIEST/ZimbraSyncClient/...
//depot/zimbra/JUDASPRIEST/ZimbraSyncCommon/... //{your-workspace-name}/JUDASPRIEST/ZimbraSyncCommon/...
//depot/zimbra/JUDASPRIEST/ZimbraTagLib/... //{your-workspace-name}/JUDASPRIEST/ZimbraTagLib/...
//depot/zimbra/JUDASPRIEST/ZimbraWebClient/... //{your-workspace-name}/JUDASPRIEST/ZimbraWebClient/...
//depot/zimbra/JUDASPRIEST/Zimlet/... //{your-workspace-name}/JUDASPRIEST/Zimlet/...
//depot/zimbra/JUDASPRIEST/ZimbraSyncPerf/... //{your-workspace-name}/JUDASPRIEST/ZimbraSyncPerf/...
//depot/zimbra/JUDASPRIEST/ZimbraSyncTools/... //{your-workspace-name}/JUDASPRIEST/ZimbraSyncTools/...
//depot/zimbra/JUDASPRIEST/ZimbraSync4j/... //{your-workspace-name}/JUDASPRIEST/ZimbraSync4j/...
//depot/zimbra/JUDASPRIEST/ZimbraVoice/... //{your-workspace-name}/JUDASPRIEST/ZimbraVoice/...
//depot/zimbra/JUDASPRIEST/ZimbraSyncPerf/... //{your-workspace-name}/JUDASPRIEST/ZimbraSyncPerf/...
//depot/zimbra/JUDASPRIEST/ZimbraLogger/... //{your-workspace-name}/JUDASPRIEST/ZimbraLogger/...
//depot/zimbra/JUDASPRIEST/ZimbraSync4j/... //{your-workspace-name}/JUDASPRIEST/ZimbraSync4j/...
//depot/zimbra/JUDASPRIEST/ZimbraNotif/... //{your-workspace-name}/JUDASPRIEST/ZimbraNotif/...
//depot/zimbra/JUDASPRIEST/ZimbraLauncher/... //{your-workspace-name}/JUDASPRIEST/ZimbraLauncher/...
//depot/zimbra/JUDASPRIEST/mvn-local-jars.sh //{your-workspace-name}/JUDASPRIEST/mvn-local-jars.sh
//depot/zimbra/JUDASPRIEST/pom.xml //{your-workspace-name}/JUDASPRIEST/pom.xml
//depot/zimbra/JUDASPRIEST/README.buildZCS //{your-workspace-name}/JUDASPRIEST/README.buildZCS
//depot/zimbra/JUDASPRIEST/Zimbra.ipr //{your-workspace-name}/JUDASPRIEST/Zimbra.ipr
//depot/zimbra/JUDASPRIEST/zimbra.l10n //{your-workspace-name}/JUDASPRIEST/zimbra.l10n
-//depot/zimbra/JUDASPRIEST/ThirdPartyBuilds/OSXx86_64/... //{your-workspace-name}/JUDASPRIEST/ThirdPartyBuilds/OSXx86_64/...
-//depot/zimbra/JUDASPRIEST/ThirdPartyBuilds/MACOSXx86_10.7/... //{your-workspace-name}/JUDASPRIEST/ThirdPartyBuilds/MACOSXx86_10.7/...
//depot/zimbra/JUDASPRIEST/ZIM/... //{your-workspace-name}/JUDASPRIEST/ZIM/...
//depot/zimbra/JUDASPRIEST/ZimbraIMExtention/... //{your-workspace-name}/JUDASPRIEST/ZimbraIMExtention/...
//depot/zimbra/JUDASPRIEST/SolrPlugins/... //{your-workspace-name}/JUDASPRIEST/SolrPlugins/...
//depot/zimbra/JUDASPRIEST/CrocoDocExt/... //{your-workspace-name}/JUDASPRIEST/CrocoDocExt/...
//depot/zimbra/JUDASPRIEST/ZimbraFOSS/... //{your-workspace-name}/JUDASPRIEST/ZimbraFOSS/...
-//depot/zimbra/JUDASPRIEST/ZimbraSelenium/... //{your-workspace-name}/JUDASPRIEST/ZimbraSelenium/...
-//depot/zimbra/JUDASPRIEST/ZimbraQA/... //{your-workspace-name}/JUDASPRIEST/ZimbraQA/...
-//depot/zimbra/JUDASPRIEST/ZimbraServer/src/windows/... //{your-workspace-name}/JUDASPRIEST/ZimbraServer/src/windows/...
-//depot/zimbra/JUDASPRIEST/ZimbraPerf/data/... //{your-workspace-name}/JUDASPRIEST/ZimbraPerf/data/...
-//depot/zimbra/JUDASPRIEST/ThirdPartyBuilds/MACOSXx86_10.6/... //{your-workspace-name}/JUDASPRIEST/ThirdPartyBuilds/MACOSXx86_10.6/...
-//depot/zimbra/JUDASPRIEST/ThirdPartyBuilds/x86_64/... //{your-workspace-name}/JUDASPRIEST/ThirdPartyBuilds/x86_64/...
-//depot/zimbra/JUDASPRIEST/ThirdPartyBuilds/RHEL6_64/... //{your-workspace-name}/JUDASPRIEST/ThirdPartyBuilds/RHEL6_64/...
-//depot/zimbra/JUDASPRIEST/ThirdPartyBuilds/RHEL7_64/... //{your-workspace-name}/JUDASPRIEST/ThirdPartyBuilds/RHEL7_64/...
-//depot/zimbra/JUDASPRIEST/ThirdPartyBuilds/wndows/... //{your-workspace-name}/JUDASPRIEST/ThirdPartyBuilds/windows/...
-//depot/zimbra/JUDASPRIEST/ThirdPartyBuilds/SLES11_64/... //{your-workspace-name}/JUDASPRIEST/ThirdPartyBuilds/SLES11_64/...
-//depot/zimbra/JUDASPRIEST/ThirdPartyBuilds/UBUNTU14_64/... //{your-workspace-name}/JUDASPRIEST/ThirdPartyBuilds/UBUNTU14_64/...
-//depot/zimbra/JUDASPRIEST/ThirdPartyBuilds/UBUNTU10_64/... //{your-workspace-name}/JUDASPRIEST/ThirdPartyBuilds/UBUNTU10_64/...
//depot/zimbra/JUDASPRIEST/ThirdPartyBuilds/UBUNTU12_64/... //{your-workspace-name}/JUDASPRIEST/ThirdPartyBuilds/UBUNTU12_64/...
-//depot/zimbra/JUDASPRIEST/ZimbraPerf/data/dbrawdata/... //{your-workspace-name}/JUDASPRIEST/ZimbraPerf/data/dbrawdata/...
-//depot/zimbra/JUDASPRIEST/ZimbraNetwork/ZimbraImportWizard/... //{your-workspace-name}/JUDASPRIEST/ZimbraNetwork/ZimbraImportWizard/...
````

That view may have a lot more than you need, so you may want to consider explicitly listing
only what you need. Take a look at the clients of others in your group for examples.

````
$ cd ~/p4/JUDASPRIEST
$ p4 sync
````

## Initialize /opt/zimbra/ folder structure
Run the following command in ZimbraServer folder. This will install jetty and copy some scripts to /opt/zimbra/bin

````
$ ant install-thirdparty init-opt-zimbra
````

## Configure MariDB
1. Copy ZimbraServer/conf/mariadb/my.cnf to /opt/zimbra/conf/my.cnf
````
$ cp ZimbraServer/conf/mariadb/my.cnf /opt/zimbra/conf/my.cnf
````

2. Create default database and tables
````
$ /opt/zimbra/common/share/mysql/scripts/mysql_install_db --basedir=/opt/zimbra/common --datadir=/opt/zimbra/db/data --defaults-file=/opt/zimbra/conf/my.cnf --user=zimbra
````

3. Start mariadb:
````
$ /opt/zimbra/bin/mysql.server start
````

4. Set password for MariaDB root user
````
$ /opt/zimbra/common/bin/mysqladmin --socket=/opt/zimbra/data/tmp/mysql/mysql.sock -u root password zimbra
````

5. Reload permissions tables
````
$ /opt/zimbra/common/bin/mysqladmin --socket=/opt/zimbra/data/tmp/mysql/mysql.sock -u root -p reload
````

6. Restart mariadb and make sure it is running:
````
$ /opt/zimbra/bin/mysql.server restart
$ /opt/zimbra/bin/mysql.server status
````

## Deploy dev build
run the following command in ZimbraServer folder
````
$ ant reset-all
````

## Configure localconfig
Find out your zimbra users uid and gid either by looking at /etc/passwd or by running
````
$ sudo id -u zimbra
$ sudo id -g zimbra
````

Edit localconfig value zimbra_id to match your zimbra user's uid. e.g., if your zimbra user's uid is 1000 and gid is 1000, which is what it would normally be if this is the first and only user account.

````
$ zmlocalconfig -e zimbra_uid=1000
$ zmlocalconfig -e zimbra_gid=1000
````

## Integration with Eclipse and perforce on your Mac
After you follow the steps outlined above, you will be able to check-out/check-in code on your Ubuntu VM. That's cool, but it is not very convenient if you want to use Eclipse or any other IDE on your Mac. To be able to manage files and use an IDE on your Mac, you will need to:
1. install and configure performce on your Mac
2. reconfigure your workspace "Root" to map to the path on your Mac, e.g. $HOME/ubuntuhome/p4 instead of /mnt/hgfs/ubuntuhome/p4
3. (bonus points) create a simlink to /mng/hgfs/ubuntuhome on the Ubuntu VM to match the path on your Mac host. This will allow you to use the same perforce client spec on both machines. 

E.g., if the shared folder on your mac is /Users/gsolovyev/ubuntuhome, run the following on your Ubuntu VM:
````
$ sudo mkdir -p /Users/gsolovyev
$ sudo ln -s /mnt/hgfs/ubuntuhome /Users/gsolovyev/
$ sudo chown zimbra:zimbra /Users/gsolovyev
````