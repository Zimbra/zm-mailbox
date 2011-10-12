/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.service.util;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServletContext;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.servlet.util.AuthUtil;

public class UserServletUtil {

    public static void resolveItems(UserServletContext context) throws ServiceException {
        for (UserServletContext.Item item : context.requestedItems) {
            try {
                if (item.versioned)
                    item.mailItem = context.targetMailbox.getItemRevision(context.opContext, item.id, MailItem.Type.UNKNOWN, item.ver);
                else
                    item.mailItem = context.targetMailbox.getItemById(context.opContext, item.id, MailItem.Type.UNKNOWN);
            } catch (NoSuchItemException x) {
                ZimbraLog.misc.info(x.getMessage());
            } catch (ServiceException x) {
                if (x.getCode().equals(ServiceException.PERM_DENIED)) {
                    ZimbraLog.misc.info(x.getMessage());
                } else {
                    throw x;
                }
            }
        }
    }

    /*
     * Parses the pathInfo, then returns MailItem corresponding to the resource in pathInfo.
     *
     * If the formatter does not require authentication, e.g. IfbFormatter,
     * then the path resolution is skipped and returns null.  That's because
     * IfbFormatter does not internally use the resource identified in the URL.
     * It gets the ifb information directly from the Mailbox.
     *
     * If the formatter declares that the authentication is not required, it's
     * the formatter's responsibility to make sure the MailItems returned to
     * the clients has gone through the access checks.
     *
     */
    public static MailItem resolveItem(UserServletContext context, boolean checkExtension) throws ServiceException {
        if (context.formatter != null && !context.formatter.requiresAuth())
            return null;

        Mailbox mbox = context.targetMailbox;

        // special-case the fetch-by-IMAP-id option
        if (context.imapId > 0) {
            // fetch the folder from the path
            Folder folder = mbox.getFolderByPath(context.opContext, context.itemPath);

            // and then fetch the item from the "imap_id" query parameter
            return mbox.getItemByImapId(context.opContext, context.imapId, folder.getId());
        }

        if (context.itemId != null) {
            context.target = mbox.getItemById(context.opContext,
                                              context.itemId.getId(),
                                              MailItem.Type.UNKNOWN);

            context.itemPath = context.target.getPath();
            if (context.target instanceof Mountpoint || context.extraPath == null || context.extraPath.equals(""))
                return context.target;
            if (context.itemPath == null)
                throw MailServiceException.NO_SUCH_ITEM("?id=" + context.itemId + "&name=" + context.extraPath);
            context.target = null;
            context.itemId = null;
        }

        if (context.extraPath != null && !context.extraPath.equals("")) {
            context.itemPath = (context.itemPath + '/' + context.extraPath).replaceAll("//+", "/");
            context.extraPath = null;
        }

        if (FormatType.FREE_BUSY.equals(context.format) || FormatType.IFB.equals(context.format)) {
            try {
                // Do the get as mailbox owner to circumvent ACL system.
                context.target = mbox.getItemByPath(null, context.itemPath);
            } catch (ServiceException e) {
                if (!(e instanceof NoSuchItemException))
                    throw e;
            }
        } else {
            // first, try the full requested path
            ServiceException failure = null;
            try {
                context.target = mbox.getItemByPath(context.opContext, context.itemPath);
            } catch (ServiceException e) {
                if (!(e instanceof NoSuchItemException) && !e.getCode().equals(ServiceException.PERM_DENIED))
                    throw e;
                failure = e;
            }

            if (context.target == null) {
                // if there is a mountpoint somewhere higher up in the requested path
                // then we need to proxy the request to the sharer's mailbox.
                try {
                    // to search for the mountpoint we use admin rights on the user's mailbox.
                    // this is done so that MailItems in the mountpoint can be resolved
                    // according to the ACL stored in the owner's Folder.  when a mountpoint
                    // is found, then the request is proxied to the owner's mailbox host,
                    // and the current requestor's credential is used to validate against
                    // the ACL.
                    Pair<Folder, String> match = mbox.getFolderByPathLongestMatch(null, Mailbox.ID_FOLDER_USER_ROOT, context.itemPath);
                    Folder reachable = match.getFirst();
                    if (reachable instanceof Mountpoint) {
                        context.target = reachable;
                        context.itemPath = reachable.getPath();
                        context.extraPath = match.getSecond();
                    }
                } catch (ServiceException e) { }
            }

            if (context.target == null) {
                // if they asked for something like "calendar.csv" (where "calendar" was the folder name), try again minus the extension
                int dot = context.itemPath.lastIndexOf('.'), slash = context.itemPath.lastIndexOf('/');
                if (checkExtension && context.format == null && dot != -1 && dot > slash) {
                    /* if path == /foo/bar/baz.html, then
                     *      format -> html
                     *      path   -> /foo/bar/baz  */
                    String unsuffixedPath = context.itemPath.substring(0, dot);
                    try {
                        context.target = mbox.getItemByPath(context.opContext, unsuffixedPath);
                        context.format = FormatType.fromString(context.itemPath.substring(dot + 1));
                        context.itemPath = unsuffixedPath;
                    } catch (ServiceException e) { }
                }
            }

            if (context.target == null)
                throw failure;
        }

        return context.target;
    }

    // public synchronized static void addFormatter(Formatter f) {
    // mFormatters.put(f.getType(), f);
    // for (String mimeType : f.getDefaultMimeTypes())
    // mDefaultFormatters.put(mimeType, f);
    // }
    //
    // public Formatter getFormatter(String type) {
    // return mFormatters.get(type);
    // }

    public static Mailbox getTargetMailbox(UserServletContext context) {
        // treat the non-existing target account the same as insufficient
        // permission
        // to access existing item in order to prevent account harvesting.
        Mailbox mbox = null;
        try {
            mbox = context.targetMailbox = MailboxManager.getInstance()
                    .getMailboxByAccount(context.targetAccount);
        } catch (Exception e) {
            // ignore IllegalArgumentException or ServiceException being thrown.
        }
        return mbox;
    }

//    public synchronized static void addFormatter(Formatter f) {
//        mFormatters.put(f.getType(), f);
//        for (String mimeType : f.getDefaultMimeTypes())
//            mDefaultFormatters.put(mimeType, f);
//    }
//
//    public Formatter getFormatter(String type) {
//        return mFormatters.get(type);
//    }

    public static void getAccount(UserServletContext context) throws IOException, ServletException, UserServletException {
        try {
            boolean isAdminRequest = AuthUtil.isAdminRequest(context.req);

            // check cookie or access key
            if (context.cookieAuthAllowed() || AuthProvider.allowAccessKeyAuth(context.req, context.getServlet())) {
                try {
                    AuthToken at = AuthProvider.getAuthToken(context.req, isAdminRequest);
                    if (at != null) {

                        if (at.isZimbraUser()) {
                            try {
                                context.setAuthAccount(AuthProvider.validateAuthToken(Provisioning.getInstance(), at, false));
                            } catch (ServiceException e) {
                                throw new UserServletException(HttpServletResponse.SC_UNAUTHORIZED,
                                        L10nUtil.getMessage(MsgKey.errMustAuthenticate, context.req));
                            }
                            context.cookieAuthHappened = true;
                            context.authToken = at;
                            return;
                        } else {
                            if (at.isExpired()) {
                                throw new UserServletException(HttpServletResponse.SC_UNAUTHORIZED,
                                        L10nUtil.getMessage(MsgKey.errMustAuthenticate, context.req));
                            }
                            context.setAuthAccount(new GuestAccount(at));
                            context.basicAuthHappened = true; // pretend that we basic authed
                            context.authToken = at;
                            return;
                        }
                    }
                } catch (AuthTokenException e) {
                    // bug 35917: malformed auth token means auth failure
                    throw new UserServletException(HttpServletResponse.SC_UNAUTHORIZED,
                            L10nUtil.getMessage(MsgKey.errMustAuthenticate, context.req));
                }
            }

            // check query string
            if (context.queryParamAuthAllowed()) {
                String auth = context.params.get(ZimbraServlet.QP_ZAUTHTOKEN);
                if (auth == null)
                    auth = context.params.get(UserServlet.QP_AUTHTOKEN);  // not sure who uses this parameter; zauthtoken is preferred
                if (auth != null) {
                    try {
                        // Only supported by ZimbraAuthProvider
                        AuthToken at = AuthProvider.getAuthToken(auth);

                        try {
                            context.setAuthAccount(AuthProvider.validateAuthToken(Provisioning.getInstance(), at, false));
                            context.qpAuthHappened = true;
                            context.authToken = at;
                            return;
                        } catch (ServiceException e) {
                            throw new UserServletException(HttpServletResponse.SC_UNAUTHORIZED,
                                    L10nUtil.getMessage(MsgKey.errMustAuthenticate, context.req));
                        }

                    } catch (AuthTokenException e) {
                        // bug 35917: malformed auth token means auth failure
                        throw new UserServletException(HttpServletResponse.SC_UNAUTHORIZED,
                                L10nUtil.getMessage(MsgKey.errMustAuthenticate, context.req));
                    }
                }
            }

            /* AP-TODO-3:
             *    http auth currently does not work for non-Zimbra auth provider,
             *    for Yahoo Y&T, will probably need to retrieve Y&T cookies from a
             *    site in the basicAuthRequest after authenticating using user/pass.
             */
            // fallback to basic auth
            if (context.basicAuthAllowed()) {
                context.setAuthAccount(AuthUtil.basicAuthRequest(context.req, context.resp, context.servlet,false));
                if (context.getAuthAccount() != null) {
                    context.basicAuthHappened = true;
                    context.authToken = AuthProvider.getAuthToken(context.getAuthAccount(), isAdminRequest);

                    // send cookie back if need be.
                    if (context.setCookie()) {
                        boolean secureCookie = context.req.getScheme().equals("https");
                        context.authToken.encode(context.resp, isAdminRequest, secureCookie);
                    }
                }
                // always return
                return;
            }

            // there is no credential at this point.  assume anonymous public access and continue.
        } catch (ServiceException e) {
            throw new ServletException(e);
        }
    }

}
