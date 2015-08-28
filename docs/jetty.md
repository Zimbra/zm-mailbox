# Jetty
`Jetty` is the open source servlet container used to run Java web applications which make up the Zimbra platform. The Zimbra Jetty process is also refered to as `mailboxd`. Backing services such as Solr may run separate Jetty processes internally, however this document covers `mailboxd` only. In a single server installation all of the Zimbra webapps run in a single Jetty container. In a multi-node environment each node may run one or more webapps, and each node which runs at least one webapp will implicitly include a Jetty/mailboxd instance.

## Configuration

Jetty configuration is primarily XML based. The files are kept in the ZimbraServer/conf/jetty directory. For many of these files, there is a 'dev' version and a 'production' version. The dev version is hard coded with settings for a development environment and the production version contains variable tokens which are populated by `zmconfigd` from values set in `zmprov` and/or `zmlocalconfig`. 

- jetty.xml - Primary configuration file. Contains TCP ports, SSL settings, thread pool sizes, buffer sizes, etc. [Jetty XML Config](http://www.eclipse.org/jetty/documentation/current/jetty-xml-config.html)
- jetty-setuid.xml - Contains parameters used during startup when Jetty runs as root to bind privileged ports, set ulimit, and other privileged operations.
- jettyrc - Jetty startup parameters including Java options and module lists
- webdefault.xml - Default webapp configuration. Zimbra keeps most of the defaults shipped with Jetty, but disables JSP compilation at the global level and reenables in the webapps that specifically need it.
- modules/*.mod - Extensions and modifications to the Jetty module system. Used to deploy utility classes related to privileged port binding into the container via a new Jetty module, and to provide a configuration template for same. Also used to disable default Jetty configuration for modules such as rewrite and deploy; so the classes are available but configuration from our jetty.xml is used instead of jetty-rewrite.xml and jetty-deploy.xml.
- **Note that other jetty-xyz.xml files may appear in the distribution but are not used**
- start.d - Module startup parameters. Based on the configuration template specififed in the modules subdirectory.

## Dev Setup

1. If this is the first time you setup your dev env, follow steps in
   INSTALL*.txt to setup /opt/zimbra with mysql, ldap, etc.

2. You can run "ant reset-all" target of ZimbraServer project to build
   and start up the dev webapps including serivce/zimbra/zimbraAdmin.

3. On Windows if you want to install jetty as a Windows service and
   start/stop it that way, skip to 4.  For those who don't want to mess
   with Windows services, use ant targets "start-jetty" and "stop-jetty"
   of ZimbraServer project to start/stop.

4. On Windows if you really want to install jetty as a Windows service
   you can use the Java Service Wrapper.  The files are checked in
   under ZimbraServer\tools\jetty; if you'd prefer to download them
   yourself you can Yahoo! search for wrapper-windows-x86-32-3.2.3.zip
   and unzip it.  Copy wrapper.exe to C:\opt\zimbra\bin, wrapper.jar and
   wrapper.dll to C:\opt\zimbra\lib, and wrapper.conf to
   C:\opt\zimbra\jetty\etc.  Now run:

     wrapper.exe -i C:\opt\zimbra\jetty\etc\wrapper.conf 

   to install jetty as a Windows Service.  Make sure the 'C' in 'C:' is
   in upper case because wrapper.exe surprisingly is sensitive to it.

   You may need to fix the permissions on the jetty directory and its
   subdirectories in order to run jetty as a service.  Open Windows 
   Explorer.  Right click jetty directory, click Sharing and Security...
   Click Security tab.  Check if you have SYSTEM user in the list.
   If there is no SYSTEM user, click Add button, type SYSTEM, 
   click Check Names button, select the System user (usually the 
   first one), click OK.  Now select the newly added SYSTEM user, click
   all the permissions to Allow.  Then click Advanced button, and check
   "Replace permission entries on all child objects with entries shown
   here that apply to child objects".  Click OK to dismiss advanced
   dialog.  Click OK to dismiss the sharing dialog.

   To start/stop jetty service on command line, use "net start jetty"
   and "net stop jetty".

   To make sure that build.xml also uses windows service to start/stop
   jetty, set environment variable ZIMBRA_JETTY_USE_SERVICE=1.

5. On Mac OS X or Linux you should be able to bounce jetty with
   command line 'jetty start' and 'jetty stop'.

## Debugging

There are more than one way to debug jetty using Eclipse.  Here we'll
cover three approaches. The suggested approach is to use the Maven Jetty
plugin. However, the old instructions for Conventional and Ant debugging
are left for reference.

### Maven Jetty plugin (Tested with Eclipse Luna)

1. Setup ZimbraServer and related projects in Eclipse

2. Install the m2e and m2e-dynamic-sources-lookup plugins

3. Right click on the ZimbraServer project; select Debug As...Debug configurations

4. Create a new Maven Build configuration

5. Enter 'jetty:run' in the Goals field

6. Click Apply, then Debug. Note that startup may be a bit slow due to annotation scanning; may be optimized once we upgrade to Jetty 9.2+


### (deprecated) Conventional Debugging

1. Don't follow the instructions in "Debugging Jetty with Eclipse" on
   jetty docs site (at least it didn't work for me).

2. In Eclipse select Run/Debug... and right click on Java Application
   to "New" a configuration.  Call it anything you want, such as
   "jetty".  Choose the ZimbraServer as the debug project. Set the main
   class to org.eclipse.jetty.start.Main.

3. Switch to the Arguments tab and set

   Program arguments to 
   --ini OPTIONS=Server,servlet,servlets,jsp,jmx,resources,websocket,ext,plus,rewrite,monitor etc/jetty.xml
   
   and VM arguments to (without quotes)
   "-DSTART=/opt/zimbra/jetty/etc/start.config -DSTOP.PORT=7867
   -DSTOP.KEY=stop -Dzimbra.config=/opt/zimbra/conf/localconfig.xml
   -Djava.library.path=/opt/zimbra/lib  -XX:PermSize=128m
   -XX:MaxPermSize=350m". Then choose "Other" under
   working directory and set it to /opt/zimbra/jetty.

4. Swtich to the Classpath tab and add the following jar files to User Entries.
   /opt/zimbra/jetty/start.jar
   /opt/zimbra/jetty/lib/monitor/jetty-monitor*.jar

5. Switch to the Source tab and add all the java projects in your
   workspace that you care about to debug.

7. Apply the debug configuration and click Debug.  If everything goes
   well, you should be able to stop in your source code with break
   points.

8. To stop jetty you can do this on command line "java
   -DSTOP.PORT=7867 -DSTOP.KEY=stop -jar start.jar --stop".  Or you
   could run the "stop-jetty-java" target under ZimbraServer.  You
   probably don't want to press the Stop button in Eclipse debugger as
   that wouldn't do a proper shutdown.


### (deprecated) Ant Jetty Plugin

There's another radically different way to run and debug jetty with
jetty-ant plugin.  Basically you run jetty as an ant task.  We use
Eclipse remote debugging with jetty-ant plugin.

To use jetty-ant, first you need to setup a shell for service webapp.
Go to ZimbraServer directory and run
ant -buildfile jetty-ant.xml jetty.webinf

You'll only need to do this once unless either you clean the build directory
or changes things such as web.xml.

To launch jetty, run the following:
ant -buildfile jetty-ant.xml jetty.run

Note that due to a jetty-ant-plugin bug you need to set this env var:
ANT_OPTS="-Djava.library.path=~/zimbra/zdesktop/lib"

To debug, run the jetty.run target with ANT_OPTS like this:

ANT_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Djava.library.path=/opt/zimbra/lib -Xrunjdwp:transport=dt_socket,address=4000,server=y,suspend=y" ant -buildfile jetty-ant.xml jetty.run

This will start jetty-ant plugin but blocks on listening on port 4000.

If you run into Out Of Memory errors, you may also want to increase the memory size by adding PermSize and MaxPermSize options.

The command line will look like this with additional memory allocation options:

ANT_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Djava.library.path=/opt/zimbra/lib -Xrunjdwp:transport=dt_socket,address=4000,server=y,suspend=y, -XX:PermSize=64M -XX:MaxPermSize=256M" ant -buildfile jetty-ant.xml jetty.run 

Now go to Eclipse and select Run/Debug... and right click on Remote
Java Application to "New" a configuration.  Call it anything you want
such as "jetty-ant".  Select any project as the debug project.  Set
Host/Port to localhost and 4000.  Then go to Source tab and add all
the projects you are interested in debugging.

Apply the configuration settings and press "Debug".  This will attach
the debugger to the jetty-ant process.  You can see activity in the
previously blocked jetty-ant console, and debugger will stop at your
break points.

To shutdown jetty, simply send Ctrl+C to the console window where you
are running jetty-ant.

## Upgrading
Upgrading to a new micro version of Jetty is typically a straightforward process. However, upgrading to a new minor or major version often requires more attention. This is often a trial and error process, however here are few hints that can save time.

- Many problems occur due to changes in the syntax or object structure in jetty.xml. It often helps to review the default configuration files shipped with Jetty to see if they have changed between versions; and then determine if those changes are used by the Zimbra jetty.xml.
- Another source of problems is changes in the module structure. The list of modules Zimbra uses is specified in jettyrc; and may need to change if jetty adds new modules or changes existing module names.
- It sometimes helps to deploy the Zimbra web applications in a vanilla Jetty installation and gradually alter the configuration to get to a working state. This usually involves enabling a few additional modules to resolve class not found exceptions. Once Jetty is running on port 8080 other Zimbra changes can be added incrementally to find which change is causing the problem.
- If a bug is suspected in Jetty, it is usually helpful to isolate the problem into a simple testcase; for example a small webapp which illustrates the problem without needing an entire Zimbra server.
- It may be possible to move the zimbra-specific configration into a new Jetty 'base' directory instead of overwriting the default distribution. If a future upgrade gets messy it may worth doing this to more cleanly separate out the customizations.
