/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.formatter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.mail.Part;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.OperatorCreationException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.collect.Maps;
import com.zimbra.common.account.ZAttrProvisioning.MtaTlsSecurityLevel;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SmimeConstants;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.UserServletContext;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;
import com.zimbra.cs.service.util.DataSigner;

public class MobileConfigFormatter extends Formatter {
    public static final String QP_CONFIG_TYPE = "configType";
    public static final String EXTENSION = ".mobileconfig";
    private DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();

    @Override
    public FormatType getType() {
        return FormatType.MOBILE_CONFIG;
    }

    @Override
    public String[] getDefaultMimeTypes() {
        return new String[] { MimeConstants.CT_TEXT_XML, "text/xml" };
    }

    @Override
    public void validateParams(UserServletContext context) throws UserServletException {
        String type = context.params.get(QP_CONFIG_TYPE);
        ConfigType configType = ConfigType.fromString(type);
        if (configType == null) {
            throw UserServletException.badRequest("invalid configType");
        }
        if (context.getAuthAccount() != null && context.targetAccount != null && (context.isAnonymousRequest() || !context.getAuthAccount().getMail().equals(context.targetAccount.getMail()))) {
            throw UserServletException.badRequest(context.targetAccount.getMail() + " must authenticate");
        }
    }

    @Override
    public void formatCallback(UserServletContext context) throws ServiceException {
        Account user = context.getAuthAccount();
        Server server = Provisioning.getInstance().getServer(user);
        Domain domain = Provisioning.getInstance().getDomain(user);
        String emailPart = user.getMail().substring(0, user.getMail().indexOf("@"));
        ConfigType configType = validateConfigType(context.req.getParameter("configType"));
        String filename = emailPart + "_" + configType.toString() + EXTENSION;

        String cd = HttpUtil.createContentDisposition(context.req, Part.ATTACHMENT, filename);
        context.resp.addHeader("Content-Disposition", cd);
        context.resp.setCharacterEncoding(MimeConstants.P_CHARSET_UTF8);
        context.resp.setContentType(MimeConstants.CT_TEXT_XML);

        try {
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document document = docBuilder.newDocument();

            // plist
            Element plistElement = document.createElement(ConfigEnum.PLIST.toString()); // root element
            plistElement.setAttribute(PLIST_VERSION, PLIST_VERSION_VALUE);
            document.appendChild(plistElement);

            // dict
            Element dictElement = document.createElement(ConfigEnum.DICT.toString()); // element

            // PayloadContent
            Element configKeyPayloadContentElement = document.createElement(ConfigEnum.KEY.toString()); // key element
            Node payloadContent = document.createTextNode(PayloadEnum.CONTENT.toString());
            configKeyPayloadContentElement.appendChild(payloadContent);
            dictElement.appendChild(configKeyPayloadContentElement);

            // Array
            Element arrayElement = document.createElement(ConfigEnum.ARRAY.toString()); // element
            dictElement.appendChild(arrayElement);

            // add caldav to array
            if (configType == ConfigType.CALDAV || configType == ConfigType.DAV || configType == ConfigType.ALL) {
                arrayElement.appendChild(getDictForCaldavAndCarddav(document, emailPart, user, server, ConfigType.CALDAV, domain));
            }
            // add carddav to array
            if (configType == ConfigType.CARDDAV || configType == ConfigType.DAV || configType == ConfigType.ALL) {
                arrayElement.appendChild(getDictForCaldavAndCarddav(document, emailPart, user, server, ConfigType.CARDDAV, domain));
            }
            // add imap to array
            if (configType == ConfigType.IMAP || configType == ConfigType.ALL) {
                arrayElement.appendChild(getDictForImap(document, emailPart, user, server, ConfigType.IMAP, domain));
            }

            // PayloadDescription
            Element keyPayloadDescriptionElement = document.createElement(ConfigEnum.KEY.toString()); // key element
            Node payloadDescription = document.createTextNode(PayloadEnum.DESCRIPTION.toString());
            keyPayloadDescriptionElement.appendChild(payloadDescription);
            dictElement.appendChild(keyPayloadDescriptionElement);
            Element stringPayloadDescriptionElement = document.createElement(ConfigEnum.STRING.toString()); // string element
            String description = emailPart + "'s " + configType.toString() + " settings";
            payloadDescription = document.createTextNode(description);
            stringPayloadDescriptionElement.appendChild(payloadDescription);
            dictElement.appendChild(stringPayloadDescriptionElement);

            // PayloadDisplayName
            Element keyPayloadDisplayNameElement = document.createElement(ConfigEnum.KEY.toString()); // key element
            Node payloadDisplayName = document.createTextNode(PayloadEnum.DISPLAY_NAME.toString());
            keyPayloadDisplayNameElement.appendChild(payloadDisplayName);
            dictElement.appendChild(keyPayloadDisplayNameElement);
            Element stringPayloadDisplayNameElement = document.createElement(ConfigEnum.STRING.toString()); // string element
            String displayName = user.getMail() + " " + configType.toString();
            payloadDisplayName = document.createTextNode(displayName);
            stringPayloadDisplayNameElement.appendChild(payloadDisplayName);
            dictElement.appendChild(stringPayloadDisplayNameElement);

            // PayloadIdentifier
            Element keyPayloadIdentifierElement = document.createElement(ConfigEnum.KEY.toString()); // key element
            Node payloadIdentifier = document.createTextNode(PayloadEnum.IDENTIFIER.toString());
            keyPayloadIdentifierElement.appendChild(payloadIdentifier);
            dictElement.appendChild(keyPayloadIdentifierElement);
            Element stringPayloadIdentifierElement = document.createElement(ConfigEnum.STRING.toString()); // string element
            String identifier = domain.getDomainName() + "." + configType.toString() + ".account." + emailPart;
            payloadIdentifier = document.createTextNode(identifier);
            stringPayloadIdentifierElement.appendChild(payloadIdentifier);
            dictElement.appendChild(stringPayloadIdentifierElement);

            // PayloadOrganization
            Element keyPayloadOrganizationElement = document.createElement(ConfigEnum.KEY.toString()); // key element
            Node payloadOrganization = document.createTextNode(PayloadEnum.ORGANIZATION.toString());
            keyPayloadOrganizationElement.appendChild(payloadOrganization);
            dictElement.appendChild(keyPayloadOrganizationElement);
            Element stringPayloadOrganizationElement = document.createElement(ConfigEnum.STRING.toString()); // string element
            payloadOrganization = document.createTextNode(domain.getDomainName());
            stringPayloadOrganizationElement.appendChild(payloadOrganization);
            dictElement.appendChild(stringPayloadOrganizationElement);

            // PayloadType
            Element keyPayloadTypeElement = document.createElement(ConfigEnum.KEY.toString()); // key element
            Node payloadType = document.createTextNode(PayloadEnum.TYPE.toString());
            keyPayloadTypeElement.appendChild(payloadType);
            dictElement.appendChild(keyPayloadTypeElement);
            Element stringPayloadTypeElement = document.createElement(ConfigEnum.STRING.toString()); // string element
            payloadType = document.createTextNode(PayloadEnum.TYPE_VALUE.toString());
            stringPayloadTypeElement.appendChild(payloadType);
            dictElement.appendChild(stringPayloadTypeElement);

            // PayloadUUID
            Element keyPayloadUUIDElement = document.createElement(ConfigEnum.KEY.toString()); // key element
            Node payloadUUID = document.createTextNode(PayloadEnum.UUID.toString());
            keyPayloadUUIDElement.appendChild(payloadUUID);
            dictElement.appendChild(keyPayloadUUIDElement);
            Element stringPayloadUUIDElement = document.createElement(ConfigEnum.STRING.toString()); // string element
            payloadUUID = document.createTextNode(user.getId());
            stringPayloadUUIDElement.appendChild(payloadUUID);
            dictElement.appendChild(stringPayloadUUIDElement);

            // PayloadVersion
            Element keyPayloadVersionElement = document.createElement(ConfigEnum.KEY.toString()); // key element
            Node payloadVersion = document.createTextNode(PayloadEnum.VERSION.toString());
            keyPayloadVersionElement.appendChild(payloadVersion);
            dictElement.appendChild(keyPayloadVersionElement);
            Element stringPayloadVersionElement = document.createElement(ConfigEnum.INTEGER.toString()); // string element
            payloadVersion = document.createTextNode("1");
            stringPayloadVersionElement.appendChild(payloadVersion);
            dictElement.appendChild(stringPayloadVersionElement);

            // add dict element to plist element
            plistElement.appendChild(dictElement);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = null;
            try {
                transformer = transformerFactory.newTransformer();
            } catch (TransformerConfigurationException tce) {
                throw ServiceException.FAILURE("Exception occured creating transformer", tce);
            }
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            DOMImplementation domImpl = document.getImplementation();
            DocumentType doctype = domImpl.createDocumentType("doctype",
                    "-//Apple//DTD PLIST 1.0//EN",
                    "http://www.apple.com/DTDs/PropertyList-1.0.dtd");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                DOMSource domSource = new DOMSource(document);
                Result responseStream = new StreamResult(baos);
                transformer.transform(domSource, responseStream);
                byte[] signedConfig = signConfig(domain, server, baos.toByteArray());
                context.resp.getOutputStream().write(signedConfig);
            } catch (IOException ioe) {
                throw ServiceException.FAILURE("Exception occured while getting writer", ioe);
            } catch (TransformerException te) {
                throw ServiceException.FAILURE("Exception occured while transforming dom", te);
            } catch (Exception e) {
                throw ServiceException.FAILURE("Unhandled exception occured", e);
            }
        } catch (ParserConfigurationException pce) {
            throw ServiceException.FAILURE("Exception occured while creating mobileconfig content", pce);
        }
    }

    private byte[] signConfig(Domain domain, Server server, byte[] config) {
        byte[] signedConfig = config;
        String certStr = null;
        String pvtKeyStr = null;
        if (domain != null) {
            certStr = domain.getMobileConfigSigningCertificate();
            pvtKeyStr = domain.getMobileConfigSigningKey();
            if (StringUtil.isNullOrEmpty(certStr) || StringUtil.isNullOrEmpty(pvtKeyStr)) {
                certStr = domain.getSSLCertificate();
                pvtKeyStr = domain.getSSLPrivateKey();
            }
            if ((StringUtil.isNullOrEmpty(certStr) || StringUtil.isNullOrEmpty(pvtKeyStr)) 
                    && server != null) {
                certStr = server.getSSLCertificate();
                pvtKeyStr = server.getSSLPrivateKey();
            }
        }

        if (!StringUtil.isNullOrEmpty(certStr) && !StringUtil.isNullOrEmpty(pvtKeyStr)) {
            try (InputStream targetStream = new ByteArrayInputStream(certStr.getBytes())) {
                CertificateFactory certFactory = CertificateFactory.getInstance(SmimeConstants.PUB_CERT_TYPE);
                X509Certificate cert = (X509Certificate) certFactory.generateCertificate(targetStream);
                StringReader reader = new StringReader(pvtKeyStr);
                PrivateKey privateKey = null;
                try (PEMParser pp = new PEMParser(reader)) {
                    Object pemKP = pp.readObject();
                    JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                    PrivateKeyInfo pkInfo = null;
                    if (pemKP instanceof PrivateKeyInfo) {
                        pkInfo = (PrivateKeyInfo) pemKP;
                    } else {
                        pkInfo = ((PEMKeyPair) pemKP).getPrivateKeyInfo();
                    }
                    privateKey = converter.getPrivateKey(pkInfo);
                }
                signedConfig = DataSigner.signData(config, cert, privateKey);
            } catch (IOException | CertificateException | OperatorCreationException | CMSException e) {
                ZimbraLog.misc.debug("exception occurred during signing config", e);
            }
        } else {
            ZimbraLog.misc.debug("SSLCertificate/SSLPrivateKey is not set, config will not be signed");
        }
        return signedConfig;
    }

    public static Node getDictForCaldavAndCarddav(Document document, String emailPart, Account user, Server server, ConfigType configType, Domain domain) throws ServiceException {
        if (configType != ConfigType.CALDAV &&  configType != ConfigType.CARDDAV) {
            throw ServiceException.INVALID_REQUEST("Must be caldav or carddav", null);
        }
        // dict
        Element dictElement = document.createElement(ConfigEnum.DICT.toString()); // element

        // CalDAVAccountDescription/CardDAVAccountDescription
        Element keyDavAccountDescriptionElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        String key;
        if (configType == ConfigType.CALDAV) {
            key = CaldavEnum.ACCOUNT_DESCRIPTION.toString();
        } else {
            key = CarddavEnum.ACCOUNT_DESCRIPTION.toString();
        }
        Node accountDescription = document.createTextNode(key);
        keyDavAccountDescriptionElement.appendChild(accountDescription);
        dictElement.appendChild(keyDavAccountDescriptionElement);
        Element stringDavAccountDescriptionElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        String description = emailPart + "'s " + configType.toString().toUpperCase();
        accountDescription = document.createTextNode(description);
        stringDavAccountDescriptionElement.appendChild(accountDescription);
        dictElement.appendChild(stringDavAccountDescriptionElement);

        // CalDAVHostName/CardDAVHostName
        Element keyDavHostnameElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        if (configType == ConfigType.CALDAV) {
            key = CaldavEnum.HOSTNAME.toString();
        } else {
            key = CarddavEnum.HOSTNAME.toString();
        }
        Node davHostname = document.createTextNode(key);
        keyDavHostnameElement.appendChild(davHostname);
        dictElement.appendChild(keyDavHostnameElement);
        Element stringDavHostNameElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        String hostname = getServiceHostname(server, domain);
        davHostname = document.createTextNode(hostname);
        stringDavHostNameElement.appendChild(davHostname);
        dictElement.appendChild(stringDavHostNameElement);

        // CalDAVPassword/CardDAVPassword
        Element keyDavPasswordElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        if (configType == ConfigType.CALDAV) {
            key = CaldavEnum.PASSWORD.toString();
        } else {
            key = CarddavEnum.PASSWORD.toString();
        }
        Node davPassword = document.createTextNode(key);
        keyDavPasswordElement.appendChild(davPassword);
        dictElement.appendChild(keyDavPasswordElement);
        Element stringDavPasswordElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        davPassword = document.createTextNode("");
        stringDavPasswordElement.appendChild(davPassword);
        dictElement.appendChild(stringDavPasswordElement);

        // CalDAVPort/CardDAVPort
        Element keyDavPortElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        if (configType == ConfigType.CALDAV) {
            key = CaldavEnum.PORT.toString();
        } else {
            key = CarddavEnum.PORT.toString();
        }
        Node davPort = document.createTextNode(key);
        keyDavPortElement.appendChild(davPort);
        dictElement.appendChild(keyDavPortElement);
        Element stringDavPortElement = document.createElement(ConfigEnum.INTEGER.toString()); // string element
        String port = String.valueOf(server.getMailSSLProxyPort());
        davPort = document.createTextNode(port);
        stringDavPortElement.appendChild(davPort);
        dictElement.appendChild(stringDavPortElement);

        // CalDAVPrincipalURL/CardDAVPrincipalURL
        Element keyDavPrincipalURLElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        if (configType == ConfigType.CALDAV) {
            key = CaldavEnum.PRINCIPAL_URL.toString();
        } else {
            key = CarddavEnum.PRINCIPAL_URL.toString();
        }
        Node davPrincipalURL = document.createTextNode(key);
        keyDavPrincipalURLElement.appendChild(davPrincipalURL);
        dictElement.appendChild(keyDavPrincipalURLElement);
        Element stringDavPrincipalURLElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        String davUrl = "/dav/" + user.getMail();
        if (configType == ConfigType.CALDAV) {
            davUrl += "/Calendar/";
        } else {
            davUrl += "/Contacts/";
        }
        davPrincipalURL = document.createTextNode(davUrl);
        stringDavPrincipalURLElement.appendChild(davPrincipalURL);
        dictElement.appendChild(stringDavPrincipalURLElement);

        // CalDAVUseSSL/CardDAVUseSSL
        Element keyDavUseSSLElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        if (configType == ConfigType.CALDAV) {
            key = CaldavEnum.USE_SSL.toString();
        } else {
            key = CarddavEnum.USE_SSL.toString();
        }
        Node davUseSSL = document.createTextNode(key);
        keyDavUseSSLElement.appendChild(davUseSSL);
        dictElement.appendChild(keyDavUseSSLElement);
        Element stringDavUseSSLElement = document.createElement(Boolean.TRUE.toString()); // string element
        dictElement.appendChild(stringDavUseSSLElement);

        // CalDAVUsername
        Element keyDavUsernameElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        if (configType == ConfigType.CALDAV) {
            key = CaldavEnum.USERNAME.toString();
        } else {
            key = CarddavEnum.USERNAME.toString();
        }
        Node davUsername = document.createTextNode(key);
        keyDavUsernameElement.appendChild(davUsername);
        dictElement.appendChild(keyDavUsernameElement);
        Element stringDavUsernameElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        davUsername = document.createTextNode(user.getMail());
        stringDavUsernameElement.appendChild(davUsername);
        dictElement.appendChild(stringDavUsernameElement);

        // add payload elements in the dict element for caldav/carddav
        appendPayloadFieldsForCaldavCarddavAndImap(document, dictElement, user, configType, emailPart, domain);

        return dictElement;
    }

    public static Node getDictForImap(Document document, String emailPart, Account user, Server server, ConfigType configType, Domain domain) throws ServiceException {
        // dict
        Element dictElement = document.createElement(ConfigEnum.DICT.toString()); // element

        // EmailAccountDescription
        Element keyAccountDescriptionElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node accountDescription = document.createTextNode(ImapEnum.ACCOUNT_DESCRIPTION.toString());
        keyAccountDescriptionElement.appendChild(accountDescription);
        dictElement.appendChild(keyAccountDescriptionElement);
        Element stringAccountDescriptionElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        String description = emailPart + "'s " + configType.toString().toUpperCase() + " email account settings";
        accountDescription = document.createTextNode(description);
        stringAccountDescriptionElement.appendChild(accountDescription);
        dictElement.appendChild(stringAccountDescriptionElement);

        // EmailAccountName
        Element keyAccountNameElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node accountName = document.createTextNode(ImapEnum.ACCOUNT_NAME.toString());
        keyAccountNameElement.appendChild(accountName);
        dictElement.appendChild(keyAccountNameElement);
        Element stringAccountNameElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        String acctName = emailPart + " email account";
        accountName = document.createTextNode(acctName);
        stringAccountNameElement.appendChild(accountName);
        dictElement.appendChild(stringAccountNameElement);

        // EmailAccountType
        Element keyAccountTypeElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node accountType = document.createTextNode(ImapEnum.ACCOUNT_TYPE.toString());
        keyAccountTypeElement.appendChild(accountType);
        dictElement.appendChild(keyAccountTypeElement);
        Element stringAccountTypeElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        accountType = document.createTextNode(EmailAccountTypeEnum.EMAIL_TYPE_IMAP.toString()); // currently zcs is going to support imap only
        stringAccountTypeElement.appendChild(accountType);
        dictElement.appendChild(stringAccountTypeElement);

        // EmailAddress
        Element keyEmailAddressElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node emailAddress = document.createTextNode(ImapEnum.ADDRESS.toString());
        keyEmailAddressElement.appendChild(emailAddress);
        dictElement.appendChild(keyEmailAddressElement);
        Element stringEmailAddressElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        emailAddress = document.createTextNode(user.getMail());
        stringEmailAddressElement.appendChild(emailAddress);
        dictElement.appendChild(stringEmailAddressElement);

        // IncomingMailServerAuthentication
        Element keyIncomingMailServerAuthenticationElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node incomingMailServerAuthentication = document.createTextNode(ImapEnum.INCOMING_MAIL_SERVER_AUTHENTICATION.toString());
        keyIncomingMailServerAuthenticationElement.appendChild(incomingMailServerAuthentication);
        dictElement.appendChild(keyIncomingMailServerAuthenticationElement);
        Element stringIncomingMailServerAuthenticationElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        incomingMailServerAuthentication = document.createTextNode(EmailAuthEnum.PASSWORD.toString());
        stringIncomingMailServerAuthenticationElement.appendChild(incomingMailServerAuthentication);
        dictElement.appendChild(stringIncomingMailServerAuthenticationElement);

        // IncomingMailServerHostName
        Element keyIncomingMailServerHostNameElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node incomingMailServerHostName = document.createTextNode(ImapEnum.INCOMING_MAIL_SERVER_HOST_NAME.toString());
        keyIncomingMailServerHostNameElement.appendChild(incomingMailServerHostName);
        dictElement.appendChild(keyIncomingMailServerHostNameElement);
        Element stringIncomingMailServerHostNameElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        String incomingHostname = getServiceHostname(server, domain);
        incomingMailServerHostName = document.createTextNode(incomingHostname);
        stringIncomingMailServerHostNameElement.appendChild(incomingMailServerHostName);
        dictElement.appendChild(stringIncomingMailServerHostNameElement);

        // IncomingMailServerPortNumber
        Element keyIncomingMailServerPortNumberElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node incomingMailServerPortNumber = document.createTextNode(ImapEnum.INCOMING_MAIL_SERVER_PORT_NUMBER.toString());
        keyIncomingMailServerPortNumberElement.appendChild(incomingMailServerPortNumber);
        dictElement.appendChild(keyIncomingMailServerPortNumberElement);
        Element stringIncomingMailServerPortNumberElement = document.createElement(ConfigEnum.INTEGER.toString()); // string element
        String incomingPort = server.getImapSSLProxyBindPortAsString();
        if (!server.isImapSSLServerEnabled()) {
            incomingPort = server.getImapProxyBindPortAsString();
        }
        incomingMailServerPortNumber = document.createTextNode(incomingPort);
        stringIncomingMailServerPortNumberElement.appendChild(incomingMailServerPortNumber);
        dictElement.appendChild(stringIncomingMailServerPortNumberElement);

        // IncomingMailServerUseSSL
        Element keyIncomingMailServerUseSSLElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node incomingMailServerUseSSL = document.createTextNode(ImapEnum.INCOMING_MAIL_SERVER_USE_SSL.toString());
        keyIncomingMailServerUseSSLElement.appendChild(incomingMailServerUseSSL);
        dictElement.appendChild(keyIncomingMailServerUseSSLElement);
        Element stringDavUseSSLElement = document.createElement(Boolean.TRUE.toString()); // string element
        if (!server.isImapSSLServerEnabled()) {
            stringDavUseSSLElement = document.createElement(Boolean.FALSE.toString());
        }
        dictElement.appendChild(stringDavUseSSLElement);

        // IncomingMailServerUsername
        Element keyIncomingMailServerUsernameElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node incomingMailServerUsername = document.createTextNode(ImapEnum.INCOMING_MAIL_SERVER_USERNAME.toString());
        keyIncomingMailServerUsernameElement.appendChild(incomingMailServerUsername);
        dictElement.appendChild(keyIncomingMailServerUsernameElement);
        Element stringIncomingMailServerUsernameElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        incomingMailServerUsername = document.createTextNode(user.getMail());
        stringIncomingMailServerUsernameElement.appendChild(incomingMailServerUsername);
        dictElement.appendChild(stringIncomingMailServerUsernameElement);

        // IncomingPassword
        Element keyIncomingPasswordElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node incomingPassword = document.createTextNode(ImapEnum.INCOMING_PASSWORD.toString());
        keyIncomingPasswordElement.appendChild(incomingPassword);
        dictElement.appendChild(keyIncomingPasswordElement);
        Element stringIncomingPasswordElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        incomingPassword = document.createTextNode("");
        stringIncomingPasswordElement.appendChild(incomingPassword);
        dictElement.appendChild(stringIncomingPasswordElement);

        // OutgoingMailServerAuthentication
        Element keyOutgoingMailServerAuthenticationElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node outgoingMailServerAuthentication = document.createTextNode(ImapEnum.OUTGOING_MAIL_SERVER_AUTHENTICATION.toString());
        keyOutgoingMailServerAuthenticationElement.appendChild(outgoingMailServerAuthentication);
        dictElement.appendChild(keyOutgoingMailServerAuthenticationElement);
        Element stringOutgoingMailServerAuthenticationElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        outgoingMailServerAuthentication = document.createTextNode(EmailAuthEnum.PASSWORD.toString());
        stringOutgoingMailServerAuthenticationElement.appendChild(outgoingMailServerAuthentication);
        dictElement.appendChild(stringOutgoingMailServerAuthenticationElement);

        // OutgoingMailServerHostName
        Element keyOutgoingMailServerHostNameElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node outgoingMailServerHostName = document.createTextNode(ImapEnum.OUTGOING_MAIL_SERVER_HOST_NAME.toString());
        keyOutgoingMailServerHostNameElement.appendChild(outgoingMailServerHostName);
        dictElement.appendChild(keyOutgoingMailServerHostNameElement);
        Element stringOutgoingMailServerHostNameElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        String smtpHostName = server.getSmtpHostname()[0];
        if (!StringUtil.isNullOrEmpty(domain.getSMTPPublicServiceHostname())) {
            smtpHostName = domain.getSMTPPublicServiceHostname();
        }
        outgoingMailServerHostName = document.createTextNode(smtpHostName);
        stringOutgoingMailServerHostNameElement.appendChild(outgoingMailServerHostName);
        dictElement.appendChild(stringOutgoingMailServerHostNameElement);

        // OutgoingMailServerPortNumber
        Element keyOutgoingMailServerPortNumberElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node outgoingMailServerPortNumber = document.createTextNode(ImapEnum.OUTGOING_MAIL_SERVER_PORT_NUMBER.toString());
        keyOutgoingMailServerPortNumberElement.appendChild(outgoingMailServerPortNumber);
        dictElement.appendChild(keyOutgoingMailServerPortNumberElement);
        Element stringOutgoingMailServerPortNumberElement = document.createElement(ConfigEnum.INTEGER.toString()); // string element
        String outgoingPort = "587";
        if (server.getMtaTlsSecurityLevel() == MtaTlsSecurityLevel.none) {
            outgoingPort = String.valueOf(server.getSmtpPort());
        }
        if (!StringUtil.isNullOrEmpty(domain.getSMTPPublicServicePortAsString())) {
            outgoingPort = domain.getSMTPPublicServicePortAsString();
        }
        outgoingMailServerPortNumber = document.createTextNode(outgoingPort);
        stringOutgoingMailServerPortNumberElement.appendChild(outgoingMailServerPortNumber);
        dictElement.appendChild(stringOutgoingMailServerPortNumberElement);

        // OutgoingMailServerUseSSL
        Element keyOutgoingMailServerUseSSLElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node outgoingMailServerUseSSL = document.createTextNode(ImapEnum.OUTGOING_MAIL_SERVER_USE_SSL.toString());
        keyOutgoingMailServerUseSSLElement.appendChild(outgoingMailServerUseSSL);
        dictElement.appendChild(keyOutgoingMailServerUseSSLElement);
        Boolean useSSL = true;
        // server.getMtaTlsSecurityLevel() is incorrect to check on mailbox server. however keeping it for backward compatibility. 
        if (server.getMtaTlsSecurityLevel() == MtaTlsSecurityLevel.none) {
            useSSL = false;
        }
        if (domain.getSMTPPublicServiceProtocol() != null) {
            if (domain.getSMTPPublicServiceProtocol().isSsl() || domain.getSMTPPublicServiceProtocol().isTls()) {
                useSSL = true;
            } else if (domain.getSMTPPublicServiceProtocol().isNone()) {
                useSSL = false;
            }
        }
        Element stringOutgoingMailServerUseSSLElement = document.createElement(useSSL.toString()); // string element
        dictElement.appendChild(stringOutgoingMailServerUseSSLElement);

        // OutgoingMailServerUsername
        Element keyOutgoingMailServerUsernameElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node outgoingMailServerUsername = document.createTextNode(ImapEnum.OUTGOING_MAIL_SERVER_USERNAME.toString());
        keyOutgoingMailServerUsernameElement.appendChild(outgoingMailServerUsername);
        dictElement.appendChild(keyOutgoingMailServerUsernameElement);
        Element stringOutgoingMailServerUsernameElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        outgoingMailServerUsername = document.createTextNode(user.getMail());
        stringOutgoingMailServerUsernameElement.appendChild(outgoingMailServerUsername);
        dictElement.appendChild(stringOutgoingMailServerUsernameElement);

        // OutgoingPassword
        Element keyOutgoingPasswordElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node outgoingPassword = document.createTextNode(ImapEnum.OUTGOING_PASSWORD.toString());
        keyOutgoingPasswordElement.appendChild(outgoingPassword);
        dictElement.appendChild(keyOutgoingPasswordElement);
        Element stringOutgoingPasswordElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        outgoingPassword = document.createTextNode("");
        stringOutgoingPasswordElement.appendChild(outgoingPassword);
        dictElement.appendChild(stringOutgoingPasswordElement);

        // OutgoingPasswordSameAsIncomingPassword
        Element keyOutgoingPasswordSameAsIncomingPasswordElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node outgoingPasswordSameAsIncomingPassword = document.createTextNode(ImapEnum.OUTGOING_PASSWORD_SAME_AS_INCOMING_PASSWORD.toString());
        keyOutgoingPasswordSameAsIncomingPasswordElement.appendChild(outgoingPasswordSameAsIncomingPassword);
        dictElement.appendChild(keyOutgoingPasswordSameAsIncomingPasswordElement);
        Element stringOutgoingPasswordSameAsIncomingPasswordElement = document.createElement(Boolean.TRUE.toString()); // string element
        dictElement.appendChild(stringOutgoingPasswordSameAsIncomingPasswordElement);

        // add payload elements in the dict element for imap
        appendPayloadFieldsForCaldavCarddavAndImap(document, dictElement, user, configType, emailPart, domain);

        // PreventAppSheet
        Element keyPreventAppSheetElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node preventAppSheet = document.createTextNode(ImapEnum.PREVENT_APP_SHEET.toString());
        keyPreventAppSheetElement.appendChild(preventAppSheet);
        dictElement.appendChild(keyPreventAppSheetElement);
        Element stringPreventAppSheetElement = document.createElement(Boolean.FALSE.toString()); // string element
        dictElement.appendChild(stringPreventAppSheetElement);

        // PreventMove
        Element keyPreventMoveElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node preventMove = document.createTextNode(ImapEnum.PREVENT_MOVE.toString());
        keyPreventMoveElement.appendChild(preventMove);
        dictElement.appendChild(keyPreventMoveElement);
        Element stringPreventMoveElement = document.createElement(Boolean.FALSE.toString()); // string element
        dictElement.appendChild(stringPreventMoveElement);

        // SMIMEEnablePerMessageSwitch
        Element keySMIMEEnablePerMessageSwitchElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node smimeEnablePerMessageSwitch = document.createTextNode(ImapEnum.SMIME_ENABLE_PER_MESSAGE_SWITCH.toString());
        keySMIMEEnablePerMessageSwitchElement.appendChild(smimeEnablePerMessageSwitch);
        dictElement.appendChild(keySMIMEEnablePerMessageSwitchElement);
        Element stringSMIMEEnablePerMessageSwitchElement = document.createElement(Boolean.FALSE.toString()); // string element
        dictElement.appendChild(stringSMIMEEnablePerMessageSwitchElement);

        // SMIMEEnabled - always false as we are not supporting smime with imap
        Element keySMIMEEnableElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node smimeEnable = document.createTextNode(ImapEnum.SMIME_ENABLED.toString());
        keySMIMEEnableElement.appendChild(smimeEnable);
        dictElement.appendChild(keySMIMEEnableElement);
        Element stringSMIMEEnableElement = document.createElement(Boolean.FALSE.toString()); // string element
        dictElement.appendChild(stringSMIMEEnableElement);

        // SMIMEEncryptionCertificateUUID
        Element keySMIMEEncryptionCertificateUUIDElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node smimeEncryptionCertificateUUID = document.createTextNode(ImapEnum.SMIME_ENCRYPTION_CERTIFICATE_UUID.toString());
        keySMIMEEncryptionCertificateUUIDElement.appendChild(smimeEncryptionCertificateUUID);
        dictElement.appendChild(keySMIMEEncryptionCertificateUUIDElement);
        Element stringSMIMEEncryptionCertificateUUIDElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        Node certificateUUID = document.createTextNode("");
        stringSMIMEEncryptionCertificateUUIDElement.appendChild(certificateUUID);
        dictElement.appendChild(stringSMIMEEncryptionCertificateUUIDElement);

        // SMIMEEncryptionEnabled
        Element keySMIMEEncryptionEnabledElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node smimeEncryptionEnabled = document.createTextNode(ImapEnum.SMIME_ENCRYPTION_ENABLED.toString());
        keySMIMEEncryptionEnabledElement.appendChild(smimeEncryptionEnabled);
        dictElement.appendChild(keySMIMEEncryptionEnabledElement);
        Element stringSMIMEEncryptionEnabledElement = document.createElement(Boolean.FALSE.toString()); // string element
        dictElement.appendChild(stringSMIMEEncryptionEnabledElement);

        // SMIMESigningCertificateUUID
        Element keySMIMESigningCertificateUUIDElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node smimeSigningCertificateUUID = document.createTextNode(ImapEnum.SMIME_SIGNING_CERTIFICATE_UUID.toString());
        keySMIMESigningCertificateUUIDElement.appendChild(smimeSigningCertificateUUID);
        dictElement.appendChild(keySMIMESigningCertificateUUIDElement);
        Element stringSMIMESigningCertificateUUIDElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        Node signingCertificateUUID = document.createTextNode("");
        stringSMIMESigningCertificateUUIDElement.appendChild(signingCertificateUUID);
        dictElement.appendChild(stringSMIMESigningCertificateUUIDElement);

        // SMIMESigningEnabled
        Element keySMIMESigningEnabledElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node smimeSigningEnabled = document.createTextNode(ImapEnum.SMIME_SIGNING_ENABLED.toString());
        keySMIMESigningEnabledElement.appendChild(smimeSigningEnabled);
        dictElement.appendChild(keySMIMESigningEnabledElement);
        Element stringSMIMESigningEnabledElement = document.createElement(Boolean.FALSE.toString()); // string element
        dictElement.appendChild(stringSMIMESigningEnabledElement);

        // allowMailDrop
        Element keyAllowMailDropElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node allowMailDrop = document.createTextNode(ImapEnum.ALLOW_MAIL_DROP.toString());
        keyAllowMailDropElement.appendChild(allowMailDrop);
        dictElement.appendChild(keyAllowMailDropElement);
        Element stringAllowMailDropElement = document.createElement(Boolean.FALSE.toString()); // string element
        dictElement.appendChild(stringAllowMailDropElement);

        // disableMailRecentsSyncing
        Element keyDisableMailRecentsSyncingElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node disableMailRecentsSyncing = document.createTextNode(ImapEnum.DISABLE_MAIL_RECENTS_SYNCING.toString());
        keyDisableMailRecentsSyncingElement.appendChild(disableMailRecentsSyncing);
        dictElement.appendChild(keyDisableMailRecentsSyncingElement);
        Element stringDisableMailRecentsSyncingElement = document.createElement(Boolean.FALSE.toString()); // string element
        dictElement.appendChild(stringDisableMailRecentsSyncingElement);

        return dictElement;
    }

    private static void appendPayloadFieldsForCaldavCarddavAndImap(Document document, Element dictElement, Account user, ConfigType configType, String emailPart, Domain domain) {
        // PayloadDescription
        Element keyPayloadDescriptionElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node payloadDescription = document.createTextNode(PayloadEnum.DESCRIPTION.toString());
        keyPayloadDescriptionElement.appendChild(payloadDescription);
        dictElement.appendChild(keyPayloadDescriptionElement);
        Element stringPayloadDescriptionElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        String description = "Configures " + configType.toString() + " profile for " + emailPart;
        payloadDescription = document.createTextNode(description);
        stringPayloadDescriptionElement.appendChild(payloadDescription);
        dictElement.appendChild(stringPayloadDescriptionElement);

        // PayloadDisplayName
        Element keyPayloadDisplayNameElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node payloadDisplayName = document.createTextNode(PayloadEnum.DISPLAY_NAME.toString());
        keyPayloadDisplayNameElement.appendChild(payloadDisplayName);
        dictElement.appendChild(keyPayloadDisplayNameElement);
        Element stringPayloadDisplayNameElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        String displayName = configType.toString() + " " + emailPart;
        payloadDisplayName = document.createTextNode(displayName);
        stringPayloadDisplayNameElement.appendChild(payloadDisplayName);
        dictElement.appendChild(stringPayloadDisplayNameElement);

        // PayloadIdentifier
        Element keyPayloadIdentifierElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node payloadIdentifier = document.createTextNode(PayloadEnum.IDENTIFIER.toString());
        keyPayloadIdentifierElement.appendChild(payloadIdentifier);
        dictElement.appendChild(keyPayloadIdentifierElement);
        Element stringPayloadIdentifierElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        String identifier = domain.getDomainName() + "." + configType.toString() + ".account." + emailPart;
        payloadIdentifier = document.createTextNode(identifier);
        stringPayloadIdentifierElement.appendChild(payloadIdentifier);
        dictElement.appendChild(stringPayloadIdentifierElement);

        // PayloadOrganization
        Element keyPayloadOrganizationElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node payloadOrganization = document.createTextNode(PayloadEnum.ORGANIZATION.toString());
        keyPayloadOrganizationElement.appendChild(payloadOrganization);
        dictElement.appendChild(keyPayloadOrganizationElement);
        Element stringPayloadOrganizationElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        payloadOrganization = document.createTextNode(domain.getDomainName());
        stringPayloadOrganizationElement.appendChild(payloadOrganization);
        dictElement.appendChild(stringPayloadOrganizationElement);

        // PayloadType
        Element keyPayloadTypeElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node payloadType = document.createTextNode(PayloadEnum.TYPE.toString());
        keyPayloadTypeElement.appendChild(payloadType);
        dictElement.appendChild(keyPayloadTypeElement);
        Element stringPayloadTypeElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        String payloadTypeValue = PayloadEnum.TYPE_VALUE.toString();
        if (configType == ConfigType.CALDAV) {
            payloadTypeValue = CaldavEnum.PAYLOAD_TYPE.toString();
        } else if (configType == ConfigType.CARDDAV) {
            payloadTypeValue = CarddavEnum.PAYLOAD_TYPE.toString();
        } else if (configType == ConfigType.IMAP) {
            payloadTypeValue = ImapEnum.PAYLOAD_TYPE.toString();
        }
        payloadType = document.createTextNode(payloadTypeValue);
        stringPayloadTypeElement.appendChild(payloadType);
        dictElement.appendChild(stringPayloadTypeElement);

        // PayloadUUID
        Element keyPayloadUUIDElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node payloadUUID = document.createTextNode(PayloadEnum.UUID.toString());
        keyPayloadUUIDElement.appendChild(payloadUUID);
        dictElement.appendChild(keyPayloadUUIDElement);
        Element stringPayloadUUIDElement = document.createElement(ConfigEnum.STRING.toString()); // string element
        String uuid = user.getId() + "_" + configType.toString();
        payloadUUID = document.createTextNode(uuid);
        stringPayloadUUIDElement.appendChild(payloadUUID);
        dictElement.appendChild(stringPayloadUUIDElement);

        // PayloadVersion
        Element keyPayloadVersionElement = document.createElement(ConfigEnum.KEY.toString()); // key element
        Node payloadVersion = document.createTextNode(PayloadEnum.VERSION.toString());
        keyPayloadVersionElement.appendChild(payloadVersion);
        dictElement.appendChild(keyPayloadVersionElement);
        Element stringPayloadVersionElement = document.createElement(ConfigEnum.INTEGER.toString()); // string element
        payloadVersion = document.createTextNode("1");
        stringPayloadVersionElement.appendChild(payloadVersion);
        dictElement.appendChild(stringPayloadVersionElement);
    }

    @Override
    public boolean supportsSave() {
        return true;
    }

    public ConfigType validateConfigType(String configType) throws ServiceException {
        ConfigType type = ConfigType.fromString(configType);
        if (type == null) {
            throw ServiceException.INVALID_REQUEST("Invalid configType received", null);
        }
        return type;
    }

    public enum ConfigType {
        CALDAV ("caldav"),
        CARDDAV ("carddav"),
        IMAP ("imap"),
        DAV ("dav"),
        ALL ("all");

        private static Map<String, ConfigType> nameToConfigType = Maps.newHashMap();
        static {
            for (ConfigType v : ConfigType.values()) {
                nameToConfigType.put(v.toString(), v);
            }
        }

        private String name;
        private ConfigType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static ConfigType fromString(String name) {
            if (name == null) {
                name = "";
            }
            return nameToConfigType.get(name);
        }
    }

    private static final String PLIST_VERSION = "version";
    private static final String PLIST_VERSION_VALUE = "1.0";
    @XmlEnum
    public enum ConfigEnum {
        @XmlEnumValue("plist") PLIST ("plist"),
        @XmlEnumValue("dict") DICT ("dict"),
        @XmlEnumValue("key") KEY ("key"),
        @XmlEnumValue("array") ARRAY ("array"),
        @XmlEnumValue("string") STRING ("string"),
        @XmlEnumValue("integer") INTEGER ("integer");

        private static Map<String, ConfigEnum> nameToConfig = Maps.newHashMap();
        static {
            for (ConfigEnum v : ConfigEnum.values()) {
                nameToConfig.put(v.toString(), v);
            }
        }

        private String name;
        private ConfigEnum(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static ConfigEnum fromString(String name) {
            if (name == null) {
                name = "";
            }
            return nameToConfig.get(name);
        }
    }

    @XmlEnum
    public enum PayloadEnum {
        @XmlEnumValue("PayloadContent") CONTENT ("PayloadContent"),
        @XmlEnumValue("PayloadDescription") DESCRIPTION ("PayloadDescription"),
        @XmlEnumValue("PayloadDisplayName") DISPLAY_NAME ("PayloadDisplayName"),
        @XmlEnumValue("PayloadIdentifier") IDENTIFIER ("PayloadIdentifier"),
        @XmlEnumValue("PayloadOrganization") ORGANIZATION ("PayloadOrganization"),
        @XmlEnumValue("PayloadType") TYPE ("PayloadType"),
        @XmlEnumValue("Configuration") TYPE_VALUE ("Configuration"),
        @XmlEnumValue("PayloadUUID") UUID ("PayloadUUID"),
        @XmlEnumValue("PayloadVersion") VERSION ("PayloadVersion");

        private static Map<String, PayloadEnum> nameToPayload = Maps.newHashMap();
        static {
            for (PayloadEnum v : PayloadEnum.values()) {
                nameToPayload.put(v.toString(), v);
            }
        }

        private String name;
        private PayloadEnum(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static PayloadEnum fromString(String name) {
            if (name == null) {
                name = "";
            }
            return nameToPayload.get(name);
        }
    }

    @XmlEnum
    public enum CaldavEnum {
        @XmlEnumValue("CardDAVAccountDescription") ACCOUNT_DESCRIPTION ("CalDAVAccountDescription"),
        @XmlEnumValue("CalDAVHostName") HOSTNAME ("CalDAVHostName"),
        @XmlEnumValue("CalDAVPassword") PASSWORD ("CalDAVPassword"),
        @XmlEnumValue("CalDAVPort") PORT ("CalDAVPort"),
        @XmlEnumValue("CalDAVPrincipalURL") PRINCIPAL_URL ("CalDAVPrincipalURL"),
        @XmlEnumValue("CalDAVUseSSL") USE_SSL ("CalDAVUseSSL"),
        @XmlEnumValue("CalDAVUsername") USERNAME ("CalDAVUsername"),
        @XmlEnumValue("com.apple.caldav.account") PAYLOAD_TYPE ("com.apple.caldav.account");

        private static Map<String, CaldavEnum> nameToCaldav = Maps.newHashMap();

        static {
            for (CaldavEnum v : CaldavEnum.values()) {
                nameToCaldav.put(v.toString(), v);
            }
        }

        private String name;
        private CaldavEnum(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static CaldavEnum fromString(String name) {
            if (name == null) {
                name = "";
            }
            return nameToCaldav.get(name);
        }
    }

    @XmlEnum
    public enum CarddavEnum {
        @XmlEnumValue("CardDAVAccountDescription") ACCOUNT_DESCRIPTION ("CardDAVAccountDescription"),
        @XmlEnumValue("CardDAVHostName") HOSTNAME ("CardDAVHostName"),
        @XmlEnumValue("CardDAVPassword") PASSWORD ("CardDAVPassword"),
        @XmlEnumValue("CardDAVPort") PORT ("CardDAVPort"),
        @XmlEnumValue("CardDAVPrincipalURL") PRINCIPAL_URL ("CardDAVPrincipalURL"),
        @XmlEnumValue("CardDAVUseSSL") USE_SSL ("CardDAVUseSSL"),
        @XmlEnumValue("CardDAVUsername") USERNAME ("CardDAVUsername"),
        @XmlEnumValue("com.apple.carddav.account") PAYLOAD_TYPE ("com.apple.carddav.account");

        private static Map<String, CarddavEnum> nameToCarddav = Maps.newHashMap();

        static {
            for (CarddavEnum v : CarddavEnum.values()) {
                nameToCarddav.put(v.toString(), v);
            }
        }

        private String name;
        private CarddavEnum(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static CarddavEnum fromString(String name) {
            if (name == null) {
                name = "";
            }
            return nameToCarddav.get(name);
        }
    }

    @XmlEnum
    public enum EmailAuthEnum {
        @XmlEnumValue("EmailAuthPassword") PASSWORD ("EmailAuthPassword"),
        @XmlEnumValue("EmailAuthCRAMMD5") CRAMMD5 ("EmailAuthCRAMMD5"),
        @XmlEnumValue("EmailAuthNTLM") NTLM ("EmailAuthNTLM"),
        @XmlEnumValue("EmailAuthHTTPMD5") HTTPMD5 ("EmailAuthHTTPMD5"),
        @XmlEnumValue("EmailAuthNone") NONE ("EmailAuthNone");

        private static Map<String, EmailAuthEnum> nameToEmailAuth = Maps.newHashMap();

        static {
            for (EmailAuthEnum v : EmailAuthEnum.values()) {
                nameToEmailAuth.put(v.toString(), v);
            }
        }

        private String name;
        private EmailAuthEnum(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static EmailAuthEnum fromString(String name) {
            if (name == null) {
                name = "";
            }
            return nameToEmailAuth.get(name);
        }
    }

    @XmlEnum
    public enum ImapEnum {
        @XmlEnumValue("EmailAccountDescription") ACCOUNT_DESCRIPTION ("EmailAccountDescription"),
        @XmlEnumValue("EmailAccountName") ACCOUNT_NAME ("EmailAccountName"),
        @XmlEnumValue("EmailAccountType") ACCOUNT_TYPE ("EmailAccountType"),
        @XmlEnumValue("EmailAddress") ADDRESS ("EmailAddress"),
        @XmlEnumValue("IncomingMailServerAuthentication") INCOMING_MAIL_SERVER_AUTHENTICATION ("IncomingMailServerAuthentication"),
        @XmlEnumValue("IncomingMailServerHostName") INCOMING_MAIL_SERVER_HOST_NAME ("IncomingMailServerHostName"),
        @XmlEnumValue("IncomingMailServerIMAPPathPrefix") INCOMING_MAIL_SERVER_IMAP_PATH_PREFIX ("IncomingMailServerIMAPPathPrefix"),
        @XmlEnumValue("IncomingMailServerPortNumber") INCOMING_MAIL_SERVER_PORT_NUMBER ("IncomingMailServerPortNumber"),
        @XmlEnumValue("IncomingMailServerUseSSL") INCOMING_MAIL_SERVER_USE_SSL ("IncomingMailServerUseSSL"),
        @XmlEnumValue("IncomingMailServerUsername") INCOMING_MAIL_SERVER_USERNAME ("IncomingMailServerUsername"),
        @XmlEnumValue("IncomingPassword") INCOMING_PASSWORD ("IncomingPassword"),
        @XmlEnumValue("OutgoingMailServerAuthentication") OUTGOING_MAIL_SERVER_AUTHENTICATION ("OutgoingMailServerAuthentication"),
        @XmlEnumValue("OutgoingMailServerHostName") OUTGOING_MAIL_SERVER_HOST_NAME ("OutgoingMailServerHostName"),
        @XmlEnumValue("OutgoingMailServerPortNumber") OUTGOING_MAIL_SERVER_PORT_NUMBER ("OutgoingMailServerPortNumber"),
        @XmlEnumValue("OutgoingMailServerUseSSL") OUTGOING_MAIL_SERVER_USE_SSL ("OutgoingMailServerUseSSL"),
        @XmlEnumValue("OutgoingMailServerUsername") OUTGOING_MAIL_SERVER_USERNAME ("OutgoingMailServerUsername"),
        @XmlEnumValue("OutgoingPassword") OUTGOING_PASSWORD ("OutgoingPassword"),
        @XmlEnumValue("OutgoingPasswordSameAsIncomingPassword") OUTGOING_PASSWORD_SAME_AS_INCOMING_PASSWORD ("OutgoingPasswordSameAsIncomingPassword"),
        @XmlEnumValue("PreventAppSheet") PREVENT_APP_SHEET ("PreventAppSheet"),
        @XmlEnumValue("PreventMove") PREVENT_MOVE ("PreventMove"),
        @XmlEnumValue("SMIMEEnablePerMessageSwitch") SMIME_ENABLE_PER_MESSAGE_SWITCH ("SMIMEEnablePerMessageSwitch"),
        @XmlEnumValue("SMIMEEnabled") SMIME_ENABLED ("SMIMEEnabled"),
        @XmlEnumValue("SMIMEEncryptionCertificateUUID") SMIME_ENCRYPTION_CERTIFICATE_UUID ("SMIMEEncryptionCertificateUUID"),
        @XmlEnumValue("SMIMEEncryptionEnabled") SMIME_ENCRYPTION_ENABLED ("SMIMEEncryptionEnabled"),
        @XmlEnumValue("SMIMESigningCertificateUUID") SMIME_SIGNING_CERTIFICATE_UUID ("SMIMESigningCertificateUUID"),
        @XmlEnumValue("SMIMESigningEnabled") SMIME_SIGNING_ENABLED ("SMIMESigningEnabled"),
        @XmlEnumValue("allowMailDrop") ALLOW_MAIL_DROP ("allowMailDrop"),
        @XmlEnumValue("disableMailRecentsSyncing") DISABLE_MAIL_RECENTS_SYNCING ("disableMailRecentsSyncing"),
        @XmlEnumValue("com.apple.mail.managed") PAYLOAD_TYPE ("com.apple.mail.managed");

        private static Map<String, ImapEnum> nameToImap = Maps.newHashMap();

        static {
            for (ImapEnum v : ImapEnum.values()) {
                nameToImap.put(v.toString(), v);
            }
        }

        private String name;
        private ImapEnum(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static ImapEnum fromString(String name) {
            if (name == null) {
                name = "";
            }
            return nameToImap.get(name);
        }
    }

    @XmlEnum
    public enum EmailAccountTypeEnum {
        @XmlEnumValue("EmailTypePOP") EMAIL_TYPE_POP ("EmailTypePOP"),
        @XmlEnumValue("EmailTypeIMAP") EMAIL_TYPE_IMAP ("EmailTypeIMAP");

        private static Map<String, EmailAccountTypeEnum> nameToEmailAccountType = Maps.newHashMap();

        static {
            for (EmailAccountTypeEnum v : EmailAccountTypeEnum.values()) {
                nameToEmailAccountType.put(v.toString(), v);
            }
        }

        private String name;
        private EmailAccountTypeEnum(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static EmailAccountTypeEnum fromString(String name) {
            if (name == null) {
                name = "";
            }
            return nameToEmailAccountType.get(name);
        }
    }

    private static String getServiceHostname(Server server, Domain domain) {
        String hostname = domain.getPublicServiceHostname();
        if (StringUtil.isNullOrEmpty(hostname)) {
            hostname = server.getServiceHostname();
        }
        return hostname;
    }
}
