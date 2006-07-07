/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.client.LmcSession;
import com.zimbra.cs.client.soap.LmcAuthRequest;
import com.zimbra.cs.client.soap.LmcAuthResponse;
import com.zimbra.cs.client.soap.LmcSoapClientException;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.StringUtil;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.SoapFaultException;

/**
 * @author bburtin
 */
public class TestUtil {
    public static Account getAccount(String userName)
    throws ServiceException {
        String address = getAddress(userName);
        Account account = Provisioning.getInstance().get(AccountBy.name, address);
        if (account == null) {
            throw new IllegalArgumentException("Could not find account for '" + address + "'");
        }
        return account;
    }

    public static String getDomain()
    throws ServiceException {
        Config config = Provisioning.getInstance().getConfig();
        String domain = config.getAttr(Provisioning.A_zimbraDefaultDomainName, null);
        assert(domain != null && domain.length() > 0);
        return domain;
    }
    
    public static String getAddress(String userName)
    throws ServiceException {
        return userName + "@" + getDomain();
    }
    
    public static String getSoapUrl() {
        String scheme;
        int port;
        try {
            port = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraMailPort, 0);
            if (port > 0) {
                scheme = "http";
            } else {
                port = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraMailSSLPort, 0);
                scheme = "https";
            }
        } catch (ServiceException e) {
            ZimbraLog.test.error("Unable to get user SOAP port", e);
            port = 80;
            scheme = "http";
        }
        return scheme + "://localhost:" + port + ZimbraServlet.USER_SERVICE_URI;
    }
    
    public static String getAdminSoapUrl() {
        int port;
        try {
            port = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        } catch (ServiceException e) {
            ZimbraLog.test.error("Unable to get admin SOAP port", e);
            port = LC.zimbra_admin_service_port.intValue();
        }
        return "https://localhost:" + port + ZimbraServlet.ADMIN_SERVICE_URI;
    }
    
    public static LmcSession getSoapSession(String userName)
    throws ServiceException, LmcSoapClientException, IOException, SoapFaultException
    {
        LmcAuthRequest auth = new LmcAuthRequest();
        auth.setUsername(getAddress(userName));
        auth.setPassword("test123");
        LmcAuthResponse authResp = (LmcAuthResponse) auth.invoke(getSoapUrl());
        return authResp.getSession();
    }

    public static LmcSession getAdminSoapSession()
    throws Exception
    {
        // Authenticate
        LmcAuthRequest auth = new LmcAuthRequest();
        auth.setUsername(getAddress("admin"));
        auth.setPassword("test123");
        LmcAuthResponse authResp = (LmcAuthResponse) auth.invoke(getAdminSoapUrl());
        return authResp.getSession();
    }
    
    private static String[] MESSAGE_TEMPLATE_LINES = {
        "From: Jeff Spiccoli <jspiccoli@${DOMAIN}>",
        "To: Test User 1 <user1@${DOMAIN}>",
        "Subject: ${SUBJECT}",
        "Date: Mon, 28 Mar 2005 10:21:10 -0700",
        "X-Zimbra-Received: Mon, 28 Mar 2005 10:21:1${MESSAGE_NUM} -0700",
        "Content-Type: text/plain",
        "",
        "Dude,",
        "",
        "All I need are some tasty waves, a cool buzz, and I'm fine.",
        "",
        "Jeff",
        "",
        "(${SUBJECT} ${MESSAGE_NUM})"
    };
    
    private static String MESSAGE_TEMPLATE = StringUtil.join("\n", MESSAGE_TEMPLATE_LINES); 

    public static Message insertMessage(Mailbox mbox, int messageNum, String subject)
    throws Exception {
        Map vars = new HashMap();
        vars.put("MESSAGE_NUM", new Integer(messageNum));
        vars.put("SUBJECT", subject);
        vars.put("DOMAIN", getDomain());
        String message = StringUtil.fillTemplate(MESSAGE_TEMPLATE, vars);
        ParsedMessage pm = new ParsedMessage(message.getBytes(), System.currentTimeMillis(), false);
        pm.analyze();
        return mbox.addMessage(null, pm, Mailbox.ID_FOLDER_INBOX, false, Flag.FLAG_UNREAD, null);
    }
 
    public static Set search(Mailbox mbox, String query, byte type)
    throws Exception {
        ZimbraLog.test.debug("Running search: '" + query + "', type=" + type);
        byte[] types = new byte[1];
        types[0] = type;

        Set ids = new HashSet();
        ZimbraQueryResults r = mbox.search(new Mailbox.OperationContext(mbox), query, types, MailboxIndex.SortBy.DATE_DESCENDING, 100);
        while (r.hasNext()) {
            ZimbraHit hit = r.getNext();
            ids.add(new Integer(hit.getItemId()));
        }
        return ids;
        
    }
    
    public static Folder getFolderByPath(Mailbox mbox, String path)
    throws Exception {
        Folder folder = null;
        try {
            folder = mbox.getFolderByPath(null, path);
        } catch (MailServiceException e) {
            if (e.getCode() != MailServiceException.NO_SUCH_FOLDER) {
                throw e;
            }
        }
        return folder;
    }
}
