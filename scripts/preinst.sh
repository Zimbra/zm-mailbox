#!/bin/bash
if lsattr -d /opt/zimbra/jetty_base/webapps/ | grep -qE '\b[i]\b'; then
	echo "**** remove restrict write access for /opt/zimbra/jetty_base/webapps/ (zimbra-mbox-war) ****"
	lsattr -d /opt/zimbra/jetty_base/webapps/
	chattr -i -R /opt/zimbra/jetty_base/webapps/
fi
