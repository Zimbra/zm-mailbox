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
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.servlet.ZimbraServlet;

/**
 * simple iCal servlet on a mailbox. URL is:
 * 
 *  http://server/service/ical/cal.ics[?...support-range-at-some-point...]
 *  
 *  need to support a range query at some point, right now get 90 days from today
 *
 */

public abstract class ZimbraBasicAuthServlet extends ZimbraServlet {

    private static final String  WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    private static final String  WWW_AUTHENTICATE_VALUE = "BASIC realm=\"Zimbra\"";
    
    private static Log mLog = LogFactory.getLog(ZimbraBasicAuthServlet.class);

    public abstract void doAuthGet(HttpServletRequest req, HttpServletResponse resp, Account acct, Mailbox mailbox)
    throws ServiceException, IOException;

    public final void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        // TODO: should/could also check for auth token cookie?
        String auth = req.getHeader("Authorization");

        // TODO: more liberal parsing of Authorization value...
        if (auth == null || !auth.startsWith("Basic ")) {
            resp.addHeader(WWW_AUTHENTICATE_HEADER, WWW_AUTHENTICATE_VALUE);            
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "must authenticate");
            return;
        }
      
        // 6 comes from "Basic ".length();
        String userPass = new String(Base64.decodeBase64(auth.substring(6).getBytes()));

        int loc = userPass.indexOf(":"); 
        if (loc == -1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid basic auth credentials");
            return;
        }
        
        String user = userPass.substring(0, loc);
        String pass = userPass.substring(loc+1);
        
        Provisioning prov = Provisioning.getInstance();
        
        try {
            Account acct = prov.getAccountByName(user);
            if (acct == null) {
                resp.addHeader(WWW_AUTHENTICATE_HEADER, WWW_AUTHENTICATE_VALUE);
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid username/password");
                return;
            }
            try {
                prov.authAccount(acct, pass);
            } catch (ServiceException se) {
                resp.addHeader(WWW_AUTHENTICATE_HEADER, WWW_AUTHENTICATE_VALUE);
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid username/password");
                return;
            }
            
            // TODO: handle mailbox not local error and send an http redirect?
            Mailbox mailbox = Mailbox.getMailboxByAccount(acct);
            if (mailbox == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "mailbox not found");
                return;             
            }

            doAuthGet(req, resp, acct, mailbox);
        } catch (ServiceException e) {
            throw new ServletException(e);
        }
  }
        
  public void init() throws ServletException {
        String name = getServletName();
        mLog.info("Servlet " + name + " starting up");
        super.init();
  }

  public void destroy() {
        String name = getServletName();
        mLog.info("Servlet " + name + " shutting down");
        super.destroy();
  }
}
