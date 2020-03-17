#
# ***** BEGIN LICENSE BLOCK *****
# Zimbra Collaboration Suite Server
# Copyright (C) 2019 Synacor, Inc.
#
# This program is free software: you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free Software Foundation,
# version 2 of the License.
#
# This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
# without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
# See the GNU General Public License for more details.
# You should have received a copy of the GNU General Public License along with this program.
# If not, see <https://www.gnu.org/licenses/>.
# ***** END LICENSE BLOCK *****
#

FROM alpine

WORKDIR root
RUN apk update && apk upgrade && apk add bash wget curl
RUN mkdir -p /opt/zimbra/lib/ext-common
COPY build/zm-bulkprovision-store/build/zm-bulkprovision-store*.jar                /opt/zimbra/lib/ext/com_zimbra_bulkprovision/com_zimbra_bulkprovision.jar
COPY build/zm-bulkprovision-store/commons-csv-1.2.jar                              /opt/zimbra/lib/ext/com_zimbra_bulkprovision/commons-csv-1.2.jar
COPY build/zm-clam-scanner-store/build/zm-clam-scanner-store*.jar                  /opt/zimbra/lib/ext/clamscanner/clamscanner.jar
COPY build/zm-certificate-manager-store/build/zm-certificate-manager-store*.jar    /opt/zimbra/lib/ext/com_zimbra_cert_manager/com_zimbra_cert_manager.jar
COPY build/zm-clientuploader-store/build/zm-clientuploader-store*.jar              /opt/zimbra/lib/ext/com_zimbra_clientuploader/com_zimbra_clientuploader.jar
COPY build/zm-nginx-lookup-store/build/zm-nginx-lookup-store*.jar                  /opt/zimbra/lib/ext/nginx-lookup/nginx-lookup.jar
COPY build/zm-openid-consumer-store/build/dist/guice*.jar                          /opt/zimbra/lib/ext/openidconsumer/
COPY build/zm-versioncheck-store/build/zm-versioncheck-store*.jar                  /opt/zimbra/lib/ext/zimbraadminversioncheck/zimbraadminversioncheck.jar
COPY build/zm-ldap-utils-store/build/zm-ldap-utils-*.jar                           /opt/zimbra/lib/ext/zimbraldaputils/zimbraldaputils.jar
COPY build/zm-oauth-social/build/zm-oauth-social*.jar                              /opt/zimbra/lib/ext/zm-oauth-social/zmoauthsocial.jar
COPY build/zm-gql/build/zm-gql*.jar                                                /opt/zimbra/lib/ext/zm-gql/zmgql.jar
COPY build/zm-gql/java-jwt-3.2.0.jar                                               /opt/zimbra/lib/ext/zm-gql/java-jwt-3.2.0.jar
COPY build/zm-gql-admin/build/zm-gql*.jar                                          /opt/zimbra/lib/ext/zm-gql-admin/zmgqladmin.jar
COPY build/zm-openid-consumer-store/build/dist/                                    /opt/zimbra/extensions-extra/openidconsumer/
COPY build/zm-ssdb-ephemeral-store/build/zm-ssdb-ephemeral-store*.jar              /opt/zimbra/lib/ext/com_zimbra_ssdb_ephemeral_store/
COPY build/saml2sp/saml2sp.jar                                                     /opt/zimbra/lib/ext/saml2sp/saml2sp.jar
COPY build/zimberg/zimberg_store_manager-0.3.0.jar                                 /opt/zimbra/lib/ext/zimberg/zimberg_store_manager-0.3.0.jar
RUN wget https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.5/gson-2.8.5.jar -P /opt/zimbra/lib/ext-common/ \
 && wget https://repo1.maven.org/maven2/org/bouncycastle/bcpkix-jdk15on/1.55/bcpkix-jdk15on-1.55.jar -P /opt/zimbra/lib/ext-common/ \
 && wget https://repo1.maven.org/maven2/org/bouncycastle/bcmail-jdk15on/1.55/bcmail-jdk15on-1.55.jar -P /opt/zimbra/lib/ext-common/ \
 && wget https://repo1.maven.org/maven2/org/bouncycastle/bcprov-jdk15on/1.55/bcprov-jdk15on-1.55.jar -P /opt/zimbra/lib/ext-common/ \
 && wget https://repo1.maven.org/maven2/com/sun/xml/messaging/saaj/saaj-impl/1.5.1/saaj-impl-1.5.1.jar -P /opt/zimbra/lib/ext-common/
COPY build/zm-license-store/build/*.jar                                            /opt/zimbra/lib/ext/zimbra-license/zimbra-license.jar
COPY build/zm-license-tools/build/*.jar                                            /opt/zimbra/lib/ext-common/zimbra-license-tools.jar
COPY build/zm-license-tools/src/bin/                                               /opt/zimbra/bin/
RUN chmod -R 755 /opt/zimbra/bin/
