Setup Development Environment for Mac with a Ubuntu Server VM
=============================================================
These instructions describe how to set up a development environment on an OSX machine with VMWare Fusion. 
Please note, that Zimbra does not run on OSX natively. Therefore, in order to run Zimbra, you need to have a Linux VM. However, you can still
use Eclipse and p4v on your Mac.

## Create a Fusion VM with Ubuntu 14.04

1. Download Ubuntu 14.04 Server ISO file
2. Create a VMWare Fusion virtual machine with Ubuntu 14.04 Server guest OS.
    - DO NOT USE "Easy Install" OPTION - uncheck the "Easy Install" checkbox".
    - Make sure to assign enough RAM and disk space to the VM (4GB RAM and 20GB disk is sufficient).
    - Default (shared) network option is sufficient.
    - Assigning 2 virtual CPUs may also speed things up.
    - You can choose all default options during installation.
    - Install OpenSSH Server when prompted to select additional packages.
    - **DO NOT INSTALL ANY OTHER ADDITIONAL PACKAGES SUGGESTED BY THE INSTALLER**.
    - During installation create the **zimbra** user account  
      (**MUST be zimbra, not your favorite account name**).  
      The account will be used both as the user for the Zimbra installation and the account for building Zimbra.

## Install and update required packages on the VM

Note: memcached and redis-server are not required components unless you want to use alwayson features

        $ sudo apt-get update  
        $ sudo apt-get install build-essential ant python-pip maven redis-server memcached
        
2. add and configure zimbra repositories  
To enable the repository on UBUNTU14, create the following file:

        /etc/apt/sources.list.d/zimbra.list
With the following contents:

        deb     [arch=amd64] https://repo.zimbra.com/apt/90 trusty zimbra
        deb-src [arch=amd64] https://repo.zimbra.com/apt/90 trusty zimbra

3. Run the following commands

        $ sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 9BE6ED79
        $ sudo apt-get install -y apt-transport-https
        $ sudo apt-get update

4. Install zimbra packages

        $ sudo apt-get install zimbra-openldap-server zimbra-openldap-client zimbra-rsync zimbra-openssl zimbra-openjdk zimbra-openjdk-cacerts zimbra-mariadb zimbra-tcmalloc-lib zimbra-openjdk zimbra-cyrus-sasl

5. Change owner of /opt/zimbra to "zimbra"

        $ sudo chown -R zimbra /opt/zimbra


## Configure your workspace environment on the VM

1. Add Helix (former Perforce) repository following instructions on this page:

        https://www.perforce.com/perforce-packages/helix-versioning-engine

2. Install helix-cli via apt

        $ sudo apt-get install helix-cli

3. Copy your SSH keys (public and private) to `$HOME/.ssh/` folder on the VM

4. Set up your environment variables (may be different depending on your office location and SSH set up)

   Add the following content to `$HOME/.profile`
````
export P4PORT=1066
export P4HOST=p4proxy.eng.zimbra.com
export P4USER={your p4 username}
export P4CONFIG=.p4config
export P4EDITOR=/usr/bin/vi
export PATH=/opt/zimbra/bin:/opt/zimbra/common/bin:$PATH:$HOME/bin
export ZIMBRA_HOSTNAME={your computer name}.local
alias ssh_p4='ssh -f -N p4'
alias ssh_web='ssh -f -N web'
alias ssh_rb='ssh -f -N rb'
alias ssh_all='ssh_p4; ssh_web; ssh_rb'
````

5. set up SSH configuration for accessing servers behind the firewall.  
Add the following content to `$HOME/.ssh/config`

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

6. load new environment settings and start SSH tunnel to perforce:

        $ source ~/.profile
        $ ssh_all

## Install VMWare tools
If you want to be able to edit files using an IDE on your Mac, you have to set up a shared folder on your Mac with read/write access for your Ubuntu VM. Follow instructions on VMWare website: http://kb.vmware.com/selfservice/microsites/search.do?language=en_US&cmd=displayKC&externalId=1022525

Once VMWare tools are installed, your Mac's shared folder will be mounted at:
	`/mnt/hgfs/`.  
You can use this path as your workspace or you can map it to another folder on the VM.

## Setup JDK 1.7

Download Sun JDK 1.7
Even though JUDASPRIEST branch runs on Java8, you need JDK 1.7 to compile JUDASPRIEST branch.  You can use either zimbra-openjdk or the Sun JDK:

### Sun JDK 1.7
- download and unpack the Sun JDK from the Oracle web site to your home folder.
- Move the folder under `/usr/lib/jvm/`.  e.g. If using Sun JDK 1.7.0 rev 79:

        export SUNJDK=jdk1.7.0_79
        sudo mv ${SUNJDK} /usr/lib/jvm/
        sudo update-alternatives --install "/usr/bin/java" "java" "/usr/lib/jvm/${SUNJDK}/bin/java" 1
        sudo update-alternatives --install "/usr/bin/javac" "javac" "/usr/lib/jvm/${SUNJDK}/bin/javac" 1
        sudo update-alternatives --install "/usr/bin/javaws" "javaws" "/usr/lib/jvm/${SUNJDK}/bin/javaws" 1

- Make it the default java
        sudo update-alternatives --config java
        There are 2 choices for the alternative java (providing /usr/bin/java).

          Selection    Path                                            Priority   Status
        ------------------------------------------------------------
        * 0            /usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java   1071      auto mode
          1            /usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java   1071      manual mode
          2            /usr/lib/jvm/jdk1.7.0_79/bin/java                1         manual mode

        Press enter to keep the current choice[*], or type selection number: 2

### zimbra-openjdk (not suitable for building JUDASPRIEST because is Java8)

If you are using zimbra-openjdk for compiling Zimbra Java code, add `/opt/zimbra/common/lib/jvm/java/bin` to your `$PATH`.  
You may also want to set `$JAVA_HOME` for other Java tools to work properly.

## Get the source code
You have to log in to perforce on the guest Ubuntu VM.

````
$ p4 login
* enter your password
$ p4 client
````
If you are configuring perforce client on your host (Mac), then make sure your workspace is mapped to the folder that you are sharing between the Ubuntu guest VM and Mac host. E.g.: /mnt/hgfs/ubuntuhome/p4
You may also want to create a simlink to /mng/hgfs/ubuntuhome on the Ubuntu VM to match the path on your Mac host. This will allow you to use the same perforce client spec on both machines. 

E.g., if the shared folder on your mac is /Users/gsolovyev/ubuntuhome, run the following on your Ubuntu VM:
````
$ sudo mkdir -p /Users/gsolovyev
$ sudo ln -s /mnt/hgfs/ubuntuhome /Users/gsolovyev/
$ sudo chown zimbra:zimbra /Users/gsolovyev
````
If the paths to your workspace on the Ubuntu VM and on your Mac do not match, use the path on the VM as the root of your workspace, because you will need to check out the code on the VM. 

````
Root:   /Users/gsolovyev/ubuntuhome/p4
````

Enter the following as the view contents:

````
//depot/zimbra/main/ant-global.xml //gregs-ubuntu-desktop/main/ant-global.xml
//depot/zimbra/main/pom.xml //gregs-ubuntu-desktop/main/pom.xml
//depot/zimbra/main/zimbra.l10n //gregs-ubuntu-desktop/main/zimbra.l10n
//depot/zimbra/main/mvn-local-jars.sh //gregs-ubuntu-desktop/main/mvn-local-jars.sh
//depot/zimbra/main/mvn-local-jars.sh //gregs-ubuntu-desktop/main/mvn-local-jars.sh
//depot/zimbra/main/ZimbraHSM/... //gregs-ubuntu-desktop/main/ZimbraHSM/...
//depot/zimbra/main/ZimbraHtmlExtras/... //gregs-ubuntu-desktop/main/ZimbraHtmlExtras/...
//depot/zimbra/main/ZimbraFreeBusyProvider/... //gregs-ubuntu-desktop/main/ZimbraFreeBusyProvider/...
//depot/zimbra/main/ZimbraAdminVersionCheck/... //gregs-ubuntu-desktop/main/ZimbraAdminVersionCheck/...
//depot/zimbra/main/ZimbraEws/... //gregs-ubuntu-desktop/main/ZimbraEws/...
//depot/zimbra/main/ZimbraLicensePortal/... //gregs-ubuntu-desktop/main/ZimbraLicensePortal/...
//depot/zimbra/main/ZimbraEwsCommon/... //gregs-ubuntu-desktop/main/ZimbraEwsCommon/...
//depot/zimbra/main/ZimbraLicenseExtension/... //gregs-ubuntu-desktop/main/ZimbraLicenseExtension/...
//depot/zimbra/main/ZimbraLicenseTools/... //gregs-ubuntu-desktop/main/ZimbraLicenseTools/...
//depot/zimbra/main/ZimbraLicenseHtmlExtras/... //gregs-ubuntu-desktop/main/ZimbraLicenseHtmlExtras/...
//depot/zimbra/main/ZimbraLDAPUtilsExtension/... //gregs-ubuntu-desktop/main/ZimbraLDAPUtilsExtension/...
//depot/zimbra/main/ZimbraNginxLookup/... //gregs-ubuntu-desktop/main/ZimbraNginxLookup/...
//depot/zimbra/main/ZimbraOpenOfficeExt/... //gregs-ubuntu-desktop/main/ZimbraOpenOfficeExt/...
//depot/zimbra/main/ZimbraPosixAccountsExtension/... //gregs-ubuntu-desktop/main/ZimbraPosixAccountsExtension/...
//depot/zimbra/main/ZimbraCharset/... //gregs-ubuntu-desktop/main/ZimbraCharset/...
//depot/zimbra/main/ThirdParty/... //gregs-ubuntu-desktop/main/ThirdParty/...
//depot/zimbra/main/ThirdPartyBuilds/... //gregs-ubuntu-desktop/main/ThirdPartyBuilds/...
//depot/zimbra/main/ZimbraServer/... //gregs-ubuntu-desktop/main/ZimbraServer/...
//depot/zimbra/main/ZimbraPerf/... //gregs-ubuntu-desktop/main/ZimbraPerf/...
//depot/zimbra/main/ZimbraPerf/data/dbrawdata/... //gregs-ubuntu-desktop/main/ZimbraPerf/data/dbrawdata/...
//depot/zimbra/main/ZimbraXMbxSearch/... //gregs-ubuntu-desktop/main/ZimbraXMbxSearch/...
//depot/zimbra/main/ZimbraCommon/... //gregs-ubuntu-desktop/main/ZimbraCommon/...
//depot/zimbra/main/ZimbraClient/... //gregs-ubuntu-desktop/main/ZimbraClient/...
//depot/zimbra/main/ZimbraBackup/... //gregs-ubuntu-desktop/main/ZimbraBackup/...
//depot/zimbra/main/ZimbraBuild/... //gregs-ubuntu-desktop/main/ZimbraBuild/...
//depot/zimbra/main/ZimbraArchive/... //gregs-ubuntu-desktop/main/ZimbraArchive/...
//depot/zimbra/main/ZimbraAdminExt/... //gregs-ubuntu-desktop/main/ZimbraAdminExt/...
//depot/zimbra/main/ZimbraConvertd/... //gregs-ubuntu-desktop/main/ZimbraConvertd/...
//depot/zimbra/main/ZimbraNative/... //gregs-ubuntu-desktop/main/ZimbraNative/...
//depot/zimbra/main/ZimbraNetwork/... //gregs-ubuntu-desktop/main/ZimbraNetwork/...
//depot/zimbra/main/ZimbraSoap/... //gregs-ubuntu-desktop/main/ZimbraSoap/...
//depot/zimbra/main/ZimbraSync/... //gregs-ubuntu-desktop/main/ZimbraSync/...
//depot/zimbra/main/ZimbraSyncClient/... //gregs-ubuntu-desktop/main/ZimbraSyncClient/...
//depot/zimbra/main/ZimbraSyncCommon/... //gregs-ubuntu-desktop/main/ZimbraSyncCommon/...
//depot/zimbra/main/ZimbraTagLib/... //gregs-ubuntu-desktop/main/ZimbraTagLib/...
//depot/zimbra/main/ZimbraWebClient/... //gregs-ubuntu-desktop/main/ZimbraWebClient/...
//depot/zimbra/main/Zimlet/... //gregs-ubuntu-desktop/main/Zimlet/...
//depot/zimbra/main/ZimbraSyncTools/... //gregs-ubuntu-desktop/main/ZimbraSyncTools/...
//depot/zimbra/main/ZimbraSync4j/... //gregs-ubuntu-desktop/main/ZimbraSync4j/...
//depot/zimbra/main/ZimbraSyncPerf/... //gregs-ubuntu-desktop/main/ZimbraSyncPerf/...
//depot/zimbra/main/ZimbraLogger/... //gregs-ubuntu-desktop/main/ZimbraLogger/...
//depot/zimbra/main/ZimbraSync4j/... //gregs-ubuntu-desktop/main/ZimbraSync4j/...
//depot/zimbra/main/ZimbraQA/... //gregs-ubuntu-desktop/main/ZimbraQA/...
//depot/zimbra/main/ZimbraLauncher/... //gregs-ubuntu-desktop/main/ZimbraLauncher/...
//depot/zimbra/main/OpenID/... //gregs-ubuntu-desktop/main/OpenID/...
//depot/zimbra/main/mvn-local-jars.sh //gregs-ubuntu-desktop/main/mvn-local-jars.sh
//depot/zimbra/main/pom.xml //gregs-ubuntu-desktop/main/pom.xml
//depot/zimbra/main/README.buildZCS //gregs-ubuntu-desktop/main/README.buildZCS
//depot/zimbra/main/Zimbra.ipr //gregs-ubuntu-desktop/main/Zimbra.ipr
//depot/zimbra/main/zimbra.l10n //gregs-ubuntu-desktop/main/zimbra.l10n
//depot/zimbra/main/ZIM/... //gregs-ubuntu-desktop/main/ZIM/...
//depot/zimbra/main/ZimbraIMExtention/... //gregs-ubuntu-desktop/main/ZimbraIMExtention/...
//depot/zimbra/main/SolrPlugins/... //gregs-ubuntu-desktop/main/SolrPlugins/...
//depot/zimbra/main/CrocoDocExt/... //gregs-ubuntu-desktop/main/CrocoDocExt/...
//depot/zimbra/main/ZimbraFOSS/... //gregs-ubuntu-desktop/main/ZimbraFOSS/...
-//depot/zimbra/main/ZimbraQA/ThirdParty/... //gregs-ubuntu-desktop/main/ZimbraQA/ThirdParty/...
-//depot/zimbra/main/ThirdPartyBuilds/MACOSXx86_10.7/... //gregs-ubuntu-desktop/main/ThirdPartyBuilds/MACOSXx86_10.7/...
-//depot/zimbra/main/ZimbraServer/src/windows/... //gregs-ubuntu-desktop/main/ZimbraServer/src/windows/...
-//depot/zimbra/main/ZimbraPerf/data/... //gregs-ubuntu-desktop/main/ZimbraPerf/data/...
-//depot/zimbra/main/ThirdPartyBuilds/MACOSXx86_10.6/... //gregs-ubuntu-desktop/main/ThirdPartyBuilds/MACOSXx86_10.6/...
-//depot/zimbra/main/ThirdPartyBuilds/OSXx86_64/... //gregs-ubuntu-desktop/main/ThirdPartyBuilds/OSXx86_64/...
-//depot/zimbra/main/ThirdPartyBuilds/x86_64/... //gregs-ubuntu-desktop/main/ThirdPartyBuilds/x86_64/...
-//depot/zimbra/main/ThirdPartyBuilds/RHEL6_64/... //gregs-ubuntu-desktop/main/ThirdPartyBuilds/RHEL6_64/...
-//depot/zimbra/main/ThirdPartyBuilds/RHEL7_64/... //gregs-ubuntu-desktop/main/ThirdPartyBuilds/RHEL7_64/...
-//depot/zimbra/main/ThirdPartyBuilds/wndows/... //gregs-ubuntu-desktop/main/ThirdPartyBuilds/windows/...
-//depot/zimbra/main/ThirdPartyBuilds/SLES11_64/... //gregs-ubuntu-desktop/main/ThirdPartyBuilds/SLES11_64/...
-//depot/zimbra/main/ThirdPartyBuilds/UBUNTU10_64/... //gregs-ubuntu-desktop/main/ThirdPartyBuilds/UBUNTU10_64/...
-//depot/zimbra/main/ThirdPartyBuilds/UBUNTU12_64/... //gregs-ubuntu-desktop/main/ThirdPartyBuilds/UBUNTU12_64/...
-//depot/zimbra/main/ZimbraPerf/data/dbrawdata/... //gregs-ubuntu-desktop/main/ZimbraPerf/data/dbrawdata/...
-//depot/zimbra/main/ThirdParty/nginx/... //gregs-ubuntu-desktop/main/ThirdParty/nginx/...
-//depot/zimbra/main/ZimbraNetwork/ZimbraImportWizard/... //gregs-ubuntu-desktop/main/ZimbraNetwork/ZimbraImportWizard/...
````

That view may have a lot more than you need, so you may want to consider explicitly listing
only what you need. Take a look at the clients of others in your group for examples.

````
$ cd ~/p4/main
$ p4 sync
````

## Initialize /opt/zimbra/ folder structure
Run the following command in ZimbraServer folder. This will install jetty and copy some scripts to /opt/zimbra/bin

````
$ ant -DskipTests=true install-thirdparty init-opt-zimbra
````

## Configure MariDB
1. Copy ZimbraServer/conf/mariadb/my.cnf to /opt/zimbra/conf/my.cnf

        cp ZimbraServer/conf/mariadb/my.cnf /opt/zimbra/conf/my.cnf


2. Create default database and tables

        mkdir -p /opt/zimbra/data/tmp
        /opt/zimbra/common/share/mysql/scripts/mysql_install_db --basedir=/opt/zimbra/common --datadir=/opt/zimbra/db/data --defaults-file=/opt/zimbra/conf/my.cnf --user=zimbra


3. Start mariadb:

        mkdir -p /opt/zimbra/log
        /opt/zimbra/bin/mysql.server start


4. Set password for MariaDB root user

        /opt/zimbra/common/bin/mysqladmin --socket=/opt/zimbra/data/tmp/mysql/mysql.sock -u root password zimbra


5. Reload permissions tables

        /opt/zimbra/common/bin/mysqladmin --socket=/opt/zimbra/data/tmp/mysql/mysql.sock -u root -p reload


6. Restart mariadb and make sure it is running:

        /opt/zimbra/bin/mysql.server restart
        /opt/zimbra/bin/mysql.server status


## Special sudo config for slapd and cacerts

Configure sudo to allow slapd (OpenLDAP) startup and cacerts modifications without a password:

````
$ sudo visudo
````

Place something similar to the following in the sudoers file:

````
zimbra ALL=NOPASSWD:/opt/zimbra/libexec/zmslapd
zimbra ALL=NOPASSWD:/bin/chmod a+w /opt/zimbra/java/lib/security/cacerts
````

## Deploy dev build
run the following command in ZimbraServer folder
````
$ ant -DskipTests=true reset-all
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

## Install Consul
This is an optional step. Consul is needed only for alwayson features.

Consul can be downloaded from <http://www.consul.io/downloads.html>

````
$ mkdir -p /opt/zimbra/common/sbin
$ cd /opt/zimbra/common/sbin
$ wget -nv https://dl.bintray.com/mitchellh/consul/0.5.2_linux_amd64.zip
$ unzip 0.5.2_linux_amd64.zip && rm 0.5.2_linux_amd64.zip && chmod 755 consul
````

Consul could be started with zmconsulctl (ZimbraServer/src/bin/zmconsulctl) with minor modifications.  For basic testing the following command may be sufficient:

````
$ /opt/zimbra/common/sbin/consul agent -server -bootstrap-expect 1 -log-level debug -data-dir /tmp/consul
````

## Integration with Eclipse and perforce on your Mac

After you follow the steps outlined above, you will be able to check-out/check-in code on your Ubuntu VM. That's cool, but it is not very convenient if you want to use Eclipse or any other IDE on your Mac.
To be able to manage files and use an IDE on your Mac, you will need to:

1. install and configure Perforce on your Mac
2. reconfigure your workspace "Root" to map to the path on your Mac e.g. `$HOME/ubuntuhome/p4` instead of `/mnt/hgfs/ubuntuhome/p4`
3. (bonus points) create a symlink to `/mng/hgfs/ubuntuhome` on the Ubuntu VM to match the path on your Mac host. This will allow you to use the same perforce client spec on both machines. 

    E.g., if the shared folder on your mac is `/Users/gsolovyev/ubuntuhome`, run the following on your Ubuntu VM:

        $ sudo mkdir -p /Users/gsolovyev
        $ sudo ln -s /mnt/hgfs/ubuntuhome /Users/gsolovyev/
        $ sudo chown zimbra:zimbra /Users/gsolovyev
