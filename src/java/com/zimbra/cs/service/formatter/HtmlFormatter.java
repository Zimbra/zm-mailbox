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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.service.UserServletException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HtmlFormatter extends Formatter {
    
    private static final String PATH_MAIN_CONTEXT  = "/zimbra";
    private static final String PATH_JSP_REST_PAGE = "/h/rest";
    private static final long   AUTH_EXPIRATION = 60L * 60L * 1000L;

    private static final String ATTR_REQUEST_URI         = "zimbra_request_uri";

    private static final String ATTR_INTERNAL_DISPATCH   = "zimbra_internal_dispatch";
    private static final String ATTR_AUTH_TOKEN          = "zimbra_authToken";
    private static final String ATTR_TARGET_ACCOUNT_NAME = "zimbra_target_account_name";
    private static final String ATTR_TARGET_ACCOUNT_ID   = "zimbra_target_account_id";
    private static final String ATTR_TARGET_ITEM_ID      = "zimbra_target_item_id";
    private static final String ATTR_TARGET_ITEM_TYPE    = "zimbra_target_item_type";
    private static final String ATTR_TARGET_ITEM_COLOR   = "zimbra_target_item_color";
    private static final String ATTR_TARGET_ITEM_VIEW    = "zimbra_target_item_view";
    private static final String ATTR_TARGET_ITEM_PATH    = "zimbra_target_item_path";
    private static final String ATTR_TARGET_ITEM_NAME    = "zimbra_target_item_name";

    private static final String ATTR_TARGET_ACCOUNT_PREF_TIME_ZONE   = "zimbra_target_account_prefTimeZoneId";
    private static final String ATTR_TARGET_ACCOUNT_PREF_SKIN   = "zimbra_target_account_prefSkin";
    private static final String ATTR_TARGET_ACCOUNT_PREF_CALENDAR_FIRST_DAY_OF_WEEK   = "zimbra_target_account_prefCalendarFirstDayOfWeek";
    private static final String ATTR_TARGET_ACCOUNT_PREF_CALENDAR_DAY_HOUR_START   = "zimbra_target_account_prefCalendarDayHourStart";
    private static final String ATTR_TARGET_ACCOUNT_PREF_CALENDAR_DAY_HOUR_END  = "zimbra_target_account_prefCalendarDayHourEnd";

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

        String uri = (String)context.req.getAttribute("requestedPath");


        context.req.setAttribute(ATTR_INTERNAL_DISPATCH, "yes");
        context.req.setAttribute(ATTR_REQUEST_URI, uri != null ? uri : context.req.getRequestURI());
        context.req.setAttribute(ATTR_AUTH_TOKEN, authString);
        context.req.setAttribute(ATTR_TARGET_ACCOUNT_NAME, targetAccount.getName());
        context.req.setAttribute(ATTR_TARGET_ACCOUNT_ID, targetAccount.getId());
        context.req.setAttribute(ATTR_TARGET_ITEM_ID, targetItem.getId());
        context.req.setAttribute(ATTR_TARGET_ITEM_TYPE, MailItem.getNameForType(targetItem));
        context.req.setAttribute(ATTR_TARGET_ITEM_PATH, targetItem.getPath());
        context.req.setAttribute(ATTR_TARGET_ITEM_NAME, targetItem.getName());

        context.req.setAttribute(ATTR_TARGET_ITEM_COLOR, targetItem.getColor());
        if (targetItem instanceof Folder)
            context.req.setAttribute(ATTR_TARGET_ITEM_VIEW, MailItem.getNameForType(((Folder)targetItem).getDefaultView()));

        context.req.setAttribute(ATTR_TARGET_ACCOUNT_PREF_TIME_ZONE, targetAccount.getAttr(Provisioning.A_zimbraPrefTimeZoneId));
        context.req.setAttribute(ATTR_TARGET_ACCOUNT_PREF_SKIN, targetAccount.getAttr(Provisioning.A_zimbraPrefSkin));
        context.req.setAttribute(ATTR_TARGET_ACCOUNT_PREF_CALENDAR_FIRST_DAY_OF_WEEK, targetAccount.getAttr(Provisioning.A_zimbraPrefCalendarFirstDayOfWeek));
        context.req.setAttribute(ATTR_TARGET_ACCOUNT_PREF_CALENDAR_DAY_HOUR_START, targetAccount.getAttr(Provisioning.A_zimbraPrefCalendarDayHourStart));
        context.req.setAttribute(ATTR_TARGET_ACCOUNT_PREF_CALENDAR_DAY_HOUR_END, targetAccount.getAttr(Provisioning.A_zimbraPrefCalendarDayHourEnd));

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
