/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap.admin;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Element;

import com.google.common.collect.Lists;
import com.zimbra.soap.admin.message.DumpSessionsResponse;
import com.zimbra.soap.admin.type.AccountSessionInfo;
import com.zimbra.soap.admin.type.InfoForSessionType;
import com.zimbra.soap.admin.type.SessionInfo;

/**
 * Unit test for {@link DumpSessionsResponse}.
 * Mostly checking the handling of @XmlAnyElement in {@link SessionInfo}
 * test xml files are hand generated and are not taken from real world data.
 */
public class DumpSessionsTest {

    private static final Logger LOG = LogManager.getLogger(DumpSessionsTest.class);

    private static Unmarshaller unmarshaller;
    private static Marshaller marshaller;

    static {
        Configurator.reconfigure();
        Configurator.setRootLevel(Level.INFO);
        Configurator.setLevel(LOG.getName(), Level.INFO);
    }

    @BeforeClass
    public static void init() throws Exception {
        JAXBContext jaxb = JAXBContext.newInstance(DumpSessionsResponse.class);
        unmarshaller = jaxb.createUnmarshaller();
        marshaller = jaxb.createMarshaller();
    }

    private static void checkImapSession(SessionInfo session,
            boolean nameAndIdPresent) {
        Assert.assertEquals("Create Date", 1300295279211L, session.getCreatedDate());
        Assert.assertEquals("Last Access Date", 1300295279212L, session.getLastAccessedDate());
        if (nameAndIdPresent) {
            Assert.assertEquals("Session Name", "user1@gren-elliots-macbook-pro.local", session.getName());
            Assert.assertEquals("Session Zimbra Id", "006584ae-cba0-400a-8414-f764ba3c7418", session.getZimbraId());
        } else {
            Assert.assertNull("Session Name", session.getName());
            Assert.assertNull("Session Zimbra Id", session.getZimbraId());
        }
        Assert.assertEquals("Session Id", "223", session.getSessionId());
        Map<QName, Object> extraAttribs = session.getExtraAttributes();
        Assert.assertEquals("Number of extra Attribs", 0, extraAttribs.size());
        List<Element> extraElements = session.getExtraElements();
        Assert.assertNotNull("Extra Elements", extraElements);
        Assert.assertEquals("Number of extra Elements", 1, extraElements.size());
        Element elem = extraElements.get(0);
        Assert.assertEquals("imap Element nodeName", "imap", elem.getNodeName());
    }

    @Test
    public void unmarshallDumpSessionsResponseTest()
    throws Exception {
        InputStream is = getClass().getResourceAsStream("DumpSessionsResponse-listSess.xml");
        DumpSessionsResponse resp = (DumpSessionsResponse) unmarshaller.unmarshal(is);
        Assert.assertEquals("Total active sessions", 1, resp.getTotalActiveSessions());
        Assert.assertNull("Admin Sessions", resp.getAdminSessions());
        Assert.assertNull("Soap Sessions", resp.getSoapSessions());
        Assert.assertNull("Wiki Sessions", resp.getWikiSessions());
        Assert.assertNull("Waitset Sessions", resp.getWaitsetSessions());
        InfoForSessionType imapInfo = resp.getImapSessions();
        Assert.assertEquals("Imap active accounts", new Integer(1), imapInfo.getActiveAccounts());
        Assert.assertEquals("Imap active sessions", 1, imapInfo.getActiveSessions());
        List<SessionInfo> sessions = imapInfo.getSessions();
        List<AccountSessionInfo> accts = imapInfo.getAccounts();
        Assert.assertEquals("Number of top level accts", 0, accts.size());
        Assert.assertEquals("Number of top level sessionInfos", 1, sessions.size());
        SessionInfo session = sessions.get(0);
        checkImapSession(session, true);
    }

    @Test
    public void unmarshallDumpSessionsResponseGroupedTest()
    throws Exception {
        InputStream is = getClass().getResourceAsStream("DumpSessionsResponse-grouped.xml");
        DumpSessionsResponse resp = (DumpSessionsResponse) unmarshaller.unmarshal(is);
        Assert.assertEquals("Total active sessions", 1, resp.getTotalActiveSessions());
        Assert.assertNull("Admin Sessions", resp.getAdminSessions());
        Assert.assertNull("Soap Sessions", resp.getSoapSessions());
        Assert.assertNull("Wiki Sessions", resp.getWikiSessions());
        Assert.assertNull("Waitset Sessions", resp.getWaitsetSessions());
        InfoForSessionType imapInfo = resp.getImapSessions();
        Assert.assertEquals("Imap active accounts", new Integer(1), imapInfo.getActiveAccounts());
        Assert.assertEquals("Imap active sessions", 1, imapInfo.getActiveSessions());
        List<SessionInfo> sessions = imapInfo.getSessions();
        Assert.assertEquals("Number of top level sessionInfos", 0, sessions.size());
        List<AccountSessionInfo> accts = imapInfo.getAccounts();
        Assert.assertEquals("Number of top level accts", 1, accts.size());
        AccountSessionInfo acct = accts.get(0);
        sessions = acct.getSessions();
        Assert.assertEquals("Number of top level sessionInfos", 1, sessions.size());
        SessionInfo session = sessions.get(0);
        checkImapSession(session, false);
    }

    @Test
    public void marshallDumpSessionsResponse() throws Exception {
        javax.xml.parsers.DocumentBuilder w3DomBuilder =
                        javax.xml.parsers.DocumentBuilderFactory.newInstance()
                            .newDocumentBuilder();
        DumpSessionsResponse gsr = new DumpSessionsResponse(1);
        InfoForSessionType imapSessions = new InfoForSessionType(1,1);
        AccountSessionInfo account = new AccountSessionInfo("user1@gren-elliots-macbook-pro.local",
                "006584ae-cba0-400a-8414-f764ba3c7418");
        SessionInfo session = new SessionInfo(null, null, "223", 1300295279211L, 1300295279212L);
        org.w3c.dom.Document doc = w3DomBuilder.newDocument();
        org.w3c.dom.Element extraElement = doc.createElementNS("urn:zimbraAdmin", "imap");
        extraElement.setAttribute("folder", "INBOX");
        extraElement.setAttribute("expunged", "0");
        extraElement.setAttribute("writable", "1");
        extraElement.setAttribute("dirty", "0");
        extraElement.setAttribute("size", "222");
        session.addExtraElement(extraElement);
        QName qn = new QName("push");
        session.addExtraAttribute(qn, "true");
        account.addSession(session);
        List<AccountSessionInfo> accounts = Lists.newArrayList();
        accounts.add(account);
        imapSessions.setAccounts(accounts);
        gsr.setImapSessions(imapSessions);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshaller.marshal(gsr, out);
        String xml = out.toString("UTF-8");
        if (LOG.isInfoEnabled())
            LOG.info("Xml:\n" + xml);
        Assert.assertTrue("Marshalled XML should contain 'push=\"true\"'", xml.indexOf("push=\"true\"") > 0);
        Assert.assertTrue("Marshalled XML should contain 'size=\"222\"'", xml.indexOf("size=\"222\"") > 0);
        Assert.assertTrue(
                "Marshalled XML should end with 'DumpSessionsResponse>'", xml.endsWith("DumpSessionsResponse>"));
    }
}
