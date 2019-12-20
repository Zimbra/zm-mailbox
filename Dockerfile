FROM busybox

# Copy mailbox-conf to /opt/zimbra/conf
COPY store-conf/build/zms/mailbox-conf/ opt/zimbra/conf/
COPY store/build/dist/conf/attrs/zimbra-attrs-schema /opt/zimbra/conf/

# Copy service webapp and web.xml
COPY store/conf/web.xml.production opt/zimbra/jetty_base/etc/service.web.xml.in
COPY store/build/service/ opt/zimbra/jetty_base/webapps/service/

# Copy core jars to /opt/zimbra/lib/jars
COPY native/build/zimbra-native.jar opt/zimbra/lib/jars/zimbra-native.jar
COPY common/build/zimbracommon.jar opt/zimbra/lib/jars/zimbracommon.jar
COPY soap/build/zimbrasoap.jar opt/zimbra/lib/jars/zimbrasoap.jar
COPY client/build/zimbraclient.jar opt/zimbra/lib/jars/zimbraclient.jar
COPY store/build/zimbrastore.jar opt/zimbra/lib/jars/zimbrastore.jar

# Copy native lib to /opt/zimbra/lib
COPY native/build/libzimbra-native.so opt/zimbra/lib/

# Copy jylibs
COPY build/zm-jython/jylibs/ opt/zimbra/common/lib/jylibs/

# Copy perl libs
COPY build/zm-build/lib/Zimbra/ opt/zimbra/common/lib/perl5/Zimbra/