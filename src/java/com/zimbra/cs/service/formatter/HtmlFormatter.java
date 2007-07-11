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
import java.util.HashMap;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;

public class HtmlFormatter extends Formatter {
    
    private static final String PATH_MAIN_CONTEXT = "/zimbra";
    private static final long   AUTH_EXPIRATION = 60 * 60 * 1000;
    
    private HashMap<Byte, String> mTargets;
    
    public HtmlFormatter() {
        mTargets = new HashMap<Byte,String>();
        mTargets.put(MailItem.TYPE_APPOINTMENT, "/h/calendar");
        //mTargets.put(MailItem.TYPE_MESSAGE, "/h");
    }
    
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
        
        ZimbraLog.misc.warn(authString);
        
        Folder f = (Folder) context.target;
        String targetPath = mTargets.get(f.getDefaultView());
        if (targetPath == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "format not supported for this item");
        
        context.req.setAttribute("zimbra-internal-dispatch", "yes");
        context.req.setAttribute("zimbra-authToken", authString);

        ServletContext sc = getServlet().getServletConfig().getServletContext();
        ServletContext targetContext = sc.getContext(PATH_MAIN_CONTEXT);
        RequestDispatcher dispatcher = targetContext.getRequestDispatcher(targetPath);
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
