#!/bin/bash
if lsattr -d /path/to/directory | grep -q 'i'; then
    echo "Immutable attribute already set on the /opt/zimbra/jetty_base/webapps/ (zimbra-mbox-war)"
else
	echo "**** apply restrict write access for /opt/zimbra/jetty_base/webapps/ (zimbra-mbox-war)****"
	chattr +i -R /opt/zimbra/jetty_base/webapps/
fi
