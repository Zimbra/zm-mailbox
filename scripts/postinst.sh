#!/bin/bash
if lsattr -d /opt/zimbra/jetty_base/webapps/ | grep -q 'i'; then
    echo "Immutable attribute already set on the /opt/zimbra/jetty_base/webapps/ (zimbra-mbox-war)"
    lsattr -d /opt/zimbra/jetty_base/webapps/
else
	echo "**** apply restrict write access for /opt/zimbra/jetty_base/webapps/ (zimbra-mbox-war)****"
	lsattr -d /opt/zimbra/jetty_base/webapps/
	chattr +i -R /opt/zimbra/jetty_base/webapps/
fi
