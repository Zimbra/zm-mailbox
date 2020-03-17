FROM alpine
WORKDIR root
USER root
RUN apk update && apk upgrade && apk add bash curl
COPY build/zm-network-store/build/zimbra*.jar                             /opt/zimbra/lib/ext-common/
COPY build/zm-network-store/build/zm-*.jar                                /opt/zimbra/lib/ext/network/zimbranetwork.jar
COPY build/zm-network-store/src/bin/zmmboxsearch                          /opt/zimbra/bin/
COPY build/zm-convertd-store/build/*jar                                   /opt/zimbra/lib/ext/convertd/zimbraconvertd.jar
COPY build/zm-convertd-store/build/dist/tika-*.jar                        /opt/zimbra/lib/ext/convertd/
COPY build/zm-twofactorauth-store/build/*.jar                             /opt/zimbra/lib/ext/twofactorauth/twofactorauth.jar
COPY build/zm-twofactorauth-store/docs/twofactorauth.md                   /opt/zimbra/docs/twofactorauth.md
COPY build/zm-ews-common/build/*.jar                                      /opt/zimbra/lib/ext/zimbraews/zm-ews-common.jar
COPY build/zm-ews-store/build/*.jar                                       /opt/zimbra/lib/ext/zimbraews/zm-ews-store.jar
COPY build/zm-ews-stub/resources/*.xsd                                    /opt/zimbra/lib/ext/zimbraews/
COPY build/zm-ews-stub/resources/*.wsdl                                   /opt/zimbra/lib/ext/zimbraews/
COPY build/zm-ews-stub/build/zm-ews-stub-2.0.jar                          /opt/zimbra/lib/jars/zm-ews-stub-2.0.jar
COPY build/zm-openoffice-store/build/dist/*.jar                           /opt/zimbra/lib/ext/com_zimbra_oo/
COPY build/zm-sync-common/build/*.jar                                     /opt/zimbra/lib/ext/zimbrasync/zm-sync-common.jar
COPY build/zm-sync-store/build/*.jar                                      /opt/zimbra/lib/ext/zimbrasync/zm-sync-store.jar
COPY build/zm-sync-store/src/bin/zmdevicesstats                           /opt/zimbra/bin/
COPY build/zm-sync-store/src/bin/zmgdcutil                                /opt/zimbra/bin/
COPY build/zm-sync-tools/src/bin/zmsyncreverseproxy                       /opt/zimbra/bin/
COPY build/zm-sync-tools/build/*.jar                                      /opt/zimbra/lib/ext/zimbrasync/zsyncreverseproxy.jar
RUN chmod -R 755 /opt/zimbra/bin/
RUN mv /opt/zimbra/lib/ext/com_zimbra_oo/zm-openoffice-store.jar /opt/zimbra/lib/ext/com_zimbra_oo/com_zimbra_oo.jar
