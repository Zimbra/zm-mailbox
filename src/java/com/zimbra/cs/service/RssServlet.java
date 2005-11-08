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
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.MessageHit;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.soap.Element;

/**
 * simple RSS feed servlet of a mailbox. URL is:
 * 
 *  http://server/service/rss/index.rss[?query={search-query}]
 *  
 *  default query is "is:unread"
 */

public class RssServlet extends ZimbraBasicAuthServlet {

    protected String getRealmHeader()  { return "BASIC realm=\"Zimbra New Mail Feed\""; }

    private static final String PARAM_QUERY = "query";

    public void doAuthGet(HttpServletRequest req, HttpServletResponse resp, Account acct)
    throws ServiceException, IOException {

        //resp.setContentType("text/xml");
        resp.setContentType("application/rss+xml");
            
        StringBuffer sb = new StringBuffer();

        sb.append("<?xml version=\"1.0\"?>");
            
        Element.XMLElement rss = new Element.XMLElement("rss");
        rss.addAttribute("version", "2.0");

        Element channel = rss.addElement("channel");
        channel.addElement("title").setText("Zimbra Mail: "+acct.getName());
            
        channel.addElement("generator").setText("Zimbra RSS Feed Servlet");
            
        ZimbraQueryResults results;
        try {
            String query = req.getParameter(PARAM_QUERY);
            if (query == null) query = "is:unread in:inbox";

            channel.addElement("description").setText(query);

            Mailbox mailbox = Mailbox.getMailboxByAccount(acct);
            if (mailbox == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "mailbox not found");
                return;             
            }

            OperationContext octxt = new OperationContext(acct);
            results = mailbox.search(octxt, query, new byte[] { MailItem.TYPE_MESSAGE }, MailboxIndex.SEARCH_ORDER_DATE_DESC, 500);
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
            while (results.hasNext()) {
                ZimbraHit hit = results.getNext();
                if (hit instanceof MessageHit) {
                    MessageHit mh = (MessageHit) hit;
                    Message m = mh.getMessage();
                    Element item = channel.addElement("item");
                    item.addElement("title").setText(m.getSubject());
                    item.addElement("description").setText(m.getFragment());
                    item.addElement("author").setText(m.getSender());
                    item.addElement("pubDate").setText(sdf.format(new Date(m.getDate())));
                    /* TODO: guid, links, etc */
                    // Element guid = item.addElement("guid");
                    // guid.setText(acct.getId()+"/"+m.getId());
                    // guid.addAttribute("isPermaLink", "false");
                }
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        } catch (ParseException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        }
        sb.append(rss.toString());
        resp.getOutputStream().write(sb.toString().getBytes());
    }
}
