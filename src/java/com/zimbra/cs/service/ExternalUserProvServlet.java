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

package com.zimbra.cs.service;

import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.BlobMetaData;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.AuthTokenKey;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ShareInfoData;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.acl.AclPushSerializer;
import com.zimbra.cs.servlet.ZimbraServlet;
import org.apache.commons.codec.binary.Hex;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExternalUserProvServlet extends ZimbraServlet {

    @Override
    public void init() throws ServletException {
        String name = getServletName();
        ZimbraLog.account.info("Servlet " + name + " starting up");
        super.init();
    }

    @Override
    public void destroy() {
        String name = getServletName();
        ZimbraLog.account.info("Servlet " + name + " shutting down");
        super.destroy();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String param = req.getParameter("p");
        if (param == null) {
            throw new ServletException("request missing param");
        }
        Map<Object, Object> tokenMap = validatePrelimToken(param);
        String ownerId = (String) tokenMap.get("aid");
//        String folderId = (String) tokenMap.get("fid");
        String extUserEmail = (String) tokenMap.get("email");

        Provisioning prov = Provisioning.getInstance();
        Account grantee;
        try {
            Account owner = prov.getAccountById(ownerId);
            Domain domain = prov.getDomain(owner);
            grantee = prov.getAccountByName(mapExtEmailToAcctName(extUserEmail, domain));
            if (grantee == null) {
                // external virtual account not created yet
                resp.addCookie(new Cookie("ZM_PRELIM_AUTH_TOKEN", param));
                resp.sendRedirect("/zimbra/public/extuserprov.jsp");
            } else {
                // create a new mountpoint in the external user's mailbox
                // TODO

                String zAuthTokenCookie = null;
                javax.servlet.http.Cookie cookies[] = req.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if (cookie.getName().equals("ZM_AUTH_TOKEN")) {
                            zAuthTokenCookie = cookie.getValue();
                            break;
                        }
                    }
                }
                AuthToken zAuthToken = null;
                if (zAuthTokenCookie != null) {
                    try {
                        zAuthToken = AuthProvider.getAuthToken(zAuthTokenCookie);
                    } catch (AuthTokenException ignored) {
                        // auth token is not valid
                    }
                }
                if (zAuthToken != null && !zAuthToken.isExpired() && grantee.getId().equals(zAuthToken.getAccountId())) {
                    // external virtual account already logged-in
                    resp.sendRedirect(prov.getLocalServer().getMailURL());
                } else {
                    resp.sendRedirect("/zimbra/public/extuserlogin.jsp?domain=" + domain.getName());
                }
            }
        } catch (ServiceException e) {
            throw new ServletException(e);
        }
    }

    private static String mapExtEmailToAcctName(String extUserEmail, Domain domain) {
        return extUserEmail.replace("@", ".") + "@" + domain.getName();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String displayName = req.getParameter("displayname");
        String password = req.getParameter("password");
        String password2 = req.getParameter("password2");
        if (!StringUtil.equal(password, password2)) {
            resp.sendRedirect("/zimbra/public/extuserprov.jsp");
            return;
        }

        String prelimToken = null;
        javax.servlet.http.Cookie cookies[] = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("ZM_PRELIM_AUTH_TOKEN")) {
                    prelimToken = cookie.getValue();
                    break;
                }
            }
        }
        if (prelimToken == null) {
            throw new ServletException("unauthorized request");
        }
        Map<Object, Object> tokenMap = validatePrelimToken(prelimToken);
        String ownerId = (String) tokenMap.get("aid");
//        String folderId = (String) tokenMap.get("fid");
        String extUserEmail = (String) tokenMap.get("email");

        Provisioning prov = Provisioning.getInstance();
        try {
            Account owner = prov.getAccountById(ownerId);
            Domain domain = prov.getDomain(owner);
            Account grantee = prov.getAccountByName(mapExtEmailToAcctName(extUserEmail, domain));
            if (grantee != null) {
                throw new ServletException("invalid request: account already exists");
            }

            // search all shares accessible to the external user
            String searchQuery =
                    String.format("(&(objectclass=zimbraAccount)(zimbraSharedItem=granteeId:%s*))", extUserEmail);
            List<NamedEntry> accounts =
                    prov.searchAccounts(domain, searchQuery,
                                        new String[] {
                                                Provisioning.A_zimbraId,
                                                Provisioning.A_displayName,
                                                Provisioning.A_zimbraSharedItem },
                                        null, false, Provisioning.SD_ACCOUNT_FLAG);
            if (accounts.isEmpty()) {
                throw new ServletException("No shares discovered. You may try again after some time.");
            }

            // create external account
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraIsExternalVirtualAccount, ProvisioningConstants.TRUE);
            attrs.put(Provisioning.A_zimbraExternalUserMailAddress, extUserEmail);
            attrs.put(Provisioning.A_zimbraMailHost, prov.getLocalServer().getServiceHostname());
            attrs.put(Provisioning.A_displayName, displayName);
            grantee = prov.createAccount(mapExtEmailToAcctName(extUserEmail, domain), password, attrs);
            // create external account mailbox
            Mailbox granteeMbox = MailboxManager.getInstance().getMailboxByAccount(grantee);

            for (NamedEntry ne : accounts) {
                Account account = (Account) ne;
                String[] sharedItems = account.getSharedItem();
                for (String sharedItem : sharedItems) {
                    ShareInfoData shareData = AclPushSerializer.deserialize(sharedItem);
                    int i = shareData.getFolderPath().lastIndexOf("/");
                    String sharedFolderName = shareData.getFolderPath().substring(i + 1);
                    String mountpointName = account.getDisplayName() + "'s " + sharedFolderName;
                    granteeMbox.createMountpoint(null, Mailbox.ID_FOLDER_USER_ROOT, mountpointName, account.getId(),
                                                 shareData.getFolderId(), shareData.getFolderDefaultViewCode(), 0,
                                                 MailItem.DEFAULT_COLOR, false);
                }
            }

            AuthToken authToken = AuthProvider.getAuthToken(grantee);
            authToken.encode(resp, false, req.getScheme().equals("https"));
            resp.sendRedirect(prov.getLocalServer().getMailURL());
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private static Map<Object, Object> validatePrelimToken(String param) throws ServletException {
        int pos = param.indexOf('_');
        if (pos == -1) {
            throw new ServletException("invalid token param");
        }
        String ver = param.substring(0, pos);
        int pos2 = param.indexOf('_', pos + 1);
        if (pos2 == -1) {
            throw new ServletException("invalid token param");
        }
        String hmac = param.substring(pos + 1, pos2);
        String data = param.substring(pos2 + 1);
        try {
            AuthTokenKey key = AuthTokenKey.getVersion(ver);
            if (key == null) {
                throw new ServletException("unknown key version");
            }
            String computedHmac = ZimbraAuthToken.getHmac(data, key.getKey());
            if (!computedHmac.equals(hmac)) {
                throw new ServletException("hmac failure");
            }
            String decoded = new String(Hex.decodeHex(data.toCharArray()));
            return BlobMetaData.decode(decoded);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
