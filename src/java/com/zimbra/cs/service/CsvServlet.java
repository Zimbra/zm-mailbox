/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.Part;
import javax.mail.internet.ContentDisposition;
import javax.mail.internet.ParseException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ContactCSV;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.servlet.ZimbraServlet;

/**
 * export of contacts
 */

public class CsvServlet extends ZimbraServlet {

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        try {
            AuthToken authToken = getAuthTokenFromCookie(req, resp);
            if (authToken == null) 
                return;

            Account account = Provisioning.getInstance().getAccountById(authToken.getAccountId());
            if (account == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "no such account");
                return;
            }

            if (!account.isCorrectHost()) {
            	proxyServletRequest(req, resp, account.getId());
                return;
            }

            Mailbox mbox = Mailbox.getMailboxByAccount(account);
            List contacts = mbox.getContactList(new Mailbox.OperationContext(account), Mailbox.ID_FOLDER_CONTACTS);
            StringBuffer sb = new StringBuffer();
            if (contacts == null)
                contacts = new ArrayList();
            ContactCSV.toCSV(contacts, sb);

            ContentDisposition cd = new ContentDisposition(Part.ATTACHMENT);
            cd.setParameter("filename", "contacts.csv");
            resp.addHeader("Content-Disposition", cd.toString());
            resp.setContentType("text/plain");
            resp.getOutputStream().print(sb.toString());
        } catch (ServiceException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (ParseException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}