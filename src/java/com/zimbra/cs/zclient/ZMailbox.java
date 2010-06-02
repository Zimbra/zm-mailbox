/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.zclient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.net.SocketFactories;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.dom4j.QName;
import org.json.JSONException;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.RemoteServiceException;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.soap.VoiceConstants;
import com.zimbra.common.soap.ZimbraNamespace;
import com.zimbra.common.soap.Element.JSONElement;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.SoapTransport.DebugListener;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ListUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DataSourceBy;
import com.zimbra.cs.account.Provisioning.IdentityBy;
import com.zimbra.cs.fb.FreeBusyQuery;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.zclient.ZFolder.Color;
import com.zimbra.cs.zclient.ZGrant.GranteeType;
import com.zimbra.cs.zclient.ZInvite.ZTimeZone;
import com.zimbra.cs.zclient.ZMailbox.ZOutgoingMessage.AttachedMessagePart;
import com.zimbra.cs.zclient.ZSearchParams.Cursor;
import com.zimbra.cs.zclient.event.*;

public class ZMailbox implements ToZJSONObject {
    public final static int MAX_NUM_CACHED_SEARCH_PAGERS = 5;
    public final static int MAX_NUM_CACHED_SEARCH_CONV_PAGERS = 5;
    public final static int MAX_NUM_CACHED_MESSAGES = LC.zmailbox_message_cachesize.intValue();
    public final static int MAX_NUM_CACHED_CONTACTS = 25;

    public final static String PATH_SEPARATOR = "/";

    public final static char PATH_SEPARATOR_CHAR = '/';

    static {
        SocketFactories.registerProtocols();
    }
    
    public enum SearchSortBy {
        dateDesc, dateAsc, subjDesc, subjAsc, nameDesc, nameAsc, durDesc, durAsc, none,
        taskDueAsc, taskDueDesc, taskStatusAsc, taskStatusDesc, taskPercCompletedAsc, taskPercCompletedDesc;

        public static SearchSortBy fromString(String s) throws ServiceException {
            try {
                return SearchSortBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid sortBy: "+s+", valid values: "+Arrays.asList(SearchSortBy.values()), e);
            }
        }
    }

    public enum Fetch {
        none, first, hits, all;

        public static Fetch fromString(String s) throws ServiceException {
            try {
                return Fetch.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid fetch: "+s+", valid values: "+Arrays.asList(Fetch.values()), e);
            }
        }
    }

    private enum NotifyPreference {
        nosession, full;

        static NotifyPreference fromOptions(Options options) {
            if (options == null)              return full;
            else if (options.getNoSession())  return nosession;
            else                              return full;
        }
    }

    public static class Options {
        private String mAccount;
        private AccountBy mAccountBy = AccountBy.name;
        private String mPassword;
        private String mNewPassword;
        private ZAuthToken mAuthToken;
        private String mVirtualHost;
        private String mUri;
        private String mClientIp;
        private String mProxyHost;
        private int mProxyPort;
        private String mProxyUser;
        private String mProxyPass;
        private String mUserAgentName;
        private String mUserAgentVersion;
        private int mTimeout = -1;
        private int mRetryCount = -1;
        private SoapProtocol mResponseProtocol = SoapProtocol.SoapJS; //12; //JS;
        private SoapProtocol mRequestProtocol = SoapProtocol.SoapJS; //12; //JS;
        private SoapTransport.DebugListener mDebugListener;
        private SoapHttpTransport.HttpDebugListener mHttpDebugListener;
        private String mTargetAccount;
        private AccountBy mTargetAccountBy = AccountBy.name;
        private boolean mNoSession;
        private boolean mAuthAuthToken;
        private ZEventHandler mHandler;
        private List<String> mAttrs;
        private List<String> mPrefs;
        private String mRequestedSkin;

		public Options() {
        }

        public Options(String account, AccountBy accountBy, String password, String uri) {
            mAccount = account;
            mAccountBy = accountBy;
            mPassword = password;
            setUri(uri);
        }

        // AP-TODO-7: retire
        public Options(String authToken, String uri) {
            mAuthToken = new ZAuthToken(null, authToken, null);
            setUri(uri);
        }

        public Options(ZAuthToken authToken, String uri) {
            mAuthToken = authToken;
            setUri(uri);
        }

        public String getClientIp() { return mClientIp; }
        public void setClientIp(String clientIp) { mClientIp = clientIp; }

        public String getAccount() { return mAccount; }
        public void setAccount(String account) { mAccount = account; }

        public AccountBy getAccountBy() { return mAccountBy; }
        public void setAccountBy(AccountBy accountBy) { mAccountBy = accountBy; }

        public String getTargetAccount() { return mTargetAccount; }
        public void setTargetAccount(String targetAccount) { mTargetAccount = targetAccount; }

        public AccountBy getTargetAccountBy() { return mTargetAccountBy; }
        public void setTargetAccountBy(AccountBy targetAccountBy) { mTargetAccountBy = targetAccountBy; }

        public String getPassword() { return mPassword; }
        public void setPassword(String password) { mPassword = password; }

        public String getNewPassword() { return mNewPassword; }
        public void setNewPassword(String newPassword) { mNewPassword = newPassword; }

        public String getVirtualHost() { return mVirtualHost; }
        public void setVirtualHost(String virtualHost) { mVirtualHost = virtualHost; }

        public ZAuthToken getAuthToken() { return mAuthToken; }
        public void setAuthToken(ZAuthToken authToken) { mAuthToken = authToken; }
        
        // AP-TODO-8: retire
        public void setAuthToken(String authToken) { mAuthToken = new ZAuthToken(null, authToken, null); } 

        public String getUri() { return mUri; }
        public void setUri(String uri) {
            setUri(uri, false);
        }

        public void setUri(String uri, boolean isAdmin) {
            try {
                mUri = resolveUrl(uri, isAdmin);
            } catch (ZClientException e) {
                mUri = uri;
            }
        }

        public String getUserAgentName() { return mUserAgentName; }
        public String getUserAgentVersion() { return mUserAgentVersion; }
        public void setUserAgent(String name, String version) {
        	mUserAgentName = name;
        	mUserAgentVersion = version;
        }

        public int getTimeout() { return mTimeout; }
        public void setTimeout(int timeout) { mTimeout = timeout; }

        public int getRetryCount() { return mRetryCount; }
        public void setRetryCount(int retryCount) { mRetryCount = retryCount; }

        public SoapProtocol getResponseProtocol() { return mResponseProtocol; }
        public void setResponseProtocol(SoapProtocol proto) { mResponseProtocol = proto; }

        public SoapProtocol getRequestProtocol() { return mRequestProtocol; }
        public void setRequestProtocol(SoapProtocol proto) { mRequestProtocol = proto; }

        public SoapTransport.DebugListener getDebugListener() { return mDebugListener; }
        public void setDebugListener(SoapTransport.DebugListener listener) { mDebugListener = listener; }

        public SoapHttpTransport.HttpDebugListener getHttpDebugListener() { return mHttpDebugListener; }
        public void setHttpDebugListener(SoapHttpTransport.HttpDebugListener listener) { mHttpDebugListener = listener; }
        
        public boolean getNoSession() { return mNoSession; }
        public void setNoSession(boolean noSession) { mNoSession = noSession; }

        public boolean getAuthAuthToken() { return mAuthAuthToken; }
        /** @param authAuthToken set to true if you want to send an AuthRequest to valid the auth token */
        public void setAuthAuthToken(boolean authAuthToken) { mAuthAuthToken = authAuthToken; }

        public ZEventHandler getEventHandler() { return mHandler; }
        public void setEventHandler(ZEventHandler handler) { mHandler = handler; }

        public List<String> getPrefs() { return mPrefs; }
        public void setPrefs(List<String> prefs) { mPrefs = prefs; }

        public List<String> getAttrs() { return mAttrs; }
        public void setAttrs(List<String> attrs) { mAttrs = attrs; }

		public String getRequestedSkin() { return mRequestedSkin; }
		public void setRequestedSkin(String skin) { mRequestedSkin = skin; }
	}

    private ZAuthToken mAuthToken;
    private SoapHttpTransport mTransport;
    private NotifyPreference mNotifyPreference;
    private Map<String, ZTag> mNameToTag;
    private Map<String, ZItem> mIdToItem;
    private ZGetInfoResult mGetInfoResult;
    private ZFolder mUserRoot;
    private ZSearchPagerCache mSearchPagerCache;
    private ZSearchPagerCache mSearchConvPagerCache;
    private ZApptSummaryCache mApptSummaryCache;
    private LRUMap mMessageCache;
    private LRUMap mContactCache;
    private ZFilterRules mRules;
    private ZAuthResult mAuthResult;
    private String mClientIp;
    private List<ZPhoneAccount> mPhoneAccounts;
    private Map<String, ZPhoneAccount> mPhoneAccountMap;
	private Element mVoiceStorePrincipal;
	private long mSize;
	private boolean mNoTagCache;
	private ZContactByPhoneCache mContactByPhoneCache; 

	private List<ZEventHandler> mHandlers = new ArrayList<ZEventHandler>();

    public static ZMailbox getMailbox(Options options) throws ServiceException {
    	return new ZMailbox(options);
    }

    /**
     * for use with changePassword
     */
    private ZMailbox() { }

    /**
     * change password. You must pass in an options with an account, password, newPassword, and Uri.
     * @param options uri/name/pass/newPass
     * @throws ServiceException on error
     */
    public static ZChangePasswordResult changePassword(Options options) throws ServiceException {
        ZMailbox mailbox = new ZMailbox();
        mailbox.mClientIp = options.getClientIp();
        mailbox.mNotifyPreference = NotifyPreference.fromOptions(options);
        mailbox.initPreAuth(options);
        return mailbox.changePassword(options.getAccount(), options.getAccountBy(), options.getPassword(), options.getNewPassword(), options.getVirtualHost());
    }

    public static ZMailbox getByName(String name, String password, String uri) throws ServiceException {
        return new ZMailbox(new Options(name, AccountBy.name, password, uri));
    }

    public static ZMailbox getByAuthToken(String authToken, String uri) throws ServiceException {
        return new ZMailbox(new Options(authToken, uri));
    }

    public static ZMailbox getByAuthToken(ZAuthToken authToken, String uri) throws ServiceException {
        return new ZMailbox(new Options(authToken, uri));
    }

    public ZMailbox(Options options) throws ServiceException {
    	mHandlers.add(new InternalEventHandler());
    	mSearchPagerCache = new ZSearchPagerCache(MAX_NUM_CACHED_SEARCH_PAGERS, true);
        mHandlers.add(mSearchPagerCache);
        mSearchConvPagerCache = new ZSearchPagerCache(MAX_NUM_CACHED_SEARCH_CONV_PAGERS, false);
        mHandlers.add(mSearchConvPagerCache);
        mMessageCache = new LRUMap(MAX_NUM_CACHED_MESSAGES);
        mContactCache = new LRUMap(MAX_NUM_CACHED_CONTACTS);
        mApptSummaryCache = new ZApptSummaryCache();
        mHandlers.add(mApptSummaryCache);
        if (options.getEventHandler() != null)
    		mHandlers.add(options.getEventHandler());

        mNotifyPreference = NotifyPreference.fromOptions(options);

        mClientIp = options.getClientIp();

        initPreAuth(options);
        if (options.getAuthToken() != null) {
            if (options.getAuthAuthToken())
                mAuthResult = authByAuthToken(options);
            initAuthToken(options.getAuthToken());
        } else {
            String password;
            if (options.getNewPassword() != null) {
                changePassword(options.getAccount(), options.getAccountBy(), options.getPassword(), options.getNewPassword(), options.getVirtualHost());
                password = options.getNewPassword();
            } else {
                password = options.getPassword();
            }
            mAuthResult = authByPassword(options, password);
            initAuthToken(mAuthResult.getAuthToken());
        }
        if (options.getTargetAccount() != null) {
            initTargetAccount(options.getTargetAccount(), options.getTargetAccountBy());
        }
    }

    public boolean addEventHandler(ZEventHandler handler) {
    	if (!mHandlers.contains(handler)) {
    		mHandlers.add(handler);
    		return true;
    	} else {
    		return false;
    	}
    }

    public boolean removeEventHandler(ZEventHandler handler) {
        return mHandlers.remove(handler);
    }

    private void initAuthToken(ZAuthToken authToken){
        mAuthToken = authToken;
        mTransport.setAuthToken(mAuthToken);
    }

    private void initPreAuth(Options options) {
        mIdToItem = new HashMap<String, ZItem>();
        setSoapURI(options);
        if (options.getDebugListener() != null) 
            mTransport.setDebugListener(options.getDebugListener());
        else if (options.getHttpDebugListener() != null) 
            mTransport.setHttpDebugListener(options.getHttpDebugListener());
    }

    private void initTargetAccount(String key, AccountBy by) {
        if (AccountBy.id.equals(by))
            mTransport.setTargetAcctId(key);
        else if (AccountBy.name.equals(by))
            mTransport.setTargetAcctName(key);
    }

    public Element newRequestElement(QName name) {
        if (mTransport.getRequestProtocol() == SoapProtocol.SoapJS) {
            return new JSONElement(name);
        } else {
            return new XMLElement(name);
        }
    }

    private ZChangePasswordResult changePassword(String key, AccountBy by, String oldPassword, String newPassword, String virtualHost) throws ServiceException {
        if (mTransport == null) throw ZClientException.CLIENT_ERROR("must call setURI before calling changePassword", null);
        Element req = newRequestElement(AccountConstants.CHANGE_PASSWORD_REQUEST);
        Element account = req.addUniqueElement(AccountConstants.E_ACCOUNT);
        account.addAttribute(AccountConstants.A_BY, by.name());
        account.setText(key);
        req.addElement(AccountConstants.E_OLD_PASSWORD).setText(oldPassword);
        req.addElement(AccountConstants.E_PASSWORD).setText(newPassword);
        if (virtualHost != null)
            req.addElement(AccountConstants.E_VIRTUAL_HOST).setText(virtualHost);
        return new ZChangePasswordResult(invoke(req));
    }

    private void addAttrsAndPrefs(Element req, Options options) {
        List<String> prefs = options.getPrefs();
        if (prefs != null && !prefs.isEmpty()) {
            Element prefsEl = req.addElement(AccountConstants.E_PREFS);
            for (String p : prefs)
                prefsEl.addElement(AccountConstants.E_PREF).addAttribute(AccountConstants.A_NAME, p);
        }
        List<String> attrs = options.getAttrs();
        if (attrs != null && !attrs.isEmpty()) {
            Element attrsEl = req.addElement(AccountConstants.E_ATTRS);
            for (String a : attrs)
                attrsEl.addElement(AccountConstants.E_ATTR).addAttribute(AccountConstants.A_NAME, a);
        }
    }

    private ZAuthResult authByPassword(Options options, String password) throws ServiceException {
        if (mTransport == null) throw ZClientException.CLIENT_ERROR("must call setURI before calling authenticate", null);
        Element req = newRequestElement(AccountConstants.AUTH_REQUEST);
        Element account = req.addElement(AccountConstants.E_ACCOUNT);
        account.addAttribute(AccountConstants.A_BY, options.getAccountBy().name());
        account.setText(options.getAccount());
        req.addElement(AccountConstants.E_PASSWORD).setText(password);
        if (options.getVirtualHost() != null)
            req.addElement(AccountConstants.E_VIRTUAL_HOST).setText(options.getVirtualHost());
		if (options.getRequestedSkin() != null) {
			req.addElement(AccountConstants.E_REQUESTED_SKIN).setText(options.getRequestedSkin());
		}
		addAttrsAndPrefs(req, options);
        ZAuthResult r = new ZAuthResult(invoke(req));
        r.setSessionId(mTransport.getSessionId());
        return r;
    }

    private ZAuthResult authByAuthToken(Options options) throws ServiceException {
        if (mTransport == null) throw ZClientException.CLIENT_ERROR("must call setURI before calling authenticate", null);
        Element req = newRequestElement(AccountConstants.AUTH_REQUEST);
        ZAuthToken zat = options.getAuthToken(); // cannot be null here
        
        zat.encodeAuthReq(req, false);
        if (options.getRequestedSkin() != null) {
			req.addElement(AccountConstants.E_REQUESTED_SKIN).setText(options.getRequestedSkin());
		}
		addAttrsAndPrefs(req, options);
        ZAuthResult r = new ZAuthResult(invoke(req));
        r.setSessionId(mTransport.getSessionId());
        return r;
    }

    public ZAuthResult getAuthResult() {
        return mAuthResult;
    }

    public ZAuthToken getAuthToken() {
        return mAuthToken;
    }

    /**
     * @param uri URI of server we want to talk to
     * @param timeout timeout for HTTP connection or 0 for no timeout
     * @param retryCount max number of times to retry the call on connection failure
     */
    private void setSoapURI(Options options) {
        if (mTransport != null) mTransport.shutdown();
        mTransport = new SoapHttpTransport(options.getUri());
        if (options.getUserAgentName() == null)
            mTransport.setUserAgent("zclient", BuildInfo.VERSION);
        else
            mTransport.setUserAgent(options.getUserAgentName(), options.getUserAgentVersion());
        mTransport.setMaxNotifySeq(0);
        mTransport.setClientIp(mClientIp);
        if (options.getTimeout() > -1)
            mTransport.setTimeout(options.getTimeout());
        if (options.getRetryCount() != -1)
            mTransport.setRetryCount(options.getRetryCount());
        if (mAuthToken != null)
            mTransport.setAuthToken(mAuthToken);
        if (options.getResponseProtocol() != null)
            mTransport.setResponseProtocol(options.getResponseProtocol());
        if (options.getRequestProtocol() != null)
            mTransport.setRequestProtocol(options.getRequestProtocol());
    }

    public Element invoke(Element request) throws ServiceException {
		return invoke(request, null);
	}

	public synchronized Element invoke(Element request, String requestedAccountId) throws ServiceException {
        try {
			boolean nosession = mNotifyPreference == NotifyPreference.nosession;
			return mTransport.invoke(request, false, nosession, requestedAccountId);
        } catch (SoapFaultException e) {
            throw e; // for now, later, try to map to more specific exception
        } catch (Exception e) {
        	Throwable t = SystemUtil.getInnermostException(e);
        	RemoteServiceException.doConnectionFailures(mTransport.getURI(), t);
        	RemoteServiceException.doSSLFailures(t.getMessage(), t);
        	if (e instanceof IOException)
        		throw ZClientException.IO_ERROR(e.getMessage(), e);
        	throw ServiceException.FAILURE(e.getMessage(), e);
        } finally {
            Element context = mTransport.getZimbraContext();
            mTransport.clearZimbraContext();
            handleResponseContext(context);
        }
    }


    private void handleResponseContext(Element context) throws ServiceException {
        if (context == null) return;
        // handle refresh blocks
        Element refresh = context.getOptionalElement(ZimbraNamespace.E_REFRESH);
        if (refresh != null)
        	handleRefresh(refresh);

        for (Element notify : context.listElements(ZimbraNamespace.E_NOTIFY)) {
            mTransport.setMaxNotifySeq(
                    Math.max(mTransport.getMaxNotifySeq(),
                             notify.getAttributeLong(HeaderConstants.A_SEQNO, 0)));
            // MUST DO IN THIS ORDER!
            handleDeleted(notify.getOptionalElement(ZimbraNamespace.E_DELETED));
            handleCreated(notify.getOptionalElement(ZimbraNamespace.E_CREATED));
            handleModified(notify.getOptionalElement(ZimbraNamespace.E_MODIFIED));
        }
    }

    private void handleRefresh(Element refresh) throws ServiceException {
        for (Element mbx : refresh.listElements(MailConstants.E_MAILBOX)) {
            // FIXME: logic should be different if ZMailbox points at another user's mailbox
            if (mbx.getAttribute(HeaderConstants.A_ACCOUNT_ID, null) == null)
                mSize = mbx.getAttributeLong(MailConstants.A_SIZE);
        }

        Element tags = refresh.getOptionalElement(ZimbraNamespace.E_TAGS);
        List<ZTag> tagList = new ArrayList<ZTag>();
        if (tags != null) {
            for (Element t : tags.listElements(MailConstants.E_TAG)) {
            	ZTag tag = new ZTag(t, this);
            	tagList.add(tag);
            }
        }
        Element folderEl = refresh.getOptionalElement(MailConstants.E_FOLDER);
        ZFolder userRoot = new ZFolder(folderEl, null, this);
        ZRefreshEvent event = new ZRefreshEvent(mSize, userRoot, tagList);
        for (ZEventHandler handler : mHandlers)
        	handler.handleRefresh(event, this);
        mRules = null;
    }

    private void handleModified(Element modified) throws ServiceException {
        if (modified == null) return;
        for (Element e : modified.listElements()) {
        	ZModifyEvent event = null;
            if (e.getName().equals(MailConstants.E_CONV)) {
                event = new ZModifyConversationEvent(e);
            } else if (e.getName().equals(MailConstants.E_MSG)) {
                event = new ZModifyMessageEvent(e);
            } else if (e.getName().equals(MailConstants.E_TAG)) {
            	event = new ZModifyTagEvent(e);
            } else if (e.getName().equals(MailConstants.E_CONTACT)) {
            	event = new ZModifyContactEvent(e);
            } else if (e.getName().equals(MailConstants.E_SEARCH)) {
            	event = new ZModifySearchFolderEvent(e);
            } else if (e.getName().equals(MailConstants.E_FOLDER)) {
            	event = new ZModifyFolderEvent(e);
            } else if (e.getName().equals(MailConstants.E_MOUNT)) {
            	event = new ZModifyMountpointEvent(e);
            } else if (e.getName().equals(MailConstants.E_MAILBOX)) {
                event = new ZModifyMailboxEvent(e);
            } else if (e.getName().equals(MailConstants.E_APPOINTMENT)) {
                event = new ZModifyAppointmentEvent(e);
            } else if (e.getName().equals(MailConstants.E_TASK)) {
                event = new ZModifyTaskEvent(e);
            }
            if (event != null)
				handleEvent(event);
		}
    }

	private void handleEvent(ZModifyEvent event) throws ServiceException {
		for (ZEventHandler handler : mHandlers)
			handler.handleModify(event, this);
	}

	private List<ZFolder> parentCheck(List<ZFolder> list, ZFolder f, ZFolder parent) {
        if (parent != null) {
            parent.addChild(f);
        } else {
            if (list == null) list = new ArrayList<ZFolder>();
            list.add(f);
        }
        return list;
    }

    private void handleCreated(Element created) throws ServiceException {
        if (created == null) return;
        List<ZCreateEvent> events = null;
        List<ZFolder> parentFixup = null;
        for (Element e : created.listElements()) {
        	ZCreateEvent event = null;
            if (e.getName().equals(MailConstants.E_CONV)) {
                event = new ZCreateConversationEvent(e);
            } else if (e.getName().equals(MailConstants.E_MSG)) {
                event = new ZCreateMessageEvent(e);
            } else if (e.getName().equals(MailConstants.E_CONTACT)) {
                event = new ZCreateContactEvent(e);
            } else if (e.getName().equals(MailConstants.E_APPOINTMENT)) {
                event = new ZCreateAppointmentEvent(e);
            } else if (e.getName().equals(MailConstants.E_TASK)) {
                event = new ZCreateTaskEvent(e);
            } else if (e.getName().equals(MailConstants.E_FOLDER)) {
                String parentId = e.getAttribute(MailConstants.A_FOLDER);
                ZFolder parent = getFolderById(parentId);
                ZFolder child = new ZFolder(e, parent, this);
                addItemIdMapping(child);
                event = new ZCreateFolderEvent(child);
                parentFixup = parentCheck(parentFixup, child, parent);
            } else if (e.getName().equals(MailConstants.E_MOUNT)) {
                String parentId = e.getAttribute(MailConstants.A_FOLDER);
                ZFolder parent = getFolderById(parentId);
                ZMountpoint child = new ZMountpoint(e, parent, this);
             	addItemIdMapping(child);
            	addRemoteItemIdMapping(child.getCanonicalRemoteId(), child);
                parentFixup = parentCheck(parentFixup, child, parent);
                event = new ZCreateMountpointEvent(child);
            } else if (e.getName().equals(MailConstants.E_SEARCH)) {
                String parentId = e.getAttribute(MailConstants.A_FOLDER);
                ZFolder parent = getFolderById(parentId);
                ZSearchFolder child = new ZSearchFolder(e, parent, this);
                addItemIdMapping(child);
                event = new ZCreateSearchFolderEvent(child);
                parentFixup = parentCheck(parentFixup, child, parent);
            } else if (e.getName().equals(MailConstants.E_TAG)) {
                event = new ZCreateTagEvent(new ZTag(e, this));
                addTag(((ZCreateTagEvent)event).getTag());
            }
            if (event != null) {
                if (events == null) events = new ArrayList<ZCreateEvent>();
                events.add(event);
            }
        }

        if (parentFixup != null) {
            for (ZFolder f : parentFixup) {
                ZFolder parent = getFolderById(f.getParentId());
                if (parent != null) {
                    parent.addChild(f);
                    f.setParent(parent);
                }
            }
        }

        if (events != null) {
            for (ZCreateEvent event : events) {
                for (ZEventHandler handler : mHandlers)
                    handler.handleCreate(event, this);
            }
        }
    }

    private void handleDeleted(Element deleted) throws ServiceException {
        if (deleted == null) return;
        String ids = deleted.getAttribute(MailConstants.A_ID, null);
        if (ids == null) return;
        ZDeleteEvent de = new ZDeleteEvent(ids);
        for (ZEventHandler handler : mHandlers)
        	handler.handleDelete(de, this);
    }

    private void addIdMappings(ZFolder folder) {
        if (folder == null) return;
    	addItemIdMapping(folder);
    	if (folder instanceof ZMountpoint) {
        	ZMountpoint mp =  (ZMountpoint) folder;
        	addRemoteItemIdMapping(mp.getCanonicalRemoteId(), mp);
    	}
    	for (ZFolder child: folder.getSubFolders()) {
    		addIdMappings(child);
    	}
    }

    class InternalEventHandler extends ZEventHandler {
        public synchronized void handleRefresh(ZRefreshEvent event, ZMailbox mailbox) {
            ZFolder root = event.getUserRoot();
            List<ZTag> tags = event.getTags();

            mIdToItem.clear();
            mMessageCache.clear();
            mContactCache.clear();
            mTransport.setMaxNotifySeq(0);
            mSize = event.getSize();
            if (root != null) {
                mUserRoot = root;
                addIdMappings(mUserRoot);
            }
            if (tags != null) {
                if (mNameToTag == null)
                    mNameToTag = new HashMap<String, ZTag>();
                else
                    mNameToTag.clear();
                for (ZTag tag : tags)
                	addTag(tag);
            }
        }

        public synchronized void handleCreate(ZCreateEvent event, ZMailbox mailbox) {
            // do nothing
        }

        public synchronized void handleModify(ZModifyEvent event, ZMailbox mailbox) throws ServiceException {
            if (event instanceof ZModifyTagEvent) {
                ZModifyTagEvent tagEvent = (ZModifyTagEvent) event;
                ZTag tag = getTagById(tagEvent.getId());
                if (tag != null) {
                    String oldName = tag.getName();
                    tag.modifyNotification(tagEvent);
                    if (mNameToTag != null && !tag.getName().equalsIgnoreCase(oldName)) {
                        mNameToTag.remove(oldName);
                        mNameToTag.put(tag.getName(), tag);
                    }
                }
            } else if (event instanceof ZModifyFolderEvent) {
                ZModifyFolderEvent mfe = (ZModifyFolderEvent) event;
                ZFolder f = getFolderById(mfe.getId());
                if (f != null) {
                    String newParentId = mfe.getParentId(null);
                    if (newParentId != null && !newParentId.equals(f.getParentId()))
                        reparent(f, newParentId);
                    f.modifyNotification(event);
                }
            } else if (event instanceof ZModifyMailboxEvent) {
                ZModifyMailboxEvent mme = (ZModifyMailboxEvent) event;
                // FIXME: logic should be different if ZMailbox points at another user's mailbox
                if (mme.getOwner(null) == null)
                    mSize = mme.getSize(mSize);
            } else if (event instanceof ZModifyMessageEvent) {
                ZModifyMessageEvent mme = (ZModifyMessageEvent) event;
                CachedMessage cm = (CachedMessage) mMessageCache.get(mme.getId());
                if (cm != null)
                    cm.zm.modifyNotification(event);
            } else if (event instanceof ZModifyContactEvent) {
                ZModifyContactEvent mce = (ZModifyContactEvent) event;
                ZContact contact = (ZContact) mContactCache.get(mce.getId());
                if (contact != null)
                    contact.modifyNotification(mce);
            }
        }

        public synchronized void handleDelete(ZDeleteEvent event, ZMailbox mailbox) {
            for (String id : event.toList()) {
                mMessageCache.remove(id);
                mContactCache.remove(id);
                ZItem item = mIdToItem.get(id);
                if (item instanceof ZMountpoint) {
                    ZMountpoint sl = (ZMountpoint) item;
                    if (sl.getParent() != null)
                        sl.getParent().removeChild(sl);
                    mIdToItem.remove(sl.getCanonicalRemoteId());
                } else if (item instanceof ZFolder) {
                    ZFolder sf = (ZFolder) item;
                    if (sf.getParent() != null)
                        sf.getParent().removeChild(sf);

                } else if (item instanceof ZTag) {
                    if (mNameToTag != null)
                        mNameToTag.remove(((ZTag) item).getName());
                }
                if (item != null)
                    mIdToItem.remove(item.getId());
            }
        }
    }

    private void addTag(ZTag tag) {
        if (mNameToTag != null)
            mNameToTag.put(tag.getName(), tag);
        addItemIdMapping(tag);
    }

    void addItemIdMapping(ZItem item) {
        mIdToItem.put(item.getId(), item);
    }

    void addRemoteItemIdMapping(String remoteId, ZItem item) {
        mIdToItem.put(remoteId, item);
    }

    private void reparent(ZFolder f, String newParentId) throws ServiceException {
        ZFolder parent = f.getParent();
        if (parent != null)
            parent.removeChild(f);
        ZFolder newParent = getFolderById(newParentId);
        if (newParent != null) {
            newParent.addChild(f);
            f.setParent(newParent);
        }
    }

    /**
     * returns the parent folder path. First removes a trailing {@link #PATH_SEPARATOR} if one is present, then
     * returns the value of the path preceeding the last {@link #PATH_SEPARATOR} in the path.
     * @param path path must be absolute
     * @throws ServiceException if an error occurs
     * @return the parent folder path
     */
    public static String getParentPath(String path) throws ServiceException {
        if (path.equals(PATH_SEPARATOR)) return PATH_SEPARATOR;
        if (path.charAt(0) != PATH_SEPARATOR_CHAR)
            throw ServiceException.INVALID_REQUEST("path must be absolute: "+path, null);
        if (path.charAt(path.length()-1) == PATH_SEPARATOR_CHAR)
            path = path.substring(0, path.length()-1);
        int index = path.lastIndexOf(PATH_SEPARATOR_CHAR);
        path = path.substring(0, index);
        if (path.length() == 0) return PATH_SEPARATOR;
        else return path;
    }

    /**
     * returns the base folder path. First removes a trailing {@link #PATH_SEPARATOR} if one is present, then
     * returns the value of the path trailing the last {@link #PATH_SEPARATOR} in the path.
     * @throws ServiceException if an error occurs
     * @return base path
     * @param path the path we are getting the base from
     */
    public static String getBasePath(String path) throws ServiceException {
        if (path.equals(PATH_SEPARATOR)) return PATH_SEPARATOR;
        if (path.charAt(0) != PATH_SEPARATOR_CHAR)
            throw ServiceException.INVALID_REQUEST("path must be absolute: "+path, null);
        if (path.charAt(path.length()-1) == PATH_SEPARATOR_CHAR)
            path = path.substring(0, path.length()-1);
        int index = path.lastIndexOf(PATH_SEPARATOR_CHAR);
        return path.substring(index+1);
    }

    /**
     * @return current size of mailbox in bytes
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public long getSize() throws ServiceException {
        populateFolderCache();
        return mSize;
    }

    /**
     * @return account name of mailbox
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public String getName() throws ServiceException {
        return getAccountInfo(false).getName();
    }

    public ZPrefs getPrefs() throws ServiceException {
        return getPrefs(false);
    }


    public ZPrefs getPrefs(boolean refresh) throws ServiceException {
        return getAccountInfo(refresh).getPrefs();
    }

    public ZFeatures getFeatures() throws ServiceException {
        return getFeatures(false);
    }

    public ZFeatures getFeatures(boolean refresh) throws ServiceException {
        return getAccountInfo(refresh).getFeatures();
    }

    public ZGetInfoResult getAccountInfo(boolean refresh) throws ServiceException {
        if (mGetInfoResult == null || refresh) {
            Element req = newRequestElement(AccountConstants.GET_INFO_REQUEST);
            mGetInfoResult = new ZGetInfoResult(invoke(req));
        }
        return mGetInfoResult;
    }
    
    public int getTimeout() {
        return mTransport.getTimeout();
    }

    public Element getItemInfoJSON(String id) throws ServiceException {
        try {
            Element req = newRequestElement(MailConstants.GET_ITEM_REQUEST);
            Element item = req.addElement(MailConstants.E_ITEM);
            item.addAttribute(MailConstants.A_ID, id);
            return mTransport.invoke(req);
        } catch (IOException e) {
            throw ZClientException.IO_ERROR("invoke "+e.getMessage(), e);
        }
    }

    public static class JsonDebugListener implements DebugListener {
            Element env;
            public void sendSoapMessage(Element envelope) {}
            public void receiveSoapMessage(Element envelope) {env = envelope; }
            public Element getEnvelope(){ return env; }
    }

    /**
     * used when bootstrapping AJAX client.
     *
     * @param url url to connect to
     * @param authToken auth token to use
     * @param itemsPerPage number of search items to return
     * @param doSearch whether or not to also do the intial search
     * @param searchTypes what to search for
     * @return top-level JSON respsonse
     * @throws ServiceException on error
     */
    public static Element getBootstrapJSON(String url, String remoteAddr, ZAuthToken authToken, boolean doSearch, String itemsPerPage, String searchTypes) throws ServiceException {
        ZMailbox.Options options = new ZMailbox.Options(authToken, url);
        JsonDebugListener debug = new JsonDebugListener();
        options.setNoSession(false);
        options.setAuthAuthToken(false);
        options.setDebugListener(debug);
        options.setClientIp(remoteAddr);
        options.setResponseProtocol(SoapProtocol.SoapJS);

        ZMailbox mbox = getMailbox(options);
        try {
            Element batch = mbox.newRequestElement(ZimbraNamespace.E_BATCH_REQUEST);
            batch.addElement(AccountConstants.GET_INFO_REQUEST);
            if (doSearch) {
                Element search = batch.addElement(MailConstants.SEARCH_REQUEST);
                if (itemsPerPage != null && itemsPerPage.length() > 0)
                    search.addAttribute(MailConstants.A_QUERY_LIMIT, itemsPerPage);
                if (searchTypes != null && searchTypes.length() > 0)
                    search.addAttribute(MailConstants.A_SEARCH_TYPES, searchTypes);
            }
            mbox.mTransport.invoke(batch);
            return debug.getEnvelope();
        } catch (IOException e) {
            throw ZClientException.IO_ERROR("invoke "+e.getMessage(), e);
        }
    }

    /**
     * used when bootstrapping AJAX client.
     *
     * @param url url to connect to
     * @param authToken auth token to use
     * @param itemsPerPage number of search items to return
     * @param searchTypes what to search for
     * @return top-level JSON respsonse
     * @throws ServiceException on error
     */
    public static Element getBootstrapCalSearchJSON(String url, String remoteAddr, ZAuthToken authToken, String itemsPerPage, String searchTypes) throws ServiceException {
        ZMailbox.Options options = new ZMailbox.Options(authToken, url);
        JsonDebugListener debug = new JsonDebugListener();
        options.setNoSession(false);
        options.setAuthAuthToken(false);
        options.setDebugListener(debug);
        options.setClientIp(remoteAddr);
        options.setResponseProtocol(SoapProtocol.SoapJS);

        ZMailbox mbox = getMailbox(options);
        try {
            TimeZone tz = mbox.getPrefs().getTimeZone();

            Calendar currentDay = tz == null ? Calendar.getInstance() : Calendar.getInstance(tz);
            currentDay.setTimeInMillis(System.currentTimeMillis());
            currentDay.set(Calendar.HOUR_OF_DAY, 0);
            currentDay.set(Calendar.MINUTE, 0);
            currentDay.set(Calendar.SECOND, 0);
            currentDay.set(Calendar.MILLISECOND, 0);
            currentDay.set(Calendar.DAY_OF_MONTH, 1);
            
            StringBuilder sb = new StringBuilder();
            getCheckedCalendarFoldersRecursive(mbox.getUserRoot(), sb);
            String checkedCalendars = sb.toString();

            Calendar other = Calendar.getInstance(currentDay.getTimeZone());
            other.setTimeInMillis(currentDay.getTimeInMillis());
            //7 days for reminder search and 1 day for timezone difference
            other.add(Calendar.DAY_OF_MONTH, -8);

            long calStart = other.getTimeInMillis();

            other.setTimeInMillis(currentDay.getTimeInMillis());
            //no of days shown for minical
            other.add(Calendar.DAY_OF_MONTH, 42);

            long calEnd = other.getTimeInMillis();

            //BatchRequest
            Element batch = new Element.JSONElement(ZimbraNamespace.E_BATCH_REQUEST);

            //GetMiniCalRequest
            Element miniCalRequest = batch.addElement(MailConstants.GET_MINI_CAL_REQUEST);
            miniCalRequest.addAttribute(MailConstants.A_CAL_START_TIME, calStart);
            miniCalRequest.addAttribute(MailConstants.A_CAL_END_TIME, calEnd);

            String [] sArray = null;
            StringBuilder searchQuery = new StringBuilder();

            if (checkedCalendars!=null) {
                if(checkedCalendars.indexOf(",") == -1){
                    sArray = new String[]{checkedCalendars};
                }else{
                    sArray = checkedCalendars.split(",");
                }
                for(int i=0; i<sArray.length; i++) {
                    Element folder = miniCalRequest.addElement(MailConstants.E_FOLDER);
                    folder.addAttribute(MailConstants.A_ID, sArray[i]);
                    if (searchQuery.length() > 1) searchQuery.append(" or ");
                    searchQuery.append("inid:").append("\""+sArray[i]+"\"");
                }
            }

            //SearchRequest
            Element search = batch.addElement(MailConstants.SEARCH_REQUEST);
            if (itemsPerPage != null && itemsPerPage.length() > 0)
                search.addAttribute(MailConstants.A_QUERY_LIMIT, itemsPerPage);
            if (searchTypes != null && searchTypes.length() > 0)
                search.addAttribute(MailConstants.A_SEARCH_TYPES, searchTypes);
            search.addAttribute(MailConstants.A_CAL_EXPAND_INST_START, calStart);
            search.addAttribute(MailConstants.A_CAL_EXPAND_INST_END, calEnd);
            search.addAttribute(MailConstants.A_QUERY_OFFSET, 0);

            Element queryEl = search.addElement(MailConstants.E_QUERY);
            queryEl.setText(searchQuery.toString());

            mbox.mTransport.invoke(batch);

            Element e = debug.getEnvelope();

            //search params included in response
            Element responseParams = e.addElement(MailConstants.E_SEARCH);
            responseParams.addAttribute(MailConstants.A_CAL_START_TIME, calStart);
            responseParams.addAttribute(MailConstants.A_CAL_END_TIME, calEnd);
            Element queryElement = responseParams.addElement(MailConstants.A_QUERY);
            queryElement.setText(searchQuery.toString());
            responseParams.addAttribute(MailConstants.A_FOLDER, checkedCalendars);
            
            return e;

        } catch (IOException e) {
            throw ZClientException.IO_ERROR("invoke "+e.getMessage(), e);
        }
    }

    private static void getCheckedCalendarFoldersRecursive(ZFolder f, StringBuilder sb) {
        if (f.getDefaultView() == ZFolder.View.appointment && f.isCheckedInUI()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(f.getId());
        }
        for (ZFolder child : f.getSubFolders()) {
            getCheckedCalendarFoldersRecursive(child, sb);
        }
    }

    public String maskRemoteItemId(String folderId, String id) throws ServiceException {
        int folderIndex = folderId.indexOf(':');
        int idIndex = id.indexOf(':');
        if (folderIndex != -1 && idIndex != -1) {
            ZFolder f = getFolderById(folderId);
            if (f != null) {
                String folderPrefix = folderId.substring(0, folderIndex);
                String idPrefix = id.substring(0, idIndex);
                if (folderPrefix.equalsIgnoreCase(idPrefix)) {
                    return f.getId() + ":" +  id.substring(idIndex+1);
                }
            }
        }
        return id;
    }

    public String unmaskRemoteItemId(String id) throws ServiceException {
        int idIndex = id.indexOf(':');
        if (idIndex != -1) {
            String idPrefix = id.substring(0, idIndex);
            ZMountpoint mp = getMountpointById(idPrefix);
            if (mp != null) {
                return mp.getOwnerId() + ":" +  id.substring(idIndex+1);
            }
        }
        return id;
    }
    
    //  ------------------------

    /**
     * @return current List of all tags in the mailbox
     * @throws com.zimbra.common.service.ServiceException on error
     */
    @SuppressWarnings("unchecked")
    public List<ZTag> getAllTags() throws ServiceException {
        populateTagCache();
        List result = new ArrayList<ZTag>(mNameToTag.values());
        Collections.sort(result);
        return result;
    }

    /**
     *
     * @return true if mailbox has any tags
     * @throws ServiceException
     */
    public boolean hasTags() throws ServiceException {
        populateTagCache();
        return !mNameToTag.isEmpty();
    }

    /**
     * @return current list of all tags names in the mailbox, sorted
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public List<String> getAllTagNames() throws ServiceException {
        populateTagCache();
        ArrayList<String> names = new ArrayList<String>(mNameToTag.keySet());
        Collections.sort(names);
        return names;
    }

    /**
     * returns the tag the specified name/id, or null if no such tag exists.
     * checks for tag by name first, then by id.
     *
     * @param name tag name
     * @return the tag, or null if tag not found
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public ZTag getTag(String nameOrId) throws ServiceException {
        ZTag result = getTagByName(nameOrId);
        return result != null ? result : getTagById(nameOrId);
    }

    /**
     * returns the tag the specified name, or null if no such tag exists.
     *
     * @param name tag name
     * @return the tag, or null if tag not found
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public ZTag getTagByName(String name) throws ServiceException {
        populateTagCache();
        return mNameToTag.get(name);
    }

    /**
     * returns the tag with the specified id, or null if no such tag exists.
     *
     * @param id the tag id
     * @return tag with given id, or null
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public ZTag getTagById(String id) throws ServiceException {
        populateTagCache();
        ZItem item = mIdToItem.get(id);
        if (item instanceof ZTag)
            return (ZTag) item;
        else
            return null;
    }

    private static final Pattern sCOMMA = Pattern.compile(",");

    /**
     * returns the tags for the specified ids.  Ignores id's that don't
     * reference existing tags.
     *
     * @param ids the tag ids
     * @return the tag list, or an empty list if no ids match
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public List<ZTag> getTags(String ids) throws ServiceException {
        List<ZTag> tags = new ArrayList<ZTag>();
        if (!StringUtil.isNullOrEmpty(ids)) {
            for (String id : sCOMMA.split(ids)) {
                ZTag tag = getTagById(id);
                if (tag != null)
                    tags.add(tag);
            }
        }
        return tags;
    }

    /**
     * create a new tag with the specified color.
     *
     * @return newly created tag
     * @param name name of the tag
     * @param color optional color of the tag
     * @throws com.zimbra.common.service.ServiceException if an error occurs
     *
     */
    public ZTag createTag(String name, ZTag.Color color) throws ServiceException {
        Element req = newRequestElement(MailConstants.CREATE_TAG_REQUEST);
        Element tagEl = req.addUniqueElement(MailConstants.E_TAG);
        tagEl.addAttribute(MailConstants.A_NAME, name);
        if (color != null) tagEl.addAttribute(MailConstants.A_COLOR, color.getValue());
        Element createdTagEl = invoke(req).getElement(MailConstants.E_TAG);
        ZTag tag = getTagById(createdTagEl.getAttribute(MailConstants.A_ID));
        return tag != null ? tag : new ZTag(createdTagEl, this);
    }

    /**
     * update a tag
     * @return action result
     * @param id id of tag to update
     * @param name new name of tag
     * @param color color of tag to modify
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public ZActionResult updateTag(String id, String name, ZTag.Color color) throws ServiceException {
        Element action = tagAction("update", id);
        if (color != null)
            action.addAttribute(MailConstants.A_COLOR, color.getValue());
        if (name != null && name.length() > 0)
            action.addAttribute(MailConstants.A_NAME, name);
        return doAction(action);
    }

    /**
     * modifies the tag's color
     * @return action result
     * @param id id of tag to modify
     * @param color color of tag to modify
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public ZActionResult modifyTagColor(String id, ZTag.Color color) throws ServiceException {
        return doAction(tagAction("color", id).addAttribute(MailConstants.A_COLOR, color.getValue()));
    }

    /** mark all items with tag as read
     * @param id id of tag to mark read
     * @return action reslult
     * @throws ServiceException on error
     */
    public ZActionResult markTagRead(String id) throws ServiceException {
        return doAction(tagAction("read", id));
    }

    /**
     * delete tag
     * @param id id of tag to delete
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult deleteTag(String id) throws ServiceException {
        return doAction(tagAction("delete", id));
    }

    /**
     * rename tag
     * @param id id of tag
     * @param name new name of tag
     * @throws ServiceException on error
     * @return action result
     */
    public ZActionResult renameTag(String id, String name) throws ServiceException {
        return doAction(tagAction("rename", id).addAttribute(MailConstants.A_NAME, name));
    }

    private Element tagAction(String op, String id) {
        Element req = newRequestElement(MailConstants.TAG_ACTION_REQUEST);
        Element actionEl = req.addUniqueElement(MailConstants.E_ACTION);
        actionEl.addAttribute(MailConstants.A_ID, id);
        actionEl.addAttribute(MailConstants.A_OPERATION, op);
        return actionEl;
    }

    private ZActionResult doAction(Element actionEl) throws ServiceException {
        Element response = invoke(actionEl.getParent());
        return new ZActionResult(response);
    }

    // ------------------------

    public enum ContactSortBy {

        nameDesc, nameAsc;

         public static ContactSortBy fromString(String s) throws ServiceException {
             try {
                 return ContactSortBy.valueOf(s);
             } catch (IllegalArgumentException e) {
                 throw ZClientException.CLIENT_ERROR("invalid sortBy: "+s+", valid values: "+Arrays.asList(ContactSortBy.values()), e);
             }
         }
    }

    /**
     *
     * @param optFolderId return contacts only in specified folder (null for all folders)
     * @param sortBy sort results (null for no sorting)
     * @param sync if true, return modified date on contacts
     * @return list of contacts
     * @throws ServiceException on error
     * @param attrs specified attrs to return, or null for all.
     */
    public List<ZContact> getAllContacts(String optFolderId, ContactSortBy sortBy, boolean sync, List<String> attrs) throws ServiceException {
        Element req = newRequestElement(MailConstants.GET_CONTACTS_REQUEST);
        if (optFolderId != null)
            req.addAttribute(MailConstants.A_FOLDER, optFolderId);
        if (sortBy != null)
            req.addAttribute(MailConstants.A_SORTBY, sortBy.name());
        if (sync)
            req.addAttribute(MailConstants.A_SYNC, sync);

        if (attrs != null) {
            for (String name : attrs)
                req.addElement(MailConstants.E_ATTRIBUTE).addAttribute(MailConstants.A_ATTRIBUTE_NAME, name);
        }

        Element response = invoke(req);
        List<ZContact> result = new ArrayList<ZContact>();
        for (Element cn : response.listElements(MailConstants.E_CONTACT)) {
            result.add(new ZContact(cn, this));
        }
        return result;
    }

    /**
     * Specifies properties for an attachment when creating or modifying
     * a contact.
     */
    public static class ZAttachmentInfo {
        private String mAttachmentId;
        private String mPartName;
        private String mItemId;
        
        public ZAttachmentInfo setAttachmentId(String attachmentId) {
            mAttachmentId = attachmentId;
            return this;
        }
        
        public ZAttachmentInfo setPartName(String partName) {
            mPartName = partName;
            return this;
        }
        
        public ZAttachmentInfo setItemId(String itemId) {
            mItemId = itemId;
            return this;
        }
        
        public String getAttachmentId() { return mAttachmentId; }
        public String getPartName() { return mPartName; }
        public String getItemId() { return mItemId; }
        
        public String toString() {
            return String.format("%s: {attachmentId=%s, partName=%s, itemId=%s}",
                ZAttachmentInfo.class.getSimpleName(), mAttachmentId, mPartName, mItemId);
        }
    }
    
    /**
     * Creates a new contact.
     * @param folderId the new contact's folder id
     * @param tags tags to set on the contact, or <tt>null</tt>
     * @param attrs contact attributes (key/value)
     * @return the new contact
     * @throws ServiceException
     */
    public ZContact createContact(String folderId, String tags, Map<String, String> attrs) throws ServiceException {
        return createContact(folderId, tags, attrs, null);
    }

    /**
     * Creates a new contact.
     * @param folderId the new contact's folder id
     * @param tags tags to set on the contact, or <tt>null</tt>
     * @param attrs contact attributes (key/value)
     * @param attachments contact attachments (key/upload id) or <tt>null</tt>
     * @param verbose <tt>false</tt> to only initialize the <tt>id</tt> of the return <tt>ZContact</tt> object
     * @return the new contact
     * @throws ServiceException
     */
    public ZContact createContact(String folderId, String tags, Map<String, String> attrs, Map<String, ZAttachmentInfo> attachments)
    throws ServiceException {
        Element req = newRequestElement(MailConstants.CREATE_CONTACT_REQUEST);
        Element cn = req.addUniqueElement(MailConstants.E_CONTACT);
        if (folderId != null)
            cn.addAttribute(MailConstants.A_FOLDER, folderId);
        if (tags != null)
            cn.addAttribute(MailConstants.A_TAGS, tags);
        addAttrsAndAttachments(cn, attrs, attachments);
        return new ZContact(invoke(req).getElement(MailConstants.E_CONTACT), this);
    }
    
    private void addAttrsAndAttachments(Element cn, Map<String, String> attrs, Map<String, ZAttachmentInfo> attachments)
    throws ServiceException {
        if (attrs != null) {
            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                cn.addKeyValuePair(entry.getKey(), entry.getValue().trim(), MailConstants.E_ATTRIBUTE,  MailConstants.A_ATTRIBUTE_NAME);
            }
        }
        if (attachments != null) {
            for (String name : attachments.keySet()) {
                ZAttachmentInfo info = attachments.get(name);
                
                Element attachEl =  cn.addElement(MailConstants.E_ATTRIBUTE);
                attachEl.addAttribute(MailConstants.A_ATTRIBUTE_NAME, name);
                if (info.getAttachmentId() != null) {
                    attachEl.addAttribute(MailConstants.A_ATTACHMENT_ID, info.getAttachmentId());
                } else if (info.getItemId() != null) {
                    attachEl.addAttribute(MailConstants.A_ID, info.getItemId());
                    attachEl.addAttribute(MailConstants.A_PART, info.getPartName());
                } else if (info.getPartName() != null) {
                    attachEl.addAttribute(MailConstants.A_PART, info.getPartName());
                }
            }
        }
    }
    
    /**
     * @param id of contact
     * @param replace if true, replace all attrs with specified attrs, otherwise merge with existing
     * @param attrs modified attrs
     * @return updated contact
     * @throws ServiceException on error
     */
    public ZContact modifyContact(String id, boolean replace, Map<String, String> attrs) throws ServiceException {
        return modifyContact(id, replace, attrs, null);
    }
    
    /**
     * @param id of contact
     * @param replace if true, replace all attrs with specified attrs, otherwise merge with existing
     * @param attrs modified attrs, or <tt>null</tt>
     * @param attachments modified attachments , or <tt>null</tt>
     * @return updated contact
     * @throws ServiceException on error
     */
    public ZContact modifyContact(String id, boolean replace, Map<String, String> attrs, Map<String, ZAttachmentInfo> attachments)
    throws ServiceException {
        Element req = newRequestElement(MailConstants.MODIFY_CONTACT_REQUEST);
        if (replace)
            req.addAttribute(MailConstants.A_REPLACE, replace);
        Element cn = req.addUniqueElement(MailConstants.E_CONTACT);
        cn.addAttribute(MailConstants.A_ID, id);
        addAttrsAndAttachments(cn, attrs, attachments);
        return new ZContact(invoke(req).getElement(MailConstants.E_CONTACT), this);
    }

    /**
     *
     * @param ids comma-separated list of contact ids
     * @param attrs limit attrs returns to given list
     * @param sortBy sort results (null for no sorting)
     * @param sync if true, return modified date on contacts
     * @return list of contacts
     * @throws ServiceException on error
     */
    public List<ZContact> getContacts(String ids, ContactSortBy sortBy, boolean sync, List<String> attrs) throws ServiceException {
        Element req = newRequestElement(MailConstants.GET_CONTACTS_REQUEST);

        if (sortBy != null)
            req.addAttribute(MailConstants.A_SORTBY, sortBy.name());
        if (sync)
            req.addAttribute(MailConstants.A_SYNC, sync);
        if (ids != null)
            req.addElement(MailConstants.E_CONTACT).addAttribute(MailConstants.A_ID, ids);
        if (attrs != null) {
            for (String name : attrs)
                req.addElement(MailConstants.E_ATTRIBUTE).addAttribute(MailConstants.A_ATTRIBUTE_NAME, name);
        }
        List<ZContact> result = new ArrayList<ZContact>();
        for (Element cn : invoke(req).listElements(MailConstants.E_CONTACT)) {
            result.add(new ZContact(cn, this));
        }
        return result;
    }

    /**
     *
     * @param id single contact id to fetch
     * @return fetched contact
     * @throws ServiceException on error
     */
    public synchronized ZContact getContact(String id) throws ServiceException {
        ZContact result = (ZContact) mContactCache.get(id);
        if (result == null) {
            Element req = newRequestElement(MailConstants.GET_CONTACTS_REQUEST);
            req.addAttribute(MailConstants.A_SYNC, true);
            req.addElement(MailConstants.E_CONTACT).addAttribute(MailConstants.A_ID, id);
            result = new ZContact(invoke(req).getElement(MailConstants.E_CONTACT), this);
            mContactCache.put(id, result);
        }
        return result;
    }

    public synchronized List<ZAutoCompleteMatch> autoComplete(String query, int limit) throws ServiceException {
    	Element req = newRequestElement(MailConstants.AUTO_COMPLETE_REQUEST);
    	req.addAttribute(MailConstants.A_LIMIT, limit);
    	req.addAttribute(MailConstants.A_INCLUDE_GAL, getFeatures().getGalAutoComplete());
    	req.addUniqueElement(MailConstants.E_NAME).setText(query);
        Element response = invoke(req);
        List<ZAutoCompleteMatch> matches = new ArrayList<ZAutoCompleteMatch>();
        for (Element match : response.listElements(MailConstants.E_MATCH))
        	matches.add(new ZAutoCompleteMatch(match, this));
    	return matches;
    }

    private Element contactAction(String op, String id) {
        Element req = newRequestElement(MailConstants.CONTACT_ACTION_REQUEST);
        Element actionEl = req.addUniqueElement(MailConstants.E_ACTION);
        actionEl.addAttribute(MailConstants.A_ID, id);
        actionEl.addAttribute(MailConstants.A_OPERATION, op);
        return actionEl;
    }

    public ZActionResult moveContact(String ids, String destFolderId) throws ServiceException {
        return doAction(contactAction("move", ids).addAttribute(MailConstants.A_FOLDER, destFolderId));
    }

    public ZActionResult deleteContact(String ids) throws ServiceException {
        return doAction(contactAction("delete", ids));
    }

    public ZActionResult trashContact(String ids) throws ServiceException {
        return doAction(contactAction("trash", ids));
    }

    public ZActionResult flagContact(String ids, boolean flag) throws ServiceException {
        return doAction(contactAction(flag ? "flag" : "!flag", ids));
    }

    public ZActionResult tagContact(String ids, String tagId, boolean tag) throws ServiceException {
        return doAction(contactAction(tag ? "tag" : "!tag", ids).addAttribute(MailConstants.A_TAG, tagId));
    }

	// Needs to be obsoleted.
	public synchronized ZContact getMyCard() throws ServiceException {
		return null;
	}

	// Needs to be obsoleted.
	public boolean getIsMyCard(String ids) throws ServiceException {
		return false;
	}
	
	/**
     * update items(s)
     * @param ids list of contact ids to update
     * @param destFolderId optional destination folder
     * @param tagList optional new list of tag ids
     * @param flags optional new value for flags
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult updateContact(String ids, String destFolderId, String tagList, String flags) throws ServiceException {
        Element actionEl = contactAction("update", ids);
        if (destFolderId != null && destFolderId.length() > 0) actionEl.addAttribute(MailConstants.A_FOLDER, destFolderId);
        if (tagList != null) actionEl.addAttribute(MailConstants.A_TAGS, tagList);
        if (flags != null) actionEl.addAttribute(MailConstants.A_FLAGS, flags);
        return doAction(actionEl);
    }

    public static class ZImportContactsResult {

        private String mIds;
        private long mCount;

        public ZImportContactsResult(Element response) throws ServiceException {
            mIds = response.getAttribute(MailConstants.A_ID, null);
            mCount = response.getAttributeLong(MailConstants.A_NUM);
        }

        public String getIds() {
            return mIds;
        }

        public long getCount() {
            return mCount;
        }
    }

    public static final String CONTACT_IMPORT_TYPE_CSV = "csv";

    public ZImportContactsResult importContacts(String folderId, String type, String attachmentId) throws ServiceException {
    	Element req = newRequestElement(MailConstants.IMPORT_CONTACTS_REQUEST);
    	req.addAttribute(MailConstants.A_CONTENT_TYPE, type);
    	req.addAttribute(MailConstants.A_FOLDER, folderId);
    	Element content = req.addUniqueElement(MailConstants.E_CONTENT);
    	content.addAttribute(MailConstants.A_ATTACHMENT_ID, attachmentId);
    	return new ZImportContactsResult(invoke(req).getElement(MailConstants.E_CONTACT));
    }


    /**
     *
     * @param id conversation id
     * @param fetch Whether or not fetch none/first/all messages in conv.
     * @return conversation
     * @throws ServiceException on error
     */
    public ZConversation getConversation(String id, Fetch fetch) throws ServiceException {
        Element req = newRequestElement(MailConstants.GET_CONV_REQUEST);
        Element convEl = req.addUniqueElement(MailConstants.E_CONV);
        convEl.addAttribute(MailConstants.A_ID, id);
        if (fetch != null && fetch != Fetch.none && fetch != Fetch.hits) {
            // use "1" for "first" for backward compat until DF is updated
            convEl.addAttribute(MailConstants.A_FETCH, fetch == Fetch.first ? "1" : fetch.name());
        }
        return new ZConversation(invoke(req).getElement(MailConstants.E_CONV), this);
    }

    /** include items in the Trash folder */
    public static final String TC_INCLUDE_TRASH = "t";

    /** include items in the Spam/Junk folder */
    public static final String TC_INCLUDE_JUNK = "j";

    /** include items in the Sent folder */
    public static final String TC_INCLUDE_SENT = "s";

    /** include items in any other folder */
    public static final String TC_INCLUDE_OTHER = "o";

    private Element convAction(String op, String id, String constraints) {
        Element req = newRequestElement(MailConstants.CONV_ACTION_REQUEST);
        Element actionEl = req.addUniqueElement(MailConstants.E_ACTION);
        actionEl.addAttribute(MailConstants.A_ID, id);
        actionEl.addAttribute(MailConstants.A_OPERATION, op);
        if (constraints != null) actionEl.addAttribute(MailConstants.A_TARGET_CONSTRAINT, constraints);
        return actionEl;
    }

    /**
     * hard delete conversation(s).
     *
     * @param ids list of conversation ids to act on
     * @param targetConstraints list of characters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items in a conversation. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult deleteConversation(String ids, String targetConstraints) throws ServiceException {
        return doAction(convAction("delete", ids, targetConstraints));
    }

    /**
     * moves conversation to trash folder.
     *
     * @param ids list of conversation ids to act on
     * @param targetConstraints list of characters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items in a conversation. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult trashConversation(String ids, String targetConstraints) throws ServiceException {
        return doAction(convAction("trash", ids, targetConstraints));
    }

    /**
     * mark conversation as read/unread
     *
     * @param ids list of conversation ids to act on
     * @param read mark read (TRUE) or unread (FALSE)
     * @param targetConstraints list of characters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items in a conversation. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult markConversationRead(String ids, boolean read, String targetConstraints) throws ServiceException {
        return doAction(convAction(read ? "read" : "!read", ids, targetConstraints));
    }

    /**
     * flag/unflag conversations
     *
     * @param ids list of conversation ids to act on
     * @param flag flag (TRUE) or unflag (FALSE)
     * @param targetConstraints list of characters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items in a conversation. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult flagConversation(String ids, boolean flag, String targetConstraints) throws ServiceException {
        return doAction(convAction(flag ? "flag" : "!flag", ids, targetConstraints));
    }

    /**
     * tag/untag conversations
     *
     * @param ids list of conversation ids to act on
     * @param tagId id of tag to tag/untag with
     * @param tag tag (TRUE) or untag (FALSE)
     * @param targetConstraints list of characters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items in a conversation. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult tagConversation(String ids, String tagId, boolean tag, String targetConstraints) throws ServiceException {
        return doAction(convAction(tag ? "tag" : "!tag", ids, targetConstraints).addAttribute(MailConstants.A_TAG, tagId));
    }

    /**
     * move conversations
     *
     * @param ids list of conversation ids to act on
     * @param destFolderId id of destination folder
     * @param targetConstraints list of characters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items in a conversation. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult moveConversation(String ids, String destFolderId, String targetConstraints) throws ServiceException {
        return doAction(convAction("move", ids, targetConstraints).addAttribute(MailConstants.A_FOLDER, destFolderId));
    }

    /**
     * spam/unspam a single conversation
     *
     * @param id conversation id to act on
     * @param spam spam (TRUE) or not spam (FALSE)
     * @param destFolderId optional id of destination folder, only used with "not spam".
     * @param targetConstraints list of characters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items in a conversation. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult markConversationSpam(String id, boolean spam, String destFolderId, String targetConstraints) throws ServiceException {
        Element actionEl = convAction(spam ? "spam" : "!spam", id, targetConstraints);
        if (destFolderId != null && destFolderId.length() > 0) actionEl.addAttribute(MailConstants.A_FOLDER, destFolderId);
        return doAction(actionEl);
    }

    private Element messageAction(String op, String id) {
        Element req = newRequestElement(MailConstants.MSG_ACTION_REQUEST);
        Element actionEl = req.addUniqueElement(MailConstants.E_ACTION);
        actionEl.addAttribute(MailConstants.A_ID, id);
        actionEl.addAttribute(MailConstants.A_OPERATION, op);
        return actionEl;
    }

    // ------------------------

    private Element itemAction(String op, String id, String constraints) {
        Element req = newRequestElement(MailConstants.ITEM_ACTION_REQUEST);
        Element actionEl = req.addUniqueElement(MailConstants.E_ACTION);
        actionEl.addAttribute(MailConstants.A_ID, id);
        actionEl.addAttribute(MailConstants.A_OPERATION, op);
        if (constraints != null) actionEl.addAttribute(MailConstants.A_TARGET_CONSTRAINT, constraints);
        return actionEl;
    }

    /**
     * hard delete item(s).
     *
     * @param ids list of item ids to act on
     * @param targetConstraints list of characters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult deleteItem(String ids, String targetConstraints) throws ServiceException {
        return doAction(itemAction("delete", ids, targetConstraints));
    }

    /**
     * move item(s) to trash
     *
     * @param ids list of item ids to act on
     * @param targetConstraints list of characters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult trashItem(String ids, String targetConstraints) throws ServiceException {
        return doAction(itemAction("trash", ids, targetConstraints));
    }


    /**
     * mark item as read/unread
     *
     * @param ids list of ids to act on
     * @param read mark read (TRUE) or unread (FALSE)
     * @param targetConstraints list of characters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult markItemRead(String ids, boolean read, String targetConstraints) throws ServiceException {
        return doAction(itemAction(read ? "read" : "!read", ids, targetConstraints));
    }

    /**
     * flag/unflag items
     *
     * @param ids list of ids to act on
     * @param flag flag (TRUE) or unflag (FALSE)
     * @param targetConstraints list of characters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult flagItem(String ids, boolean flag, String targetConstraints) throws ServiceException {
        return doAction(itemAction(flag ? "flag" : "!flag", ids, targetConstraints));
    }

    /**
     * tag/untag items
     *
     * @param ids list of ids to act on
     * @param tagId id of tag to tag/untag with
     * @param tag tag (TRUE) or untag (FALSE)
     * @param targetConstraints list of characters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult tagItem(String ids, String tagId, boolean tag, String targetConstraints) throws ServiceException {
        return doAction(itemAction(tag ? "tag" : "!tag", ids, targetConstraints).addAttribute(MailConstants.A_TAG, tagId));
    }

    /**
     * move conversations
     *
     * @param ids list of item ids to act on
     * @param destFolderId id of destination folder
     * @param targetConstraints list of characters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult moveItem(String ids, String destFolderId, String targetConstraints) throws ServiceException {
        return doAction(itemAction("move", ids, targetConstraints).addAttribute(MailConstants.A_FOLDER, destFolderId));
    }

    /**
     * update items(s)
     * @param ids list of items to act on
     * @param destFolderId optional destination folder
     * @param tagList optional new list of tag ids
     * @param flags optional new value for flags
     * @param targetConstraints list of characters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult updateItem(String ids, String destFolderId, String tagList, String flags, String targetConstraints) throws ServiceException {
        Element actionEl = itemAction("update", ids, targetConstraints);
        if (destFolderId != null && destFolderId.length() > 0) actionEl.addAttribute(MailConstants.A_FOLDER, destFolderId);
        if (tagList != null) actionEl.addAttribute(MailConstants.A_TAGS, tagList);
        if (flags != null) actionEl.addAttribute(MailConstants.A_FLAGS, flags);
        return doAction(actionEl);
    }

    /* ------------------------------------------------- */

    /**
     * Uploads files to <tt>FileUploadServlet</tt>.
     * @return the attachment id
     */
    public String uploadAttachments(File[] files, int msTimeout) throws ServiceException {
        Part[] parts = new Part[files.length];
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            String contentType = URLConnection.getFileNameMap().getContentTypeFor(file.getName());
            try {
                parts[i] = new FilePart(file.getName(), file, contentType, "UTF-8");
            } catch (IOException e) {
                throw ZClientException.IO_ERROR(e.getMessage(), e);
            }
        }

        return uploadAttachments(parts, msTimeout);
    }

    /**
     * Uploads a byte array to <tt>FileUploadServlet</tt>.
     * @return the attachment id
     */
    public String uploadAttachment(String name, byte[] content, String contentType, int msTimeout) throws ServiceException {
        FilePart part = new FilePart(name, new ByteArrayPartSource(name, content));
        part.setContentType(contentType);

        return uploadAttachments(new Part[] { part }, msTimeout);
    }

    /**
     * Uploads multiple byte arrays to <tt>FileUploadServlet</tt>.
     * @param attachments the attachments.  The key to the <tt>Map</tt> is the attachment
     * name and the value is the content.
     * @return the attachment id
     */
    public String uploadAttachments(Map<String, byte[]> attachments, int msTimeout) throws ServiceException {
        if (attachments == null || attachments.size() == 0) {
            return null;
        }
        Part[] parts = new Part[attachments.size()];
        int i = 0;
        for (String name : attachments.keySet()) {
            byte[] content = attachments.get(name);
            parts[i++] = createAttachmentPart(name, content);
        }

        return uploadAttachments(parts, msTimeout);
    }
    
    /**
     * Creates an <tt>HttpClient FilePart</tt> from the given filename and content.
     */
    public FilePart createAttachmentPart(String filename, byte[] content) {
        FilePart part = new FilePart(filename, new ByteArrayPartSource(filename, content));
        String contentType = URLConnection.getFileNameMap().getContentTypeFor(filename);
        part.setContentType(contentType);
        return part;
    }

    /**
     * Uploads HTTP post parts to <tt>FileUploadServlet</tt>.
     * @return the attachment id
     */
    public String uploadAttachments(Part[] parts, int msTimeout) throws ServiceException {
        String aid = null;

        URI uri = getUploadURI();
        HttpClient client = getHttpClient(uri);
        
        // make the post
        PostMethod post = new PostMethod(uri.toString());
        post.getParams().setSoTimeout(msTimeout);

        int statusCode;
        try {
            post.setRequestEntity( new MultipartRequestEntity(parts, post.getParams()) );
            statusCode = HttpClientUtil.executeMethod(client, post);

            // parse the response
            if (statusCode == 200) {
                String response = post.getResponseBodyAsString();
                aid = getAttachmentId(response);
            } else {
                throw ZClientException.UPLOAD_FAILED("Attachment post failed, status=" + statusCode, null);
            }
        } catch (IOException e) {
            throw ZClientException.IO_ERROR(e.getMessage(), e);
        } finally {
            post.releaseConnection();
        }
        return aid;
    }

    public String uploadContentAsStream(String name, InputStream in, String contentType, long contentLength, int msTimeout)
    throws ServiceException {
        String aid = null;
        if (name != null) {
            contentType += "; name=" + name;
        }

        URI uri = getUploadURI();
        HttpClient client = getHttpClient(uri);

        // make the post
        PostMethod post = new PostMethod(uri.toString());
        post.getParams().setSoTimeout(msTimeout);

        int statusCode;
        try {
            post.setRequestEntity(new InputStreamRequestEntity(in, contentLength, contentType));
            statusCode = HttpClientUtil.executeMethod(client, post);

            // parse the response
            if (statusCode == 200) {
                String response = post.getResponseBodyAsString();
                aid = getAttachmentId(response);
            } else {
                throw ZClientException.UPLOAD_FAILED("Attachment post failed, status=" + statusCode, null);
            }
        } catch (IOException e) {
            throw ZClientException.IO_ERROR(e.getMessage(), e);
        } finally {
            post.releaseConnection();
        }
        return aid;
    }

    public URI getUploadURI()  throws ServiceException {
        try {
            URI uri = new URI(mTransport.getURI());
            return  uri.resolve("/service/upload?fmt=raw");
        } catch (URISyntaxException e) {
            throw ZClientException.CLIENT_ERROR("unable to parse URI: "+mTransport.getURI(), e);
        }
    }

    private static Pattern sAttachmentId = Pattern.compile("\\d+,'.*','(.*)'");

    private String getAttachmentId(String result) throws ZClientException {
        if (result.startsWith(HttpServletResponse.SC_OK+"")) {
            Matcher m = sAttachmentId.matcher(result);
            return m.find() ? m.group(1) : null;
        } else if (result.startsWith(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE+"")) {
            throw ZClientException.UPLOAD_SIZE_LIMIT_EXCEEDED("upload size limit exceeded", null);
        }
        throw ZClientException.UPLOAD_FAILED("upload failed, response: " + result, null);
    }
    
    private void addAuthCookie(Map<String, String> cookieMap, URI uri, HttpState state) {
        if (cookieMap == null) 
            return;
        
        for (Map.Entry<String, String> ck : cookieMap.entrySet()) {
            Cookie cookie = new Cookie(uri.getHost(), ck.getKey(), ck.getValue(), "/", -1, false);
            state.addCookie(cookie);
        }
    }

    public HttpClient getHttpClient(URI uri) {
        boolean isAdmin = uri.getPort() == LC.zimbra_admin_service_port.intValue();
        HttpState initialState = new HttpState();
        
        Map<String, String> cookieMap;
        if (isAdmin) {
            cookieMap = getAuthToken().cookieMap(isAdmin);
            addAuthCookie(cookieMap, uri, initialState);
        }
        
        cookieMap = getAuthToken().cookieMap(false);
        addAuthCookie(cookieMap, uri, initialState);
        
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        client.setState(initialState);
        client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        return client;
    }

    /**
     * @param folderId (required) folderId of folder to add message to
     * @param flags non-comma-separated list of flags, e.g. "sf" for "sent by me and flagged",
     *        or <tt>null</tt>
     * @param tags coma-spearated list of tags, or null for no tags, or <tt>null</tt>
     * @param receivedDate time the message was originally received, in MILLISECONDS since the epoch,
     *        or <tt>0</tt> for the current time
     * @param content message content
     * @param noICal if TRUE, then don't process iCal attachments.
     * @return ID of newly created message
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public String addMessage(String folderId, String flags, String tags, long receivedDate, String content, boolean noICal) throws ServiceException {
        Element req = newRequestElement(MailConstants.ADD_MSG_REQUEST);
        Element m = req.addUniqueElement(MailConstants.E_MSG);
        m.addAttribute(MailConstants.A_FOLDER, folderId);
        if (flags != null && flags.length() > 0)
            m.addAttribute(MailConstants.A_FLAGS, flags);
        if (tags != null && tags.length() > 0)
            m.addAttribute(MailConstants.A_TAGS, tags);
        if (receivedDate != 0)
            m.addAttribute(MailConstants.A_DATE, receivedDate);
        m.addAttribute(MailConstants.A_NO_ICAL, noICal);
        m.addElement(MailConstants.E_CONTENT).setText(content);
        return invoke(req).getElement(MailConstants.E_MSG).getAttribute(MailConstants.A_ID);
    }

    /**
     * @param folderId (required) folderId of folder to add message to
     * @param flags non-comma-separated list of flags, e.g. "sf" for "sent by me and flagged",
     *        or <tt>null</tt>
     * @param tags comma-spearated list of tags, or null for no tags, or <tt>null</tt>
     * @param receivedDate time the message was originally received, in MILLISECONDS since the epoch,
     *        or <tt>0</tt> for the current time
     * @param content message content
     * @param noICal if TRUE, then don't process iCal attachments.
     * @return ID of newly created message
     * @throws ServiceException on error
     */
    public String addMessage(String folderId, String flags, String tags, long receivedDate, byte[] content, boolean noICal) throws ServiceException {
        return addMessage(folderId, flags, tags, receivedDate, new ByteArrayInputStream(content), content.length, noICal);
    }
    
    /**
     * @param folderId (required) folderId of folder to add message to
     * @param flags non-comma-separated list of flags, e.g. "sf" for "sent by me and flagged",
     *        or <tt>null</tt>
     * @param tags comma-spearated list of tags, or null for no tags, or <tt>null</tt>
     * @param receivedDate time the message was originally received, in MILLISECONDS since the epoch,
     *        or <tt>0</tt> for the current time
     * @param in content stream
     * @param contentLength number of bytes in the content stream
     * @param noICal if TRUE, then don't process iCal attachments.
     * @return ID of newly created message
     * @throws ServiceException on error
     */
    public String addMessage(String folderId, String flags, String tags, long receivedDate,
                             InputStream in, long contentLength, boolean noICal)
    throws ServiceException {
        // first, upload the content via the FileUploadServlet
        String aid = uploadContentAsStream("message", in, "message/rfc822", contentLength, 5000);

        // now, use the returned upload ID to do the message send
        Element req = newRequestElement(MailConstants.ADD_MSG_REQUEST);
        Element m = req.addUniqueElement(MailConstants.E_MSG);
        m.addAttribute(MailConstants.A_FOLDER, folderId);
        if (flags != null && flags.length() > 0)
            m.addAttribute(MailConstants.A_FLAGS, flags);
        if (tags != null && tags.length() > 0)
            m.addAttribute(MailConstants.A_TAGS, tags);
        if (receivedDate > 0)
            m.addAttribute(MailConstants.A_DATE, receivedDate);
        m.addAttribute(MailConstants.A_ATTACHMENT_ID, aid);
        m.addAttribute(MailConstants.A_NO_ICAL, noICal);
        return invoke(req).getElement(MailConstants.E_MSG).getAttribute(MailConstants.A_ID);
    }

    static class CachedMessage {
        ZGetMessageParams params;
        ZMessage zm;
    }

    public synchronized ZMessage getMessage(ZGetMessageParams params) throws ServiceException {
        CachedMessage cm = (CachedMessage) mMessageCache.get(params.getId());
        if (cm == null || !cm.params.equals(params)) {
            Element req = newRequestElement(MailConstants.GET_MSG_REQUEST);
            Element msgEl = req.addUniqueElement(MailConstants.E_MSG);
            msgEl.addAttribute(MailConstants.A_ID, params.getId());
            if (params.getPart() != null) msgEl.addAttribute(MailConstants.A_PART, params.getPart());
            msgEl.addAttribute(MailConstants.A_MARK_READ, params.isMarkRead());
            msgEl.addAttribute(MailConstants.A_WANT_HTML, params.isWantHtml());
            msgEl.addAttribute(MailConstants.A_NEUTER, params.isNeuterImages());
            msgEl.addAttribute(MailConstants.A_RAW, params.isRawContent());
            if (params.getMax() != null) {
                msgEl.addAttribute(MailConstants.A_MAX_INLINED_LENGTH, params.getMax());
            }
            //header request bug:33054
            String reqHdrs = params.getReqHeaders();
            if (reqHdrs != null && reqHdrs.length() > 0) {
                for (String hdrName : reqHdrs.split(",")) {
                    Element headerEl = msgEl.addElement(MailConstants.A_HEADER);
                    headerEl.addAttribute(MailConstants.A_ATTRIBUTE_NAME, hdrName);
                }
            }
            ZMessage zm = new ZMessage(invoke(req).getElement(MailConstants.E_MSG), this);
            cm = new CachedMessage();
            cm.zm = zm;
            cm.params = params;
            mMessageCache.put(params.getId(), cm);
        } else {
            if (params.isMarkRead() && cm.zm.isUnread())
                markMessageRead(cm.zm.getId(), true);
        }
        return cm.zm;
    }
    
    public synchronized ZMessage getMessageById(String id) throws ServiceException {
        ZGetMessageParams params = new ZGetMessageParams();
        params.setId(id);
        return getMessage(params);
    }

    /**
     * hard delete message(s)
     * @param ids ids to act on
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult deleteMessage(String ids) throws ServiceException {
        return doAction(messageAction("delete", ids));
    }

    /**
     * move message(s) to trash
     * @param ids ids to act on
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult trashMessage(String ids) throws ServiceException {
        return doAction(messageAction("trash", ids));
    }

    /**
     * mark message(s) as read/unread
     * @param ids ids to act on
     * @return action result
     * @throws ServiceException on error
     * @param read mark read/unread
     */
    public ZActionResult markMessageRead(String ids, boolean read) throws ServiceException {
        return doAction(messageAction(read ? "read" : "!read", ids));
    }

    /**
     *  mark message as spam/not spam
     * @param spam spam (TRUE) or not spam (FALSE)
     * @param id id of message
     * @param destFolderId optional id of destination folder, only used with "not spam".
     * @throws ServiceException on error
     * @return action result
     */
    public ZActionResult markMessageSpam(String id, boolean spam, String destFolderId) throws ServiceException {
        Element actionEl = messageAction(spam ? "spam" : "!spam", id);
        if (destFolderId != null && destFolderId.length() > 0) actionEl.addAttribute(MailConstants.A_FOLDER, destFolderId);
        return doAction(actionEl);
    }

    /** flag/unflag message(s)
     *
     * @return action result
     * @param ids of messages to flag
     * @param flag flag on /off
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public ZActionResult flagMessage(String ids, boolean flag) throws ServiceException {
        return doAction(messageAction(flag ? "flag" : "!flag", ids));
    }

    /** tag/untag message(s)
     * @param ids ids of messages to tag
     * @param tagId tag id to tag with
     * @param tag tag/untag
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult tagMessage(String ids, String tagId, boolean tag) throws ServiceException {
        return doAction(messageAction(tag ? "tag" : "!tag", ids).addAttribute(MailConstants.A_TAG, tagId));
    }

    /** move message(s)
     * @param ids list of ids to move
     * @param destFolderId destination folder id
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult moveMessage(String ids, String destFolderId) throws ServiceException {
        return doAction(messageAction("move", ids).addAttribute(MailConstants.A_FOLDER, destFolderId));
    }

    /**
     * update message(s)
     * @param ids ids of messages to update
     * @param destFolderId optional destination folder
     * @param tagList optional new list of tag ids
     * @param flags optional new value for flags
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult updateMessage(String ids, String destFolderId, String tagList, String flags) throws ServiceException {
        Element actionEl = messageAction("update", ids);
        if (destFolderId != null && destFolderId.length() > 0) actionEl.addAttribute(MailConstants.A_FOLDER, destFolderId);
        if (tagList != null) actionEl.addAttribute(MailConstants.A_TAGS, tagList);
        if (flags != null) actionEl.addAttribute(MailConstants.A_FLAGS, flags);
        return doAction(actionEl);
    }

    // ------------------------

    /**
     * return the root user folder
     * @return user root folder
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public ZFolder getUserRoot() throws ServiceException {
        populateFolderCache();
        return mUserRoot;
    }

    /**
     * find the folder with the specified path, starting from the user root.
     * @param path path of folder. Must start with {@link #PATH_SEPARATOR}.
     * @return ZFolder if found, null otherwise.
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public ZFolder getFolderByPath(String path) throws ServiceException {
        populateFolderCache();
        if (!path.startsWith(ZMailbox.PATH_SEPARATOR))
            path = ZMailbox.PATH_SEPARATOR + path;
        if (mUserRoot == null)
            return null;
        return mUserRoot.getSubFolderByPath(path.substring(1));
    }

    public ZFolder getInbox() throws ServiceException { return getFolderById(ZFolder.ID_INBOX); }
    public ZFolder getTrash() throws ServiceException { return getFolderById(ZFolder.ID_TRASH); }
    public ZFolder getSpam() throws ServiceException { return getFolderById(ZFolder.ID_SPAM); }
    public ZFolder getJunk() throws ServiceException { return getFolderById(ZFolder.ID_SPAM); }
    public ZFolder getSent() throws ServiceException { return getFolderById(ZFolder.ID_SENT); }
    public ZFolder getDrafts() throws ServiceException { return getFolderById(ZFolder.ID_DRAFTS); }
    public ZFolder getContacts() throws ServiceException { return getFolderById(ZFolder.ID_CONTACTS); }
    public ZFolder getCalendar() throws ServiceException { return getFolderById(ZFolder.ID_CALENDAR); }
    public ZFolder getNotebok() throws ServiceException { return getFolderById(ZFolder.ID_NOTEBOOK); }
    public ZFolder getAutoContacts() throws ServiceException { return getFolderById(ZFolder.ID_AUTO_CONTACTS); }
    public ZFolder getChats() throws ServiceException { return getFolderById(ZFolder.ID_CHATS); }
    public ZFolder getTasks() throws ServiceException { return getFolderById(ZFolder.ID_TASKS); }
    public ZFolder getBriefcase() throws ServiceException { return getFolderById(ZFolder.ID_BRIEFCASE); }

    /**
     * find the folder with the specified id.
     * @param id id of  folder
     * @return ZFolder if found, null otherwise.
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public ZFolder getFolderById(String id) throws ServiceException {
        populateFolderCache();
        ZItem item = mIdToItem.get(id);
        if (!(item instanceof ZFolder)) return null;
        ZFolder folder = (ZFolder) item;
        return folder.isHierarchyPlaceholder() ? null : folder;
    }

    /**
     * find the folder with the specified path/id. Look up by path first, then id if path not found.
     * @param pathOrId path or id of  folder
     * @return ZFolder if found, null otherwise.
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public ZFolder getFolder(String pathOrId) throws ServiceException {
        ZFolder result = getFolderByPath(pathOrId);
        return result != null ? result : getFolderById(pathOrId);
    }
    
    /**
     * always bypass caching and issues a GetFolderRequest
     * 
     * @param id
     * @return
     * @throws ServiceException
     */
    public ZFolder getFolderRequestById(String id) throws ServiceException {
        Element req = newRequestElement(MailConstants.GET_FOLDER_REQUEST).addAttribute(MailConstants.A_VISIBLE, true);
        req.addElement(MailConstants.E_FOLDER).addAttribute(MailConstants.A_FOLDER, id);
        
        Element response = invoke(req);
        Element eFolder = response.getOptionalElement(MailConstants.E_FOLDER);
        if (eFolder == null)
            eFolder = response.getOptionalElement(MailConstants.E_MOUNT);
        if (eFolder == null)
            return null;
        
        ZFolder folder = new ZFolder(eFolder, null, this);
        return folder.isHierarchyPlaceholder() ? null : folder;
    }
    
    /**
     * to be backward compatible with YCal which currently calls this method.
     * should switch to getFolderRequestById
     * 
     * delete this methods when it's time
     * 
     * @param id
     * @return
     * @throws ServiceException
     */
    public ZFolder getFolderRequest(String id) throws ServiceException {
        return getFolderRequestById(id);
    }
    
    /**
     * Returns all folders and subfolders in this mailbox.
     * @throws ServiceException on error
     * @return all folders and subfolders in this mailbox
     */
    public List<ZFolder> getAllFolders() throws ServiceException {
        populateFolderCache();
        List<ZFolder> allFolders = new ArrayList<ZFolder>();
        if (getUserRoot() != null)
            addSubFolders(getUserRoot(), allFolders);
        return allFolders;
    }

    private void addSubFolders(ZFolder folder, List<ZFolder> folderList) throws ServiceException {
        if (!folder.isHierarchyPlaceholder()) {
            folderList.add(folder);
        }
        for (ZFolder subFolder : folder.getSubFolders()) {
            addSubFolders(subFolder, folderList);
        }
    }

    /**
     * returns a rest URL relative to this mailbox.
     * @param relativePath a relative path (i.e., "/Calendar", "Inbox?fmt=rss", etc).
     * @return URI of path
     * @throws ServiceException on error
     */
    public URI getRestURI(String relativePath) throws ServiceException {
        return getRestURI(relativePath, null);
    }
    
    /**
     * returns a rest URL relative to this mailbox.
     * @param relativePath a relative path (i.e., "/Calendar", "Inbox?fmt=rss", etc).
     * @param alternateUrl alternate url to connect to
     * @return URI of path
     * @throws ServiceException on error
     */
    private URI getRestURI(String relativePath, String alternateUrl) throws ServiceException {
        String pathPrefix = "/";
        if (relativePath.startsWith("/")) {
            pathPrefix = "";
        }
        
        try {
            String restURI = getAccountInfo(false).getRestURLBase();
            if (alternateUrl != null) {
                // parse the URI and extract path
                URI uri = new URI(restURI);
                restURI = alternateUrl + uri.getPath();
            }
            
            if (restURI == null) {
                URI uri = new URI(mTransport.getURI());
                return  uri.resolve("/home/" + getName() + pathPrefix + relativePath);
            } else {
                return new URI(restURI + pathPrefix + relativePath);
            }
        } catch (URISyntaxException e) {
            throw ZClientException.CLIENT_ERROR("unable to parse URI: "+mTransport.getURI(), e);
        }
    }

    /**
     *
     * @param relativePath a relative path (i.e., "/Calendar", "Inbox?fmt=rss", etc).
     * @param os the stream to send the output to
     * @param closeOs whether or not to close the output stream when done
     * @param msecTimeout connection timeout
     * @param alternateUrl if not <tt>null</tt>, this URL will be used instead of
     * <tt>relativePath</tt>
     * @throws ServiceException on error
     */
    public void getRESTResource(
            String relativePath, OutputStream os, boolean closeOs,
            String startTimeArg, String endTimeArg, int msecTimeout, String alternateUrl)
    throws ServiceException {
        InputStream in = null;
        try {
            in = getRESTResource(relativePath, startTimeArg, endTimeArg, msecTimeout, alternateUrl);
            ByteUtil.copy(in, false, os, closeOs);
        } catch (IOException e) {
            throw ZClientException.IO_ERROR("Unable to get " + relativePath, e);
        } finally {
            ByteUtil.closeStream(in);
        }
    }

    private InputStream getRESTResource(String relativePath, String startTimeArg, String endTimeArg, 
            int msecTimeout, String alternateUrl)
    throws ServiceException {
        GetMethod get = null;
        URI uri = null;

        int statusCode;
        try {
            if (startTimeArg != null) {
                String encodedArg = URLEncoder.encode(startTimeArg, "UTF-8");
                if (!relativePath.contains("?"))
                    relativePath = relativePath + "?start=" + encodedArg;
                else
                    relativePath = relativePath + "&start=" + encodedArg;
            }
            if (endTimeArg != null) {
                String encodedArg = URLEncoder.encode(endTimeArg, "UTF-8");
                if (!relativePath.contains("?"))
                    relativePath = relativePath + "?end=" + encodedArg;
                else
                    relativePath = relativePath + "&end=" + encodedArg;
            }

            uri = getRestURI(relativePath, alternateUrl);
            HttpClient client = getHttpClient(uri);

            get = new GetMethod(uri.toString());
            
            if (msecTimeout > -1)
                get.getParams().setSoTimeout(msecTimeout);

            statusCode = HttpClientUtil.executeMethod(client, get);
            // parse the response
            if (statusCode == 200) {
                return new GetMethodInputStream(get);
            } else {
                String msg = String.format("GET from %s failed, status=%d.  %s", uri.toString(), statusCode, get.getStatusText());
                throw ServiceException.FAILURE(msg, null);
            }
        } catch (IOException e) {
            String fromUri = "";
            if (uri != null) {
                fromUri = " from " + uri.toString(); 
            }
            String msg = String.format("Unable to get REST resource%s: %s", fromUri, e.getMessage());
            throw ZClientException.IO_ERROR(msg, e);
        }
    }
    
    /**
     * @param relativePath a relative path (i.e., "/Calendar", "Inbox?fmt=rss", etc).
     * @throws ServiceException on error
     */
    public InputStream getRESTResource(String relativePath)
    throws ServiceException {
        return getRESTResource(relativePath, null, null, getTimeout(), null);
    }

    /**
     *
     * @param relativePath a relative path (i.e., "/Calendar", "Inbox?fmt=rss", etc).
     * @param is the input stream to post
     * @param closeIs whether to close the input stream when done
     * @param length length of inputstream, or 0/-1 if length is unknown.
     * @param contentType optional content-type header value (defaults to "application/octect-stream")
     * @param ignoreAndContinueOnError if true, set optional ignore=1 query string parameter
     * @param preserveAlarms if true, set optional preserveAlarms=1 query string parameter
     * @param msecTimeout connection timeout in milliseconds, or <tt>-1</tt> for no timeout
     * @param url alternate url to connect to
     * @throws ServiceException on error
     */
    public void postRESTResource(String relativePath, InputStream is, boolean closeIs, long length,
                                 String contentType, boolean ignoreAndContinueOnError, boolean preserveAlarms,
                                 int msecTimeout, String alternateUrl)
    throws ServiceException {
        PostMethod post = null;

        try {
            if (ignoreAndContinueOnError) {
                if (!relativePath.contains("?"))
                    relativePath = relativePath + "?ignore=1";
                else
                    relativePath = relativePath + "&ignore=1";
            }
            if (preserveAlarms) {
                if (!relativePath.contains("?"))
                    relativePath = relativePath + "?preserveAlarms=1";
                else
                    relativePath = relativePath + "&preserveAlarms=1";
            }
            URI uri = getRestURI(relativePath, alternateUrl);
            HttpClient client = getHttpClient(uri);

            post = new PostMethod(uri.toString());
            
            if (msecTimeout > -1)
                post.getParams().setSoTimeout(msecTimeout);
            
            RequestEntity entity = (length > 0) ?
                    new InputStreamRequestEntity(is, length, contentType != null ? contentType:  "application/octet-stream") :
                    new InputStreamRequestEntity(is, contentType);
            post.setRequestEntity(entity);
            int statusCode = HttpClientUtil.executeMethod(client, post);
            // parse the response
            if (statusCode == 200) {
                //
            } else {
                throw ServiceException.FAILURE("POST failed, status=" + statusCode+" "+post.getStatusText(), null);
            }
        } catch (IOException e) {
            throw ZClientException.IO_ERROR(e.getMessage(), e);
        } finally {
            if (closeIs)
                ByteUtil.closeStream(is);
            if (post != null)
                post.releaseConnection();
        }
    }

    /**
     *
     * @param relativePath a relative path (i.e., "/Calendar", "Inbox?fmt=rss", etc).
     * @param is the input stream to post
     * @param closeIs whether to close the input stream when done
     * @param length length of inputstream, or 0/-1 if length is unknown.
     * @param contentType optional content-type header value (defaults to "application/octect-stream")
     * @param msecTimeout connection timeout in milliseconds, or <tt>-1</tt> for no timeout
     * @throws ServiceException on error
     */
    public void postRESTResource(String relativePath, InputStream is, boolean closeIs, long length,
                                 String contentType,
                                 int msecTimeout)
    throws ServiceException {
        postRESTResource(relativePath, is, closeIs, length, contentType, false, false, msecTimeout, null);
    }

    /**
     * find the search folder with the specified id.
     * @param id id of  folder
     * @return ZSearchFolder if found, null otherwise.
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public ZSearchFolder getSearchFolderById(String id) throws ServiceException {
        populateFolderCache();
        ZItem item = mIdToItem.get(id);
        if (item instanceof ZSearchFolder)
            return (ZSearchFolder) item;
        else
            return null;
    }

    /**
     * find the mountpoint with the specified id.
     * @param id id of mountpoint
     * @return ZMountpoint if found, null otherwise.
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public ZMountpoint getMountpointById(String id) throws ServiceException {
        populateFolderCache();
        ZItem item = mIdToItem.get(id);
        if (item instanceof ZMountpoint)
            return (ZMountpoint) item;
        else
            return null;
    }

    /**
     * create a new sub folder of the specified parent folder.
     *
     * @param parentId parent folder id
     * @param name name of new folder
     * @param defaultView default view of new folder or null.
     * @param color color of folder, or null to use default
     * @param flags flags for folder, or null
     *
     * @return newly created folder
     * @throws ServiceException on error
     * @param url remote url for rss/atom/ics feeds
     */
    public ZFolder createFolder(String parentId, String name, ZFolder.View defaultView, ZFolder.Color color, String flags, String url) throws ServiceException {
        Element req = newRequestElement(MailConstants.CREATE_FOLDER_REQUEST);
        Element folderEl = req.addUniqueElement(MailConstants.E_FOLDER);
        folderEl.addAttribute(MailConstants.A_NAME, name);
        folderEl.addAttribute(MailConstants.A_FOLDER, parentId);
        if (defaultView != null) folderEl.addAttribute(MailConstants.A_DEFAULT_VIEW, defaultView.name());
        if (color != null) folderEl.addAttribute(MailConstants.A_COLOR, color.getValue());
        if (flags != null) folderEl.addAttribute(MailConstants.A_FLAGS, flags);
        if (url != null && url.length() > 0) folderEl.addAttribute(MailConstants.A_URL, url);
        Element newFolderEl = invoke(req).getElement(MailConstants.E_FOLDER);
        ZFolder newFolder = getFolderById(newFolderEl.getAttribute(MailConstants.A_ID));
        return newFolder != null ? newFolder : new ZFolder(newFolderEl, null, this);
    }

    /**
     * create a new sub folder of the specified parent folder.
     *
     * @param parentId parent folder id
     * @param name name of new folder
     * @param query search query (required)
     * @param types comma-sep list of types to search for. See {@link SearchParams} for more info. Use null for default value.
     * @param sortBy how to sort the result. Use null for default value.
     * @see {@link ZSearchParams#TYPE_MESSAGE}
     * @return newly created search folder
     * @throws ServiceException on error
     * @param color color of folder
     */
    public ZSearchFolder createSearchFolder(String parentId, String name,
                String query, String types, SearchSortBy sortBy, ZFolder.Color color) throws ServiceException {
        Element req = newRequestElement(MailConstants.CREATE_SEARCH_FOLDER_REQUEST);
        Element folderEl = req.addUniqueElement(MailConstants.E_SEARCH);
        folderEl.addAttribute(MailConstants.A_NAME, name);
        folderEl.addAttribute(MailConstants.A_FOLDER, parentId);
        folderEl.addAttribute(MailConstants.A_QUERY, query);
        if (color != null) folderEl.addAttribute(MailConstants.A_COLOR, color.getValue());
        if (types != null) folderEl.addAttribute(MailConstants.A_SEARCH_TYPES, types);
        if (sortBy != null) folderEl.addAttribute(MailConstants.A_SORTBY, sortBy.name());
        Element newSearchEl = invoke(req).getElement(MailConstants.E_SEARCH);
        ZSearchFolder newSearch = getSearchFolderById(newSearchEl.getAttribute(MailConstants.A_ID));
        return newSearch != null ? newSearch : new ZSearchFolder(newSearchEl, null, this);
    }

    /**
     * modify a search folder.
     *
     * @param id id of search folder
     * @param query search query or null to leave unchanged.
     * @param types new types or null to leave unchanged.
     * @param sortBy new sortBy or null to leave unchanged
     * @return modified search folder
     * @throws ServiceException on error
     */
    public ZSearchFolder modifySearchFolder(String id, String query, String types, SearchSortBy sortBy) throws ServiceException {
        Element req = newRequestElement(MailConstants.MODIFY_SEARCH_FOLDER_REQUEST);
        Element folderEl = req.addUniqueElement(MailConstants.E_SEARCH);
        folderEl.addAttribute(MailConstants.A_ID, id);
        if (query != null) folderEl.addAttribute(MailConstants.A_QUERY, query);
        if (types != null) folderEl.addAttribute(MailConstants.A_SEARCH_TYPES, types);
        if (sortBy != null) folderEl.addAttribute(MailConstants.A_SORTBY, sortBy.name());
        invoke(req);
        // this assumes notifications will modify the search folder
        return getSearchFolderById(id);
    }

    public static class ZActionResult {
        private String mIds;
        private Element mResponse;

        public ZActionResult(Element response) throws ServiceException {
            String ids = response.getElement(MailConstants.E_ACTION).getAttribute(MailConstants.A_ID);
            if (ids == null) ids = "";
            mIds = ids;
            mResponse = response;
        }

        public String getIds() {
            return mIds;
        }

        public String[] getIdsAsArray() {
            return mIds.split(",");
        }

        public String toString() {
            return String.format("[ZActionResult %s]", mIds);
        }
        
        Element getResponse() {
            return mResponse;
        }
    }

    private Element folderAction(String op, String ids) {
        Element req = newRequestElement(MailConstants.FOLDER_ACTION_REQUEST);
        Element actionEl = req.addUniqueElement(MailConstants.E_ACTION);
        actionEl.addAttribute(MailConstants.A_ID, ids);
        actionEl.addAttribute(MailConstants.A_OPERATION, op);
        return actionEl;
    }

    /** sets or unsets the folder's checked state in the UI
     * @param ids ids of folder to check
     * @param checked checked/unchecked
     * @throws ServiceException on error
     * @return action result
     */
    public ZActionResult modifyFolderChecked(String ids, boolean checked) throws ServiceException {
        return doAction(folderAction(checked ? "check" : "!check", ids));
    }

    /** modifies the folder's color
     * @param ids ids to modify
     * @param color new color
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult modifyFolderColor(String ids, ZFolder.Color color) throws ServiceException {
        return doAction(folderAction("color", ids).addAttribute(MailConstants.A_COLOR, color.getValue()));
    }

    /** hard delete the folder, all items in folder and all sub folders
     * @param ids ids to delete
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult deleteFolder(String ids) throws ServiceException {
        return doAction(folderAction("delete", ids));
    }

    /** move the folder to the Trash, marking all contents as read and
     * renaming the folder if a folder by that name is already present in the Trash
     * @param ids ids to delete
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult trashFolder(String ids) throws ServiceException {
        return doAction(folderAction("trash", ids));
    }

    /** hard delete all items in folder and sub folders (doesn't delete the folder itself)
     * @param ids ids of folders to empty
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult emptyFolder(String ids) throws ServiceException {
        return emptyFolder(ids, true);
    }

    /** hard delete all items in folder (doesn't delete the folder itself)
     *  deletes subfolders contained in the specified folder(s) if <tt>subfolders</tt> is set
     *
     * @param ids ids of folders to empty
     * @param subfolders whether to delete subfolders of this folder
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult emptyFolder(String ids, boolean subfolders) throws ServiceException {
        return doAction(folderAction("empty", ids).addAttribute(MailConstants.A_RECURSIVE, true));
    }

    /** mark all items in folder as read
     * @param ids ids of folders to mark as read
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult markFolderRead(String ids) throws ServiceException {
        return doAction(folderAction("read", ids));
    }

    /** add the contents of the remote feed at target-url to the folder (one time action)
     * @param id of folder to import into
     * @param url url to import
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult importURLIntoFolder(String id, String url) throws ServiceException {
        return doAction(folderAction("import", id).addAttribute(MailConstants.A_URL, url));
    }

    /** move the folder to be a child of {target-folder}
     * @param id folder id to move
     * @param targetFolderId id of target folder
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult moveFolder(String id, String targetFolderId) throws ServiceException {
        return doAction(folderAction("move", id).addAttribute(MailConstants.A_FOLDER, targetFolderId));
    }

    /** change the folder's name; if new name  begins with '/', the folder is moved to the new path and any missing path elements are created
     * @param id id of folder to rename
     * @param name new name
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult renameFolder(String id, String name) throws ServiceException {
        return renameFolder(id, name, null);
    }
    
    /** changes the folder's name and moves it be a child of the given target folder.
     * @param id folder id
     * @param name new name
     * @param targetFolderId new parent, or <tt>null</tt> to keep the current parent
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult renameFolder(String id, String name, String targetFolderId) throws ServiceException {
        Element folderAction = folderAction("rename", id);
        folderAction.addAttribute(MailConstants.A_NAME, name);
        if (targetFolderId != null) {
            folderAction.addAttribute(MailConstants.A_FOLDER, targetFolderId);
        }
        return doAction(folderAction);
    }

    /** sets or unsets the folder's exclude from free busy state
     * @param ids folder id
     * @param state exclude/not-exclude
     * @throws ServiceException on error
     * @return action result
     */
    public ZActionResult modifyFolderExcludeFreeBusy(String ids, boolean state) throws ServiceException {
        return doAction(folderAction("fb", ids).addAttribute(MailConstants.A_EXCLUDE_FREEBUSY, state));
    }

    /**
     *
     * @param folderId to modify
     * @param granteeType type of grantee
     * @param grantreeId id of grantree
     * @param perms permission mask ("rwid")
     * @param args extra args
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult modifyFolderGrant(
            String folderId, GranteeType granteeType, String grantreeId,
            String perms, String args) throws ServiceException {
        Element action = folderAction("grant", folderId);
        Element grant = action.addUniqueElement(MailConstants.E_GRANT);
        grant.addAttribute(MailConstants.A_RIGHTS, perms);
        grant.addAttribute(MailConstants.A_DISPLAY, grantreeId);
        grant.addAttribute(MailConstants.A_GRANT_TYPE, granteeType.name());
        if (args != null) {
            if (granteeType == GranteeType.key)
                grant.addAttribute(MailConstants.A_ACCESSKEY, args);
            else
                grant.addAttribute(MailConstants.A_ARGS, args);
        }
        ZActionResult r = doAction(action);
        
        /*
         * for key grantee type, the accesskey is not encoded in the <notify> 
         * block in FolderAction or the <refresh> block for any calls.  
         * accesskey is only returned on explicit GetFolderRequest.
         * 
         * add a convenient hack here so client does not have to call 
         * mbox.getFolderRequest after a modifyFolderGrant in order to get 
         * the (new or modified) accesskey.
         */
        if (granteeType == GranteeType.key) {
            ZFolder folder = getFolderById(folderId);
            for (ZGrant g : folder.getGrants()) {
                if (g.getGranteeType() == GranteeType.key && 
                    g.getGranteeId().equals(grantreeId)) {
                    String key = null;
                    Element eAction = r.getResponse().getOptionalElement(MailConstants.E_ACTION);
                    if (eAction != null)
                        key = eAction.getAttribute(MailConstants.A_ACCESSKEY, null);
                    if (key != null)
                        g.setAccessKey(key);
                    break;
                }
            }
        }
        
        return r;
    }

    /**
     * revoke a grant
     * @param folderId folder id to modify
     * @param grantreeId zimbra ID
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult modifyFolderRevokeGrant(String folderId, String grantreeId) throws ServiceException
    {
        Element action = folderAction("!grant", folderId);
        action.addAttribute(MailConstants.A_ZIMBRA_ID, grantreeId);
        return doAction(action);
    }

    /**
     * set the synchronization url on the folder to {target-url}, empty the folder, and
     * synchronize the folder's contents to the remote feed, also sets {exclude-free-busy-boolean}
     * @param id id of folder
     * @param url new URL
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult modifyFolderURL(String id, String url) throws ServiceException {
        return doAction(folderAction("url", id).addAttribute(MailConstants.A_URL, url));
    }

    public ZActionResult updateFolder(String id, String name, String parentId, Color newColor, String flags, List<ZGrant> acl) throws ServiceException {
        Element action = folderAction("update", id);
        if (name != null && name.length() > 0)
            action.addAttribute(MailConstants.A_NAME, name);
        if (parentId != null && parentId.length() > 0)
            action.addAttribute(MailConstants.A_FOLDER, parentId);
        if (newColor != null)
            action.addAttribute(MailConstants.A_COLOR, newColor.getValue());
        if (flags != null)
            action.addAttribute(MailConstants.A_FLAGS, flags);
        if (acl != null) {
            Element aclEl = action.addElement(MailConstants.E_ACL);
            for (ZGrant grant : acl) {
                grant.toElement(aclEl);
            }
        }
        return doAction(action);
    }

    /**
     * sync the folder's contents to the remote feed specified by the folders URL
     * @param ids folder id
     * @throws ServiceException on error
     * @return action result
     */
    public ZActionResult syncFolder(String ids) throws ServiceException {
        return doAction(folderAction("sync", ids));
    }

    /** sets or unsets the folder's sync flag
     * @param id folder id
     * @param syncon turn sync flag on
     * @throws ServiceException on error
     * @return action result
     */
    public ZActionResult modifyFolderSyncFlag(String id, boolean syncon) throws ServiceException {
        return doAction(folderAction(syncon ? "syncon" : "!syncon", id));
    }

    // ------------------------

    private synchronized ZSearchResult internalSearch(String convId, ZSearchParams params, boolean nest) throws ServiceException {
        QName name;
        if (convId != null) {
        	name = MailConstants.SEARCH_CONV_REQUEST;
        } else if (params.getTypes().equals(ZSearchParams.TYPE_VOICE_MAIL) ||
                   params.getTypes().equals(ZSearchParams.TYPE_CALL)) {
        	name = VoiceConstants.SEARCH_VOICE_REQUEST;
        } else if (params.getTypes().equals(ZSearchParams.TYPE_GAL)) {
            name = AccountConstants.SEARCH_GAL_REQUEST;
        } else {
        	name = MailConstants.SEARCH_REQUEST;
        }
        
        Element req = newRequestElement(name);

        if (params.getTypes().equals(ZSearchParams.TYPE_GAL)) {
            req.addAttribute(AccountConstants.A_TYPE, GalEntryType.account.name());
            req.addElement(AccountConstants.E_NAME).setText(params.getQuery());
            //req.addAttribute(MailConstants.A_SORTBY, SearchSortBy.nameAsc.name());

        }

        req.addAttribute(MailConstants.A_CONV_ID, convId);
        if (nest) req.addAttribute(MailConstants.A_NEST_MESSAGES, true);
        if (params.getLimit() != 0) req.addAttribute(MailConstants.A_QUERY_LIMIT, params.getLimit());
        if (params.getOffset() != 0) req.addAttribute(MailConstants.A_QUERY_OFFSET, params.getOffset());
        if (params.getSortBy() != null) req.addAttribute(MailConstants.A_SORTBY, params.getSortBy().name());
        if (params.getTypes() != null) req.addAttribute(MailConstants.A_SEARCH_TYPES, params.getTypes());
        if (params.getFetch() != null && params.getFetch() != Fetch.none) {
            // use "1" for "first" for backward compat until DF is updated
            req.addAttribute(MailConstants.A_FETCH, params.getFetch() == Fetch.first ? "1" : params.getFetch().name());
        }
        if (params.getCalExpandInstStart() != 0) req.addAttribute(MailConstants.A_CAL_EXPAND_INST_START, params.getCalExpandInstStart());
        if (params.getCalExpandInstEnd() != 0) req.addAttribute(MailConstants.A_CAL_EXPAND_INST_END, params.getCalExpandInstEnd());

        if (params.isPreferHtml()) req.addAttribute(MailConstants.A_WANT_HTML, params.isPreferHtml());
        if (params.isMarkAsRead()) req.addAttribute(MailConstants.A_MARK_READ, params.isMarkAsRead());
        if (params.isRecipientMode()) req.addAttribute(MailConstants.A_RECIPIENTS, params.isRecipientMode());
        if (params.getField() != null) req.addAttribute(MailConstants.A_FIELD, params.getField());

        req.addAttribute(MailConstants.E_QUERY, params.getQuery(), Element.Disposition.CONTENT);

        if (params.getCursor() != null) {
            Cursor cursor = params.getCursor();
            Element cursorEl = req.addElement(MailConstants.E_CURSOR);
            if (cursor.getPreviousId() != null) cursorEl.addAttribute(MailConstants.A_ID, cursor.getPreviousId());
            if (cursor.getPreviousSortValue() != null) cursorEl.addAttribute(MailConstants.A_SORTVAL, cursor.getPreviousSortValue());
        }

		if (params.getTypes().equals(ZSearchParams.TYPE_VOICE_MAIL) ||
			params.getTypes().equals(ZSearchParams.TYPE_CALL)) {
			getAllPhoneAccounts();
			setVoiceStorePrincipal(req);
		}
        Element resp = invoke(req);
        if (params.getTypes().equals(ZSearchParams.TYPE_GAL)) {
             try{
                 resp.getAttribute(MailConstants.A_SORTBY);

             }catch (Exception e){
                    resp.addAttribute(MailConstants.A_SORTBY,params.getSortBy().name());
             }
             try{
                 resp.getAttribute(MailConstants.A_QUERY_OFFSET);

             }catch (Exception e){
                    resp.addAttribute(MailConstants.A_QUERY_OFFSET,params.getOffset());
             }
        }
		return new ZSearchResult(resp, nest, params.getTimeZone() != null ? params.getTimeZone() : getPrefs().getTimeZone());
    }

    /**
     * do a search
     * @param params search prams
     * @return search result
     * @throws ServiceException on error
     */
    public synchronized ZSearchResult search(ZSearchParams params) throws ServiceException {
        return internalSearch(null, params, false);
    }

    /**
     * do a search, using potentially cached results for efficient paging forward/backward.
     * Search hits are kept up to date via notifications.
     *
     * @param params search prams. Should not change from call to call.
     * @return search result
     * @throws ServiceException on error
     * @param page page of results to return. page size is determined by limit in params.
     * @param useCache use the cache if possible
     * @param useCursor true to use search cursors, false to use offsets
     */
    public synchronized ZSearchPagerResult search(ZSearchParams params, int page, boolean useCache, boolean useCursor) throws ServiceException {
        return mSearchPagerCache.search(this, params, page, useCache, useCursor);
    }

    /**
     *
     * @param type if non-null, clear only cached searches of the specified tape
     */
    public synchronized void clearSearchCache(String type) {
        mSearchPagerCache.clear(type);
    }

    /**
     *  do a search conv
     * @param convId id of conversation to search
     * @param params convId onversation id
     * @return search result
     * @throws ServiceException on error
     */
    public synchronized ZSearchResult searchConversation(String convId, ZSearchParams params) throws ServiceException {
        if (convId == null) throw ZClientException.CLIENT_ERROR("conversation id must not be null", null);
        return internalSearch(convId, params, true);
    }

    public synchronized ZSearchPagerResult searchConversation(String convId, ZSearchParams params, int page, boolean useCache, boolean useCursor) throws ServiceException {
        if (params.getConvId() == null)
            params.setConvId(convId);
        return mSearchConvPagerCache.search(this, params, page, useCache, useCursor);
    }

    private void populateFolderCache() throws ServiceException {
        if (mUserRoot != null)
            return;
        if (mNotifyPreference == null || mNotifyPreference == NotifyPreference.full) {
            noOp();
            if (mUserRoot != null)
                return;
        }

        Element response = invoke(newRequestElement(MailConstants.GET_FOLDER_REQUEST).addAttribute(MailConstants.A_VISIBLE, true));
        Element eFolder = response.getOptionalElement(MailConstants.E_FOLDER);
        ZFolder userRoot = (eFolder != null ? new ZFolder(eFolder, null, this) : null);

        ZRefreshEvent event = new ZRefreshEvent(mSize, userRoot, null);
        for (ZEventHandler handler : mHandlers)
            handler.handleRefresh(event, this);
    }

    private void populateTagCache() throws ServiceException {
        if (mNameToTag != null)
            return;
        if (mNotifyPreference == null || mNotifyPreference == NotifyPreference.full) {
            noOp();
            if (mNameToTag != null)
                return;
        }

        List<ZTag> tagList = new ArrayList<ZTag>();
        if (!mNoTagCache) {
            try {
                Element response = invoke(newRequestElement(MailConstants.GET_TAG_REQUEST));
                for (Element t : response.listElements(MailConstants.E_TAG))
                    tagList.add(new ZTag(t, this));
            } catch (SoapFaultException sfe) {
                if (!sfe.getCode().equals(ServiceException.PERM_DENIED))
                    throw sfe;
            }
        }

        ZRefreshEvent event = new ZRefreshEvent(mSize, null, tagList);
        for (ZEventHandler handler : mHandlers)
            handler.handleRefresh(event, this);
    }

    /**
     * A request that does nothing and always returns nothing. Used to keep a session alive, and return
     * any pending notifications.
     *
     * @throws ServiceException on error
     */
    public void noOp() throws ServiceException {
        invoke(newRequestElement(MailConstants.NO_OP_REQUEST));
    }

    public enum OwnerBy {
        BY_ID, BY_NAME;

        public static OwnerBy fromString(String s) throws ServiceException {
            try {
                return OwnerBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid ownerBy: "+s+", valid values: "+Arrays.asList(OwnerBy.values()), e);
            }
        }
    }

    public enum SharedItemBy {
        BY_ID, BY_PATH;

        public static SharedItemBy fromString(String s) throws ServiceException {
            try {
                return SharedItemBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid sharedItemBy: "+s+", valid values: "+Arrays.asList(SharedItemBy.values()), e);
            }
        }
    }

    /**
     * create a new mointpoint in the specified parent folder.
     *
     * @param parentId parent folder id
     * @param name name of new folder
     * @param defaultView default view of new folder.
     * @param ownerBy used to specify whether owner is an id or account name (email address)
     * @param owner either the id or name of the owner
     * @param itemBy used to specify whether sharedItem is an id or path to the shared item
     * @param sharedItem either the id or path of the item
     *
     * @return newly created folder
     * @throws ServiceException on error
     * @param color initial color
     * @param flags initial flags
     */
    public ZMountpoint createMountpoint(String parentId, String name,
            ZFolder.View defaultView, ZFolder.Color color, String flags,
            OwnerBy ownerBy, String owner, SharedItemBy itemBy, String sharedItem) throws ServiceException {
        Element req = newRequestElement(MailConstants.CREATE_MOUNTPOINT_REQUEST);
        Element linkEl = req.addUniqueElement(MailConstants.E_MOUNT);
        linkEl.addAttribute(MailConstants.A_NAME, name);
        linkEl.addAttribute(MailConstants.A_FOLDER, parentId);
        if (defaultView != null) linkEl.addAttribute(MailConstants.A_DEFAULT_VIEW, defaultView.name());
        if (color != null) linkEl.addAttribute(MailConstants.A_COLOR, color.getValue());
        if (flags != null) linkEl.addAttribute(MailConstants.A_FLAGS, flags);
        linkEl.addAttribute(ownerBy == OwnerBy.BY_ID ? MailConstants.A_ZIMBRA_ID : MailConstants.A_OWNER_NAME, owner);
        linkEl.addAttribute(itemBy == SharedItemBy.BY_ID ? MailConstants.A_REMOTE_ID: MailConstants.A_PATH, sharedItem);
        Element newMountEl = invoke(req).getElement(MailConstants.E_MOUNT);
        ZMountpoint newMount = getMountpointById(newMountEl.getAttribute(MailConstants.A_ID));
        return newMount != null ? newMount : new ZMountpoint(newMountEl, null, this);
    }

    /**
     * Sends an iCalendar REPLY object
     * @param ical iCalendar data
     * @throws ServiceException on error
     */
    public void iCalReply(String ical) throws ServiceException {
        Element req = newRequestElement(MailConstants.ICAL_REPLY_REQUEST);
        Element icalElem = req.addUniqueElement(MailConstants.E_CAL_ICAL);
        icalElem.setText(ical);
        invoke(req);
    }

    public static class ZSendMessageResponse {

        private String mId;

        public ZSendMessageResponse(String id) {
            mId = id;
        }

        public String getId() {
            return mId;
        }

        public void setId(String id) {
            mId = id;
        }
    }

    public static class ZOutgoingMessage {

        public static class AttachedMessagePart {
            private String mMessageId;
            private String mPartName;
            private String mContentId;

            public AttachedMessagePart(String messageId, String partName, String contentId) {
                mMessageId = messageId;
                mPartName = partName;
                mContentId = contentId;
            }

            public String getMessageId()  {return mMessageId; }
            public void setMessageId(String messageId) { mMessageId = messageId; }

            public String getContentId()  {return mContentId; }
            public void setContentId(String contentId) { mContentId = contentId; }

            public String getPartName() { return mPartName; }
            public void setPartName(String partName) { mPartName = partName; }
        }

        public static class MessagePart {
            private String mContentType;
            private String mContent;
            private List<MessagePart> mSubParts;
            private List<AttachedMessagePart> mAttachSubParts;

            /**
             * create a new message part with the given content type and content.
             *
             * @param contentType MIME content type
             * @param content content for the part (null if content-type is multi-part)
             */
            public MessagePart(String contentType, String content) {
                mContent = content;
                mContentType = contentType;
            }
            public MessagePart(String contentType, String content, List<AttachedMessagePart> attachSubParts) {
                mContent = content;
                mContentType = contentType;
                mAttachSubParts = attachSubParts;
            }
            public MessagePart(String contentType, MessagePart... parts) {
                mContent = null;
                mContentType = contentType;
                mSubParts = new ArrayList<MessagePart>();
                for (MessagePart sub : parts)
                    mSubParts.add(sub);
            }

            public String getContentType() { return mContentType; }
            public void setContentType(String contentType) { mContentType = contentType; }

            public String getContent() { return mContent; }
            public void setContent(String content) { mContentType = content; }

            public List<MessagePart> getSubParts() { return mSubParts; }
            public void setSubParts(List<MessagePart> subParts) { mSubParts = subParts; }

            public List<AttachedMessagePart> getAttachSubParts() { return mAttachSubParts; }
            public void setAttachSubParts(List<AttachedMessagePart> attachSubParts) { mAttachSubParts = attachSubParts; }

            public Element toElement(Element parent) {
                Element mpEl = parent.addElement(MailConstants.E_MIMEPART);
                mpEl.addAttribute(MailConstants.A_CONTENT_TYPE, mContentType);
                if (mContent != null)
                    mpEl.addElement(MailConstants.E_CONTENT).setText(mContent);
                if (mSubParts != null) {
                    for (MessagePart subPart : mSubParts) {
                        subPart.toElement(mpEl);
                    }
                }

                if (mAttachSubParts != null) {

                    for (AttachedMessagePart subPart : mAttachSubParts) {
                        Element e = parent.addElement(MailConstants.E_MIMEPART);
                        e.addAttribute(MailConstants.A_CONTENT_ID, subPart.getContentId());
                        Element attach = e.addElement(MailConstants.E_ATTACH);
                        Element el = attach.addElement(MailConstants.E_MIMEPART);
                        el.addAttribute(MailConstants.A_MESSAGE_ID,subPart.getMessageId());
                        el.addAttribute(MailConstants.A_PART,subPart.getPartName());

                    }
                }
                return mpEl;
            }
        }

        private List<ZEmailAddress> mAddresses;
        private String mSubject;
        private String mPriority;
        private String mInReplyTo;
        private MessagePart mMessagePart;
        private String mAttachmentUploadId;
        private List<AttachedMessagePart> mMessagePartsToAttach;
        private List<String> mContactIdsToAttach;
        private List<String> mMessageIdsToAttach;
        private List<String> mDocIdsToAttach;
        private String mOriginalMessageId;
        private String mReplyType;

        public List<ZEmailAddress> getAddresses() { return mAddresses; }
        public void setAddresses(List<ZEmailAddress> addresses) { mAddresses = addresses; }

        public String getAttachmentUploadId() { return mAttachmentUploadId; }
        public void setAttachmentUploadId(String attachmentUploadId) { mAttachmentUploadId = attachmentUploadId; }

        public List<String> getContactIdsToAttach() { return mContactIdsToAttach; }
        public void setContactIdsToAttach(List<String> contactIdsToAttach) { mContactIdsToAttach = contactIdsToAttach; }

        public MessagePart getMessagePart() { return mMessagePart; }
        public void setMessagePart(MessagePart messagePart) { mMessagePart = messagePart; }

        public List<AttachedMessagePart> getMessagePartsToAttach() { return mMessagePartsToAttach; }
        public void setMessagePartsToAttach(List<AttachedMessagePart> messagePartsToAttach) { mMessagePartsToAttach = messagePartsToAttach; }

        public String getOriginalMessageId() { return mOriginalMessageId; }
        public void setOriginalMessageId(String originalMessageId) { mOriginalMessageId = originalMessageId; }

        public String getInReplyTo() { return mInReplyTo; }
        public void setInReplyTo(String inReplyTo) { mInReplyTo = inReplyTo; }

        public String getReplyType() { return mReplyType; }
        public void setReplyType(String replyType) { mReplyType = replyType; }

        public String getSubject() { return mSubject; }
        public void setSubject(String subject) { mSubject = subject; }

        public String getPriority() { return mPriority; }
        public void setPriority(String priority) { mPriority = priority; }

        public List<String> getMessageIdsToAttach() { return mMessageIdsToAttach; }
        public void setMessageIdsToAttach(List<String> messageIdsToAttach) { mMessageIdsToAttach = messageIdsToAttach; }

    public List<String> getDocIdsToAttach() { return mDocIdsToAttach; }
        public void setDocIdsToAttach(List<String> docIdsToAttach) { mDocIdsToAttach = docIdsToAttach; }


        public List<AttachedMessagePart> getInlineMessagePartsToAttach() {
            List<AttachedMessagePart> attachments = new ArrayList<AttachedMessagePart>();
            if(mMessagePartsToAttach == null || mMessagePartsToAttach.size() <= 0) return attachments;
            for (AttachedMessagePart part: mMessagePartsToAttach){
                if(part.getContentId() != null && !part.getContentId().equals("")){
                    attachments.add(part);
                }
            }
            return attachments;
        }
    }

    public Element getMessageElement(Element req, ZOutgoingMessage message, ZMountpoint mountpoint) throws ServiceException {
        Element m = req.addElement(MailConstants.E_MSG);

		String id = message.getOriginalMessageId();
		if (mountpoint != null) {
			// Use normalized id for a shared folder
			int idx = id.indexOf(":");
			if (idx != -1) {
				id = id.substring(idx + 1);
			}
		}
		if (id != null) {
			m.addAttribute(MailConstants.A_ORIG_ID, id);
		}

		if (message.getReplyType() != null)
            m.addAttribute(MailConstants.A_REPLY_TYPE, message.getReplyType());

        if (message.getAddresses() != null) {
            for (ZEmailAddress addr : message.getAddresses()) {
				if (mountpoint != null && addr.getType().equals(ZEmailAddress.EMAIL_TYPE_FROM)) {
					//  For on behalf of messages, replace the from: and add a sender:
					Element e = m.addElement(MailConstants.E_EMAIL);
					e.addAttribute(MailConstants.A_TYPE, ZEmailAddress.EMAIL_TYPE_SENDER);
					e.addAttribute(MailConstants.A_ADDRESS, addr.getAddress());

					e = m.addElement(MailConstants.E_EMAIL);
					e.addAttribute(MailConstants.A_TYPE, ZEmailAddress.EMAIL_TYPE_FROM);
					e.addAttribute(MailConstants.A_ADDRESS, mountpoint.getOwnerDisplayName());
				} else {
					Element e = m.addElement(MailConstants.E_EMAIL);
					e.addAttribute(MailConstants.A_TYPE, addr.getType());
					e.addAttribute(MailConstants.A_ADDRESS, addr.getAddress());
					e.addAttribute(MailConstants.A_PERSONAL, addr.getPersonal());
				}
            }
        }

        if (message.getSubject() != null)
            m.addElement(MailConstants.E_SUBJECT).setText(message.getSubject());

        if (message.getPriority() != null && message.getPriority().length() != 0) {
            m.addAttribute(MailConstants.A_FLAGS, message.getPriority());
        }

        if (message.getInReplyTo() != null)
            m.addElement(MailConstants.E_IN_REPLY_TO).setText(message.getInReplyTo());

        if (message.getMessagePart() != null)
            message.getMessagePart().toElement(m);

        Element attach = null;

        if (message.getAttachmentUploadId() != null) {
            attach = m.addElement(MailConstants.E_ATTACH);
            attach.addAttribute(MailConstants.A_ATTACHMENT_ID, message.getAttachmentUploadId());
        }

        if (message.getMessageIdsToAttach() != null) {
            if (attach == null) attach = m.addElement(MailConstants.E_ATTACH);
            for (String mid: message.getMessageIdsToAttach()) {
                attach.addElement(MailConstants.E_MSG).addAttribute(MailConstants.A_ID, mid);
            }
        }
        if (message.getDocIdsToAttach() != null) {
            if (attach == null) attach = m.addElement(MailConstants.E_ATTACH);
            for (String did: message.getDocIdsToAttach()) {
                attach.addElement(MailConstants.E_DOC).addAttribute(MailConstants.A_ID, did);
            }
        }
        if (message.getMessagePartsToAttach() != null) {
            if (attach == null) attach = m.addElement(MailConstants.E_ATTACH);
            for (AttachedMessagePart part: message.getMessagePartsToAttach()) {
                if(part.getContentId() == null || part.getContentId().equals(""))
                attach.addElement(MailConstants.E_MIMEPART).addAttribute(MailConstants.A_MESSAGE_ID, part.getMessageId()).addAttribute(MailConstants.A_PART, part.getPartName());
            }
        }
        return m;
    }

	private ZMountpoint getMountpoint(ZOutgoingMessage message) throws ServiceException {
		ZMountpoint mountpoint = null;
		String oringinalId = message.getOriginalMessageId();
		if (oringinalId != null) {
			ZGetMessageParams params = new ZGetMessageParams();
			params.setId(oringinalId);
			params.setPart("");
			ZMessage original = getMessage(params);
			ZFolder folder = getFolderById(original.getFolderId());
			if (folder instanceof ZMountpoint) {
				mountpoint = (ZMountpoint) folder;
			}
		}
		return mountpoint;
	}

	public ZSendMessageResponse sendMessage(ZOutgoingMessage message, String sendUid, boolean needCalendarSentByFixup) throws ServiceException {
        Element req = newRequestElement(MailConstants.SEND_MSG_REQUEST);

        if (sendUid != null && sendUid.length() > 0)
            req.addAttribute(MailConstants.A_SEND_UID, sendUid);

        if (needCalendarSentByFixup)
            req.addAttribute(MailConstants.A_NEED_CALENDAR_SENTBY_FIXUP, needCalendarSentByFixup);

		ZMountpoint mountpoint = getMountpoint(message);

		//noinspection UnusedDeclaration
        getMessageElement(req, message, mountpoint);

		String requestedAccountId = mountpoint == null ? null : mountpoint.getOwnerId();
        Element resp = invoke(req, requestedAccountId);
        Element msg = resp.getOptionalElement(MailConstants.E_MSG);
        String id = msg == null ? null : msg.getAttribute(MailConstants.A_ID, null);
        return new ZSendMessageResponse(id);
    }

    /**
     * Saves a message draft.
     * 
     * @param message the message
     * @param existingDraftId id of existing draft or <tt>null</tt>
     * @param folderId folder to save to or <tt>null</tt> to save to the <tt>Drafts</tt> folder
     * 
     * @return the message
     */
    public synchronized ZMessage saveDraft(ZOutgoingMessage message, String existingDraftId, String folderId) throws ServiceException {
        Element req = newRequestElement(MailConstants.SAVE_DRAFT_REQUEST);

		ZMountpoint mountpoint = getMountpoint(message);
        Element m = getMessageElement(req, message, mountpoint);

        if (existingDraftId != null && existingDraftId.length() > 0) {
            mMessageCache.remove(existingDraftId);
            m.addAttribute(MailConstants.A_ID, existingDraftId);
        }

        if (folderId != null)
            m.addAttribute(MailConstants.A_FOLDER, folderId);

		String requestedAccountId = mountpoint == null ? null : mGetInfoResult.getId();
        return new ZMessage(invoke(req, requestedAccountId).getElement(MailConstants.E_MSG), this);
    }

    public static class CheckSpellingResult {
        private boolean mAvailable;
        private List<Misspelling> mMisspellings;

        public CheckSpellingResult(Element response) throws ServiceException  {
            mAvailable = response.getAttributeBool(MailConstants.A_AVAILABLE);
            List<Element> list = response.listElements(MailConstants.E_MISSPELLED);
            mMisspellings = new ArrayList<Misspelling>(list.size());
            for (Element misspelled : list) {
                mMisspellings.add(new Misspelling(misspelled));
            }
        }

        public boolean getIsAvailable() { return mAvailable; }
        public List<Misspelling> getMisspellings() { return mMisspellings; }
    }
    
    public static class Misspelling {
        private String mWord;
        private String[] mSuggestions;
        public Misspelling(Element element) throws ServiceException {
            mWord = element.getAttribute(MailConstants.A_WORD);
            String suggestions = element.getAttribute(MailConstants.A_SUGGESTIONS);
            mSuggestions = suggestions.length() > 0 ? sCOMMA.split(suggestions) : new String[0];
        }
        public String getWord() {
            return mWord;
        }
        public String[] getSuggestions() {
            return mSuggestions;
        }
    }

    public synchronized CheckSpellingResult checkSpelling(String text) throws ServiceException {
        return checkSpelling(text, null, null);
    }

    public synchronized CheckSpellingResult checkSpelling(String text, String dictionary)
    throws ServiceException {
        return checkSpelling(text, dictionary, null);
    }
    
    public synchronized CheckSpellingResult checkSpelling(String text, String dictionary, List<String> ignore)
    throws ServiceException {
        Element req = newRequestElement(MailConstants.CHECK_SPELLING_REQUEST);
        if (dictionary != null) {
            req.addAttribute(MailConstants.A_DICTIONARY, dictionary);
        }
        if (!ListUtil.isEmpty(ignore)) {
            req.addAttribute(MailConstants.A_IGNORE, StringUtil.join(",", ignore));
        }
        req.setText(text);
        Element response = invoke(req);
        return new CheckSpellingResult(response);
    }

    public void createIdentity(ZIdentity identity) throws ServiceException {
        Element req = newRequestElement(AccountConstants.CREATE_IDENTITY_REQUEST);
        identity.toElement(req);
        invoke(req);
    }

    public List<ZIdentity> getIdentities() throws ServiceException {
        Element req = newRequestElement(AccountConstants.GET_IDENTITIES_REQUEST);
        Element resp = invoke(req);
        List<ZIdentity> result = new ArrayList<ZIdentity>();
        for (Element identity : resp.listElements(AccountConstants.E_IDENTITY)) {
            result.add(new ZIdentity(identity));
        }
        return result;
    }

    public void deleteIdentity(String name) throws ServiceException {
        deleteIdentity(IdentityBy.name, name);
    }

    public void deleteIdentity(IdentityBy by, String key) throws ServiceException {
        Element req = newRequestElement(AccountConstants.DELETE_IDENTITY_REQUEST);
        if (by == IdentityBy.name)
            req.addUniqueElement(AccountConstants.E_IDENTITY).addAttribute(AccountConstants.A_NAME, key);
        else if (by == IdentityBy.id)
            req.addUniqueElement(AccountConstants.E_IDENTITY).addAttribute(AccountConstants.A_ID, key);
        invoke(req);
    }

    public void modifyIdentity(ZIdentity identity) throws ServiceException {
        Element req = newRequestElement(AccountConstants.MODIFY_IDENTITY_REQUEST);
        identity.toElement(req);
        invoke(req);
    }

    /**
     * Creates a data source.
     *
     * @return the new data source id
     */
    public String createDataSource(ZDataSource source) throws ServiceException {
        Element req = newRequestElement(MailConstants.CREATE_DATA_SOURCE_REQUEST);
        source.toElement(req);
        return invoke(req).listElements().get(0).getAttribute(MailConstants.A_ID);
    }

    /**
     * Tests a data source.
     * 
     * @return <tt>null</tt> on success, or the error string on failure
     */
    public String testDataSource(ZDataSource source) throws ServiceException {
        Element req = newRequestElement(MailConstants.TEST_DATA_SOURCE_REQUEST);
        source.toElement(req);
        Element resp = invoke(req);
        List<Element> children = resp.listElements();
        if (children.size() == 0) {
            return MailConstants.TEST_DATA_SOURCE_RESPONSE + " has no child elements";
        }
        Element dsEl = children.get(0);
        boolean success = dsEl.getAttributeBool(MailConstants.A_DS_SUCCESS, false);
        if (!success) {
            return resp.getAttribute(MailConstants.A_DS_ERROR, "error");
        } else {
            return null;
        }
    }

    public List<ZDataSource> getAllDataSources() throws ServiceException {
        Element req = newRequestElement(MailConstants.GET_DATA_SOURCES_REQUEST);
        Element response = invoke(req);
        List<ZDataSource> result = new ArrayList<ZDataSource>();
        for (Element ds : response.listElements()) {
            if (ds.getName().equals(MailConstants.E_DS_POP3)) {
                result.add(new ZPop3DataSource(ds));
            } else if (ds.getName().equals(MailConstants.E_DS_IMAP)) {
                result.add(new ZImapDataSource(ds));
            } else if (ds.getName().equals(MailConstants.E_DS_RSS)) {
                result.add(new ZRssDataSource(ds));
            } else if (ds.getName().equals(MailConstants.E_DS_CAL)) {
                result.add(new ZCalDataSource(ds));
            }
        }
        return result;
    }
    
    /**
     * Gets a data source by id.
     * @return the data source, or <tt>null</tt> if no data source with the
     * given id exists
     */
    public ZDataSource getDataSourceById(String id) throws ServiceException {
        for (ZDataSource ds : getAllDataSources()) {
            if (ds.getId().equals(id)) {
                return ds;
            }
        }
        return null;
    }

    public void modifyDataSource(ZDataSource source) throws ServiceException {
        Element req = newRequestElement(MailConstants.MODIFY_DATA_SOURCE_REQUEST);
        source.toElement(req);
        invoke(req);
    }

    public void deleteDataSource(ZDataSource source) throws ServiceException {
        Element req = newRequestElement(MailConstants.DELETE_DATA_SOURCE_REQUEST);
        source.toIdElement(req);
        invoke(req);
    }

    public ZFilterRules getFilterRules() throws ServiceException {
        return getFilterRules(false);
    }

    public synchronized ZFilterRules getFilterRules(boolean refresh) throws ServiceException {
        if (mRules == null || refresh) {
            Element req = newRequestElement(MailConstants.GET_FILTER_RULES_REQUEST);
            mRules = new ZFilterRules(invoke(req).getElement(MailConstants.E_FILTER_RULES));
        }
        return new ZFilterRules(mRules);
    }

    public synchronized void saveFilterRules(ZFilterRules rules) throws ServiceException {
        Element req = newRequestElement(MailConstants.MODIFY_FILTER_RULES_REQUEST);
        rules.toElement(req);
        invoke(req);
        mRules = new ZFilterRules(rules);
    }

    public void deleteDataSource(DataSourceBy by, String key) throws ServiceException {
        Element req = newRequestElement(MailConstants.DELETE_DATA_SOURCE_REQUEST);
        if (by == DataSourceBy.name)
            req.addUniqueElement(MailConstants.E_DS).addAttribute(MailConstants.A_NAME, key);
        else if (by == DataSourceBy.id)
            req.addUniqueElement(MailConstants.E_DS).addAttribute(MailConstants.A_ID, key);
        else
            throw ServiceException.INVALID_REQUEST("must specify data source by id or name", null);
        invoke(req);
    }

    public void importData(List<ZDataSource> sources) throws ServiceException {
        Element req = newRequestElement(MailConstants.IMPORT_DATA_REQUEST);
        for (ZDataSource src : sources) {
            src.toIdElement(req);
        }
        invoke(req);
    }

    public static class ZImportStatus {
        private String mType;
        private boolean mIsRunning;
        private boolean mSuccess;
        private String mError;
        private String mId;

        ZImportStatus(Element e) throws ServiceException {
            mType = e.getName();
            mId = e.getAttribute(MailConstants.A_ID);
            mIsRunning = e.getAttributeBool(MailConstants.A_DS_IS_RUNNING, false);
            mSuccess = e.getAttributeBool(MailConstants.A_DS_SUCCESS, true);
            mError = e.getAttribute(MailConstants.A_DS_ERROR, null);
        }

        public String getType() { return mType; }
        public String getId() { return mId; }
        public boolean isRunning() { return mIsRunning; }
        public boolean getSuccess() { return mSuccess; }
        public String getError() { return mError; }
    }

    public List<ZImportStatus> getImportStatus() throws ServiceException {
        Element req = newRequestElement(MailConstants.GET_IMPORT_STATUS_REQUEST);
        Element response = invoke(req);
        List<ZImportStatus> result = new ArrayList<ZImportStatus>();
        for (Element status : response.listElements()) {
            result.add(new ZImportStatus(status));
        }
        return result;
    }

    public String createDocument(String folderId, String name, String attachmentId) throws ServiceException {
    	Element req = newRequestElement(MailConstants.SAVE_DOCUMENT_REQUEST);
    	Element doc = req.addUniqueElement(MailConstants.E_DOC);
    	doc.addAttribute(MailConstants.A_NAME, name);
    	doc.addAttribute(MailConstants.A_FOLDER, folderId);
    	Element upload = doc.addElement(MailConstants.E_UPLOAD);
    	upload.addAttribute(MailConstants.A_ID, attachmentId);
    	return invoke(req).getElement(MailConstants.E_DOC).getAttribute(MailConstants.A_ID);
    }

    public ZDocument getDocument(String id) throws ServiceException {
        Element req = newRequestElement(MailConstants.GET_ITEM_REQUEST);
        Element item = req.addUniqueElement(MailConstants.E_ITEM);
        item.addAttribute(MailConstants.A_ID, id);
        Element e = invoke(req).getElement(MailConstants.E_DOC);
        return new ZDocument(e);
    }

    public String createWiki(String folderId, String name, String contents) throws ServiceException {
    	Element req = newRequestElement(MailConstants.SAVE_WIKI_REQUEST);
    	Element doc = req.addUniqueElement(MailConstants.E_WIKIWORD);
    	doc.addAttribute(MailConstants.A_NAME, name);
    	doc.addAttribute(MailConstants.A_FOLDER, folderId);
    	doc.setText(contents);
    	return invoke(req).getElement(MailConstants.E_WIKIWORD).getAttribute(MailConstants.A_ID);
    }

    /**
     * modify prefs. The key in the map is the pref name, and the value should be a String[],
     * a Collection of String objects, or a single String/Object.toString.
     * @param prefs prefs to modify
     * @throws ServiceException on error
     */
    @SuppressWarnings("unchecked")
    public void modifyPrefs(Map<String, ? extends Object> prefs) throws ServiceException {
        Element req = newRequestElement(AccountConstants.MODIFY_PREFS_REQUEST);
        for (Map.Entry<String, ? extends Object> entry : prefs.entrySet()){
            Object vo = entry.getValue();
            if (vo instanceof String[]) {
                String[] values = (String[]) vo;
                for (String v : values) {
                    req.addKeyValuePair(entry.getKey(), v, AccountConstants.E_PREF,  AccountConstants.A_NAME);
                }
            } else if (vo instanceof Collection) {
                Collection values = (Collection) vo;
                for (Object v : values) {
                    req.addKeyValuePair(entry.getKey(), v.toString(), AccountConstants.E_PREF,  AccountConstants.A_NAME);
                }
            } else {
                req.addKeyValuePair(entry.getKey(), vo.toString(), AccountConstants.E_PREF,  AccountConstants.A_NAME);
            }
        }
        invoke(req);
    }

    public List<String> getAvailableSkins() throws ServiceException {
        Element req = newRequestElement(AccountConstants.GET_AVAILABLE_SKINS_REQUEST);
        Element resp = invoke(req);
        List<String> result = new ArrayList<String>();
        for (Element skin : resp.listElements(AccountConstants.E_SKIN)) {
            String name = skin.getAttribute(AccountConstants.A_NAME, null);
            if (name != null)
                result.add(name);
        }
        Collections.sort(result);
        return result;
    }

    public enum GalEntryType {
        account, resource, all;

        public static GalEntryType fromString(String s) throws ServiceException {
            try {
                return GalEntryType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid GalType: "+s+", valid values: "+Arrays.asList(GalEntryType.values()), e);
            }
        }
    }

    public static class ZSearchGalResult {
        private boolean mMore;
        private List<ZContact> mContacts;
        private String mQuery;
        private GalEntryType mType;

        public ZSearchGalResult(List<ZContact> contacts, boolean more, String query, GalEntryType type) {
            mMore = more;
            mContacts = contacts;
            mQuery = query;
            mType = type;
        }

        public boolean getHasMore() { return mMore; }

        public List<ZContact> getContacts() { return mContacts; }

        public String getQuery() { return mQuery; }

        public GalEntryType getGalEntryType() { return mType; }
    }

    public ZSearchGalResult searchGal(String query, GalEntryType type) throws ServiceException {
        Element req = newRequestElement(AccountConstants.SEARCH_GAL_REQUEST);
        if (type != null)
        req.addAttribute(AccountConstants.A_TYPE, type.name());
        req.addElement(AccountConstants.E_NAME).setText(query);
        Element resp = invoke(req);
        List<ZContact> contacts = new ArrayList<ZContact>();
        for (Element contact : resp.listElements(MailConstants.E_CONTACT)) {
                contacts.add(new ZContact(contact, true, this));
        }
        return new ZSearchGalResult(contacts, resp.getAttributeBool(AccountConstants.A_MORE, false), query, type);
    }

    public ZSearchGalResult autoCompleteGal(String query, GalEntryType type, int limit) throws ServiceException {
        Element req = newRequestElement(AccountConstants.AUTO_COMPLETE_GAL_REQUEST);
        if (type != null)
        req.addAttribute(AccountConstants.A_TYPE, type.name());
        req.addAttribute(AccountConstants.A_LIMIT, limit);
        req.addElement(AccountConstants.E_NAME).setText(query);
        Element resp = invoke(req);
        List<ZContact> contacts = new ArrayList<ZContact>();
        for (Element contact : resp.listElements(MailConstants.E_CONTACT)) {
                contacts.add(new ZContact(contact, true, this));
        }
        return new ZSearchGalResult(contacts, resp.getAttributeBool(AccountConstants.A_MORE, false), query, type);
    }

    public static class ZApptSummaryResult {
        private String mFolderId;
        private List<ZAppointmentHit> mAppointments;
        private long mStart;
        private long mEnd;
        private TimeZone mTimeZone;
        private String mQuery;

        ZApptSummaryResult(long start, long end, String folderId, TimeZone timeZone, List<ZAppointmentHit> appointments, String query) {
            mFolderId = folderId;
            mAppointments = appointments;
            mStart = start;
            mEnd = end;
            mTimeZone = timeZone;
            mQuery = query;
        }

        public String getFolderId() {
            return mFolderId;
        }

        public TimeZone getTimeZone() {
            return mTimeZone;
        }

        public long getStart() { return mStart; }
        public long getEnd() { return mEnd; }

        public List<ZAppointmentHit> getAppointments() {
            return mAppointments;
        }

        public String getQuery() {
            return mQuery;
        }
    }

    /**
     * clear all entries in the appointment summary cache. This is normally handled automatically
     * via notifications, except in the case of shared calendars.
     */
    public synchronized void clearApptSummaryCache() {
        mApptSummaryCache.clear();
    }

    public synchronized Set<String> getMiniCal(long startMsec, long endMsec, String folderIds[]) throws ServiceException {
       Set<String> result = mApptSummaryCache.getMiniCal(startMsec, endMsec,folderIds);

        if (result == null) {
            Element req = newRequestElement(MailConstants.GET_MINI_CAL_REQUEST);
            req.addAttribute(MailConstants.A_CAL_START_TIME, startMsec);
            req.addAttribute(MailConstants.A_CAL_END_TIME, endMsec);
            for (String folderId : folderIds) {
                Element folderElem = req.addElement(MailConstants.E_FOLDER);
                folderElem.addAttribute(MailConstants.A_ID, folderId);
            }
            Element resp = invoke(req);
            result = new HashSet<String>();
            for (Element date : resp.listElements(MailConstants.E_CAL_MINICAL_DATE)) {
                result.add(date.getTextTrim());
            }
            mApptSummaryCache.putMiniCal(result, startMsec, endMsec, folderIds);
        }
        return result;
    }

    /**
     * @param query optional seach query to limit appts returend
     * @param startMsec starting time of range, in msecs
     * @param endMsec ending time of range, in msecs
     * @param folderIds list of folder ids
     * @param timeZone TimeZone used to correct allday appts
     * @param types ZSearchParams.TYPE_APPOINTMENT and/or ZSearchParams.TYPE_TASK. If null, TYPE_APPOINTMENT is used.
     * @return list of appts within the specified range
     * @throws ServiceException on error
     */
    public synchronized List<ZApptSummaryResult> getApptSummaries(String query, long startMsec, long endMsec, String folderIds[], TimeZone timeZone, String types) throws ServiceException {

        if (types == null) types = ZSearchParams.TYPE_APPOINTMENT;
        if (query == null) query = "";
        if (folderIds == null || folderIds.length == 0)
            folderIds = new String[] { ZFolder.ID_CALENDAR };

        List<ZApptSummaryResult> summaries = new ArrayList<ZApptSummaryResult>();
        List<String> idsToFetch = new ArrayList<String>(folderIds.length);

        for (String folderId : folderIds) {
            if (folderId == null) folderId = ZFolder.ID_CALENDAR;
            ZApptSummaryResult cached = mApptSummaryCache.get(startMsec, endMsec, folderId, timeZone, query);
            if (cached == null) {
                ZFolder folder = getFolderById(folderId);
                /*
                 * Folder cache might not always result in a hit, still go ahead and fetch the appointments.
                 */
                if ((folder == null) || !(folder instanceof ZMountpoint) || (folder instanceof ZMountpoint && folder.getEffectivePerms() != null)) {
                    idsToFetch.add(folderId);
                }
            } else {
                summaries.add(cached);
            }
        }

        Map<String, ZApptSummaryResult> folder2List = new HashMap<String, ZApptSummaryResult>();
        Map<String, String> folderIdMapper = new HashMap<String, String>();

        String targetId = mTransport.getTargetAcctId();

        if (!idsToFetch.isEmpty()) {
            StringBuilder searchQuery = new StringBuilder();
            searchQuery.append("(");
            for (String folderId : idsToFetch) {
                if (searchQuery.length() > 1) searchQuery.append(" or ");
                searchQuery.append("inid:").append("\""+folderId+"\"");
                //folder2List.
                List<ZAppointmentHit> appts = new ArrayList<ZAppointmentHit>();
                ZApptSummaryResult result = new ZApptSummaryResult(startMsec, endMsec, folderId, timeZone, appts, query);
                summaries.add(result);
                folder2List.put(folderId, result);
                ZFolder folder = targetId != null ? null : getFolderById(folderId);
                if (folder != null && folder instanceof ZMountpoint) {
                    folderIdMapper.put(((ZMountpoint)folder).getCanonicalRemoteId(), folderId);
                } else if (targetId != null) {
                    folderIdMapper.put(mTransport.getTargetAcctId()+":"+folderId, folderId);
                    folderIdMapper.put(folderId, folderId);
                } else {
                    folderIdMapper.put(folderId, folderId);
                }
            }
            searchQuery.append(")");

            if (query.length() > 0) {
                searchQuery.append("AND (").append(query).append(")");
            }

            ZSearchParams params = new ZSearchParams(searchQuery.toString());
            params.setCalExpandInstStart(startMsec);
            params.setCalExpandInstEnd(endMsec);
            params.setTypes(types);
            params.setLimit(2000);
            params.setSortBy(SearchSortBy.none);
            params.setTimeZone(timeZone);
            
            int offset = 0;
            int n = 0;
            // really while(true), but add in a safety net?
            while (n++ < 100) {
                params.setOffset(offset);
                ZSearchResult result = search(params);
                for (ZSearchHit hit : result.getHits()) {
                    offset++;
                    if (hit instanceof ZAppointmentHit) {
                        ZAppointmentHit as = (ZAppointmentHit) hit;
                        String fid = folderIdMapper.get(as.getFolderId());
                        if (fid == null) fid = as.getFolderId();
                        ZApptSummaryResult r = folder2List.get(fid);
                        if (r == null) {
                            List<ZAppointmentHit> appts = new ArrayList<ZAppointmentHit>();
                            r = new ZApptSummaryResult(startMsec, endMsec, fid, timeZone, appts, query);
                            summaries.add(r);
                            folder2List.put(fid, r);
                        }
                        r.getAppointments().add(as);
                    }
                }
                List<ZSearchHit> hits = result.getHits();
                if (result.hasMore() && !hits.isEmpty()) {
                    params.setOffset(offset);
                } else {
                    break;
                }
            }
            for (ZApptSummaryResult r : folder2List.values())
                mApptSummaryCache.add(r, timeZone);
        }
        return summaries;
    }

    public static class ZAppointmentResult {

        private String mCalItemId;
        private String mInviteId;

        public ZAppointmentResult(Element response) {
            mCalItemId = response.getAttribute(MailConstants.A_CAL_ID, null);
            mInviteId = response.getAttribute(MailConstants.A_CAL_INV_ID, null);
        }

        public String getCalItemId() {
            return mCalItemId;
        }

        public String getInviteId() {
            return mInviteId;
        }
    }

    public ZAppointmentResult createAppointment(String folderId, String flags, ZOutgoingMessage message, ZInvite invite, String optionalUid) throws ServiceException {
        Element req = newRequestElement(MailConstants.CREATE_APPOINTMENT_REQUEST);

        //noinspection UnusedDeclaration
        Element mEl = getMessageElement(req, message, null);

        if (flags != null)
            mEl.addAttribute(MailConstants.A_FLAGS, flags);

        if (folderId != null)
            mEl.addAttribute(MailConstants.A_FOLDER, folderId);

        Element invEl = invite.toElement(mEl);
        if (optionalUid != null)
            invEl.addAttribute(MailConstants.A_UID, optionalUid);

        return new ZAppointmentResult(invoke(req));
    }

    public ZAppointmentResult createAppointmentException(String id, String component, ZDateTime exceptionId, ZOutgoingMessage message, ZInvite invite, String optionalUid) throws ServiceException {
        Element req = newRequestElement(MailConstants.CREATE_APPOINTMENT_EXCEPTION_REQUEST);

        req.addAttribute(MailConstants.A_ID, id);
        req.addAttribute(MailConstants.E_INVITE_COMPONENT, component);

        Element mEl = getMessageElement(req, message, null);

        Element invEl = invite.toElement(mEl);
        Element compEl = invEl.getElement(MailConstants.E_INVITE_COMPONENT);
        exceptionId.toElement(MailConstants.E_CAL_EXCEPTION_ID, compEl);

        if (optionalUid != null)
            invEl.addAttribute(MailConstants.A_UID, optionalUid);

        return new ZAppointmentResult(invoke(req));
    }

    public ZAppointmentResult modifyAppointment(String id, String component, ZDateTime exceptionId, ZOutgoingMessage message, ZInvite invite) throws ServiceException {
        Element req = newRequestElement(MailConstants.MODIFY_APPOINTMENT_REQUEST);

        req.addAttribute(MailConstants.A_ID, id);
        req.addAttribute(MailConstants.E_INVITE_COMPONENT, component);

        Element mEl = getMessageElement(req, message, null);

        Element invEl = invite.toElement(mEl);

        if (exceptionId != null) {
            Element compEl = invEl.getElement(MailConstants.E_INVITE_COMPONENT);
            exceptionId.toElement(MailConstants.E_CAL_EXCEPTION_ID, compEl);
        }

        return new ZAppointmentResult(invoke(req));
    }

    public enum CancelRange { THISANDFUTURE, THISANDPRIOR }

    public void cancelAppointment(String id, String component, ZTimeZone tz, ZDateTime instance, CancelRange range, ZOutgoingMessage message)  throws ServiceException {
        Element req = newRequestElement(MailConstants.CANCEL_APPOINTMENT_REQUEST);

        req.addAttribute(MailConstants.A_ID, id);
        req.addAttribute(MailConstants.E_INVITE_COMPONENT, component);

        if (tz != null) tz.toElement(req);

        if (instance != null) {
            Element instEl = instance.toElement(MailConstants.E_INSTANCE, req);
            if (range != null)
                instEl.addAttribute(MailConstants.A_CAL_RANGE, range.name());
        }

        if (message != null) getMessageElement(req, message, null);

        mMessageCache.remove(id);

        invoke(req);
    }

    public static class ZSendInviteReplyResult {

        public static final String STATUS_OK = "OK";
        public static final String STATUS_OLD = "OLD";
        public static final String STATUS_ALREADY_REPLIED = "ALREADY-REPLIED";
        public static final String STATUS_FAIL = "FAIL";

        private String mStatus;

        public ZSendInviteReplyResult(Element response) {
            mStatus = response.getAttribute(MailConstants.A_STATUS, "OK");
        }

        public String getStatus() {
            return mStatus;
        }

        public boolean isOk() { return mStatus.equals(STATUS_OK); }
        public boolean isOld() { return mStatus.equals(STATUS_OLD); }
        public boolean isAlreadyReplied() { return mStatus.equals(STATUS_ALREADY_REPLIED); }
        public boolean isFail() { return mStatus.equals(STATUS_FAIL); }
    }

    public enum ReplyVerb {

        ACCEPT, COMPLETED, DECLINE, DELEGATED, TENTATIVE;

        public static ReplyVerb fromString(String s) throws ServiceException {
            try {
                return ReplyVerb.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid reply verb: "+s+", valid values: "+Arrays.asList(ReplyVerb.values()), e);
            }
        }
    }

    public ZSendInviteReplyResult sendInviteReply(String id, String component, ReplyVerb verb, boolean updateOrganizer, ZTimeZone tz, ZDateTime instance, ZOutgoingMessage message)  throws ServiceException {
        Element req = newRequestElement(MailConstants.SEND_INVITE_REPLY_REQUEST);

        req.addAttribute(MailConstants.A_ID, id);
        req.addAttribute(MailConstants.A_CAL_COMPONENT_NUM, component);
        req.addAttribute(MailConstants.A_VERB, verb.name());
        req.addAttribute(MailConstants.A_CAL_UPDATE_ORGANIZER, updateOrganizer);

        if (tz != null) tz.toElement(req);

        if (instance != null) {
            instance.toElement(MailConstants.E_CAL_EXCEPTION_ID, req);
        }

        if (message != null) getMessageElement(req, message, null);

        mMessageCache.remove(id);

        return new ZSendInviteReplyResult(invoke(req));
    }

    public ZAppointment getAppointment(String id) throws ServiceException {
        Element req = newRequestElement(MailConstants.GET_APPOINTMENT_REQUEST);
        req.addAttribute(MailConstants.A_ID, id);
        req.addAttribute(MailConstants.A_SYNC, true);
        return new ZAppointment(invoke(req));
    }

    public void clearMessageCache() {
        mMessageCache.clear();
    }

    public void clearContactCache() {
        mContactCache.clear();
    }

    public static class ZImportAppointmentsResult {

        private String mIds;
        private long mCount;

        public ZImportAppointmentsResult(Element response) throws ServiceException {
            mIds = response.getAttribute(MailConstants.A_ID, null);
            mCount = response.getAttributeLong(MailConstants.A_NUM);
        }

        public String getIds() {
            return mIds;
        }

        public long getCount() {
            return mCount;
        }
    }

    public static final String APPOINTMENT_IMPORT_TYPE_ICS= "ics";

    public ZImportAppointmentsResult importAppointments(String folderId, String type, String attachmentId) throws ServiceException {
    	Element req = newRequestElement(MailConstants.IMPORT_APPOINTMENTS_REQUEST);
    	req.addAttribute(MailConstants.A_CONTENT_TYPE, type);
    	req.addAttribute(MailConstants.A_FOLDER, folderId);
    	Element content = req.addElement(MailConstants.E_CONTENT);
    	content.addAttribute(MailConstants.A_ATTACHMENT_ID, attachmentId);
    	return new ZImportAppointmentsResult(invoke(req).getElement(MailConstants.E_APPOINTMENT));
    }

    public static class ZGetFreeBusyResult {
        private String mId;
        private List<ZFreeBusyTimeSlot> mTimeSlots;

        public ZGetFreeBusyResult(String id, List<ZFreeBusyTimeSlot> timeSlots) {
            mId = id;
            mTimeSlots = timeSlots;
        }

        public String getId() { return mId; }
        public List<ZFreeBusyTimeSlot> getTimeSlots() { return mTimeSlots; }
    }

    public enum ZFreeBusySlotType {
        FREE, BUSY, TENTATIVE, UNAVAILABLE, NO_DATA, OUT_OF_OFFICE;

        public static ZFreeBusySlotType fromString(String s) throws ServiceException {
            try {
                return ZFreeBusySlotType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid free busy slot type: "+s+", valid values: "+Arrays.asList(ZFreeBusySlotType.values()), e);
            }
        }
    }

    public static class ZFreeBusyTimeSlot {
        private ZFreeBusySlotType mType;
        private long mStart;
        private long mEnd;

        public ZFreeBusyTimeSlot(ZFreeBusySlotType type, long start, long end) {
            mType = type;
            mStart = start;
            mEnd = end;
        }

        public ZFreeBusySlotType getType() { return mType; }
        public long getStartTime() { return mStart; }
        public long getEndTime() { return mEnd; }
    }

    public List<ZGetFreeBusyResult> getFreeBusy(String email, long startTime, long endTime, int folder) throws ServiceException {
        Element req = newRequestElement(MailConstants.GET_FREE_BUSY_REQUEST);
        req.addAttribute(MailConstants.A_CAL_START_TIME, startTime);
        req.addAttribute(MailConstants.A_CAL_END_TIME, endTime);
        Element userElem = req.addElement(MailConstants.E_FREEBUSY_USER);
        userElem.addAttribute(MailConstants.A_NAME, email);
        if (folder != FreeBusyQuery.CALENDAR_FOLDER_ALL)
            userElem.addAttribute(MailConstants.A_FOLDER, folder);
        Element resp = invoke(req);
        List<ZGetFreeBusyResult> result = new ArrayList<ZGetFreeBusyResult>();
        for (Element user : resp.listElements(MailConstants.E_FREEBUSY_USER)) {
            String userId = user.getAttribute(MailConstants.A_ID);
            List<ZFreeBusyTimeSlot> slots = new ArrayList<ZFreeBusyTimeSlot>();
            for (Element slot : user.listElements()) {
                ZFreeBusySlotType type;
                if (slot.getName().equals(MailConstants.E_FREEBUSY_FREE)) {
                    type = ZFreeBusySlotType.FREE;
                } else if (slot.getName().equals(MailConstants.E_FREEBUSY_BUSY)) {
                    type = ZFreeBusySlotType.BUSY;
                } else if (slot.getName().equals(MailConstants.E_FREEBUSY_BUSY_TENTATIVE)) {
                    type = ZFreeBusySlotType.TENTATIVE;
                } else if (slot.getName().equals(MailConstants.E_FREEBUSY_BUSY_UNAVAILABLE)) {
                    type = ZFreeBusySlotType.UNAVAILABLE;
                } else if (slot.getName().equals(MailConstants.E_FREEBUSY_BUSY_OUT_OF_OFFICE)) {
                    type = ZFreeBusySlotType.OUT_OF_OFFICE;
                } else {
                    type = ZFreeBusySlotType.NO_DATA;
                }
                slots.add(new ZFreeBusyTimeSlot(
                        type,
                        slot.getAttributeLong(MailConstants.A_CAL_START_TIME),
                        slot.getAttributeLong(MailConstants.A_CAL_END_TIME)));
            }
            result.add(new ZGetFreeBusyResult(userId, slots));
        }
        return result;
    }

    public List<ZAppointmentHit> createAppointmentHits(List<ZFreeBusyTimeSlot> slots) {
        List<ZAppointmentHit> result = new ArrayList<ZAppointmentHit>();
        for (ZFreeBusyTimeSlot slot : slots) {
            switch (slot.getType()) {
                case BUSY:
                case TENTATIVE:
                case UNAVAILABLE:
                case OUT_OF_OFFICE:    
                    result.add(new ZAppointmentHit(slot));
                    break;
            }
        }
        return result;
    }

    /* tasks */

    public ZAppointmentResult createTask(String folderId, String flags, ZOutgoingMessage message, ZInvite invite, String optionalUid) throws ServiceException {
        Element req = newRequestElement(MailConstants.CREATE_TASK_REQUEST);

        //noinspection UnusedDeclaration
        Element mEl = getMessageElement(req, message, null);

        if (flags != null)
            mEl.addAttribute(MailConstants.A_FLAGS, flags);

        if (folderId != null)
            mEl.addAttribute(MailConstants.A_FOLDER, folderId);

        Element invEl = invite.toElement(mEl);
        if (optionalUid != null)
            invEl.addAttribute(MailConstants.A_UID, optionalUid);

        return new ZAppointmentResult(invoke(req));
    }

    public ZAppointmentResult createTaskException(String id, String component, ZDateTime exceptionId, ZOutgoingMessage message, ZInvite invite, String optionalUid) throws ServiceException {
        Element req = newRequestElement(MailConstants.CREATE_TASK_EXCEPTION_REQUEST);

        req.addAttribute(MailConstants.A_ID, id);
        req.addAttribute(MailConstants.E_INVITE_COMPONENT, component);

        Element mEl = getMessageElement(req, message, null);

        Element invEl = invite.toElement(mEl);
        Element compEl = invEl.getElement(MailConstants.E_INVITE_COMPONENT);
        exceptionId.toElement(MailConstants.E_CAL_EXCEPTION_ID, compEl);

        if (optionalUid != null)
            invEl.addAttribute(MailConstants.A_UID, optionalUid);

        return new ZAppointmentResult(invoke(req));
    }

    public ZAppointmentResult modifyTask(String id, String component, ZDateTime exceptionId, ZOutgoingMessage message, ZInvite invite) throws ServiceException {
        Element req = newRequestElement(MailConstants.MODIFY_TASK_REQUEST);

        req.addAttribute(MailConstants.A_ID, id);
        req.addAttribute(MailConstants.E_INVITE_COMPONENT, component);

        Element mEl = getMessageElement(req, message, null);

        Element invEl = invite.toElement(mEl);

        if (exceptionId != null) {
            Element compEl = invEl.getElement(MailConstants.E_INVITE_COMPONENT);
            exceptionId.toElement(MailConstants.E_CAL_EXCEPTION_ID, compEl);
        }

        return new ZAppointmentResult(invoke(req));
    }

    public void cancelTask(String id, String component, ZTimeZone tz, ZDateTime instance, CancelRange range, ZOutgoingMessage message)  throws ServiceException {
        Element req = newRequestElement(MailConstants.CANCEL_TASK_REQUEST);

        req.addAttribute(MailConstants.A_ID, id);
        req.addAttribute(MailConstants.E_INVITE_COMPONENT, component);

        if (tz != null) tz.toElement(req);

        if (instance != null) {
            Element instEl = instance.toElement(MailConstants.E_INSTANCE, req);
            if (range != null)
                instEl.addAttribute(MailConstants.A_CAL_RANGE, range.name());
        }

        if (message != null) getMessageElement(req, message, null);

        mMessageCache.remove(id);

        invoke(req);
    }

	public synchronized List<ZPhoneAccount> getAllPhoneAccounts() throws ServiceException {
        if (mPhoneAccounts == null) {
			ArrayList<ZPhoneAccount> accounts = new ArrayList<ZPhoneAccount>();
			mPhoneAccountMap = new HashMap<String, ZPhoneAccount>();
            Element req = newRequestElement(VoiceConstants.GET_VOICE_INFO_REQUEST);
            Element response = invoke(req);
			Element storePrincipalEl = response.getElement(VoiceConstants.E_STOREPRINCIPAL);
			mVoiceStorePrincipal = storePrincipalEl.clone();
			List<Element> phoneElements = response.listElements(VoiceConstants.E_PHONE);
            for (Element element : phoneElements) {
                ZPhoneAccount account = new ZPhoneAccount(element, this);
                accounts.add(account);
                mPhoneAccountMap.put(account.getPhone().getName(), account);
            }
			mPhoneAccounts = Collections.unmodifiableList(accounts);
		}
        return mPhoneAccounts;
    }

	private void setVoiceStorePrincipal(Element req) {
		req.addElement(mVoiceStorePrincipal.clone());
	}

	public ZPhoneAccount getPhoneAccount(String name) throws ServiceException {
        getAllPhoneAccounts(); // Make sure they're loaded.
        return mPhoneAccountMap.get(name);
    }

    public String uploadVoiceMail(String phone, String id) throws ServiceException {
        Element req = newRequestElement(VoiceConstants.UPLOAD_VOICE_MAIL_REQUEST);
		setVoiceStorePrincipal(req);
		Element actionEl = req.addElement(VoiceConstants.E_VOICEMSG);
        actionEl.addAttribute(MailConstants.A_ID, id);
        actionEl.addAttribute(VoiceConstants.A_PHONE, phone);
        Element response = invoke(req);
        return response.getElement(VoiceConstants.E_UPLOAD).getAttribute(MailConstants.A_ID);
    }

    public void loadCallFeatures(ZCallFeatures features) throws ServiceException {
        Element req = newRequestElement(VoiceConstants.GET_VOICE_FEATURES_REQUEST);
		setVoiceStorePrincipal(req);
        Element phoneEl = req.addElement(VoiceConstants.E_PHONE);
        phoneEl.addAttribute(MailConstants.A_NAME, features.getPhone().getName());
        Collection<ZCallFeature> featureList = features.getSubscribedFeatures();
        for (ZCallFeature feature : featureList) {
            phoneEl.addElement(feature.getName());
        }
        Element response = invoke(req);

        phoneEl = response.getElement(VoiceConstants.E_PHONE);
        for (ZCallFeature feature : featureList) {
            String name = feature.getName();
            Element element = phoneEl.getOptionalElement(name);
			if (element != null) {
				feature.fromElement(element);
			}
		}
    }

    public void saveCallFeatures(ZCallFeatures newFeatures) throws ServiceException {
        // Build up the soap request.
        Element req = newRequestElement(VoiceConstants.MODIFY_VOICE_FEATURES_REQUEST);
		setVoiceStorePrincipal(req);
        Element phoneEl = req.addElement(VoiceConstants.E_PHONE);
        phoneEl.addAttribute(MailConstants.A_NAME, newFeatures.getPhone().getName());
        Collection<ZCallFeature> list = newFeatures.getAllFeatures();
        for (ZCallFeature newFeature : list) {
            Element element = phoneEl.addElement(newFeature.getName());
            newFeature.toElement(element);
        }
        invoke(req);

        // Copy new data into cache.
        ZPhoneAccount account = getPhoneAccount(newFeatures.getPhone().getName());
        ZCallFeatures oldFeatures = account.getCallFeatures();
        for (ZCallFeature newFeature : list) {
            ZCallFeature oldFeature = oldFeatures.getFeature(newFeature.getName());
            oldFeature.assignFrom(newFeature);
        }
    }

    public ZActionResult trashVoiceMail(String phone, String id) throws ServiceException {
		return moveVoiceMail(phone, id, VoiceConstants.FID_TRASH);
	}

    public ZActionResult moveVoiceMail(String phone, String id, int folderId) throws ServiceException {
		ZActionResult result = doAction(voiceAction("move", phone, id, folderId));
		ZModifyEvent event = new ZModifyVoiceMailItemFolderEvent(Integer.toString(folderId));
		handleEvent(event);
		refreshVoiceMailInbox(phone);
		return result;
	}

    public ZActionResult emptyVoiceMailTrash(String phone, String folderId) throws ServiceException {
		ZActionResult result = doAction(voiceAction("empty", phone, folderId, 0));

		// Don't use a delete event, since it deals with the ids of the deleted items and we don't have those.
		// Instead just clear the cache that we know know of that might need to be rebuilt.
		mSearchPagerCache.clear(null);
		return result;
	}

	/** Makes a server call to get updated message/unheard counts for the folders */ 
	private void refreshVoiceMailInbox(String phone) throws ServiceException {
		ZPhoneAccount account = getPhoneAccount(phone);
		if (account == null) {
			return;
		}

		Element req = newRequestElement(VoiceConstants.GET_VOICE_FOLDER_REQUEST);
		setVoiceStorePrincipal(req);
		Element phoneEl = req.addElement(VoiceConstants.E_PHONE);
		phoneEl.addAttribute(MailConstants.A_NAME, phone);
		Element response = invoke(req);

		Element phoneResponse = response.getElement(VoiceConstants.E_PHONE);
		if (phoneResponse != null) {
			ZFolder rootFolder = account.getRootFolder();
			Element rootEl = phoneResponse.getElement(MailConstants.E_FOLDER);
			for (Element childEl : rootEl.listElements(MailConstants.E_FOLDER)) {
				String name = childEl.getAttribute(MailConstants.A_NAME);
				ZFolder childFolder = rootFolder.getSubFolderByPath(name);
				if (childFolder != null) {
					childFolder.setUnreadCount((int) childEl.getAttributeLong(MailConstants.A_UNREAD, 0));
					childFolder.setMessageCount((int) childEl.getAttributeLong(MailConstants.A_NUM, 0));
				}
			}
		}
	}

	public ZActionResult markVoiceMailHeard(String phone, String idList, boolean heard) throws ServiceException {
        String op = heard ? "read" : "!read";
		ZActionResult result = doAction(voiceAction(op, phone, idList, 0));
		int changeCount = 0;
		boolean needRefresh = false;
		for (String id : sCOMMA.split(idList)) {
			ZModifyVoiceMailItemEvent event = new ZModifyVoiceMailItemEvent(id, heard);
			handleEvent(event);
			if (event.getMadeChange()) {
				changeCount++;
			} else {
				needRefresh = true;
			}
		}
		if (needRefresh) {
			refreshVoiceMailInbox(phone);
		} else if (changeCount > 0) {
			ZPhoneAccount account = getPhoneAccount(phone);
			ZFolder inbox = account.getRootFolder().getSubFolderByPath(VoiceConstants.FNAME_VOICEMAILINBOX);
			int diff = heard ? -changeCount : changeCount;
			inbox.setUnreadCount(inbox.getUnreadCount() + diff);
		}
		return result;
    }

    private Element voiceAction(String op, String phone, String id, int folderId) {
        Element req = newRequestElement(VoiceConstants.VOICE_MSG_ACTION_REQUEST);
		setVoiceStorePrincipal(req);
        Element actionEl = req.addElement(MailConstants.E_ACTION);
        actionEl.addAttribute(MailConstants.A_ID, id);
        actionEl.addAttribute(MailConstants.A_OPERATION, op);
        actionEl.addAttribute(VoiceConstants.A_PHONE, phone);
        if (folderId != 0) {
            actionEl.addAttribute(MailConstants.A_FOLDER, Integer.toString(folderId) + '-' + phone);
        }
        return actionEl;
    }

	public synchronized ZContactByPhoneCache.ContactPhone getContactByPhone(String phone) throws ServiceException {
		if (mContactByPhoneCache == null) {
			mContactByPhoneCache = new ZContactByPhoneCache();
			mHandlers.add(mContactByPhoneCache);
		}
		return mContactByPhoneCache.getByPhone(phone, this);
	}

	private void updateSigs() {
        try {
            if (mGetInfoResult != null)
                mGetInfoResult.setSignatures(getSignatures());
        } catch (ServiceException e) {
            /* ignore */
        }
    }
    public synchronized List<String> saveAttachmentsToBriefcase(String mid, String[] partIds, String folderId ) throws ServiceException {
        if(partIds == null || partIds.length <= 0 ){
            return null;
        }
        List<String> docIds = new ArrayList<String>();
        for(String pid: partIds){               //!TODO We should do batch request for performance
            Element req = newRequestElement(MailConstants.SAVE_DOCUMENT_REQUEST);
            Element doc = req.addElement(MailConstants.E_DOC).addAttribute(MailConstants.A_FOLDER,folderId);
            Element m = doc.addElement(MailConstants.E_MSG).addAttribute(MailConstants.A_ID,mid);
            m.addAttribute(MailConstants.A_PART,pid);
            Element rDoc = invoke(req).getElement(MailConstants.E_DOC);
            if(rDoc == null){
                continue;
            }
            docIds.add(rDoc.getAttribute(MailConstants.A_ID));
        }
        return docIds;
    }
    public synchronized String createSignature(ZSignature signature) throws ServiceException {
        Element req = newRequestElement(AccountConstants.CREATE_SIGNATURE_REQUEST);
        signature.toElement(req);
        String id = invoke(req).getElement(AccountConstants.E_SIGNATURE).getAttribute(AccountConstants.A_ID);
        updateSigs();
        return id;
    }

    public List<ZSignature> getSignatures() throws ServiceException {
        Element req = newRequestElement(AccountConstants.GET_SIGNATURES_REQUEST);
        Element resp = invoke(req);
        List<ZSignature> result = new ArrayList<ZSignature>();
        for (Element signature : resp.listElements(AccountConstants.E_SIGNATURE)) {
            result.add(new ZSignature(signature));
        }
        return result;
    }

    public synchronized void deleteSignature(String id) throws ServiceException {
        Element req = newRequestElement(AccountConstants.DELETE_SIGNATURE_REQUEST);
        req.addElement(AccountConstants.E_SIGNATURE).addAttribute(AccountConstants.A_ID, id);
        invoke(req);
        updateSigs();
    }

    public synchronized void modifySignature(ZSignature signature) throws ServiceException {
        Element req = newRequestElement(AccountConstants.MODIFY_SIGNATURE_REQUEST);
        signature.toElement(req);
        invoke(req);
        updateSigs();
    }
    
    public List<ZAce> getPermission(String[] rights) throws ServiceException {
        Element req = newRequestElement(MailConstants.GET_PERMISSION_REQUEST);
        if (rights != null && rights.length > 0) {
            for (String right : rights)
                req.addElement(MailConstants.E_ACE).addAttribute(MailConstants.A_RIGHT, right);
        }
        Element resp = invoke(req);
        List<ZAce> result = new ArrayList<ZAce>();
        for (Element ace : resp.listElements(MailConstants.E_ACE)) {
            result.add(new ZAce(ace));
        }
        return result;
    }
    
    public List<ZAce> grantPermission(ZAce ace) throws ServiceException {
        Element req = newRequestElement(MailConstants.GRANT_PERMISSION_REQUEST);
        ace.toElement(req);
        Element resp = invoke(req);
        List<ZAce> result = new ArrayList<ZAce>();
        for (Element a : resp.listElements(MailConstants.E_ACE)) {
            result.add(new ZAce(a));
        }
        return result;
    }

    public List<ZAce> revokePermission(ZAce ace) throws ServiceException {
        Element req = newRequestElement(MailConstants.REVOKE_PERMISSION_REQUEST);
        ace.toElement(req);
        Element resp = invoke(req);
        List<ZAce> result = new ArrayList<ZAce>();
        for (Element a : resp.listElements(MailConstants.E_ACE)) {
            result.add(new ZAce(a));
        }
        return result;
    }

    public String toString() {
        try {
            return String.format("[ZMailbox %s]", getName());
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        try {
            ZJSONObject jo = new ZJSONObject();
            jo.put("name", getName());
            jo.put("size", mSize);
            jo.put("hasTags", hasTags());
            jo.put("userRoot", getUserRoot());
            jo.put("tags", getAllTags());
            return jo;
        } catch (ServiceException se) {
            throw new JSONException(se);
        }
    }
    
    public String dump() {
        return ZJSONObject.toString(this);
    }

    public ZSearchContext searchContext(ZSearchParams params) {
        return new ZSearchContext(params, this);
    }

    public ZSearchContext searchContext(String query) {
        return new ZSearchContext(new ZSearchParams(query), this);
    }

    private static final int ADMIN_PORT = LC.zimbra_admin_service_port.intValue();
    
    public static String resolveUrl(String url, boolean isAdmin) throws ZClientException {
        try {
            URI uri = new URI(url);

            if (isAdmin && uri.getPort() == -1) {
                uri = new URI("https", uri.getUserInfo(), uri.getHost(), ADMIN_PORT, uri.getPath(), uri.getQuery(), uri.getFragment());
                url = uri.toString();
            }

            String service = (uri.getPort() == ADMIN_PORT) ? AdminConstants.ADMIN_SERVICE_URI : AccountConstants.USER_SERVICE_URI;
            if (uri.getPath() == null || uri.getPath().length() <= 1) {
                if (url.charAt(url.length()-1) == '/')
                    url = url.substring(0, url.length()-1) + service;
                else
                    url = url + service;
            }
            return url;
        } catch (URISyntaxException e) {
            throw ZClientException.CLIENT_ERROR("invalid URL: "+url, e);
        }
    }
}

