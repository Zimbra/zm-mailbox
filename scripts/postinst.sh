#!/bin/bash
echo "**** apply restrict write access for /opt/zimbra/jetty_base/webapps/ (zimbra-mbox-war)****"
chattr +i -R /opt/zimbra/jetty_base/webapps/
