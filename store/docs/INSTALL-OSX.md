Setup Development Environment for Mac OSX
=========================================

IMPORTANT: 
These instructions are obsolete and will not work with source code newer than November 17th, 2015


## Install Homebrew

Install the [Homebrew](http://brew.sh) package manager for OS X.

## Install & Configure P4 (Perforce Command-Line Client)

Download from <http://www.perforce.com/downloads/Perforce/Customer>.

Perforce Clients -> P4: Command-line client ->
Macintosh -> Mac OS X 10.5 for x86_64

````
$ sudo mkdir -p /usr/local/bin
$ sudo mv p4 /usr/local/bin
$ chmod +x /usr/local/bin/p4
````

You may also want to download and install P4V, a visual Perforce client.

More information is available at <http://www.perforce.com/perforce/doc.current/manuals/p4guide/01_install.html#1070774>

````
$ vi ~/.bash_profile
````

Add the following contents:

````
export P4PORT={the p4 server}:1066
export P4USER={your p4 username}
export P4CONFIG=.p4config
export P4EDITOR=/usr/bin/vi
export PATH=$PATH:/opt/zimbra/bin
export ZIMBRA_HOSTNAME={your computer name}.local
````

````
$ source ~/.bash_profile
$ sudo mkdir -p /opt/zimbra
$ mkdir -p ~/p4/main
$ p4 login
* enter your password
$ p4 client
````

Enter the following as the view contents:

````
//depot/zimbra/main/... //{workspace}/...
-//depot/zimbra/main/ThirdParty/... //{workspace}/ThirdParty/...
-//depot/zimbra/main/ThirdPartyBuilds/... //{workspace}/ThirdPartyBuilds/...
-//depot/zimbra/main/ZimbraAppliance/... //{workspace}/ZimbraAppliance/...
-//depot/zimbra/main/ZimbraDocs/... //{workspace}/ZimbraDocs/...
-//depot/zimbra/main/Prototypes/... //{workspace}/Prototypes/...
-//depot/zimbra/main/Support/... //{workspace}/Support/...
-//depot/zimbra/main/Gallery/... //{workspace}/Gallery/...
-//depot/zimbra/main/ZimbraSupportPortal/... //{workspace}/ZimbraSupportPortal/...
-//depot/zimbra/main/ZimbraQA/data/... //{workspace}/ZimbraQA/data/...
-//depot/zimbra/main/ZimbraPerf/data/... //{workspace}/ZimbraPerf/data/...
````

That view may have a lot more than you need, so you may want to consider explicitly listing
only what you need. Take a look at the clients of others in your group for examples.

````
$ cd ~/p4/main
$ p4 sync
````

## Install MariaDB

````
$ brew install mariadb
$ sudo ln -s /usr/local/opt/mariadb /opt/zimbra/mysql
$ sudo ln -s /usr/local/opt/mariadb /opt/zimbra/mariadb
$ sudo chown {username} /opt/zimbra
$ sudo vi /usr/local/etc/my.cnf
````

````
[client-server]
port = 7306
socket = /opt/zimbra/data/tmp/mysql/mysql.sock
````

````
$ mysql.server restart
$ /opt/zimbra/mariadb/bin/mysqladmin -S /opt/zimbra/data/tmp/mysql/mysql.sock -u root password zimbra
````

## Install Memcached

````
$ brew install memcached
$ ln -sfv /usr/local/opt/memcached/*.plist ~/Library/LaunchAgents
$ launchctl load ~/Library/LaunchAgents/homebrew.mxcl.memcached.plist
````

## Install JDK

Install both JDK 1.7 and JDK 1.8 from Oracle if not already present on your system. JDK 1.7 is used for compilation while JDK 1.8 is used at runtime.

Set the JDK 1.7 installation as your default using the JAVA_HOME environment variable.

$ export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.7.0_79.jdk/Contents/Home

Create a symlink from /opt/zimbra/java to the JDK 1.8 installation.

$ sudo ln -s /Library/Java/JavaVirtualMachines/jdk1.8.0_51.jdk/Contents/Home/jre /opt/zimbra/java


## Configure OpenLDAP

````
$ sudo visudo
{username}	ALL=NOPASSWD:/opt/zimbra/libexec/zmslapd
````

{username} is your local username. Be sure to insert a [TAB] between {username} and "ALL".


---


## Give nopasswd access to cacerts //required for importing the self signed jetty certificate in truststore.

````
$ sudo visudo
{username}  ALL=NOPASSWD:/bin/chmod a+w /opt/zimbra/java/lib/security/cacerts
````

{username} is your local username. Be sure to insert a [TAB] between {username} and "ALL".


---

FOSS Edition - Build & Deploy
=============================

````
$ cd ~/p4/main/ZimbraServer
$ mkdir ~/p4/main/ZimbraWebClient/WebRoot/help
$ ant reset-all
$ ant -p
Buildfile: build.xml

Main targets:

   build-init              Creates directories required for compiling
   clean                   Deletes classes from build directories
   clean-opt-zimbra        Deletes deployed jars, classes, and zimlets
   dev-dist                Initializes build/dist
   dir-init                Creates directories in /opt/zimbra
   init-opt-zimbra         Copies build/dist to /opt/zimbra
   reset-all               Reset the world plus jetty and OpenLDAP
   reset-jetty             Resets jetty
   reset-open-ldap         Resets OpenLDAP
   reset-the-world         Reset the world
   reset-the-world-stage1  Cleans deployed files, compiles, and initializes /opt/zimbra.
   reset-the-world-stage2  Run when web server is running.
   service-deploy          Not just war file deployment, but a /opt/zimbra refresh as well!
   stop-webserver          Stops Jetty.  If Jetty is not installed, does nothing.
   test                    Run unit tests
  Default target: jar
````

## Test Running System

Login to WebMail. Open <http://localhost:7070/zimbra>

* Username: user1
* Password: test123

Login to Admin Console. Open <https://localhost:7071/zimbraAdmin>

* Username: admin
* Password: test123


---


NETWORK EDITION - Build & Deploy
================================

````
$ cd ~/p4/main/ZimbraServer
$ ant reset-all
 
$ cd ~/p4/main/ZimbraLicenseExtension
$ ant deploy-dev
 
$ cd ~/p4/main/ZimbraNetwork
$ ant dev-deploy
````

Many of the admin functions require that you deploy admin extensions.  These may require additional paths in your P4 workspace and additional services be deployed for those extensions to function correctly. ymmv.

Example for deploying an individual admin extension (ex. delegated admin extension):

````
$ cd ~p4/main/ZimbraNetwork/ZimbraAdminExt
$ ant -Dext.name=com_zimbra_delegatedadmin -Dext.dir=DelegatedAdmin deploy-zimlet
````
