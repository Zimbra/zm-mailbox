#!/bin/bash
echo "**** remove restrict write access for /opt/zimbra/jetty_base/webapps/ (zimbra-mbox-war-post)****"
chattr -i -R /opt/zimbra/jetty_base/webapps/
echo "**** apply restrict write access for /opt/zimbra/jetty_base/webapps/ (zimbra-mbox-war)****"
chattr +i -R /opt/zimbra/jetty_base/webapps/
