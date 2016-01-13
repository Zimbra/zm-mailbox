Setup Development Environment for Mac with a Ubuntu Server VM
=============================================================

These instructions describe how to set up a development environment on an OSX machine with VMWare Fusion. 
Please note, that Zimbra does not run on OSX natively. Therefore, in order to run Zimbra, you need to have a Linux VM. However, you can still
use Eclipse or IntelliJ and p4v on your Mac.


## Make sure your version of Fusion is current (recommended)

1. Start Fusion
2. Go to VMware Fusion / Check for Updates ...
3. Update to the latest version if applicable

The current version as of Jan 2016 is 7.1.3. If you're on an early version of Fusion 7, eg 7.0.1, you won't be able to share folders between
your Mac and your Ubuntu VM due to a bug in the sharing module of VMware Tools.


## Create a Fusion VM with Ubuntu 14.04

1. Download Ubuntu 14.04 Server ISO file from <http://releases.ubuntu.com/14.04/>. You'll want the 64-bit server install image.

2. Create the VM:

    - Start Fusion
    - File|New
    - Select "Install from disc or image", click Continue
    - Browse to the downloaded Ubuntu image in Finder, click Open
    - Uncheck "User Easy Install", click Continue
    - A VM window and a settings window will pop up. Next, we adjust settings.
    - Go to "Processors & Memory". Select "2 processor cores" and 4096MB memory.
    - Click the "play" arrow in the VM window. Now it's time to install Ubuntu.
    - Hit Enter to accept English as the language for the install
    - Hit Enter to begin the install. Keep hitting Enter to accept defaults until you get to "Set up users and passwords".
    - Create the account Zimbra/zimbra/zimbra (you will be asked to pick a better password, select No)
    - During the install, accept defaults except:
    	- Select Yes when asked to install LVM
    	- Make sure it sets aside at least 20G of disk space
    	- Select Yes to write changes
    - When you get to "Software selection", hit Space to select "OpenSSH server", then Enter to continue. **DO NOT** install any
    other packages.

3. Ubuntu has been installed and booted. Login with zimbra/zimbra. The account 'zimbra' is the only one you should need in your VM.

To regain control of your mouse from the VM, type Ctrl-Cmd. At this point it would also be a good idea to find and write down your
VM's IP address. To find it, run the command

    $ ifconfig -a

and look for the address for eth0.


## Install and update required packages on the VM

1. Update apt:

        $ sudo apt-get update
        $ sudo apt-get install build-essential ant python-pip
        
2. Add and configure zimbra-related package repositories. To enable the repository on Ubuntu, create the file `/etc/apt/sources.list.d/zimbra.list` with the following contents:

        deb     [arch=amd64] https://repo.zimbra.com/apt/87 trusty zimbra
        deb-src [arch=amd64] https://repo.zimbra.com/apt/87 trusty zimbra

You can use `sudo vi` to edit the file.

3. Run the following commands:

        $ sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 9BE6ED79
        $ sudo apt-get install -y apt-transport-https
        $ sudo apt-get update

4. Install zimbra packages:

        $ sudo apt-get install zimbra-openldap-server zimbra-openldap-client zimbra-rsync zimbra-openssl zimbra-openjdk zimbra-openjdk-cacerts zimbra-mariadb zimbra-tcmalloc-lib

5. Change owner of `/opt/zimbra` to "zimbra":

        $ sudo chown -R zimbra /opt/zimbra


## Configure your workspace environment on the VM

1. Add Helix (Perforce) repository by following the instructions for "How to Configure": <https://www.perforce.com/perforce-packages/helix-versioning-engine>

Follow the instructions for APT. The {distro} is 'trusty'.

2. Install helix-cli via apt:

        $ sudo apt-get install helix-cli

3. Copy your SSH keys (public and private) to `/home/zimbra/.ssh/` on the VM. You may want to copy your local `.ssh` directory over.
Your keys are in `id_rsa` and `id_rsa.pub`, but it might be handy to also have the `config` and `known_hosts` files. (Note: I was not able to get
copy/paste working, so I used ftp via an external domain of mine.) You should now be able to use `scp` to copy files between your Mac and
the VM.

[ TODO: If you get copy/paste working, please edit this document with instructions. ]

4. Set up your environment variables (may be different depending on your office location and SSH set up):

Add the following content to `/home/zimbra/.profile`:

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

The `ssh_p4` alias is the only one you really need. The other two are only needed if you want to browse *.eng.zimbra.com from your VM, or post reviews from it.

5. Set up SSH configuration for accessing servers behind the firewall (if you haven't copied over your `~/.ssh/config` file):

Add the following content to `/home/zimbra/.ssh/config`:

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

6. Load new environment settings and start SSH tunnel to perforce:

        $ source ~/.profile
        $ ssh_all


## Install VMWare Tools

If you want to be able to edit files on your Mac, you have to set up a shared folder on your Mac with read/write access for your Ubuntu VM.
Follow the instructions at <http://kb.vmware.com/selfservice/microsites/search.do?language=en_US&cmd=displayKC&externalId=1022525>.

Once VMWare Tools is installed, your Mac's shared folder will be mounted at `/mnt/hgfs/`. You can use this path as your workspace or you can
map it to another folder on the VM.


## Set up JDK 1.7 (JUDASPRIEST)

0. Note: Instead of steps 1-4 below, you may be able to install Java7 much more simply by running the command:

        $ apt-get install openjdk-7-jdk

[ TODO: If you try this and it works, please update this document by removing steps 1-4 below. ]

1. Download Sun JDK 1.7 from <http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html>. You'll want the 64-bit Linux
version in tar.gz form.

2. Copy the file from your Mac to the VM (the exact version may vary):

        scp ~/Downloads/jdk-7u79-linux-x64.gz zimbra@{VM IP address}:/home/zimbra

3. On the VM, move the folder under `/usr/lib/jvm/`.  For example, if using Sun JDK 1.7.0 rev 79:

        export SUNJDK=jdk1.7.0_79
        sudo mv ${SUNJDK} /usr/lib/jvm/
        sudo update-alternatives --install "/usr/bin/java" "java" "/usr/lib/jvm/${SUNJDK}/bin/java" 1
        sudo update-alternatives --install "/usr/bin/javac" "javac" "/usr/lib/jvm/${SUNJDK}/bin/javac" 1
        sudo update-alternatives --install "/usr/bin/javaws" "javaws" "/usr/lib/jvm/${SUNJDK}/bin/javaws" 1

4. Make it the default java:

        sudo update-alternatives --config java

        There are three choices for the alternative java (providing /usr/bin/java). Choose #2:

          Selection    Path                                            Priority   Status
        ------------------------------------------------------------
        * 0            /usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java   1071      auto mode
          1            /usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java   1071      manual mode
          2            /usr/lib/jvm/jdk1.7.0_79/bin/java                1         manual mode

        Press enter to keep the current choice[*], or type selection number: 2


### Set up zimbra-openjdk (main, or any branch based on Java8)

If you are using zimbra-openjdk for compiling Zimbra Java code, add `/opt/zimbra/common/lib/jvm/java/bin` to your `$PATH`.
You may also want to set `$JAVA_HOME` for other Java tools to work properly.


## Get the source code

1. Create one or more Perforce clients. You could do that on your VM, but since clients (workspace specifications) are stored on the Perforce server,
it's much easier to create it on your Mac. The simplest way is to use P4V to create a new client based on an existing client, for example the one you
use for JUDASPRIEST. The new client could be named something like "{myusername}-ubuntu-judaspriest". You'll want to update the base directory to the
location on your VM in your shared folder, for example `/mnt/hgfs/ubuntuhome/p4`. You'll also need to either update or remove the host constraint.

2. Login to Perforce on your VM, set the client, and sync down the code:

        $ p4 login
        $ export P4CLIENT={myusername}-ubuntu-judaspriest
        $ p4 sync

Note: The sync will take a while. Though the files are already there, according to the client spec they are not, so everything will be synced.


## Install Jetty and scripts

Run the following command in ZimbraServer folder. This will install jetty and copy some scripts to `/opt/zimbra/bin`.

        $ ant install-thirdparty init-opt-zimbra


## Configure MariaDB

1. Copy the config file:

        $ cp ZimbraServer/conf/mariadb/my.cnf /opt/zimbra/conf/my.cnf


2. Create default database and tables:

        $ mkdir -p /opt/zimbra/data/tmp
        $ /opt/zimbra/common/share/mysql/scripts/mysql_install_db --basedir=/opt/zimbra/common --datadir=/opt/zimbra/db/data --defaults-file=/opt/zimbra/conf/my.cnf --user=zimbra


3. Start mariadb:

        $ mkdir -p /opt/zimbra/log
        $ /opt/zimbra/bin/mysql.server start


4. Set password for MariaDB root user:

        $ /opt/zimbra/common/bin/mysqladmin --socket=/opt/zimbra/data/tmp/mysql/mysql.sock -u root password zimbra

5. Reload permissions tables:

        $ /opt/zimbra/common/bin/mysqladmin --socket=/opt/zimbra/data/tmp/mysql/mysql.sock -u root -p reload

(You'll need to enter the root password 'zimbra'.)

6. Restart mariadb and make sure it is running:

        $ /opt/zimbra/bin/mysql.server restart
        $ /opt/zimbra/bin/mysql.server status


## Deploy dev build

Run the following command in ZimbraServer folder

        $ ant reset-all

(You may get asked partway through for the `sudo` password.)


## Configure localconfig

1.Find out your zimbra user's uid and gid either by looking at /etc/passwd or by running

        $ sudo id -u zimbra
        $ sudo id -g zimbra

2. Edit localconfig value zimbra_id to match your zimbra user's uid. e.g., if your zimbra user's uid is 1000 and gid is 1000, which is what it would normally be if this is the first and only user account.

        $ zmlocalconfig -e zimbra_uid=1000
        $ zmlocalconfig -e zimbra_gid=1000


## Integration with your IDE (Eclipse or IntelliJ IDEA) and Perforce on your Mac

After you follow the steps outlined above, you will be able to check-out/check-in code on your Ubuntu VM. That's cool, but it is not very convenient if you want to use an IDE on your Mac.
To be able to manage files and use an IDE on your Mac, you will need to:

1. Install and configure Perforce on your Mac
2. Reconfigure your workspace "Root" to map to the path on your Mac e.g. `$HOME/ubuntuhome/p4` instead of `/mnt/hgfs/ubuntuhome/p4`
3. (bonus points) create a symlink to `/mng/hgfs/ubuntuhome` on the Ubuntu VM to match the path on your Mac host. This will allow you to use the same perforce client spec on both machines. 

    E.g., if the shared folder on your mac is `/Users/gsolovyev/ubuntuhome`, run the following on your Ubuntu VM:

        $ sudo mkdir -p /Users/gsolovyev
        $ sudo ln -s /mnt/hgfs/ubuntuhome /Users/gsolovyev/
        $ sudo chown zimbra:zimbra /Users/gsolovyev


## Summary and Overview

For those who are used to doing everything on their Mac via localhost, if you are able to share folders between your Mac and VM, the only things that have to live in your VM are your SSH keys
and the deployed instance of Zimbra under `/opt/zimbra`. The only commands you'll need to run in the VM are `ant` commands such as `ant sync` or `ant deploy`, in order to update the deploy area.
If you run those on your Mac, then `/opt/zimbra` on your Mac will be updated, but that's not where your server is running.

To test your server, go to

        https://{your VM IP}:7070

If you try to load in dev mode with ?dev=1 and the load doesn't finish due to HTTP 429 errors, you have run into the server's
DOS throttling protection - it senses a flood of requests and stops serving them. To fix that, either raise the limit or add
an exemption for your server:

        $ zmprov mcf zimbraHttpDosFilterMaxRequestsPerSec 200
        $ zmprov mcf +zimbraHttpThrottleSafeIPs {your VM IP}

Surprisingly, you may still get 429 errors, but setting those config values helps. The client will typically load without errors if you try again.

Your VM will not work while you are using a VPN (like the Synacor one) that does not support split tunneling. You will not be able to SSH into it,
or even ping it. Even the loopback address 127.0.0.1 will not be available. You'll need to disconnect from the VPN to use your VM.

As far as I can tell, you only get one VM window. But you can ssh into your VM from Mac terminals, with the bonus that copy/paste will work. Make sure
you're not connected to the Synacor VPN, as that will disable Mac/VM networking. On your Mac, you might find it useful to add this to your ~/.ssh/config:

    Host ubuntu
      Hostname {your VM IP}
      User zimbra

Then on your Mac, you can just start up a terminal window and run

        % ssh ubuntu

and you'll have a VM session.



