/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Hex;

import com.google.common.collect.Lists;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMountpoint;
import com.zimbra.common.account.ForgetPasswordEnums.CodeConstants;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.account.ZAttrProvisioning.FeatureAddressVerificationStatus;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.util.BlobMetaData;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.ExtAuthTokenKey;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchAccountsOptions;
import com.zimbra.cs.account.ShareInfoData;
import com.zimbra.cs.account.TokenUtil;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.acl.AclPushSerializer;
import com.zimbra.cs.service.util.JWEUtil;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.WebClientServiceUtil;
import com.zimbra.soap.mail.message.FolderActionRequest;
import com.zimbra.soap.mail.type.FolderActionSelector;

public class ExternalUserProvServlet extends ZimbraServlet {

    private static final Log logger = LogFactory.getLog(ExternalUserProvServlet.class);
    private static final String EXT_USER_PROV_ON_UI_NODE = "/fromservice/extuserprov";
    private static final String PUBLIC_LOGIN_ON_UI_NODE = "/fromservice/publiclogin";
    public static final String PUBLIC_EXTUSERPROV_JSP = "/public/extuserprov.jsp";
    public static final String PUBLIC_ADDRESS_VERIFICATION_JSP = "/public/addressVerification.jsp";
    public static final String PUBLIC_LOGIN_JSP = "/public/login.jsp";
    public static final String ERROR_CODE = "errorCode";
    public static final String MESSAGE_KEY = "messageKey";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String EXPIRED = "expired";

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
        ZimbraLog.account.info("Servlet " + getServletName() + " doGet()");
        String param = req.getParameter("p");
        if (param == null) {
            throw new ServletException("request missing param");
        }
        Map<Object, Object> tokenMap = validatePrelimToken(param);
        Map<String, String> reqHeaders = new HashMap<String, String>();
        String ownerId = (String) tokenMap.get(AccountConstants.P_ACCOUNT_ID);
        String folderId = (String) tokenMap.get(AccountConstants.P_FOLDER_ID);
        String extUserEmail = (String) tokenMap.get(AccountConstants.P_EMAIL);
        String addressVerification = (String) tokenMap.get(AccountConstants.P_ADDRESS_VERIFICATION);
        String accountVerification = (String) tokenMap.get(AccountConstants.P_ACCOUNT_VERIFICATION);
        String code = (String) tokenMap.get(AccountConstants.P_CODE);
        if ("1".equals(addressVerification)) {
            Boolean expired = false;
            if (tokenMap.get(EXPIRED) != null) {
                expired = (Boolean) tokenMap.get(EXPIRED);
            }
            Map<String, String> attributes = handleAddressVerification(req, resp, ownerId, extUserEmail, expired);
            redirectRequest(req, resp, attributes, EXT_USER_PROV_ON_UI_NODE, PUBLIC_ADDRESS_VERIFICATION_JSP);
        } else if("1".equals(accountVerification)) {
            ZimbraLog.account.info("Account Verfication and Password Setting");
            //get the ResetPasswordRecoveryCode
            try {
                Provisioning prov = Provisioning.getInstance();
                Account account = prov.getAccountById(ownerId);
                String encoded = account.getResetPasswordRecoveryCode();
                Map<String, String> recoveryCodeMap =  JWEUtil.getDecodedJWE(encoded);
                if (recoveryCodeMap != null && !recoveryCodeMap.isEmpty()) {
                    // check if the codes are same
                    if (ownerId.equals(recoveryCodeMap.get(CodeConstants.ACCOUNT_ID.toString()))
                            && code.equals(recoveryCodeMap.get(CodeConstants.CODE.toString()))) {
                        ZimbraLog.account.info("Account Verfication and Password Setting : URL authenticated");
                        // clear ResetPasswordRecoveryCode LDAP entry
                        account.unsetResetPasswordRecoveryCode();
                        // forward to appropriate page
                        // redirectRequest(req, resp, attributes, EXT_USER_PROV_ON_UI_NODE, PUBLIC_ADDRESS_VERIFICATION_JSP);
                    } else {
                        // unauthorized
                        ZimbraLog.account.info(
                                "Account Verfication and Password Setting Failed. The code or account id for the URL didn't match.");
                        throw ServiceException.PERM_DENIED("The URL is invalid.");
                    }
                } else {
                    // it has already been processed
                    ZimbraLog.account.info(
                            "Account Verfication and Password Setting Failed. The ResetPasswordRecoveryCode entry missing.It has already been used once.");
                    throw ServiceException.PERM_DENIED("The URL is invalid. It has already been used.");
                }
            } catch (ServiceException se) {
                ZimbraLog.account.warn("Error while resetting the password using URL:", se);
            } catch (Exception e) {
                ZimbraLog.account.warn("Error while resetting the password using URL:", e);
            }
        } else {
            Provisioning prov = Provisioning.getInstance();
            Account grantee;
            try {
                Account owner = prov.getAccountById(ownerId);
                Domain domain = prov.getDomain(owner);
                grantee = prov.getAccountByName(mapExtEmailToAcctName(extUserEmail, domain));
                if (grantee == null) {
                    // external virtual account not created yet
                    if (prov.isOctopus() && DebugConfig.skipVirtualAccountRegistrationPage) {
                        // provision using 'null' password and display name
                        // UI will ask the user to set these post provisioning
                        provisionVirtualAccountAndRedirect(req, resp, null, null, ownerId, extUserEmail);
                    } else {
                        resp.addCookie(new Cookie("ZM_PRELIM_AUTH_TOKEN", param));
                        Map<String, String> attrs = new HashMap<String, String>();
                        attrs.put("extuseremail", extUserEmail);
                        reqHeaders.put("ZM_PRELIM_AUTH_TOKEN", param);
                        redirectRequest(req, resp, attrs, reqHeaders, EXT_USER_PROV_ON_UI_NODE, PUBLIC_EXTUSERPROV_JSP);
                    }
                } else {
                    // create a new mountpoint in the external user's mailbox if not already created

                    String[] sharedItems = owner.getSharedItem();
                    int sharedFolderId = Integer.valueOf(folderId);
                    String sharedFolderPath = null;
                    MailItem.Type sharedFolderView = null;
                    for (String sharedItem : sharedItems) {
                        ShareInfoData sid = AclPushSerializer.deserialize(sharedItem);
                        if (sid.getItemId() == sharedFolderId && extUserEmail.equalsIgnoreCase(sid.getGranteeId())) {
                            sharedFolderPath = sid.getPath();
                            sharedFolderView = sid.getFolderDefaultViewCode();
                            break;
                        }
                    }
                    if (sharedFolderPath == null) {
                        throw new ServletException("share not found");
                    }
                    String mountpointName = getMountpointName(owner, grantee, sharedFolderPath);

                    ZMailbox.Options options = new ZMailbox.Options();
                    options.setNoSession(true);
                    options.setAuthToken(AuthProvider.getAuthToken(grantee).toZAuthToken());
                    options.setUri(AccountUtil.getSoapUri(grantee));
                    ZMailbox zMailbox = new ZMailbox(options);
                    ZMountpoint zMtpt = null;
                    try {
                        zMtpt = zMailbox.createMountpoint(
                                String.valueOf(getMptParentFolderId(sharedFolderView, prov)), mountpointName,
                                ZFolder.View.fromString(sharedFolderView.toString()), ZFolder.Color.DEFAULTCOLOR, null,
                                ZMailbox.OwnerBy.BY_ID, ownerId, ZMailbox.SharedItemBy.BY_ID, folderId, false);
                    } catch (ServiceException e) {
                        logger.debug("Error in attempting to create mountpoint. Probably it already exists.", e);
                    }
                    if (zMtpt != null) {
                        if (sharedFolderView == MailItem.Type.APPOINTMENT) {
                            // make sure that the mountpoint is checked in the UI by default
                            FolderActionSelector actionSelector = new FolderActionSelector(zMtpt.getId(), "check");
                            FolderActionRequest actionRequest = new FolderActionRequest(actionSelector);
                            try {
                                zMailbox.invokeJaxb(actionRequest);
                            } catch (ServiceException e) {
                                logger.warn("Error in invoking check action on calendar mountpoint", e);
                            }
                        }
                        HashSet<MailItem.Type> types = new HashSet<MailItem.Type>();
                        types.add(sharedFolderView);
                        enableAppFeatures(grantee, types);
                    }

                    // check if the external user is already logged-in
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
                    if (zAuthToken != null && !zAuthToken.isExpired() && zAuthToken.isRegistered() && grantee.getId().equals(zAuthToken.getAccountId())) {
                        // external virtual account already logged-in
                        resp.sendRedirect("/");
                    } else if (prov.isOctopus() && !grantee.isVirtualAccountInitialPasswordSet() &&
                            DebugConfig.skipVirtualAccountRegistrationPage) {
                        // seems like the virtual user did not set his password during his last visit, after an account was
                        // provisioned for him
                        setCookieAndRedirect(req, resp, grantee);
                    } else {
                        Map<String, String> attrs = new HashMap<String, String>();
                        attrs.put("virtualacctdomain", domain.getName());
                        redirectRequest(req, resp, attrs, PUBLIC_LOGIN_ON_UI_NODE, PUBLIC_LOGIN_JSP);
                    }
                }
            } catch (ServiceException e) {
                Map<String, String> errorAttrs = new HashMap<String, String>();
                errorAttrs.put(ERROR_CODE, e.getCode());
                errorAttrs.put(ERROR_MESSAGE, e.getMessage());
                redirectRequest(req, resp, errorAttrs, EXT_USER_PROV_ON_UI_NODE, PUBLIC_EXTUSERPROV_JSP);
            } catch (Exception e) {
                Map<String, String> errorAttrs = new HashMap<String, String>();
                errorAttrs.put(ERROR_CODE, ServiceException.FAILURE);
                errorAttrs.put(ERROR_MESSAGE, e.getMessage());
                redirectRequest(req, resp, errorAttrs, EXT_USER_PROV_ON_UI_NODE, PUBLIC_EXTUSERPROV_JSP);
            }
        }
    }

    public Map<String, String> handleAddressVerification(HttpServletRequest req, HttpServletResponse resp, String accountId, String emailVerified, Boolean expired) throws ServletException, IOException {
        Map<String, String> attrs = new HashMap<String, String>();
        HashMap<String, String> prefs = new HashMap<String, String>();
        Provisioning prov = Provisioning.getInstance();
        try {
            Account acct = prov.getAccountById(accountId);
            if (expired) {
                prefs.put(Provisioning.A_zimbraFeatureAddressVerificationStatus, FeatureAddressVerificationStatus.expired.toString());
                attrs.put(MESSAGE_KEY, "Expired");
            } else {
                prefs.put(Provisioning.A_zimbraPrefMailForwardingAddress, emailVerified);
                prefs.put(Provisioning.A_zimbraFeatureAddressVerificationStatus, FeatureAddressVerificationStatus.verified.toString());
                acct.unsetFeatureAddressUnderVerification();
                attrs.put(MESSAGE_KEY, "Success");
            }
            prov.modifyAttrs(acct, prefs, true, null);
        } catch (ServiceException e) {
            Map<String, String> errorAttrs = new HashMap<String, String>();
            errorAttrs.put(MESSAGE_KEY, "Failure");
            redirectRequest(req, resp, errorAttrs, EXT_USER_PROV_ON_UI_NODE, PUBLIC_ADDRESS_VERIFICATION_JSP);
        }
        return attrs;
    }

    private static String getMountpointName(Account owner, Account grantee, String sharedFolderPath)
            throws ServiceException {
        if (sharedFolderPath.startsWith("/")) {
            sharedFolderPath = sharedFolderPath.substring(1);
        }
        int index = sharedFolderPath.indexOf('/');
        if (index != -1) {
            // exclude the top level folder name, such as "Briefcase"
            sharedFolderPath = sharedFolderPath.substring(index + 1);
        }
        return L10nUtil.getMessage(L10nUtil.MsgKey.shareNameDefault, grantee.getLocale(),
                getDisplayName(owner), sharedFolderPath.replace("/", " "));
    }

    private static String getDisplayName(Account owner) {
        return owner.getDisplayName() != null ? owner.getDisplayName() : owner.getName();
    }

    private static String mapExtEmailToAcctName(String extUserEmail, Domain domain) {
        return extUserEmail.replace("@", ".") + "@" + domain.getName();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String displayName = req.getParameter("displayname");
        String password = req.getParameter("password");

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

        try {
            provisionVirtualAccountAndRedirect(req, resp, displayName, password, ownerId, extUserEmail);
        } catch (ServiceException e) {
            Map<String, String> errorAttrs = new HashMap<String, String>();
            errorAttrs.put(ERROR_CODE, e.getCode());
            errorAttrs.put(ERROR_MESSAGE, e.getMessage());
            redirectRequest(req, resp, errorAttrs, EXT_USER_PROV_ON_UI_NODE, PUBLIC_EXTUSERPROV_JSP);
        } catch (Exception e) {
            Map<String, String> errorAttrs = new HashMap<String, String>();
            errorAttrs.put(ERROR_CODE, ServiceException.FAILURE);
            errorAttrs.put(ERROR_MESSAGE, e.getMessage());
            redirectRequest(req, resp, errorAttrs, EXT_USER_PROV_ON_UI_NODE, PUBLIC_EXTUSERPROV_JSP);
        }
    }

    private void redirectRequest(HttpServletRequest req, HttpServletResponse resp, Map<String, String> attrs, String htmlResponsePage, String reqForwardPage)
        throws ServletException, IOException {
        HashMap<String, String> reqHeaders = new HashMap<String, String>();
        redirectRequest(req, resp, attrs, reqHeaders, htmlResponsePage, reqForwardPage);
    }

    private void redirectRequest(HttpServletRequest req, HttpServletResponse resp, Map<String, String> attrs, Map<String, String> reqHeaders, String htmlResponsePage, String reqForwardPage)
        throws ServletException, IOException {
        if (WebClientServiceUtil.isServerInSplitMode()) {
            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                reqHeaders.put(entry.getKey(), entry.getValue());
            }
            sendHtmlResponse(resp, reqHeaders, htmlResponsePage);
        } else {
            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                req.setAttribute(entry.getKey(), entry.getValue());
            }
            forward(req, resp, reqForwardPage);
        }
    }

    private void forward(HttpServletRequest req, HttpServletResponse resp, String reqForwardPage) throws ServletException, IOException {
        ServletContext context = getServletContext().getContext("/zimbra");
        if (context != null) {
            RequestDispatcher dispatcher = context.getRequestDispatcher(reqForwardPage);
            dispatcher.forward(req, resp);
        } else {
            logger.warn("Could not access servlet context url /zimbra");
            throw new ServletException("service temporarily unavailable");
        }
    }

    private void sendHtmlResponse(HttpServletResponse resp, Map<String, String> reqHeaders, String htmlResponsePage)
        throws ServletException, IOException {
        String htmlresp = "";
        try {
            htmlresp = WebClientServiceUtil.sendServiceRequestToOneRandomUiNode(htmlResponsePage, reqHeaders);
        } catch (ServiceException e1) {
            throw new ServletException("service temporarily unavailable");
        }
        resp.getWriter().print(htmlresp);
    }

    private static void provisionVirtualAccountAndRedirect(HttpServletRequest req, HttpServletResponse resp,
            String displayName, String password, String grantorId, String extUserEmail)
            throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        try {
            Account owner = prov.getAccountById(grantorId);
            Domain domain = prov.getDomain(owner);
            Account grantee = prov.getAccountByName(mapExtEmailToAcctName(extUserEmail, domain));
            if (grantee != null) {
                throw AccountServiceException.ACCOUNT_EXISTS(extUserEmail);
            }

            // search all shares accessible to the external user
            SearchAccountsOptions searchOpts = new SearchAccountsOptions(
                    domain, new String[] {
                            Provisioning.A_zimbraId,
                            Provisioning.A_displayName,
                            Provisioning.A_zimbraSharedItem });
            // get all groups extUserEmail belongs to
            GuestAccount guestAcct = new GuestAccount(extUserEmail, null);
            List<String> groupIds = prov.getGroupMembership(guestAcct, false).groupIds();
            List<String> grantees = Lists.newArrayList(extUserEmail);
            grantees.addAll(groupIds);
            searchOpts.setFilter(ZLdapFilterFactory.getInstance().accountsByGrants(grantees, false, false));
            List<NamedEntry> accounts = prov.searchDirectory(searchOpts);

            if (accounts.isEmpty()) {
                throw AccountServiceException.NO_SHARE_EXISTS();
            }

            // create external account
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraIsExternalVirtualAccount, ProvisioningConstants.TRUE);
            attrs.put(Provisioning.A_zimbraExternalUserMailAddress, extUserEmail);
            attrs.put(Provisioning.A_zimbraMailHost, prov.getLocalServer().getServiceHostname());
            if (!StringUtil.isNullOrEmpty(displayName)) {
                attrs.put(Provisioning.A_displayName, displayName);
            }
            attrs.put(Provisioning.A_zimbraHideInGal, ProvisioningConstants.TRUE);
            attrs.put(Provisioning.A_zimbraMailStatus, Provisioning.MailStatus.disabled.toString());
            if (!StringUtil.isNullOrEmpty(password)) {
                attrs.put(Provisioning.A_zimbraVirtualAccountInitialPasswordSet, ProvisioningConstants.TRUE);
            }

            // create external account mailbox
            Mailbox granteeMbox;
            try {
                grantee = prov.createAccount(mapExtEmailToAcctName(extUserEmail, domain), password, attrs);
                granteeMbox = MailboxManager.getInstance().getMailboxByAccount(grantee);
            } catch (ServiceException e) {
                // mailbox creation failed; delete the account also so that it is a clean state before
                // the next attempt
                if (grantee != null) {
                    prov.deleteAccount(grantee.getId());
                }
                throw e;
            }

            // create mountpoints
            Set<MailItem.Type> viewTypes = new HashSet<MailItem.Type>();
            for (NamedEntry ne : accounts) {
                Account account = (Account) ne;
                String[] sharedItems = account.getSharedItem();
                for (String sharedItem : sharedItems) {
                    ShareInfoData shareData = AclPushSerializer.deserialize(sharedItem);
                    if (!granteeMatchesShare(shareData, grantee)) {
                        continue;
                    }
                    String sharedFolderPath = shareData.getPath();
                    String mountpointName = getMountpointName(account, grantee, sharedFolderPath);
                    MailItem.Type viewType = shareData.getFolderDefaultViewCode();
                    Mountpoint mtpt = granteeMbox.createMountpoint(
                            null, getMptParentFolderId(viewType, prov), mountpointName, account.getId(),
                            shareData.getItemId(), shareData.getItemUuid(), viewType, 0, MailItem.DEFAULT_COLOR, false);
                    if (viewType == MailItem.Type.APPOINTMENT) {
                        // make sure that the mountpoint is checked in the UI by default
                        granteeMbox.alterTag(null, mtpt.getId(), mtpt.getType(), Flag.FlagInfo.CHECKED, true, null);
                    }
                    viewTypes.add(viewType);
                }
            }
            enableAppFeatures(grantee, viewTypes);

            setCookieAndRedirect(req, resp, grantee);
        } catch (ServiceException e) {
            ZimbraLog.account.debug("Exception while creating virtual account for %s", extUserEmail, e);
            throw e;
        }  catch (Exception e) {
            ZimbraLog.account.debug("Exception while creating virtual account for %s", extUserEmail, e);
            throw ServiceException.TEMPORARILY_UNAVAILABLE();
        }
    }

    private static boolean granteeMatchesShare(ShareInfoData shareData, Account acct) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        String grantee = shareData.getGranteeId();
        byte granteeType = shareData.getGranteeTypeCode();
        switch (granteeType) {
            case ACL.GRANTEE_GROUP:    return prov.inACLGroup(acct, grantee);
            case ACL.GRANTEE_GUEST:    return grantee.equalsIgnoreCase(acct.getExternalUserMailAddress());
            default: return false;
        }
    }

    private static void setCookieAndRedirect(HttpServletRequest req, HttpServletResponse resp, Account grantee)
            throws ServiceException, IOException {
        AuthToken authToken = AuthProvider.getAuthToken(grantee);
        authToken.encode(resp, false, req.getScheme().equals("https"));
        resp.sendRedirect("/");
    }

    private static int getMptParentFolderId(MailItem.Type viewType, Provisioning prov) throws ServiceException {
        switch (viewType) {
            case DOCUMENT:
                if (prov.isOctopus()) {
                    return Mailbox.ID_FOLDER_BRIEFCASE;
                }
            default:
                return Mailbox.ID_FOLDER_USER_ROOT;
        }
    }

    private static void enableAppFeatures(Account grantee, Set<MailItem.Type> viewTypes) throws ServiceException {
        Map<String, Object> appFeatureAttrs = new HashMap<String, Object>();
        for (MailItem.Type type : viewTypes) {
            switch (type) {
                case DOCUMENT:
                    appFeatureAttrs.put(Provisioning.A_zimbraFeatureBriefcasesEnabled, ProvisioningConstants.TRUE);
                    break;
                case APPOINTMENT:
                    appFeatureAttrs.put(Provisioning.A_zimbraFeatureCalendarEnabled, ProvisioningConstants.TRUE);
                    break;
                case CONTACT:
                    appFeatureAttrs.put(Provisioning.A_zimbraFeatureContactsEnabled, ProvisioningConstants.TRUE);
                    break;
                case TASK:
                    appFeatureAttrs.put(Provisioning.A_zimbraFeatureTasksEnabled, ProvisioningConstants.TRUE);
                    break;
                case MESSAGE:
                    appFeatureAttrs.put(Provisioning.A_zimbraFeatureMailEnabled, ProvisioningConstants.TRUE);
                    break;
                default:
                    // we don't care about other types
            }
        }
        grantee.modify(appFeatureAttrs);
    }

    public static Map<Object, Object> validatePrelimToken(String param) throws ServletException {
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
        Map<Object, Object> map;
        try {
            ExtAuthTokenKey key = ExtAuthTokenKey.getVersion(ver);
            if (key == null) {
                throw new ServletException("unknown key version");
            }
            String computedHmac = TokenUtil.getHmac(data, key.getKey());
            if (!computedHmac.equals(hmac)) {
                throw new ServletException("hmac failure");
            }
            String decoded = new String(Hex.decodeHex(data.toCharArray()));
            map = BlobMetaData.decode(decoded);
        } catch (Exception e) {
            throw new ServletException(e);
        }
        Object expiry = map.get(AccountConstants.P_LINK_EXPIRY);
        if (expiry != null) {
            // check validity
            if (System.currentTimeMillis() > Long.parseLong((String) expiry)) {
                String addressVerification = (String) map.get(AccountConstants.P_ADDRESS_VERIFICATION);
                if ("1".equals(addressVerification)) {
                    map.put(EXPIRED, true);
                } else {
                    throw new ServletException("url no longer valid");
                }
            }
        }
        return map;
    }
}
