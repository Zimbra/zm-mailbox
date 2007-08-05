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
package com.zimbra.cs.service.formatter;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;

public class HtmlFormatter extends Formatter {
    
    private static final String PATH_MAIN_CONTEXT  = "/zimbra";
    private static final String PATH_JSP_REST_PAGE = "/h/rest";
    private static final long   AUTH_EXPIRATION = 60L * 60L * 1000L;
    
    private static final String ATTR_INTERNAL_DISPATCH   = "zimbra-internal-dispatch";
    private static final String ATTR_AUTH_TOKEN          = "zimbra-authToken";
    private static final String ATTR_TARGET_ACCOUNT_NAME = "zimbra-target-account-name";
    private static final String ATTR_TARGET_ACCOUNT_ID   = "zimbra-target-account-id";
    private static final String ATTR_TARGET_ITEM_ID      = "zimbra-target-item-id";
    private static final String ATTR_TARGET_ITEM_TYPE    = "zimbra-target-item-type";
    private static final String ATTR_TARGET_ITEM_COLOR   = "zimbra-target-item-color";
    private static final String ATTR_TARGET_ITEM_VIEW    = "zimbra-target-item-view";
    
    @Override
    public boolean canBeBlocked() {
        return false;
    }

    @Override
    public void formatCallback(Context context) throws UserServletException,
            ServiceException, IOException, ServletException {
        AuthToken auth = null;
        long expiration = System.currentTimeMillis() + AUTH_EXPIRATION;
        if (context.basicAuthHappened) {
            Account acc = context.authAccount;
            if (acc instanceof ACL.GuestAccount)
                auth = new AuthToken(acc.getId(), acc.getName(), null, ((ACL.GuestAccount)acc).getDigest(), expiration);
            else
                auth = new AuthToken(context.authAccount, expiration);
        } else if (context.cookieAuthHappened) {
            auth = UserServlet.getAuthTokenFromCookie(context.req, context.resp, true);
        } else {
            auth = new AuthToken(ACL.GUID_PUBLIC, null, null, null, expiration);
        }
        
        String authString = null;
        try {
            if (auth != null)
                authString = auth.getEncoded();
        } catch (AuthTokenException e) {
            throw new ServletException("error generating the authToken", e);
        }
        
        Account targetAccount = context.targetAccount;
        MailItem targetItem = context.target;
        
        context.req.setAttribute(ATTR_INTERNAL_DISPATCH, "yes");
        context.req.setAttribute(ATTR_AUTH_TOKEN, authString);
        context.req.setAttribute(ATTR_TARGET_ACCOUNT_NAME, targetAccount.getName());
        context.req.setAttribute(ATTR_TARGET_ACCOUNT_ID, targetAccount.getId());
        context.req.setAttribute(ATTR_TARGET_ITEM_ID, targetItem.getId());
        context.req.setAttribute(ATTR_TARGET_ITEM_TYPE, targetItem.getType());
        context.req.setAttribute(ATTR_TARGET_ITEM_COLOR, targetItem.getColor());
        if (targetItem instanceof Folder)
            context.req.setAttribute(ATTR_TARGET_ITEM_VIEW, ((Folder)targetItem).getDefaultView());

        ServletContext sc = getServlet().getServletConfig().getServletContext();
        ServletContext targetContext = sc.getContext(PATH_MAIN_CONTEXT);
        RequestDispatcher dispatcher = targetContext.getRequestDispatcher(PATH_JSP_REST_PAGE);
        dispatcher.forward(context.req, context.resp);
    }

    @Override
    public String getType() {
        return "html";
    }

    @Override
    public void saveCallback(byte[] body, Context context, String contentType,
            Folder folder, String filename) throws UserServletException,
            ServiceException, IOException, ServletException {
        throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "format not supported for save");
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length == 0)
            return;
        SoapHttpTransport tr;
        //AuthToken auth = new AuthToken(ACL.GUID_PUBLIC, "foo@bar.com", "test123", null, System.currentTimeMillis() + AUTH_EXPIRATION);
        AuthToken auth = new AuthToken(ACL.GUID_PUBLIC, null, null, null, System.currentTimeMillis() + AUTH_EXPIRATION);
        String url = "http://localhost:7070/service/soap";
        tr = new SoapHttpTransport(url);
        tr.setAuthToken(auth.getEncoded());
        tr.setTargetAcctName("user1");
        XMLElement req = new XMLElement(MailConstants.GET_WIKI_REQUEST);
        Element w = req.addElement(MailConstants.E_WIKIWORD);
        w.addAttribute(MailConstants.A_ID, args[0]);
        if (args.length > 1)
            w.addAttribute(MailConstants.A_VERSION, args[1]);
        Element resp = tr.invoke(req);
        System.out.println(resp);
    }
}
