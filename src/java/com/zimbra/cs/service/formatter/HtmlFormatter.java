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
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;

public class HtmlFormatter extends Formatter {
    
    private static final String PATH_MAIN_CONTEXT  = "/zimbra";
    private static final String PATH_JSP_REST_PAGE = "/h/rest";
    private static final long   AUTH_EXPIRATION = 60 * 60 * 1000;
    
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
        if (!(context.target instanceof Folder))
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "format not supported for this item");

        AuthToken auth = null;
        if (context.basicAuthHappened) {
            long expiration = System.currentTimeMillis() + AUTH_EXPIRATION;
            auth = new AuthToken(context.authAccount, expiration, false, null);
        } else if (context.cookieAuthHappened) {
            auth = UserServlet.getAuthTokenFromCookie(context.req, context.resp, true);
        }
        
        String authString = null;
        try {
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
}
