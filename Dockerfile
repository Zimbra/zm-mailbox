# Default values for DOCKER_REPO_NS, DOCKER_BUILD_TAG are defined.
# Change the DOCKER_REPO_NS, DOCKER_BUILD_TAG value if required here or pass as docker build argument.

ARG DOCKER_REPO_NS=dev-registry.zimbra-docker-registry.tk
#Build stage image
FROM ${DOCKER_REPO_NS}/zms-core-utils:1.1 as utils
FROM ${DOCKER_REPO_NS}/zms-zcs-lib:1.0.3 as lib
FROM ${DOCKER_REPO_NS}/zms-jetty-conf:1.2 as jetty-conf
FROM ${DOCKER_REPO_NS}/zms-jython:1.1 as jython
FROM ${DOCKER_REPO_NS}/zms-perl:1.1 as perl
FROM ${DOCKER_REPO_NS}/zms-db-conf:1.1 as db-conf
FROM ${DOCKER_REPO_NS}/zms-admin-console:1.0.2 as admin-console
FROM ${DOCKER_REPO_NS}/zms-ldap-utilities:1.1 as ldap
FROM ${DOCKER_REPO_NS}/zms-timezones:1.1 as timezone
FROM ${DOCKER_REPO_NS}/zms-core-network-extension:1.0.4 as ext-core-network
FROM ${DOCKER_REPO_NS}/zms-core-zimlets:1.0.2 as zimlet-webapp
FROM ${DOCKER_REPO_NS}/zms-classic-webclient:0.0.1 as zimbra-classic-webclient

# Final stage, copy contents from build stage
FROM ${DOCKER_REPO_NS}/zms-base:1.0.10
ENV EXT_REPO_URL http://zimbraqa.s3.amazonaws.com/api-team/extensions
ENV S3_DOCKER_URL https://s3.amazonaws.com/docker.zimbra.com/assets
COPY --from=utils /opt/zimbra/ /opt/zimbra/
COPY --from=lib /opt/zimbra/lib/ /opt/zimbra/lib/
COPY --from=lib /opt/zimbra/jetty_base/common/ /opt/zimbra/jetty_base/common/
COPY --from=zimlet-webapp /opt/zimbra/jetty_base/ /opt/zimbra/jetty_base/
COPY --from=jetty-conf /opt/zimbra/ /opt/zimbra/
COPY --from=jython /opt/zimbra/ /opt/zimbra/
COPY --from=perl /opt/zimbra/ /opt/zimbra/
COPY --from=db-conf /opt/zimbra/ /opt/zimbra/
COPY --from=admin-console /opt/zimbra/ /opt/zimbra/
COPY --from=ldap /opt/zimbra/ /opt/zimbra/
COPY --from=timezone /opt/zimbra/ /opt/zimbra/
COPY --from=zimbra-classic-webclient /opt/zimbra/ /opt/zimbra/

# Pull core extensions and put into /opt/zimbra/bin(for license), /opt/zimbra/lib/ext, /opt/zimbra/lib/ext-common and /opt/zimbra/extensions-extra
# zm-license-tools and zm-licnese-store
RUN mkdir -p /opt/zimbra/lib/ext /opt/zimbra/extensions-extra /opt/zimbra/lib/ext/zimbra-license \
&& curl -k -s -o /opt/zimbra/lib/ext-common/zimbra-license-tools.jar ${EXT_REPO_URL}/zm-license-tools/latest/zimbra-license-tools.jar \
&& curl -k -s -o /opt/zimbra/bin/zmhactl ${EXT_REPO_URL}/zm-license-tools/latest/zmhactl \
&& curl -k -s -o /opt/zimbra/bin/zmlicense ${EXT_REPO_URL}/zm-license-tools/latest/zmlicense \
&& curl -k -s -o /opt/zimbra/libexec/vmware-heartbeat ${EXT_REPO_URL}/zm-license-tools/latest/vmware-heartbeat \
&& curl -k -s -o /opt/zimbra/lib/ext/zimbra-license/zimbra-license.jar ${EXT_REPO_URL}/zm-license-store/latest/zimbra-license.jar

# zm-gql and zm-gql-admin
RUN mkdir -p /opt/zimbra/lib/ext/zm-gql /opt/zimbra/lib/ext/zm-gql-admin \
&& curl -k -s -o /opt/zimbra/lib/ext/zm-gql/zmgql.jar ${EXT_REPO_URL}/zm-gql/latest/zmgql.jar \
&& curl -k -s -o /opt/zimbra/lib/ext/zm-gql/java-jwt-3.2.0.jar https://repo1.maven.org/maven2/com/auth0/java-jwt/3.2.0/java-jwt-3.2.0.jar \
&& curl -k -s -o /opt/zimbra/lib/ext/zm-gql-admin/zmgqladmin.jar ${EXT_REPO_URL}/zm-gql-admin/latest/zmgqladmin.jar

# zm-oauth-social and zm-ssdb-ephemeral-store
RUN mkdir -p /opt/zimbra/lib/ext/zm-oauth-social /opt/zimbra/lib/ext/com_zimbra_ssdb_ephemeral_store \
&& curl -k -s -o /opt/zimbra/lib/ext/zm-oauth-social/zmoauthsocial.jar ${EXT_REPO_URL}/zm-oauth-social/latest/zmoauthsocial.jar \
&& curl -k -s -o /opt/zimbra/lib/ext/com_zimbra_ssdb_ephemeral_store/zm-ssdb-ephemeral-store.jar ${EXT_REPO_URL}/zm-ssdb-ephemeral-store/latest/zm-ssdb-ephemeral-store.jar

# zm-ldap-utils-store, zm-versioncheck-store, zm-bulkprovision-store and zm-clam-scanner-store, saml2sp, zimberg
RUN mkdir -p /opt/zimbra/lib/ext/zimbraldaputils /opt/zimbra/lib/ext/zimbraadminversioncheck /opt/zimbra/lib/ext/com_zimbra_bulkprovision /opt/zimbra/lib/ext/clamscanner \
&& mkdir -p /opt/zimbra/lib/ext/saml2sp /opt/zimbra/lib/ext/zimberg \
&& curl -k -s -o /opt/zimbra/lib/ext/zimbraldaputils/zimbraldaputils.jar ${EXT_REPO_URL}/zm-ldap-utils-store/latest/zmldaputils.jar \
&& curl -k -s -o /opt/zimbra/lib/ext/zimbraadminversioncheck/zimbraadminversioncheck.jar ${EXT_REPO_URL}/zm-versioncheck-store/latest/zimbraadminversioncheck.jar \
&& curl -k -s -o /opt/zimbra/lib/ext/com_zimbra_bulkprovision/com_zimbra_bulkprovision.jar ${EXT_REPO_URL}/zm-bulkprovision-store/latest/com_zimbra_bulkprovision.jar \
&& curl -k -s -o /opt/zimbra/lib/ext/com_zimbra_bulkprovision/commons-csv-1.2.jar https://repo1.maven.org/maven2/org/apache/commons/commons-csv/1.2/commons-csv-1.2.jar \
&& curl -k -s -o /opt/zimbra/lib/ext/clamscanner/clamscanner.jar ${EXT_REPO_URL}/zm-clam-scanner-store/latest/clamscanner.jar \
&& curl -k -s -o /opt/zimbra/lib/ext/saml2sp/saml2sp.jar ${S3_DOCKER_URL}/saml2sp_zimbra8.jar \
&& curl -k -s -o /opt/zimbra/lib/ext/zimberg/zimberg_store_manager-0.3.0.jar ${S3_DOCKER_URL}/zimberg_store_manager-0.3.0.jar

# zm-openid-consumer-store, zm-nginx-lookup-store, zm-clientuploader-store and zm-certificate-manager-store
RUN mkdir -p /opt/zimbra/lib/ext/openidconsumer /opt/zimbra/lib/ext/nginx-lookup /opt/zimbra/lib/ext/com_zimbra_clientuploader /opt/zimbra/lib/ext/com_zimbra_cert_manager \
&& curl -k -s -o /opt/zimbra/lib/ext/openidconsumer/guice-2.0.jar ${EXT_REPO_URL}/zm-openid-consumer-store/latest/guice-2.0.jar \
&& curl -k -s -o /opt/zimbra/lib/ext/com_zimbra_cert_manager/com_zimbra_cert_manager.jar ${EXT_REPO_URL}/zm-certificate-manager-store/latest/com_zimbra_cert_manager.jar \
&& curl -k -s -o /opt/zimbra/lib/ext/com_zimbra_clientuploader/com_zimbra_clientuploader.jar ${EXT_REPO_URL}/zm-clientuploader-store/latest/com_zimbra_clientuploader.jar \
&& curl -k -s -o /opt/zimbra/lib/ext/nginx-lookup/nginx-lookup.jar ${EXT_REPO_URL}/zm-nginx-lookup-store/latest/nginx-lookup.jar \
&& mkdir -p /opt/zimbra/extensions-extra/openidconsumer \
&& curl -k -s -o /opt/zimbra/extensions-extra/openidconsumer/guice-2.0.jar ${EXT_REPO_URL}/zm-openid-consumer-store/latest/guice-2.0.jar \
&& curl -k -s -o /opt/zimbra/extensions-extra/openidconsumer/formredirection.jsp ${EXT_REPO_URL}/zm-openid-consumer-store/latest/formredirection.jsp \
&& curl -k -s -o /opt/zimbra/extensions-extra/openidconsumer/openid4java-1.0.0.jar ${EXT_REPO_URL}/zm-openid-consumer-store/latest/openid4java-1.0.0.jar \
&& curl -k -s -o /opt/zimbra/extensions-extra/openidconsumer/README.txt ${EXT_REPO_URL}/zm-openid-consumer-store/latest/README.txt \
&& curl -k -s -o /opt/zimbra/extensions-extra/openidconsumer/zm-openid-consumer-store.jar ${EXT_REPO_URL}/zm-openid-consumer-store/latest/zm-openid-consumer-store.jar

COPY --from=ext-core-network /opt/zimbra/ /opt/zimbra/

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
RUN cd /opt/zimbra/jetty_base/webapps/zimbraAdmin/ && jar -xf zimbraAdmin.war && rm -rf zimbraAdmin.war
RUN cat /opt/zimbra/jetty_base/webapps/zimbraAdmin/WEB-INF/web.xml | sed -e '/REDIRECTBEGIN/ s/\$/ %%comment VAR:zimbraMailMode,-->,redirect%%/' -e '/REDIRECTEND/ s/^/%%comment VAR:zimbraMailMode,<!--,redirect%% /' > /opt/zimbra/jetty_base/etc/zimbraAdmin.web.xml.in
RUN cp -f /opt/zimbra/conf/zmlogrotate.mailbox /etc/logrotate.d/zimbra.mailbox \
    && touch /opt/zimbra/.platform && echo "@@BUILD_PLATFORM@@" >> /opt/zimbra/.platform

COPY --from=lib /opt/zimbra/jetty_base/webapps/service/WEB-INF/lib/ /opt/zimbra/jetty_base/webapps/service/WEB-INF/lib/
COPY --from=zimlet-webapp /opt/zimbra/jetty_base/webapps/service/WEB-INF/ /opt/zimbra/jetty_base/webapps/service/WEB-INF/

#RUN true added to fix issue described in https://github.com/moby/moby/issues/37965
#(COPY fail in multistage build: layer does not exist )
RUN true

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
    && chown -h root:root jetty mailboxd jetty_base/common \
    && chmod 444 /opt/zimbra/lib/jars/* && chmod 755 /opt/zimbra/lib/*.so && chmod 664 /opt/zimbra/lib/*.nar
