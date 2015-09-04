#Maven

The Maven build system is used to compile and package Zimbra Java modules. This document describes Zimbra specific details. Maven is called indirectly during full product builds invoked via Ant or make, and can be used to develop and debug individual modules.

##Maven Installation

We require Maven 3; and all testing to date has been done with Maven 3.2.2 and 3.0.5 versions.

To check if you have Maven installed; execute `$ mvn -version`

This should return Apache Maven 3.0.0 or a higher version. If you do not have Maven, you will need to install it.

###Mac OS X

Maven was included by default in 10.8 and earlier, but is not included from 10.9 on. The easiest way to install it is via homebrew: `$ brew install maven`

If you do not already have homebrew installed, see the [homebrew website](http://brew.sh/) for instructions .

If you would like to attempt to install Maven by hand here is a [stackoverflow discussion](http://stackoverflow.com/questions/8826881/maven-install-on-mac-os-x) which includes some suggestions.

###Linux

Available in apt-get/yum package managers.

###Windows

Download Maven binaries and install per instructions on [Maven website](https://maven.apache.org/install.html).


###Configuration

Maven uses the MAVEN_OPTS environment variable to pass arguments to the underlying Java process. 

It sometimes helps to set a higher max memory size than default:

`$ export MAVEN_OPTS="-Xmx1g"`

It also helps to set the JAVA_HOME environment variable:

`$ export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_20.jdk/Contents/Home`

###Eclipse

The instructions below have been tested with Eclipse Luna. Later versions should include support for the same plugins; YMMV. Earlier versions probably will not work. 

Two plugins which are highly recommended (i.e. required):

[m2e](https://www.eclipse.org/m2e/download/)

[m2e dynamic source lookup](https://marketplace.eclipse.org/content/m2e-dynamic-sources-lookup)

The .project/.classpath files have been updated to use the Maven build nature for projects which have been converted. The m2e plugin will allow Eclipse to interact with Maven, and actually may be installed by default in new downloads of Eclipse Luna.

The m2e dynamic source lookup plugin which allows Eclipse to resolve/fetch the sources for Maven dependencies when debugging (most Maven dependencies publish sources, although we will likely find some do not). Without this plugin you can still debug your own source but cannot easily step into third party libraries. This plugin appears to require Eclipse Luna.

More details on debugging the Zimbra webapps can be found in jetty.md located in the same directory as this document.

####Legacy Eclipse Support

If you are running an old version of Eclipse and/or cannot install the m2e plugin, you can ask Maven to generate .project and .classpath files for you. This not not ideal or recommended, but if you are stuck on an old version this could be a viable workaround. These instructions are mutually exclusive with m2e, so use one or the other but not both.

To do this, first open the files in question so they are writable, then run the eclipse:clean and eclipse:eclipse goals

```sh
cd ZimbraServer
p4 open .classpath
p4 open .project
mvn eclipse:clean eclipse:eclipse
```

Now when you open Eclipse you'll see each dependency referenced via a M2_REPO classpath variable. If Eclipse has not configured this for you, you can add it in Preferences->Java->Build Path->Classath Variables, or through the mvn eclipse:configure-workspace goal as described here: [m2 repo variable repair](http://www.mkyong.com/maven/how-to-configure-m2_repo-variable-in-eclipse-ide/)

##Maven Usage

###Usage from Ant

If you don't want to use Maven directly, just call your normal Ant targets which should still work.

`$ ant reset-all`

`$ ant compile`

One note on this is that some Ant subprojects call compile/jar in parent projects and can create a bit of a cycle compiling the various Maven projects repeatedly. To avoid this pass the -Dskip.maven=true argument on the ant command line.

`$ ant -Dskip.maven=true compile-just-my-jar`

Probably will be useful with targets like dev-sync. The normal reset-all takes care of this implicitly by calling Maven once at the start of the build and then skipping it on subsequent calls in the same ant run.

###Direct Maven Usage

As mentioned in the section covering the migration from Ant, most developers can simply continue to use Ant and the build process will trigger Maven when required. However, Java developers are encouraged to use new features provided by Maven to streamline their workflow.

* Build everything which is Maven enabled: From the basedir (i.e. //depot/zim bra/main) invoke `$ mvn install`

* Run unit tests `$ mvn test`

* The ZimbraServer project supports `$ mvn jetty:run` so you can quickly deploy a test version of the server. Other webapp modules such as ZimbraRedologService also support this goal.

###Jars which are not available in Maven

Several existing Zimbra dependencies are not available in Maven public repositories, for example jar files which Zimbra has patched or jar files which simply are not published in Maven by their author. These are kept in Perforce under ZimbraCommon/jars-bootstrap. During an `ant reset-all` invocation, the `maven-seed-local-repo` target is called to install these jars in the local Maven repository. While it should generally not be necessary; if you happen to remove your Maven local repository (e.g. by deleting ~/.m2/repository) you will also want to remove ZimbraServer/mvn.seed.properties; as this is used by Ant to determine which bootstrap jars have been installed in earlier Ant runs.

You may also run the `mvn-local-jars.sh` shell script to populate the local repository with these jars.

###Test Failures

One of the nice features of Maven's standard lifecycle is that unit tests are run automatically during every build. In general this is great because it prevents developers from accidentally forgetting to run unit tests and submitting bad code. However, it may be necessary to skip tests under some circumstances; for example if there are sporadic/environmental failures, or if you need to build your component even though some unrelated test is failing.

In these cases, the -DskipTests=true argument can be passed to Maven or Ant.

```sh
$ mvn -DskipTests=true clean install
```
or

```sh
$ ant -DskipTests=true reset-all
```

###Test Profiles

Some of the unit tests in the ZimbraServer project take a long time to run. It is sometimes helpful to spot-check the project without running the entire suite. Two profiles have been created for this purpose in ZimbraServer/pom.xml. These profiles could be applied to other projects that have long-running tests.

`skipSlowTests` - Skip several tests that are identified as taking longer than 1 minute on a typical dev laptop.

`smokeTests` - Run a small set of tests which exercise the core server components quickly. 

When running Maven directly these profiles can be enabled with the -P argument

```sh
$ mvn -PsmokeTests test
```

When building the server via Ant, pass the profile in a -D argument, and set it to true.

```sh
$ ant -DskipSlowTests=true reset-all
```

###Module Profiles

By default, a build from the top level directory builds all of the core FOSS modules. These modules are required to complete a full build.

* ZimbraCharset
* ZimbraNative
* ZimbraCommon
* ZimbraSoap
* ZimbraClient
* ZimbraServer
* ZimbraTagLib

A number of other optional modules for FOSS and NE components are compiled if they are present in the workspace. This is intended to ensure that changes to core modules are validated against modules which may depend upon them during development. In some cases it may be helpful to exclude certain, which can be accomplished by standard Maven -P variants. See the top level zimbra-parent pom.xml for the current profile names

For example:

* Disable compilation of the nginx lookup extension: `$ mvn -P-FOSS-zimbra-nginxlookup clean`
* Disable compilation of the NE codebase: `$ mvn -P-NE clean`

##Troubleshooting

###Perforce client workspace inclusions

You may run into Maven errors building the first time if you do not have all of the standard directories in your P4 client workspace. For example you may see errors such as this:

```sh
[exec] [ERROR] The goal you specified requires a project to execute but there is no POM in this directory (/Users/devuser/zimbra/p4/main). Please verify you invoked Maven from the correct directory. -> [Help 1]

Child module /home/devuser/p4/devuser-u14-main/main/ZimbraNative does not exist
```

If you see these errors make sure you have the following in your P4 client specification. 

* //depot/zimbra/main/pom.xml
* //depot/zimbra/main/ZimbraNative/...
* //depot/zimbra/main/ZimbraCharset/...

