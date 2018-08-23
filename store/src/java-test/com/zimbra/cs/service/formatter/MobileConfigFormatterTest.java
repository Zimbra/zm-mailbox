/*
 * ***** BEGIN LICENSE BLOCK ***** Zimbra Collaboration Suite Server Copyright
 * (C) 2018 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>. *****
 * END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.formatter;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.collect.Maps;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;
import com.zimbra.cs.service.formatter.MobileConfigFormatter.ConfigType;

public class MobileConfigFormatterTest {
    private static Formatter formatter;
    private static final String USER_NAME = "test1@test.domain.com";
    private static final String DOMAIN_NAME = "test.domain.com";
    private static Account user;
    private static final String EMAIL_PART = "test1";
    private static Server server;
    private static Domain domain;
    private static DocumentBuilder docBuilder;
    private static Document document;
    private static TransformerFactory transformerFactory;
    private static Transformer transformer;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createDomain(DOMAIN_NAME, Maps.<String, Object>newHashMap());
        prov.createAccount(USER_NAME, "secret", Maps.<String, Object>newHashMap());
        formatter = new MobileConfigFormatter();
        user = prov.getAccount(USER_NAME);
        user.setMail(USER_NAME);
        server = prov.getServer(user);
        server.setMailSSLProxyPort(443);
        String[] smtpHostnames = {DOMAIN_NAME};
        server.setSmtpHostname(smtpHostnames);
        domain = prov.getDomain(user);
        domain.setDomainName(DOMAIN_NAME);
        domain.setPublicServiceHostname(DOMAIN_NAME);
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docBuilder = docFactory.newDocumentBuilder();
        transformerFactory = TransformerFactory.newInstance();
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        document = docBuilder.newDocument();
        transformer = transformerFactory.newTransformer();
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
    }

    @Test
    public void testGetType() throws Exception {
        Assert.assertEquals(FormatType.MOBILE_CONFIG, formatter.getType());
    }

    @Test
    public void testGetDefaultMimeTypes() throws Exception {
        String[] expectedArray = new String[] { MimeConstants.CT_TEXT_XML, "text/xml" };
        String[] actualArray = formatter.getDefaultMimeTypes();
        Assert.assertEquals(2, actualArray.length);
        Assert.assertArrayEquals(expectedArray, actualArray);
    }

    @Test
    public void testGetDictForCaldav() throws Exception {
        Element fakeParent = document.createElement("parent");

        Node dict = MobileConfigFormatter.getDictForCaldavAndCarddav(document, EMAIL_PART, user, server,
                ConfigType.CALDAV, domain);
        fakeParent.appendChild(dict);
        document.appendChild(fakeParent);

        DOMSource domSource = new DOMSource(document);
        StreamResult responseStream = new StreamResult(new StringWriter());
        transformer.transform(domSource, responseStream);

        String actual = responseStream.getWriter().toString();
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                + "<!DOCTYPE parent PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
                + "<parent>\n"
                + "    <dict>\n"
                + "        <key>CalDAVAccountDescription</key>\n"
                + "        <string>test1's CALDAV</string>\n"
                + "        <key>CalDAVHostName</key>\n"
                + "        <string>test.domain.com</string>\n"
                + "        <key>CalDAVPassword</key>\n"
                + "        <string/>\n"
                + "        <key>CalDAVPort</key>\n"
                + "        <integer>443</integer>\n"
                + "        <key>CalDAVPrincipalURL</key>\n"
                + "        <string>/dav/test1@test.domain.com/Calendar/</string>\n"
                + "        <key>CalDAVUseSSL</key>\n"
                + "        <true/>\n"
                + "        <key>CalDAVUsername</key>\n"
                + "        <string>test1@test.domain.com</string>\n"
                + "        <key>PayloadDescription</key>\n"
                + "        <string>Configures caldav profile for test1</string>\n"
                + "        <key>PayloadDisplayName</key>\n"
                + "        <string>caldav test1</string>\n"
                + "        <key>PayloadIdentifier</key>\n"
                + "        <string>test.domain.com.caldav.account.test1</string>\n"
                + "        <key>PayloadOrganization</key>\n"
                + "        <string>test.domain.com</string>\n"
                + "        <key>PayloadType</key>\n"
                + "        <string>com.apple.caldav.account</string>\n"
                + "        <key>PayloadUUID</key>\n"
                + "        <string>00000000-0000-0000-0000-000000000000_caldav</string>\n"
                + "        <key>PayloadVersion</key>\n"
                + "        <integer>1</integer>\n"
                + "    </dict>\n"
                + "</parent>\n";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetDictForCarddav() throws Exception {
        Element fakeParent = document.createElement("parent");

        Node dict = MobileConfigFormatter.getDictForCaldavAndCarddav(document, EMAIL_PART, user, server,
                ConfigType.CARDDAV, domain);
        fakeParent.appendChild(dict);
        document.appendChild(fakeParent);

        DOMSource domSource = new DOMSource(document);
        StreamResult responseStream = new StreamResult(new StringWriter());
        transformer.transform(domSource, responseStream);

        String actual = responseStream.getWriter().toString();
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                + "<!DOCTYPE parent PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
                + "<parent>\n"
                + "    <dict>\n"
                + "        <key>CardDAVAccountDescription</key>\n"
                + "        <string>test1's CARDDAV</string>\n"
                + "        <key>CardDAVHostName</key>\n"
                + "        <string>test.domain.com</string>\n"
                + "        <key>CardDAVPassword</key>\n"
                + "        <string/>\n"
                + "        <key>CardDAVPort</key>\n"
                + "        <integer>443</integer>\n"
                + "        <key>CardDAVPrincipalURL</key>\n"
                + "        <string>/dav/test1@test.domain.com/Contacts/</string>\n"
                + "        <key>CardDAVUseSSL</key>\n"
                + "        <true/>\n"
                + "        <key>CardDAVUsername</key>\n"
                + "        <string>test1@test.domain.com</string>\n"
                + "        <key>PayloadDescription</key>\n"
                + "        <string>Configures carddav profile for test1</string>\n"
                + "        <key>PayloadDisplayName</key>\n"
                + "        <string>carddav test1</string>\n"
                + "        <key>PayloadIdentifier</key>\n"
                + "        <string>test.domain.com.carddav.account.test1</string>\n"
                + "        <key>PayloadOrganization</key>\n"
                + "        <string>test.domain.com</string>\n"
                + "        <key>PayloadType</key>\n"
                + "        <string>com.apple.carddav.account</string>\n"
                + "        <key>PayloadUUID</key>\n"
                + "        <string>00000000-0000-0000-0000-000000000000_carddav</string>\n"
                + "        <key>PayloadVersion</key>\n"
                + "        <integer>1</integer>\n"
                + "    </dict>\n"
                + "</parent>\n";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetDictForImap() throws Exception {
        Element fakeParent = document.createElement("parent");

        Node dict = MobileConfigFormatter.getDictForImap(document, EMAIL_PART, user, server, ConfigType.IMAP, domain);
        fakeParent.appendChild(dict);
        document.appendChild(fakeParent);

        DOMSource domSource = new DOMSource(document);
        StreamResult responseStream = new StreamResult(new StringWriter());
        transformer.transform(domSource, responseStream);

        String actual = responseStream.getWriter().toString();
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                + "<!DOCTYPE parent PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
                + "<parent>\n"
                + "    <dict>\n"
                + "        <key>EmailAccountDescription</key>\n"
                + "        <string>test1's IMAP email account settings</string>\n"
                + "        <key>EmailAccountName</key>\n"
                + "        <string>test1 email account</string>\n"
                + "        <key>EmailAccountType</key>\n"
                + "        <string>EmailTypeIMAP</string>\n"
                + "        <key>EmailAddress</key>\n"
                + "        <string>test1@test.domain.com</string>\n"
                + "        <key>IncomingMailServerAuthentication</key>\n"
                + "        <string>EmailAuthPassword</string>\n"
                + "        <key>IncomingMailServerHostName</key>\n"
                + "        <string>test.domain.com</string>\n"
                + "        <key>IncomingMailServerPortNumber</key>\n"
                + "        <integer>993</integer>\n"
                + "        <key>IncomingMailServerUseSSL</key>\n"
                + "        <true/>\n"
                + "        <key>IncomingMailServerUsername</key>\n"
                + "        <string>test1@test.domain.com</string>\n"
                + "        <key>IncomingPassword</key>\n"
                + "        <string/>\n"
                + "        <key>OutgoingMailServerAuthentication</key>\n"
                + "        <string>EmailAuthPassword</string>\n"
                + "        <key>OutgoingMailServerHostName</key>\n"
                + "        <string>test.domain.com</string>\n"
                + "        <key>OutgoingMailServerPortNumber</key>\n"
                + "        <integer>587</integer>\n"
                + "        <key>OutgoingMailServerUseSSL</key>\n"
                + "        <true/>\n"
                + "        <key>OutgoingMailServerUsername</key>\n"
                + "        <string>test1@test.domain.com</string>\n"
                + "        <key>OutgoingPassword</key>\n"
                + "        <string/>\n"
                + "        <key>OutgoingPasswordSameAsIncomingPassword</key>\n"
                + "        <true/>\n"
                + "        <key>PayloadDescription</key>\n"
                + "        <string>Configures imap profile for test1</string>\n"
                + "        <key>PayloadDisplayName</key>\n"
                + "        <string>imap test1</string>\n"
                + "        <key>PayloadIdentifier</key>\n"
                + "        <string>test.domain.com.imap.account.test1</string>\n"
                + "        <key>PayloadOrganization</key>\n"
                + "        <string>test.domain.com</string>\n"
                + "        <key>PayloadType</key>\n"
                + "        <string>com.apple.mail.managed</string>\n"
                + "        <key>PayloadUUID</key>\n"
                + "        <string>00000000-0000-0000-0000-000000000000_imap</string>\n"
                + "        <key>PayloadVersion</key>\n"
                + "        <integer>1</integer>\n"
                + "        <key>PreventAppSheet</key>\n"
                + "        <false/>\n"
                + "        <key>PreventMove</key>\n"
                + "        <false/>\n"
                + "        <key>SMIMEEnablePerMessageSwitch</key>\n"
                + "        <false/>\n"
                + "        <key>SMIMEEnabled</key>\n"
                + "        <false/>\n"
                + "        <key>SMIMEEncryptionCertificateUUID</key>\n"
                + "        <string/>\n"
                + "        <key>SMIMEEncryptionEnabled</key>\n"
                + "        <false/>\n"
                + "        <key>SMIMESigningCertificateUUID</key>\n"
                + "        <string/>\n"
                + "        <key>SMIMESigningEnabled</key>\n"
                + "        <false/>\n"
                + "        <key>allowMailDrop</key>\n"
                + "        <false/>\n"
                + "        <key>disableMailRecentsSyncing</key>\n"
                + "        <false/>\n"
                + "    </dict>\n"
                + "</parent>\n";
        Assert.assertEquals(expected, actual);
    }
}
