# Default values for DOCKER_REPO_NS, DOCKER_BUILD_TAG are defined.
# Change the DOCKER_REPO_NS, DOCKER_BUILD_TAG value if required here or pass as docker build argument.

ARG DOCKER_REPO_NS=dev-registry.zimbra-docker-registry.tk

#Build stage image
FROM ${DOCKER_REPO_NS}/zms-core-utils:1.0 as utils
FROM ${DOCKER_REPO_NS}/zms-zcs-lib:1.0 as lib
FROM ${DOCKER_REPO_NS}/zms-extension:1.0 as ext
# This will be inclued for network version
# FROM ${DOCKER_REPO_NS}/zms-extension-network:1.0 as ext-network
FROM ${DOCKER_REPO_NS}/zms-jetty-conf:1.0 as jetty-conf
FROM ${DOCKER_REPO_NS}/zms-jython:1.0 as jython
FROM ${DOCKER_REPO_NS}/zms-perl:1.0 as perl
FROM ${DOCKER_REPO_NS}/zms-db-conf:1.0 as db-conf
FROM ${DOCKER_REPO_NS}/zms-admin-console:1.0 as admin-console
FROM ${DOCKER_REPO_NS}/zms-ldap-utilities:1.0 as ldap
FROM ${DOCKER_REPO_NS}/zms-timezones:1.0 as timezone

# Final stage, copy contents from build stage
FROM ${DOCKER_REPO_NS}/zms-base:1.0
COPY --from=utils /opt/zimbra/ /opt/zimbra/
COPY --from=lib /opt/zimbra/ /opt/zimbra/
COPY --from=ext /opt/zimbra/ /opt/zimbra/
# This will be inclued for network version
# COPY --from=ext-network /opt/zimbra/ /opt/zimbra/
COPY --from=jetty-conf /opt/zimbra/ /opt/zimbra/
COPY --from=jython /opt/zimbra/ /opt/zimbra/
COPY --from=perl /opt/zimbra/ /opt/zimbra/
COPY --from=db-conf /opt/zimbra/ /opt/zimbra/
COPY --from=admin-console /opt/zimbra/ /opt/zimbra/
COPY --from=ldap /opt/zimbra/ /opt/zimbra/
COPY --from=timezone /opt/zimbra/ /opt/zimbra/

COPY store-conf/conf/antisamy.xml /opt/zimbra/conf/
COPY store/conf/attrs /opt/zimbra/conf/attrs/
COPY store-conf/conf/datasource.xml /opt/zimbra/conf/
COPY store-conf/conf/globs2 /opt/zimbra/conf/
COPY store-conf/conf/globs2.zimbra /opt/zimbra/conf/
COPY store-conf/conf/imapd.log4j.properties /opt/zimbra/conf/
COPY store-conf/conf/localconfig.xml.production /opt/zimbra/conf/localconfig.xml.in
COPY store-conf/conf/log4j.properties.production /opt/zimbra/conf/log4j.properties.in
COPY store-conf/conf/magic /opt/zimbra/conf/
COPY store-conf/conf/magic.zimbra /opt/zimbra/conf/
COPY milter-conf/conf/milter.log4j.properties /opt/zimbra/conf/
COPY store-conf/conf/msgs/ /opt/zimbra/conf/msgs/
COPY milter-conf/conf/mta_milter_options.in /opt/zimbra/conf/
COPY store-conf/conf/owasp_policy.xml /opt/zimbra/conf/
COPY store-conf/conf/rights/*.xml /opt/zimbra/conf/rights/
COPY store-conf/conf/spnego_java_options.in /opt/zimbra/conf/
COPY store-conf/conf/stats.conf.in /opt/zimbra/conf/
COPY store/conf/unbound.conf.in /opt/zimbra/conf/
COPY store/build/dist/conf/attrs/zimbra-attrs-schema /opt/zimbra/conf/
COPY store-conf/conf/contacts/zimbra-contact-fields.xml /opt/zimbra/conf/
# Copy service webapp and web.xml
COPY store-conf/conf/logback-access.xml /opt/zimbra/jetty_base/resources/logback-access.xml
COPY store/conf/web.xml.production /opt/zimbra/jetty_base/etc/service.web.xml.in
COPY store/build/service.war /opt/zimbra/jetty_base/webapps/service/
RUN cd /opt/zimbra/jetty_base/webapps/service/ && jar -xf service.war && rm -rf service.war
RUN cp /opt/zimbra/jetty_base/webapps/zimbraAdmin/WEB-INF/web.xml /opt/zimbra/jetty_base/etc/zimbraAdmin.web.xml.in \
    && cp /opt/zimbra/jetty_base/webapps/zimbraAdmin/WEB-INF/jetty-env.xml /opt/zimbra/jetty_base/etc/zimbraAdmin-jetty-env.xml.in \
    && cp -f /opt/zimbra/conf/zmlogrotate.mailbox /etc/logrotate.d/zimbra.mailbox \
    && touch /opt/zimbra.platform && echo "@@BUILD_PLATFORM@@" >> /opt/zimbra.platform
# Copy native lib to /opt/zimbra/lib & core jars to /opt/zimbra/lib/jars
COPY native/build/libzimbra-native.so /opt/zimbra/lib/
COPY native/build/zimbra-native.jar /opt/zimbra/lib/jars/
COPY common/build/zimbracommon.jar /opt/zimbra/lib/jars/
COPY soap/build/zimbrasoap.jar /opt/zimbra/lib/jars/
COPY client/build/zimbraclient.jar /opt/zimbra/lib/jars/
COPY store/build/zimbrastore.jar /opt/zimbra/lib/jars/

RUN chown -R root:root /opt/zimbra/mailboxd/common/lib \
    && chown -R zimbra:zimbra db \
    && chown zimbra:zimbra -R conf jetty_base log db extensions-extra zimlets-deployed \
    && chmod -R +x bin libexec \
    && chown root:root jetty mailboxd jetty_base/common \
    && chmod 444 /opt/zimbra/lib/jars/* && chmod 755 /opt/zimbra/lib/*.so && chmod 664 /opt/zimbra/lib/*.nar
