/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.soap;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;

import org.dom4j.QName;
import org.eclipse.jetty.continuation.Continuation;

import com.google.common.base.Strings;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ShareInfoData;
import com.zimbra.cs.account.accesscontrol.Rights;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.acl.AclPushSerializer;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.servlet.continuation.ResumeContinuationListener;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.cs.session.SoapSession.PushChannel;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.BuildInfo;

/**
 * This class models the soap context (the data from the soap envelope)
 * for a single request.
 *
 * @since May 29, 2004
 */
public final class ZimbraSoapContext {
    public static String DEFAULT_NOTIFICATION_FORMAT = "DEFAULT";
    public static final String soapRequestIdAttr = "zimbraSoapRequestId";
    /* seed randomly so that unlikely to get same ID used on different machines in network
     * at the same time. */
    private static AtomicInteger soapIdBase = new AtomicInteger(new Random().nextInt(Integer.MAX_VALUE));

    final class SessionInfo {
        String sessionId;
        int sequence;
        boolean created;

        SessionInfo(String id, int seqNo, boolean newSession) {
            sessionId = id;  sequence = seqNo;  created = newSession;
        }

        @Override
        public String toString() {
            return sessionId;
        }

        private class SoapPushChannel implements SoapSession.PushChannel {
            private final boolean mLocalChangesOnly;

            SoapPushChannel(boolean localOnly)  { mLocalChangesOnly = localOnly; }

            @Override
            public void closePushChannel() {
                // don't allow more than one NoOp to hang on an account
                signalNotification(true);
            }

            @Override
            public int getLastKnownSequence() {
                return sequence;
            }

            @Override
            public ZimbraSoapContext getSoapContext() {
                return ZimbraSoapContext.this;
            }

            @Override
            public boolean localChangesOnly() {
                return mLocalChangesOnly;
            }

            @Override
            public boolean isPersistent() {
                return false;
            }

            @Override
            public void notificationsReady() {
                signalNotification(false);
            }
        }

        PushChannel getPushChannel(boolean localChangesOnly) {
            return new SoapPushChannel(localChangesOnly);
        }
    }

    private static final Log sLog = LogFactory.getLog(ZimbraSoapContext.class);
    public static final int MAX_HOP_COUNT = 5;

    private ZAuthToken mRawAuthToken;
    private AuthToken mAuthToken;
    private String mAuthTokenAccountId;
    private String mRequestedAccountId;

    private SoapProtocol mRequestProtocol;
    private SoapProtocol mResponseProtocol;

    private boolean mChangeConstraintType = OperationContext.CHECK_MODIFIED;
    private int mMaximumChangeId = -1;

    private boolean mSessionEnabled;  // whether to create a new session for this request
    private boolean mSessionProxied;  // whether to create a new session for this request
    private SessionInfo mSessionInfo;
    private boolean mUnqualifiedItemIds;
    private boolean mWaitForNotifications;
    private boolean mCanceledWaitForNotifications = false;
    private ResumeContinuationListener continuationResume;  // used for blocking requests

    private ProxyTarget mProxyTarget;
    private boolean mIsProxyRequest;
    private int mHopCount;
    private boolean mMountpointTraversed;

    private String mOriginalUserAgent;
    private String mUserAgent;
    private String mRequestIP;
    private Integer mPort;
    private String mVia;
    private String soapRequestId;
    private String mNotificationFormat = DEFAULT_NOTIFICATION_FORMAT;
    private String mCurWaitSetID;
    //zdsync: for parsing locally constructed soap requests
    public ZimbraSoapContext(AuthToken authToken, String accountId,
            SoapProtocol reqProtocol, SoapProtocol respProtocol) throws ServiceException {
        this(authToken, accountId, reqProtocol, respProtocol, 0);
    }

    /**
     * For Search-Proxying, allows us to manually specify the HopCount to use.
     * <p>
     * Hop count is not checked for TOO_MANY_HOPS in this route.
     */
    public ZimbraSoapContext(AuthToken authToken, String accountId,
            SoapProtocol reqProtocol, SoapProtocol respProtocol, int hopCount)
        throws ServiceException {

        mAuthToken = authToken;
        mRawAuthToken = authToken.toZAuthToken();
        mAuthTokenAccountId = authToken.getAccountId();
        mRequestedAccountId = accountId;
        mRequestProtocol = reqProtocol;
        mResponseProtocol = respProtocol;
        mSessionEnabled = false;
        mHopCount = hopCount;

        mUserAgent = mRequestIP = mVia = null;
        soapRequestId = null;
    }

    /**
     * Creates a {@link ZimbraSoapContext} from another existing
     * {@link ZimbraSoapContext} for use in proxying.
     *
     * @param zsc context to clone
     */
    public ZimbraSoapContext(ZimbraSoapContext zsc) throws ServiceException {
        this(zsc, zsc.mRequestedAccountId);
    }

    /**
     * Creates a {@link ZimbraSoapContext} from another existing
     * {@link ZimbraSoapContext} for use in proxying.
     *
     * @param zsc context to clone
     * @param targetAccountId different account ID from the original request
     */
    public ZimbraSoapContext(ZimbraSoapContext zsc, String targetAccountId)
    throws ServiceException {
        this(zsc, targetAccountId, null);
    }

    /**
     * Creates a {@link ZimbraSoapContext} from another existing
     * {@link ZimbraSoapContext} for use in proxying.
     *
     * @param zsc context to clone
     * @param targetAccountId different account ID from the original request
     * @param session If session is non-null, it will be used for proxy notifications
     * @throws ServiceException
     */
    public ZimbraSoapContext(ZimbraSoapContext zsc, String targetAccountId, Session session)
    throws ServiceException {
        this(zsc, null, targetAccountId, session);
    }

    /** Creates a <code>ZimbraSoapContext</code> from another existing
     *  <code>ZimbraSoapContext</code> for use in proxying.
     *  If session is non-null, it will be used for proxy notifications.
     *  If authToken is not null, the auth token in the clone will be replaced by authToken.
     */
    public ZimbraSoapContext(ZimbraSoapContext zsc, AuthToken authToken, String targetAccountId, Session session)
    throws ServiceException {
        mUserAgent = zsc.mUserAgent;
        mRequestIP = zsc.mRequestIP;
        mPort = zsc.mPort;
        mVia = zsc.mVia;
        soapRequestId = zsc.soapRequestId;

        mRawAuthToken = authToken == null? zsc.mRawAuthToken : authToken.toZAuthToken();
        mAuthToken = authToken == null? zsc.mAuthToken : authToken;
        mAuthTokenAccountId = authToken == null? zsc.mAuthTokenAccountId : authToken.getAccountId();

        mRequestedAccountId = targetAccountId;
        mRequestProtocol = zsc.mRequestProtocol;
        mResponseProtocol = zsc.mResponseProtocol;
        mSessionInfo = zsc.mSessionInfo;
        mSessionEnabled = zsc.mSessionEnabled;
        mSessionProxied = true;
        mUnqualifiedItemIds = zsc.mUnqualifiedItemIds;
        mMountpointTraversed = zsc.mMountpointTraversed;
        setHopCount(zsc.mHopCount + 1);
        if (session != null) {
            mSessionEnabled = true;
            mSessionInfo = new SessionInfo(session.getSessionId(), (session instanceof SoapSession?((SoapSession)session).getCurrentNotificationSequence():0),false);
        }
    }

    /**
     * Creates a {@link ZimbraSoapContext} from the {@code <context>}
     * {@link Element} from the SOAP header.
     *
     * @param ctxt {@code <context>} Element (can be null if not present in request)
     * @param requestName - The SOAP request name - may be null
     * @param context The engine context, which might contain the auth token
     * @param requestProtocol  The SOAP protocol used for the request */
    public ZimbraSoapContext(Element ctxt, QName requestName, DocumentHandler handler, Map<String, Object> context,
            SoapProtocol requestProtocol) throws ServiceException {
        this(ctxt, requestName, handler, context, requestProtocol, null);
    }

    /**
     * Creates a {@link ZimbraSoapContext} from the {@code <context>}
     * {@link Element} from the SOAP header.
     *
     * @param ctxt {@code <context>} Element (can be null if not present in request)
     * @param requestName - The SOAP request name - may be null
     * @param context The engine context, which might contain the auth token
     * @param requestProtocol  The SOAP protocol used for the request
     * @param body The SOAP request body */
    public ZimbraSoapContext(Element ctxt, QName requestName, DocumentHandler handler, Map<String, Object> context,
            SoapProtocol requestProtocol, Element body) throws ServiceException {

        if (ctxt != null && !ctxt.getQName().equals(HeaderConstants.CONTEXT))
            throw new IllegalArgumentException("expected ctxt, got: " + ctxt.getQualifiedName());

        Provisioning prov = Provisioning.getInstance();

        // figure out if we're explicitly asking for a return format
        mResponseProtocol = mRequestProtocol = requestProtocol;
        Element eFormat = ctxt == null ? null : ctxt.getOptionalElement(HeaderConstants.E_FORMAT);
        if (eFormat != null) {
            String format = eFormat.getAttribute(HeaderConstants.A_TYPE, HeaderConstants.TYPE_XML);
            if (format.equals(HeaderConstants.TYPE_XML) && requestProtocol == SoapProtocol.SoapJS)
                mResponseProtocol = SoapProtocol.Soap12;
            else if (format.equals(HeaderConstants.TYPE_JAVASCRIPT))
                mResponseProtocol = SoapProtocol.SoapJS;
        }

        try {
            mAuthToken = AuthProvider.getAuthToken(ctxt, context);
            if (mAuthToken == null) {
                mAuthToken = AuthProvider.getJWToken(ctxt, context);
            }
            if (mAuthToken != null) {
                boolean isRegistered = mAuthToken.isRegistered();
                boolean isExpired = mAuthToken.isExpired();
                if (isExpired || !isRegistered) {
                    boolean voidOnExpired = false;

                    if (ctxt != null) {
                        Element eAuthTokenControl = ctxt.getOptionalElement(HeaderConstants.E_AUTH_TOKEN_CONTROL);
                        if (eAuthTokenControl != null) {
                            voidOnExpired = eAuthTokenControl.getAttributeBool(HeaderConstants.A_VOID_ON_EXPIRED, false);
                        }
                    }

                    if (voidOnExpired) {
                        // erase the auth token and continue
                        mAuthToken = null;
                    } else {
                        if (sLog.isDebugEnabled()) {
                            sLog.debug("Throwing AUTH_EXPIRED for token:%s expired=%s registered=%s",
                                    mAuthToken, isExpired, isRegistered);
                        }
                        throw ServiceException.AUTH_EXPIRED();
                    }
                } else {
                    mRawAuthToken = mAuthToken.toZAuthToken();
                    mAuthTokenAccountId = mAuthToken.getAccountId();
                }
            }
        } catch (AuthTokenException e) {
            // ignore and leave null
            mAuthToken = null;
            if (sLog.isDebugEnabled())
                sLog.debug("ZimbraContext AuthToken error: " + e.getMessage(), e);
        }

        // find out if we're executing in another user's context
        Element eAccount = ctxt == null ? null : ctxt.getOptionalElement(HeaderConstants.E_ACCOUNT);
        if (eAccount != null) {
            String key = eAccount.getAttribute(HeaderConstants.A_BY, null);
            String value = eAccount.getText();

            if (key == null) {
                mRequestedAccountId = null;
            } else if (key.equals(HeaderConstants.BY_NAME)) {
                if (mAuthToken == null) {
                    throw ServiceException.AUTH_REQUIRED();
                }
                Account account = prov.get(AccountBy.name, value, mAuthToken);
                // Overriding the value for sendMsgRequest if the account in header is not equal to the from address.
                // Because in case of Persona(added for the owner's account), the account passed in header is not equal to from address.
                // if zimbraAllowAnyFromAddress = TRUE for delegate's account then we can create Persona with any email address and the
                // from address could be different in this case.
                if (body != null && requestName.equals(MailConstants.SEND_MSG_REQUEST)) {
                   String fromAddress = AccountUtil.extractFromAddress(body);
                    if (!StringUtil.isNullOrEmpty(fromAddress) && !value.equals(fromAddress)
                            && !account.getBooleanAttr(Provisioning.A_zimbraAllowAnyFromAddress, false)) {
                        value = fromAddress;
                        account = prov.get(AccountBy.name, value, mAuthToken);
                    }
                }
                if (account == null) {
                    if (!mAuthToken.isAdmin()) {
                        throw ServiceException.DEFEND_ACCOUNT_HARVEST(value);
                    } else {
                        throw AccountServiceException.NO_SUCH_ACCOUNT(value);
                    }
                }

                mRequestedAccountId = account.getId();
                validateDelegatedAccess(account, handler, requestName, value);
            } else if (key.equals(HeaderConstants.BY_ID)) {
                if (mAuthToken == null) {
                    throw ServiceException.AUTH_REQUIRED();
                }
                Account account = prov.get(AccountBy.id, value, mAuthToken);
                if (body != null && requestName.equals(MailConstants.SEND_MSG_REQUEST)) {
                    String fromAddress = AccountUtil.extractFromAddress(body);
                    if (!StringUtil.isNullOrEmpty(fromAddress) && !account.getBooleanAttr(Provisioning.A_zimbraAllowAnyFromAddress, false)) {
                        Account fromAccount = prov.get(AccountBy.name, fromAddress, mAuthToken);
                        if(!value.equals(fromAccount.getId())) {
                            value = fromAccount.getId();
                            account = prov.get(AccountBy.id, value, mAuthToken);
                        }
                    }
                }
                if (account == null) {
                    if (!mAuthToken.isAdmin()) {
                        throw ServiceException.DEFEND_ACCOUNT_HARVEST(value);
                    } else {
                        throw AccountServiceException.NO_SUCH_ACCOUNT(value);
                    }
                }

                mRequestedAccountId = value;
                validateDelegatedAccess(account, handler, requestName, value);
            } else {
                throw ServiceException.INVALID_REQUEST("unknown value for by: " + key, null);
            }

            mMountpointTraversed = eAccount.getAttributeBool(HeaderConstants.A_MOUNTPOINT, false);
        } else if (body != null && requestName.equals(MailConstants.SEND_MSG_REQUEST)) {
            // To handle SendMsgRequest sent using zmsoap command where header does not exists.
            // Check if from Address is not equal to auth user email address then set `mRequestedAccountId`
            // as the from address accountId.
            if (mAuthToken == null) {
                throw ServiceException.AUTH_REQUIRED();
            }
            Account authAccount = mAuthToken.getAccount();
            String fromAddress = AccountUtil.extractFromAddress(body);
            String identityId = body.getElement(MailConstants.E_MSG).getAttribute(MailConstants.A_IDENTITY_ID, null);
            if (!StringUtil.isNullOrEmpty(fromAddress) && AccountUtil.isMessageSentUsingOwnersPersona(identityId, authAccount, mAuthToken)) {
                Account account = prov.get(AccountBy.name, fromAddress, mAuthToken);
                if (account == null) {
                    if (!mAuthToken.isAdmin()) {
                        throw ServiceException.DEFEND_ACCOUNT_HARVEST(fromAddress);
                    } else {
                        throw AccountServiceException.NO_SUCH_ACCOUNT(fromAddress);
                    }
                }
                if (!account.getId().equals(authAccount.getId())) {
                    mRequestedAccountId = account.getId();
                    validateDelegatedAccess(account, handler, requestName, fromAddress);
                }
            }
        } else {
            mRequestedAccountId = null;
        }

        // retrieve hop count from the SOAP context and check the hop count to detect loops
        if (ctxt != null) {
            int hopCount = (int) Math.max(ctxt.getAttributeLong(HeaderConstants.A_HOPCOUNT, 0), 0);
            setHopCount(hopCount);
        }

        // constrain operations if we know the max change number the client knows about
        Element change = (ctxt == null ? null : ctxt.getOptionalElement(HeaderConstants.E_CHANGE));
        if (change != null) {
            try {
                String token = change.getAttribute(HeaderConstants.A_CHANGE_ID, "-1");
                int delimiter = token.indexOf('-');

                mMaximumChangeId = Integer.parseInt(delimiter < 1 ? token : token.substring(0, delimiter));
                if (change.getAttribute(HeaderConstants.A_TYPE, HeaderConstants.CHANGE_MODIFIED).equals(HeaderConstants.CHANGE_MODIFIED))
                    mChangeConstraintType = OperationContext.CHECK_MODIFIED;
                else
                    mChangeConstraintType = OperationContext.CHECK_CREATED;
            } catch (NumberFormatException nfe) { }
        }

        // if the caller specifies an execution host or if we're on the wrong host, proxy
        mIsProxyRequest = false;
        String targetServerId = ctxt == null ? null : ctxt.getAttribute(HeaderConstants.E_TARGET_SERVER, null);
        if (targetServerId != null) {
            HttpServletRequest req = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
            if (req != null) {
                mProxyTarget = new ProxyTarget(targetServerId, mAuthToken, req);
                mIsProxyRequest = !mProxyTarget.isTargetLocal();
            } else {
                sLog.warn("Missing SERVLET_REQUEST key in request context");
            }
        }

        // look for the notification sequence id, for notification reliability
        //   <notify seq="nn">
        int seqNo = -1;
        Element notify = (ctxt == null ? null : ctxt.getOptionalElement(HeaderConstants.E_NOTIFY));
        if (notify != null)
            seqNo = (int) notify.getAttributeLong(HeaderConstants.A_SEQNO, 0);

        // record session-related info and validate any specified sessions
        //   (don't create new sessions yet)
        if (ctxt != null) {
            boolean suppress = ctxt.getOptionalElement(HeaderConstants.E_NO_NOTIFY) != null;
            suppress |= ctxt.getOptionalElement(HeaderConstants.E_NO_SESSION) != null;
            // if sessions are enabled, create a SessionInfo to encapsulate (will fetch the Session object during the request)
            if (!suppress) {
                Element session = ctxt.getOptionalElement(HeaderConstants.E_SESSION);
                if (session != null) {
                    mSessionEnabled = true;
                    mSessionProxied = session.getAttributeBool(HeaderConstants.A_PROXIED, false);

                    String sessionId = null;
                    if ("".equals(sessionId = session.getTextTrim())) {
                        sessionId = session.getAttribute(HeaderConstants.A_ID, null);
                    }
                    if (sessionId != null) {
                        mSessionInfo = new SessionInfo(sessionId, (int) session.getAttributeLong(HeaderConstants.A_SEQNO, seqNo), false);
                    }
                    mNotificationFormat = session.getAttribute(HeaderConstants.E_FORMAT, DEFAULT_NOTIFICATION_FORMAT);
                    mCurWaitSetID = session.getAttribute(HeaderConstants.A_WAITSET_ID, null);
                }
            }
        }

        // temporary hack: don't qualify item ids in responses, if so requested
        mUnqualifiedItemIds = (ctxt != null && ctxt.getOptionalElement(HeaderConstants.E_NO_QUALIFY) != null);

        // Handle user agent if specified by the client.  The user agent string is formatted
        // as "name/version".
        Element userAgent = (ctxt == null ? null : ctxt.getOptionalElement(HeaderConstants.E_USER_AGENT));
        if (userAgent != null) {
            String name = userAgent.getAttribute(HeaderConstants.A_NAME, null);
            String version = userAgent.getAttribute(HeaderConstants.A_VERSION, null);
            if (!Strings.isNullOrEmpty(name)) {
                if (!Strings.isNullOrEmpty(version)) {
                    mUserAgent = name + "/" + version;
                } else {
                    mUserAgent = name;
                }
            }
        }

        if (ctxt != null) {
            Element via = ctxt.getOptionalElement(HeaderConstants.E_VIA);
            if (via != null) {
                mVia = via.getText();
            }
            if (this.soapRequestId == null) {
                Element reqId = ctxt.getOptionalElement(HeaderConstants.E_SOAP_ID);
                if (null != reqId) {
                    this.soapRequestId = reqId.getText();
                }
            }
        }

        mRequestIP = (String) context.get(SoapEngine.REQUEST_IP);
        mPort = (Integer) context.get(SoapEngine.REQUEST_PORT);
        mOriginalUserAgent = (String) context.get(SoapEngine.ORIG_REQUEST_USER_AGENT);

    }

    /**
     * tells SoapSession how to format notification elements in SOAP headers.
     * Remote IMAP server uses IMAP format, whereas default SOAP and JSON clients use default format.
     * @return
     */
    public String getNotificationFormat() {
        return mNotificationFormat;
    }

    /**
     * WaitSetSession will NOT trigger a response for this Waitset, since the notifications
     * will be returned in SOAP headers instead. This ensures that the the remote server
     * isn't notified of the same notifications twice using different mechanisms.
     */
    public String getCurWaitSetID() {
        return mCurWaitSetID;
    }

    /**
     * Validate delegation rights. Request for delegated access requires a grant on at least one object in the target
     * account or admin login rights.
     * @param targetAccount - Account which requested is targeted for
     * @param requestedKey - The key sent in request which mapped to target account.
     *                       Passed in so error only reports back what was requested (i.e. can't harvest accountId if
     *                       you only know the email or vice-versa)
     * @param requestName - The SOAP request name - may be null
     * @throws ServiceException
     */
    private void validateDelegatedAccess(Account targetAccount, DocumentHandler handler,
            QName requestName, String requestedKey)
    throws ServiceException {

        if (!isDelegatedRequest()) {
            return;
        }

        if ((handler != null) && handler.handlesAccountHarvesting()) {
            return;
        }

        //if delegated one of the following MUST be true
        //1. authed account is an admin AND has admin rights for the target
        //2. authed account has been granted access (i.e. login) to the target account
        //3. target account has shared at least one item with authed account or enclosing group/cos/domain
        //4. target account has granted sendAs or sendOnBehalfOf right to authed account

        Account authAccount = null;
        boolean isAdmin = AuthToken.isAnyAdmin(mAuthToken);

        if (!GuestAccount.GUID_PUBLIC.equals(mAuthToken.getAccountId())) {
            authAccount = mAuthToken.getAccount();
            if (isAdmin && AccessManager.getInstance().canAccessAccount(mAuthToken, targetAccount, true)) {
                //case 1 - admin
                return;
            }

            if (isAdmin && (handler != null) && handler.defendsAgainstDelegateAdminAccountHarvesting()) {
                return;
            }

            if (AccessManager.getInstance().canAccessAccount(mAuthToken, targetAccount, false)) {
                //case 2 - access rights
                return;
            }
        }

        String externalEmail = null;
        if (authAccount != null && authAccount.getBooleanAttr(Provisioning.A_zimbraIsExternalVirtualAccount, false)) {
            externalEmail = authAccount.getAttr(Provisioning.A_zimbraExternalUserMailAddress, externalEmail);
        }

        Provisioning prov = Provisioning.getInstance();

        //case 3 - shared items
        boolean needRecheck = false;
        do {
            String[] sharedItems = targetAccount.getSharedItem();
            Set<String> groupIds = null;
            for (String sharedItem : sharedItems) {
                ShareInfoData shareData = AclPushSerializer.deserialize(sharedItem);
                switch (shareData.getGranteeTypeCode()) {
                    case ACL.GRANTEE_USER:
                        if (authAccount != null && authAccount.getId().equals(shareData.getGranteeId())) {
                            return;
                        }
                        break;
                    case ACL.GRANTEE_GUEST:
                        if (shareData.getGranteeId().equals(externalEmail)) {
                            return;
                        }
                        break;
                    case ACL.GRANTEE_PUBLIC:
                        return;
                    case ACL.GRANTEE_GROUP:
                        if (authAccount != null) {
                            if (groupIds == null) {
                                groupIds = new HashSet<String>();
                            }
                            groupIds.add(shareData.getGranteeId());
                        }
                        break;
                    case ACL.GRANTEE_AUTHUSER:
                        if (authAccount != null) {
                            return;
                        }
                        break;
                    case ACL.GRANTEE_DOMAIN:
                        if (authAccount != null && authAccount.getDomainId() != null
                                && authAccount.getDomainId().equals(shareData.getGranteeId())) {
                            return;
                        }
                        break;
                    case ACL.GRANTEE_COS:
                        if (authAccount != null && authAccount.getCOSId() != null
                                && authAccount.getCOSId().equals(shareData.getGranteeId())) {
                            return;
                        }
                        break;
                    case ACL.GRANTEE_KEY:
                        if (authAccount instanceof GuestAccount && mAuthToken.getAccessKey() != null) {
                            return;
                        }
                        break;
                 }
            }

            if (groupIds != null) {
                for (String groupId : groupIds) {
                    if (prov.inACLGroup(authAccount, groupId)) {
                        return;
                    }
                }
            }

            if (needRecheck) {
                break;
            } else if (!Provisioning.onLocalServer(targetAccount)) {
                //if target on different server we might not have up-to-date shared item list
                //reload and check one more time to be sure
                prov.reload(targetAccount);
                needRecheck = true;
            }
        } while (needRecheck);

        //case 4 - sendAs/sendOnBehalfOf
        AccessManager accessMgr = AccessManager.getInstance();
        if (accessMgr.canDo(authAccount, targetAccount, Rights.User.R_sendAs, isAdmin) ||
                accessMgr.canDo(authAccount, targetAccount, Rights.User.R_sendOnBehalfOf, isAdmin)) {
            return;
        }

        throw ServiceException.DEFEND_ACCOUNT_HARVEST(requestedKey);
    }

    /** Records in this context that we've traversed a mountpoint to get here.
     *  Enforces the "cannot mount a mountpoint" rule forbidding one link from
     *  pointing to another link.  The rationale for this limitation is that
     *  we want to avoid having the indirectly addressed resource completely
     *  change without the user knowing, and we want to know that the target
     *  of the link is also the owner of the items found therein.  Basically,
     *  things are just simpler this way.
     *
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><tt>service.PERM_DENIED</tt> - if you try to traverse two
     *        mountpoints in the course of a single proxy</ul> */
    public void recordMountpointTraversal() throws ServiceException {
        if (mMountpointTraversed)
            throw ServiceException.PERM_DENIED("cannot mount a mountpoint");
        mMountpointTraversed = true;
    }

    @Override
    public String toString() {
        String sessionPart = mSessionEnabled ? ", session=" + mSessionInfo : "";
        return "LC(mbox=" + mAuthTokenAccountId + sessionPart + ")";
    }

    /** Returns the account id the request is supposed to operate on.  This
     *  can be explicitly specified in the supplied context; it defaults to
     *  the account id in the auth token. */
    public String getRequestedAccountId() {
        return (mRequestedAccountId != null ? mRequestedAccountId : mAuthTokenAccountId);
    }

    /** Returns the id of the account in the auth token.  Operations should
     *  normally use {@link #getRequestedAccountId}, as that's the context
     *  that the operations is executing in. */
    public String getAuthtokenAccountId() {
        return mAuthTokenAccountId;
    }

    /** Sets the AuthToken of the authenticated account.  This should only be called
     *  in the course of some flavor of <tt>Auth</tt> request. */
    void setAuthToken(AuthToken auth) {
        mAuthToken = auth;
        mAuthTokenAccountId = auth.getAccountId();
    }

    /** Returns whether the authenticated user is the same as the user whose
     *  context the operation is set to execute in. */
    public boolean isDelegatedRequest() {
        return mAuthTokenAccountId != null && !mAuthTokenAccountId.equalsIgnoreCase(getRequestedAccountId());
    }

    public boolean isUsingAdminPrivileges() {
        return mAuthToken != null && AuthToken.isAnyAdmin(mAuthToken);
    }

    public ZimbraSoapContext disableNotifications() {
        mSessionEnabled = false;
        mSessionInfo = null;
        return this;
    }

    public boolean isNotificationEnabled() {
        return mSessionEnabled;
    }

    public boolean isSessionProxied() {
        return mSessionProxied;
    }

    /** Returns the {@link SessionInfo} item associated with this
     *  SOAP request.  SessionInfo objects correspond to either:<ul>
     *  <li>sessions specified in a <tt>&lt;sessionId></tt> element in the
     *      <tt>&lt;context></tt> SOAP header block, or</li>
     *  <li>new sessions created during the course of the SOAP call</li></ul> */
    SessionInfo getSessionInfo() {
        return mSessionInfo;
    }

    public SessionInfo setProxySession(String sessionId) {
        if (!mSessionEnabled || sessionId == null) {
            mSessionInfo = null;
        } else {
            mSessionInfo = new SessionInfo(sessionId, 0, false);
        }
        mSessionProxied = true;
        return mSessionInfo;
    }

    SessionInfo recordNewSession(String sessionId) {
        if (!mSessionEnabled || sessionId == null) {
            mSessionInfo = null;
        } else if (mSessionInfo == null) {
            mSessionInfo = new SessionInfo(sessionId, 0, true);
        } else {
            mSessionInfo.sessionId = sessionId;  mSessionInfo.created = true;
        }
        return mSessionInfo;
    }

    protected void clearSessionInfo() {
        mSessionInfo = null;
        mSessionProxied = false;
    }

    /**
     * Returns {@code TRUE} if our referenced session is brand-new.
     * <p>
     * This special-case API is used to short-circuit blocking handlers so that
     * they return immediately to send a {@code <refresh>} block if one is
     * needed.
     */
    public boolean hasCreatedSession() {
        return mSessionInfo != null && mSessionInfo.created;
    }

    public boolean hasSession() {
        return mSessionInfo != null;
    }

    public boolean beginWaitForNotifications(Continuation continuation, boolean includeDelegates) throws ServiceException {
        mWaitForNotifications = true;
        continuationResume = new ResumeContinuationListener(continuation);

        Session session = SessionCache.lookup(mSessionInfo.sessionId, mAuthTokenAccountId);
        if (!(session instanceof SoapSession))
            return false;

        SoapSession ss = (SoapSession) session;
        SoapSession.RegisterNotificationResult result = ss.registerNotificationConnection(mSessionInfo.getPushChannel(!includeDelegates));
        switch (result) {
            case NO_NOTIFY:   return false;
            case DATA_READY:  return false;
            case BLOCKING:    return true;
            default:          return false;
        }
    }

    public void suspendAndUndispatch(long timeout) {
        continuationResume.suspendAndUndispatch(timeout);
    }

    /** Called by the Session object if a new notification comes in. */
    synchronized public void signalNotification(boolean canceled) {
        mWaitForNotifications = false;
        mCanceledWaitForNotifications = canceled;
        continuationResume.resumeIfSuspended();
    }

    synchronized public boolean isCanceledWaitForNotifications() {
        return mCanceledWaitForNotifications;
    }

    synchronized public boolean waitingForNotifications() {
        return mWaitForNotifications;
    }

    /**
     * Serializes this object for use in a proxied SOAP request.
     * <p>
     * The attributes encapsulated by the {@link ZimbraSoapContext} -- the response protocol, the auth token, etc. are
     * carried forward, except that if {@code excludeAccountDetails} is set, any account details are omitted.
     */
    Element toProxyContext(SoapProtocol proto, boolean excludeAccountDetails) {
        Element ctxt = proto.getFactory().createElement(HeaderConstants.CONTEXT);

        ctxt.addAttribute(HeaderConstants.A_HOPCOUNT, mHopCount);

        String proxyAuthToken = null;

        if (mAuthToken != null) {
            proxyAuthToken = mAuthToken.getProxyAuthToken();
        }

        if (proxyAuthToken != null) {
            new ZAuthToken(proxyAuthToken).encodeSoapCtxt(ctxt);
        } else if (mRawAuthToken != null) {
            mRawAuthToken.encodeSoapCtxt(ctxt);
        }
        if (mResponseProtocol != mRequestProtocol) {
            ctxt.addNonUniqueElement(HeaderConstants.E_FORMAT).addAttribute(HeaderConstants.A_TYPE,
                    mResponseProtocol == SoapProtocol.SoapJS ?
                            HeaderConstants.TYPE_JAVASCRIPT : HeaderConstants.TYPE_XML);
        }

        if (!mSessionEnabled) {
            // be backwards-compatible for sanity-preservation purposes
            ctxt.addUniqueElement(HeaderConstants.E_NO_SESSION);
        } else if (mSessionInfo != null) {
            encodeSession(ctxt, mSessionInfo.sessionId, null).addAttribute(HeaderConstants.A_PROXIED, true);
        } else {
            ctxt.addUniqueElement(HeaderConstants.E_SESSION).addAttribute(HeaderConstants.A_PROXIED, true);
        }

        if (!excludeAccountDetails) {
            Element eAcct = ctxt.addNonUniqueElement(HeaderConstants.E_ACCOUNT).addAttribute(
                                            HeaderConstants.A_MOUNTPOINT, mMountpointTraversed);
            if (mRequestedAccountId != null && !mRequestedAccountId.equalsIgnoreCase(mAuthTokenAccountId)) {
                eAcct.addAttribute(HeaderConstants.A_BY, HeaderConstants.BY_ID).setText(mRequestedAccountId);
            }
        }

        if (mUnqualifiedItemIds) {
            ctxt.addUniqueElement(HeaderConstants.E_NO_QUALIFY);
        }
        Element ua = ctxt.addUniqueElement(HeaderConstants.E_USER_AGENT);
        ua.addAttribute(HeaderConstants.A_NAME, SoapTransport.DEFAULT_USER_AGENT_NAME);
        ua.addAttribute(HeaderConstants.A_VERSION, BuildInfo.VERSION);
        ctxt.addUniqueElement(HeaderConstants.E_VIA).setText(getNextVia());
        if (null != soapRequestId) {
            ctxt.addUniqueElement(HeaderConstants.E_SOAP_ID).setText(soapRequestId);
        }
        return ctxt;
    }

    /**
     * Serializes this object for use in a proxied SOAP request.
     * <p>
     * The attributes encapsulated by the {@link ZimbraSoapContext} -- the
     * response protocol, the auth token, etc. -- are carried forward.
     */
    Element toProxyContext(SoapProtocol proto) {
        return toProxyContext(proto, false);
    }

    /**
     * Serializes a {@link Session} object to return it to a client.
     * <p>
     * The serialized XML representation of a Session is:
     * {@code <sessionId [type="admin"] id="12">12</sessionId>}
     *
     * @param parent The {@link Element} to add the serialized Session to
     * @param sessionId TODO
     * @param sessionType TODO
     * @return The created {@code <sessionId>} Element
     */
    public static Element encodeSession(Element parent, String sessionId, Session.Type sessionType) {
        Element oldSession = parent.getOptionalElement(HeaderConstants.E_SESSION);
        if (oldSession != null)
            oldSession.detach();

        String typeStr = (sessionType == Session.Type.ADMIN ? HeaderConstants.SESSION_ADMIN : null);
        Element eSession = parent.addUniqueElement(HeaderConstants.E_SESSION);
        eSession.addAttribute(HeaderConstants.A_TYPE, typeStr).addAttribute(HeaderConstants.A_ID, sessionId).setText(sessionId);
        return eSession;
    }

    public SoapProtocol getRequestProtocol() {
        return mRequestProtocol;
    }

    public SoapProtocol getResponseProtocol() {
        return mResponseProtocol;
    }

    public Element createElement(String name) {
        return mResponseProtocol.getFactory().createElement(name);
    }

    public Element createElement(QName qname) {
        return mResponseProtocol.getFactory().createElement(qname);
    }

    public Element createRequestElement(String name) {
        return mRequestProtocol.getFactory().createElement(name);
    }

    public Element createRequestElement(QName qname) {
        return mRequestProtocol.getFactory().createElement(qname);
    }

    public <T> T elementToJaxb(Element e) throws ServiceException {
        return JaxbUtil.elementToJaxb(e);
    }

    /**
     * Only use this for response objects (or requests)
     * {@link jaxbToNamedElement} should be used for all other cases.
     */
    public Element jaxbToElement(Object resp) throws ServiceException {
        return JaxbUtil.jaxbToElement(resp, mResponseProtocol.getFactory());
    }

    /**
     * Use this rather than {@link jaxbToElement} when dealing with a
     * class that is not in the JAXB context.  This will be true if the
     * class is not for a top level Soap request or response.
     */
    public Element jaxbToNamedElement(String name, String namespace,
            Object o) throws ServiceException {
        return JaxbUtil.jaxbToNamedElement(name, namespace, o, mResponseProtocol.getFactory());
    }

    /**
     * Returns the parsed {@link AuthToken} for this SOAP request.
     * <p>
     * This can come either from an HTTP cookie attached to the SOAP request or
     * from an {@code <authToken>} element in the SOAP Header.
     */
    public AuthToken getAuthToken() {
        return mAuthToken;
    }

    /**
     * Returns the raw, encoded {@link AuthToken} for this SOAP request.
     * <p>
     * This can come either from an HTTP cookies attached to the SOAP request
     * or from an {@code <authToken>} element in the SOAP Header.
     */
    public ZAuthToken getRawAuthToken() {
        return mRawAuthToken;
    }

    public ProxyTarget getProxyTarget() {
        return mProxyTarget;
    }

    public boolean isProxyRequest() {
        return mIsProxyRequest;
    }

    public String getOriginalUserAgent() {
        return mOriginalUserAgent;
    }

    /**
     * Returns the name and version of the client that's making the current
     * request, in the format "name/version".
     */
    public String getUserAgent() {
        return mUserAgent;
    }

    public String getRequestIP() {
        return mRequestIP;
    }

    public Integer getPort() {
        return mPort;
    }

    public String getSoapRequestId() {
        return soapRequestId;
    }

    public void setSoapRequestId(String soapId) {
        soapRequestId = soapId;
        ZimbraLog.addSoapIdToContext(getSoapRequestId());
    }

    /**
     * Create an ID to use to follow a SOAP request and any associated proxied request going forward.
     * Not 100% guaranteed to be unique but probably good enough.
     */
    protected void setNewSoapRequestId() {
        /* note that relies on overflowing going -ve rather than throwing an exception.
         * Tested with Java 8 and that is what happens there */
        int nextId = soapIdBase.incrementAndGet();
        if (nextId < 0) {
            soapIdBase.set(1);  // restricting to +ve integers keeps hex short
        }
        setSoapRequestId(Long.toHexString(nextId));
    }

    /**
     * Returns {@code via} header value of the SOAP request.
     *
     * @return {@code via} header value
     */
    String getVia() {
        return mVia;
    }

    /**
     * Creates a {@code via} header value for next proxy hop.
     *
     * @return new {@code via}
     */
    String getNextVia() {
        StringBuilder result = new StringBuilder();

        if (mVia != null) {
            result.append(mVia);
            result.append(',');
        }

        result.append(mRequestIP != null ? mRequestIP : '?');
        result.append('(');
        result.append(mUserAgent != null ? mUserAgent : '?');
        result.append(')');

        return result.toString();
    }


    boolean getChangeConstraintType() {
        return mChangeConstraintType;
    }

    int getChangeConstraintLimit() {
        return mMaximumChangeId;
    }

    public boolean wantsUnqualifiedIds() {
        return mUnqualifiedItemIds;
    }

    private void setHopCount(int hopCount) throws ServiceException {
        if (hopCount > MAX_HOP_COUNT)
            throw ServiceException.TOO_MANY_HOPS(mRequestedAccountId);
        mHopCount = hopCount;
    }

    public int getHopCount() {
        return mHopCount;
    }

    public void resetProxyAuthToken() {
        mAuthToken.resetProxyAuthToken();
        mRawAuthToken.resetProxyAuthToken();
    }

    public boolean isAuthUserOnLocalhost() {
        if (mAuthTokenAccountId != null) {
            try {
                Account a = Provisioning.getInstance().getAccountById(mAuthTokenAccountId);
                if (a != null) {
                    return Provisioning.onLocalServer(a);
                }
            } catch (ServiceException e) {
            }
        }
        return false;
    }

    public SoapProtocol getmResponseProtocol() {
        return mResponseProtocol;
    }
}
