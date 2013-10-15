/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;

import com.zimbra.common.mailbox.Color;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.WebSplitUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
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
import com.zimbra.cs.service.UserServlet.HttpInputStream;
import com.zimbra.cs.service.UserServletContext;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;

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
    public void formatCallback(UserServletContext context) throws UserServletException,
            ServiceException, IOException, ServletException {
        dispatchJspRest(context.getServlet(), context);
    }

    @Override
    public FormatType getType() {
        return FormatType.HTML;
    }

    static void dispatchJspRest(Servlet servlet, UserServletContext context)
            throws ServiceException, ServletException, IOException {
        AuthToken auth = null;
        long expiration = System.currentTimeMillis() + AUTH_EXPIRATION;
        if (context.basicAuthHappened) {
            Account acc = context.getAuthAccount();
            if (acc instanceof GuestAccount) {
                auth = AuthToken.getAuthToken(acc.getId(), acc.getName(), null, ((GuestAccount)acc).getDigest(), expiration);
            } else {
                auth = AuthProvider.getAuthToken(context.getAuthAccount(), expiration);
            }
        } else if (context.cookieAuthHappened) {
            auth = UserServlet.getAuthTokenFromCookie(context.req, context.resp, true);
        } else {
            auth = AuthToken.getAuthToken(GuestAccount.GUID_PUBLIC, null, null, null, expiration);
        }
        if (auth != null && context.targetAccount != null && context.targetAccount != context.getAuthAccount()) {
            auth.setProxyAuthToken(Provisioning.getInstance().getProxyAuthToken(context.targetAccount.getId(), null));
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

        if (targetItem instanceof Mountpoint && ((Mountpoint)targetItem).getDefaultView() != MailItem.Type.APPOINTMENT) {
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
            context.req.setAttribute(ATTR_TARGET_ITEM_VIEW, mp.getDefaultView().toByte());
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
        } else {
            // Useful when faking results - e.g. FREEBUSY html view for non-existent account
            if (context.fakeTarget != null) {
                context.req.setAttribute(ATTR_TARGET_ACCOUNT_NAME, context.fakeTarget.getAccount());
            }
            com.zimbra.cs.account.Cos defaultCos = Provisioning.getInstance().get(com.zimbra.common.account.Key.CosBy.name, Provisioning.DEFAULT_COS_NAME);
            context.req.setAttribute(ATTR_TARGET_ACCOUNT_PREF_TIME_ZONE,
                        defaultCos.getAttr(Provisioning.A_zimbraPrefTimeZoneId));
            context.req.setAttribute(ATTR_TARGET_ACCOUNT_PREF_SKIN, defaultCos.getAttr(Provisioning.A_zimbraPrefSkin));
            context.req.setAttribute(ATTR_TARGET_ACCOUNT_PREF_LOCALE, defaultCos.getAttr(Provisioning.A_zimbraPrefLocale));
            context.req.setAttribute(ATTR_TARGET_ACCOUNT_PREF_CALENDAR_FIRST_DAY_OF_WEEK, defaultCos.getAttr(Provisioning.A_zimbraPrefCalendarFirstDayOfWeek));
            context.req.setAttribute(ATTR_TARGET_ACCOUNT_PREF_CALENDAR_DAY_HOUR_START, defaultCos.getAttr(Provisioning.A_zimbraPrefCalendarDayHourStart));
            context.req.setAttribute(ATTR_TARGET_ACCOUNT_PREF_CALENDAR_DAY_HOUR_END, defaultCos.getAttr(Provisioning.A_zimbraPrefCalendarDayHourEnd));
        }
        if (targetItem != null) {
            context.req.setAttribute(ATTR_TARGET_ITEM_ID, targetItem.getId());
            context.req.setAttribute(ATTR_TARGET_ITEM_PATH, targetItem.getPath());
            context.req.setAttribute(ATTR_TARGET_ITEM_NAME, targetItem.getName());
            context.req.setAttribute(ATTR_TARGET_ITEM_TYPE, targetItem.getType().toString());

            context.req.setAttribute(ATTR_TARGET_ITEM_COLOR, targetItem.getColor());
            if (targetItem instanceof Folder) {
                context.req.setAttribute(ATTR_TARGET_ITEM_VIEW, ((Folder) targetItem).getDefaultView().toString());
            }
        } else {
            context.req.setAttribute(ATTR_TARGET_ITEM_COLOR, Color.getMappedColor(null));
        }
        if (context.fakeTarget != null) {  // Override to avoid address harvesting
            context.req.setAttribute(ATTR_TARGET_ITEM_PATH, context.fakeTarget.getPath());
            context.req.setAttribute(ATTR_TARGET_ITEM_NAME, context.fakeTarget.getName());
        }
        String mailUrl = PATH_MAIN_CONTEXT;
        List<String> zimbraServiceInstalled = Arrays.asList(Provisioning.getInstance().getLocalServer().getServiceInstalled());
        if (WebSplitUtil.isZimbraServiceSplitEnabled(zimbraServiceInstalled)) {
            mailUrl = Provisioning.getInstance().getLocalServer().getWebClientURL() + PATH_JSP_REST_PAGE;
            HttpClient httpclient = ZimbraHttpConnectionManager.getInternalHttpConnMgr().getDefaultHttpClient();
            /*
             * Retest the code with POST to check whether it works
            PostMethod postMethod = new PostMethod(mailUrl);
            Enumeration<String> attributeNames = context.req.getAttributeNames();
            List<Part> parts = new ArrayList<Part>();
            while(attributeNames.hasMoreElements())
            {
                String attrName = (String) attributeNames.nextElement();
                String attrValue = context.req.getAttribute(attrName).toString();
                Part part = new StringPart(attrName, attrValue);
                parts.add(part);
            }
            postMethod.setRequestEntity(new MultipartRequestEntity(parts.toArray(new Part[0]), new HttpMethodParams()));

            HttpClientUtil.executeMethod(httpclient, postMethod);
            ByteUtil.copy(postMethod.getResponseBodyAsStream(), true, context.resp.getOutputStream(), true);
            */

            Enumeration<String> attributeNames = context.req.getAttributeNames();
            StringBuilder sb = new StringBuilder(mailUrl);
            sb.append("?");
            while(attributeNames.hasMoreElements())
            {
                String attrName = (String) attributeNames.nextElement();
                String attrValue = context.req.getAttribute(attrName).toString();
                sb.append(attrName).append("=").append(HttpUtil.urlEscape(attrValue)).append("&");
            }
            GetMethod postMethod = new GetMethod(sb.toString());

            HttpClientUtil.executeMethod(httpclient, postMethod);
            ByteUtil.copy(postMethod.getResponseBodyAsStream(), true, context.resp.getOutputStream(), false);
        } else {
            try {
                mailUrl = Provisioning.getInstance().getLocalServer().getMailURL();
            } catch (Exception e) {
            }
            ServletContext targetContext = servlet.getServletConfig().getServletContext().getContext(mailUrl);
            RequestDispatcher dispatcher = targetContext.getRequestDispatcher(PATH_JSP_REST_PAGE);
            dispatcher.forward(context.req, context.resp);
        }
    }
}
