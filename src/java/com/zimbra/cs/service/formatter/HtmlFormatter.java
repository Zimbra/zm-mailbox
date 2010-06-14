/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.formatter;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.service.UserServlet.HttpInputStream;
import com.zimbra.cs.service.UserServletException;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.httpclient.Header;

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
    private static final String ATTR_TARGET_ACCOUNT_PREF_LOCALE   = "zimbra_target_account_prefLocale";
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
        dispatchJspRest(context.getServlet(), context);
    }

    @Override
    public String getType() {
        return "html";
    }

    static void dispatchJspRest(Servlet servlet, Context context)
    throws ServiceException, ServletException, IOException {
        AuthToken auth = null;
        long expiration = System.currentTimeMillis() + AUTH_EXPIRATION;
        if (context.basicAuthHappened) {
            Account acc = context.authAccount;
            if (acc instanceof GuestAccount)
                auth = AuthToken.getAuthToken(acc.getId(), acc.getName(), null, ((GuestAccount)acc).getDigest(), expiration);
            else
                auth = AuthProvider.getAuthToken(context.authAccount, expiration);
        } else if (context.cookieAuthHappened) {
            auth = UserServlet.getAuthTokenFromCookie(context.req, context.resp, true);
        } else {
            auth = AuthToken.getAuthToken(GuestAccount.GUID_PUBLIC, null, null, null, expiration);
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
        String uri = (String) context.req.getAttribute("requestedPath");
        
        if (targetItem instanceof Mountpoint && ((Mountpoint)targetItem).getDefaultView() != MailItem.TYPE_APPOINTMENT) {
        	Mountpoint mp = (Mountpoint) targetItem;
            Provisioning prov = Provisioning.getInstance();
            targetAccount = prov.getAccountById(mp.getOwnerId());
        	Pair<Header[], HttpInputStream> remoteItem = UserServlet.getRemoteResourceAsStream((auth == null) ? null : auth.toZAuthToken(), mp.getTarget(), context.extraPath);
        	remoteItem.getSecond().close();
        	String remoteItemId = null;
        	String remoteItemType = null;
        	String remoteItemName = null;
        	String remoteItemPath = null;
        	for (Header h : remoteItem.getFirst())
        		if (h.getName().compareToIgnoreCase("X-Zimbra-ItemId") == 0)
        			remoteItemId = h.getValue();
        		else if (h.getName().compareToIgnoreCase("X-Zimbra-ItemType") == 0)
        			remoteItemType = h.getValue();
        		else if (h.getName().compareToIgnoreCase("X-Zimbra-ItemName") == 0)
        			remoteItemName = h.getValue();
        		else if (h.getName().compareToIgnoreCase("X-Zimbra-ItemPath") == 0)
        			remoteItemPath = h.getValue();
        	
            context.req.setAttribute(ATTR_TARGET_ITEM_ID, remoteItemId);
            context.req.setAttribute(ATTR_TARGET_ITEM_TYPE, remoteItemType);
            context.req.setAttribute(ATTR_TARGET_ITEM_NAME, remoteItemName);
            context.req.setAttribute(ATTR_TARGET_ITEM_PATH, remoteItemPath);
            context.req.setAttribute(ATTR_TARGET_ITEM_COLOR, mp.getColor());
            context.req.setAttribute(ATTR_TARGET_ITEM_VIEW, MailItem.getNameForType(mp.getDefaultView()));
            targetItem = null;
        }

        context.req.setAttribute(ATTR_INTERNAL_DISPATCH, "yes");
        context.req.setAttribute(ATTR_REQUEST_URI, uri != null ? uri : context.req.getRequestURI());
        context.req.setAttribute(ATTR_AUTH_TOKEN, authString);
        if (targetAccount != null) {
            context.req.setAttribute(ATTR_TARGET_ACCOUNT_NAME, targetAccount.getName());
            context.req.setAttribute(ATTR_TARGET_ACCOUNT_ID, targetAccount.getId());
            context.req.setAttribute(ATTR_TARGET_ACCOUNT_PREF_TIME_ZONE, targetAccount.getAttr(Provisioning.A_zimbraPrefTimeZoneId));
            context.req.setAttribute(ATTR_TARGET_ACCOUNT_PREF_SKIN, targetAccount.getAttr(Provisioning.A_zimbraPrefSkin));
            context.req.setAttribute(ATTR_TARGET_ACCOUNT_PREF_LOCALE, targetAccount.getAttr(Provisioning.A_zimbraPrefLocale));            
            context.req.setAttribute(ATTR_TARGET_ACCOUNT_PREF_CALENDAR_FIRST_DAY_OF_WEEK, targetAccount.getAttr(Provisioning.A_zimbraPrefCalendarFirstDayOfWeek));
            context.req.setAttribute(ATTR_TARGET_ACCOUNT_PREF_CALENDAR_DAY_HOUR_START, targetAccount.getAttr(Provisioning.A_zimbraPrefCalendarDayHourStart));
            context.req.setAttribute(ATTR_TARGET_ACCOUNT_PREF_CALENDAR_DAY_HOUR_END, targetAccount.getAttr(Provisioning.A_zimbraPrefCalendarDayHourEnd));
        }
        if (targetItem != null) {
            context.req.setAttribute(ATTR_TARGET_ITEM_ID, targetItem.getId());
            context.req.setAttribute(ATTR_TARGET_ITEM_TYPE, MailItem.getNameForType(targetItem));
            context.req.setAttribute(ATTR_TARGET_ITEM_PATH, targetItem.getPath());
            context.req.setAttribute(ATTR_TARGET_ITEM_NAME, targetItem.getName());
    
            context.req.setAttribute(ATTR_TARGET_ITEM_COLOR, targetItem.getColor());
            if (targetItem instanceof Folder)
                context.req.setAttribute(ATTR_TARGET_ITEM_VIEW, MailItem.getNameForType(((Folder)targetItem).getDefaultView()));
        }

        String mailUrl = PATH_MAIN_CONTEXT;
        try {
            mailUrl = Provisioning.getInstance().getLocalServer().getMailURL();
        } catch (Exception e) {
        }
        ServletContext targetContext = servlet.getServletConfig().getServletContext().getContext(mailUrl);
        RequestDispatcher dispatcher = targetContext.getRequestDispatcher(PATH_JSP_REST_PAGE);
        dispatcher.forward(context.req, context.resp);
    }
}
