FROM busybox

# Copy mailbox-conf to /opt/zimbra/conf
COPY store-conf/build/zms/mailbox-conf/ opt/zimbra/conf/
COPY store/build/dist/conf/attrs/zimbra-attrs-schema /opt/zimbra/conf/

# Copy jetty-conf to opt/zimbra/jetty_base
COPY store-conf/build/zms/jetty-conf/jettyrc  opt/zimbra/jetty_base/etc/
COPY store-conf/build/zms/jetty-conf/zimbra.policy.example opt/zimbra/jetty_base/etc/
COPY store-conf/build/zms/jetty-conf/jetty.xml.production opt/zimbra/jetty_base/etc/jetty.xml.in
COPY store-conf/build/zms/jetty-conf/webdefault.xml.production opt/zimbra/jetty_base/etc/webdefault.xml
COPY store-conf/build/zms/jetty-conf/jetty-setuid.xml opt/zimbra/jetty_base/etc/jetty-setuid.xml
COPY store-conf/build/zms/jetty-conf/spnego/etc/spnego.properties opt/zimbra/jetty_base/etc/spnego.properties.in
COPY store-conf/build/zms/jetty-conf/spnego/etc/spnego.conf opt/zimbra/jetty_base/etc/spnego.conf.in
COPY store-conf/build/zms/jetty-conf/spnego/etc/krb5.ini opt/zimbra/jetty_base/etc/krb5.ini.in
COPY store-conf/build/zms/jetty-conf/modules/*.mod  opt/zimbra/jetty_base/modules/
COPY store-conf/build/zms/jetty-conf/modules/*.mod.in opt/zimbra/jetty_base/modules/
COPY store-conf/build/zms/jetty-conf/start.d/*.ini.in   opt/zimbra/jetty_base/start.d/
COPY store-conf/build/zms/jetty-conf/modules/npn/*.mod  opt/zimbra/jetty_base/modules/npn/

# Copy db-conf to opt/zimbra/db
COPY store-conf/zms/db-conf/ opt/zimbra/db/

# Copy service webapp and web.xml
COPY store/conf/web.xml.production opt/zimbra/jetty_base/etc/service.web.xml.in
COPY store/build/service/ opt/zimbra/jetty_base/webapps/service/

# Copy core jars to /opt/zimbra/lib/jars
COPY native/build/zimbra-native.jar opt/zimbra/lib/jars/zimbra-native.jar
COPY common/build/zimbracommon.jar opt/zimbra/lib/jars/zimbracommon.jar
COPY soap/build/zimbrasoap.jar opt/zimbra/lib/jars/zimbrasoap.jar
COPY client/build/zimbraclient.jar opt/zimbra/lib/jars/zimbraclient.jar
COPY store/build/zimbrastore.jar opt/zimbra/lib/jars/zimbrastore.jar