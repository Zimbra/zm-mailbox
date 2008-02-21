#!/bin/bash
source "/opt/zimbra/bin/zmshutil" || exit 1

mkdir -p /opt/zimbra/data/postfix
mkdir -p /opt/zimbra/data/amavisd

chown zimbra:zimbra /opt/zimbra/data

/opt/zimbra/bin/zmlocalconfig -e postfix_queue_directory=/opt/zimbra/data/postfix/spool

if [ -d "/opt/zimbra/postfix-2.4.3.4z/spool" ]; then
	mv -f /opt/zimbra/postfix-2.4.3.4z/spool /opt/zimbra/data/postfix/
fi

if [ -d "/opt/zimbra/amavisd/.spamassassin" ]; then
	mv -f /opt/zimbra/amavisd/.spamassassin /opt/zimbra/data/amavisd/
fi

if [ -d "/opt/zimbra/amavisd/db" ]; then
	mv -f /opt/zimbra/amavisd/db /opt/zimbra/data/amavisd/
fi

if [ -d "/opt/zimbra/amavisd/quarantine" ]; then
	mv -f /opt/zimbra/amavisd/quarantine /opt/zimbra/data/amavisd/
fi

if [ -d "/opt/zimbra/amavisd/tmp" ]; then
	mv -f /opt/zimbra/amavisd/tmp /opt/zimbra/data/amavisd/
fi

if [ -d "/opt/zimbra/amavisd/var" ]; then
	mv -f /opt/zimbra/amavisd/var /opt/zimbra/data/amavisd/
fi

/opt/zimbra/libexec/zmfixperms
