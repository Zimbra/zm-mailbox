Setup Development Environment for Ubuntu
========================================

## Vagrant

Note, if you are familiar with [Vagrant][], you may find <https://github.com/plobbes/vagrant-provision-zimbra/> useful in helping to jumpstart the setup of a development (full ZCS server development) or even simpler build/packaging environment (for deb/rpm packages).

[Vagrant]: https://www.vagrantup.com/

## Sudo

Make sure you have sudo privileges, see [Ubuntu Sudo docs][] for assistance.

[Ubuntu Sudo docs]: https://help.ubuntu.com/community/RootSudo#Allowing_other_users_to_run_sudo

## Install & Configure P4 (Perforce Command-Line Client)

Download from [Perforce Downloads][].

[Perforce Downloads]: https://www.perforce.com/downloads/

Clients -> P4: COMMAND-LINE CLIENT -> Linux -> Linux 2.6 for for 64-bit Intel (x64)

````
$ sudo mkdir -p /usr/local/bin
$ sudo mv p4 /usr/local/bin
$ chmod +x /usr/local/bin/p4
````

You may also want to download and install P4V, a visual Perforce client.

More information is available at <http://www.perforce.com/perforce/doc.current/manuals/p4guide/chapter.install.html>

## Environment

Set up environment variables.

````
$ vi ~/.bash_profile
````

Add content similar to the following, substituting appropriate values for the variables below:

````
export P4PORT=${p4_server}:${p4_port}
export P4USER=${p4_username}
export P4CONFIG=.p4config
export P4EDITOR=/usr/bin/vi
export PATH=$PATH:/opt/zimbra/bin
export ZIMBRA_HOSTNAME={your computer name}.local
````

The Zimbra engineering "welcome kit" wiki contains details on how to setup SSH tunnels to access perforce if necessary.

Pick up the new environment settings (and startup any VPN or SSH tunnels as necessary) and create /opt/zimbra with permissions so that you can work in that directory as yourself:

````
$ source ~/.bash_profile
$ sudo mkdir -p /opt/zimbra
$ sudo chown "$USER" /opt/zimbra  # could instead use perms of 1777
````

## Workspace / P4 Client

Create a workspace folder (p4 client) for "main" branch and login to perforce:

````
$ mkdir -p ~/p4/main 
$ cd ~/p4/main
$ p4 login           # enter your password when prompted
$ p4 client          # see recommended view contents below...
````

Enter the following as the view contents (see perforce documentation on configuring the p4 client if needed):

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

This view may have a lot more than you need, so you may want to consider explicitly listing only what you need (if you are able to determine that). Take a look at the clients of others in your group for examples.

Sync the workspace (this may take a while):

````
$ cd ~/p4/main
$ p4 sync
````

## Install/Configure MariaDB

Install MariaDB using apt:
* when prompted for the "admin" password, enter "zimbra"
* change the listening port from 3306 to 7306
* change the location of datadir
* copy data from the old datadir to the new one
* create a link to the default mysqld.sock

````
$ mkdir -p /opt/zimbra/mysql/data
$ sudo apt-get install mariadb-server
$ sudo service mysql stop
$ sudo perl -pi -e "s,3306,7306,g" /etc/mysql/my.cnf
$ sudo perl -pi -e 's,/var/lib/mysql,/opt/zimbra/mysql/data,g if /^datadir/' /etc/mysql/my.cnf
$ sudo cp -pr /var/lib/mysql/* /opt/zimbra/mysql/data/
$ ln -s /var/run/mysqld/mysqld.sock /opt/zimbra/mysql/data/mysqld.sock
````

If apparmor is enabled for mariadb (off by default), also edit /etc/apparmor.d/usr.sbin.mysqld and add lines similar to the following where appropriate:

````
  /opt/zimbra/mysql/data/ r,
  /opt/zimbra/mysql/data/** rwk,
````

Restart mariadb and trouble shoot if it fails:

````
$ sudo service mysql start
````

## Install Consul

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

## Install Java / OpenJDK 8

Add a repo for openjdk and then install version 8 (Java JDK 1.8)

````
$ sudo add-apt-repository -y ppa:openjdk-r/ppa
$ sudo apt-get update -qq
$ sudo apt-get install openjdk-8-jdk
$ sudo update-java-alternatives -s java-1.8.0-openjdk-amd64
````

## Install Ant / Maven

````
$ sudo add-apt-repository -y ppa:andrei-pozolotin/maven3
$ sudo apt-get update -qq
$ sudo apt-get install ant maven3
````

## Install Redis / Memcached

````
$ sudo apt-get install redis-server memcached
````

## Special sudo config for slapd and cacerts

Configure sudo to allow slapd (OpenLDAP) startup and cacerts modifications without a password:

````
$ sudo visudo
````

Place something similar to the following in the sudoers file (replace {username} with your username):

````
{username} ALL=NOPASSWD:/opt/zimbra/libexec/zmslapd
{username} ALL=NOPASSWD:/bin/chmod a+w /opt/zimbra/java/lib/security/cacerts
````

## Attempt to build ZCS

````
$ cd ~/p4/main/ZimbraServer
$ mvn compile      # verify compiliation works, then test...
$ ant reset-all    # or may need to use: ant -DskipTests=true reset-all
````

Often, the very first reset-all fails on tests, so you may need to run with -DskipTests=true to bootstrap the build/test process.

Also, while it should generally not be necessary; if you happen to remove your Maven local repository (e.g. by deleting ~/.m2/repository) you will also want to remove ZimbraServer/mvn.seed.properties; as this is used by Ant to determine which bootstrap jars have been installed in earlier Ant runs.

You may also run the mvn-local-jars shell script to populate the local repository with these jars.

Build targets from build.xml can be viewed with:

````
$ ant -p
````

Some commonly used targets include:

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

## Test Running System

Login to WebMail. Open <http://localhost:7070/zimbra>

* Username: user1
* Password: test123

Login to Admin Console. Open <https://localhost:7071/zimbraAdmin>

* Username: admin
* Password: test123

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
