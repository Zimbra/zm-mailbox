/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2018 Synacor, Inc.
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

package com.zimbra.client;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.dom4j.QName;
import org.json.JSONException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.zimbra.client.ZFolder.Color;
import com.zimbra.client.ZGrant.GranteeType;
import com.zimbra.client.ZInvite.ZTimeZone;
import com.zimbra.client.ZMailbox.ZOutgoingMessage.AttachedMessagePart;
import com.zimbra.client.ZSearchParams.Cursor;
import com.zimbra.client.event.ZCreateAppointmentEvent;
import com.zimbra.client.event.ZCreateContactEvent;
import com.zimbra.client.event.ZCreateConversationEvent;
import com.zimbra.client.event.ZCreateEvent;
import com.zimbra.client.event.ZCreateFolderEvent;
import com.zimbra.client.event.ZCreateMessageEvent;
import com.zimbra.client.event.ZCreateMountpointEvent;
import com.zimbra.client.event.ZCreateSearchFolderEvent;
import com.zimbra.client.event.ZCreateTagEvent;
import com.zimbra.client.event.ZCreateTaskEvent;
import com.zimbra.client.event.ZDeleteEvent;
import com.zimbra.client.event.ZEventHandler;
import com.zimbra.client.event.ZModifyAppointmentEvent;
import com.zimbra.client.event.ZModifyContactEvent;
import com.zimbra.client.event.ZModifyConversationEvent;
import com.zimbra.client.event.ZModifyEvent;
import com.zimbra.client.event.ZModifyFolderEvent;
import com.zimbra.client.event.ZModifyMailboxEvent;
import com.zimbra.client.event.ZModifyMessageEvent;
import com.zimbra.client.event.ZModifyMountpointEvent;
import com.zimbra.client.event.ZModifySearchFolderEvent;
import com.zimbra.client.event.ZModifyTagEvent;
import com.zimbra.client.event.ZModifyTaskEvent;
import com.zimbra.client.event.ZModifyVoiceMailItemEvent;
import com.zimbra.client.event.ZModifyVoiceMailItemFolderEvent;
import com.zimbra.client.event.ZRefreshEvent;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.auth.twofactor.TOTPAuthenticator;
import com.zimbra.common.auth.twofactor.TwoFactorOptions.Encoding;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.httpclient.InputStreamRequestHttpRetryHandler;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.ExistingParentFolderStoreAndUnmatchedPart;
import com.zimbra.common.mailbox.FolderStore;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.mailbox.MailItemType;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.mailbox.OpContext;
import com.zimbra.common.mailbox.ZimbraMailItem;
import com.zimbra.common.mailbox.ZimbraQueryHitResults;
import com.zimbra.common.mailbox.ZimbraSearchParams;
import com.zimbra.common.net.SocketFactories;
import com.zimbra.common.service.RemoteServiceException;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.JSONElement;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.soap.SoapTransport.NotificationFormat;
import com.zimbra.common.soap.VoiceConstants;
import com.zimbra.common.soap.ZimbraNamespace;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ListUtil;
import com.zimbra.common.util.MapUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.message.AuthRequest;
import com.zimbra.soap.account.message.AuthResponse;
import com.zimbra.soap.account.message.ChangePasswordRequest;
import com.zimbra.soap.account.message.ChangePasswordResponse;
import com.zimbra.soap.account.message.DisableTwoFactorAuthRequest;
import com.zimbra.soap.account.message.DisableTwoFactorAuthResponse;
import com.zimbra.soap.account.message.EnableTwoFactorAuthRequest;
import com.zimbra.soap.account.message.EnableTwoFactorAuthResponse;
import com.zimbra.soap.account.message.EndSessionRequest;
import com.zimbra.soap.account.message.GetIdentitiesRequest;
import com.zimbra.soap.account.message.GetIdentitiesResponse;
import com.zimbra.soap.account.message.GetInfoRequest;
import com.zimbra.soap.account.message.GetInfoResponse;
import com.zimbra.soap.account.message.GetShareInfoRequest;
import com.zimbra.soap.account.message.GetShareInfoResponse;
import com.zimbra.soap.account.message.GetSignaturesRequest;
import com.zimbra.soap.account.message.GetSignaturesResponse;
import com.zimbra.soap.account.type.AuthToken;
import com.zimbra.soap.account.type.InfoSection;
import com.zimbra.soap.base.SpecifyContact;
import com.zimbra.soap.mail.message.BeginTrackingIMAPRequest;
import com.zimbra.soap.mail.message.CheckSpellingRequest;
import com.zimbra.soap.mail.message.CheckSpellingResponse;
import com.zimbra.soap.mail.message.CreateContactRequest;
import com.zimbra.soap.mail.message.CreateDataSourceRequest;
import com.zimbra.soap.mail.message.CreateDataSourceResponse;
import com.zimbra.soap.mail.message.CreateSearchFolderRequest;
import com.zimbra.soap.mail.message.CreateSearchFolderResponse;
import com.zimbra.soap.mail.message.DeleteDataSourceRequest;
import com.zimbra.soap.mail.message.GetAppointmentIdsSinceRequest;
import com.zimbra.soap.mail.message.GetAppointmentIdsSinceResponse;
import com.zimbra.soap.mail.message.GetAppointmentRequest;
import com.zimbra.soap.mail.message.GetAppointmentResponse;
import com.zimbra.soap.mail.message.GetDataSourcesRequest;
import com.zimbra.soap.mail.message.GetDataSourcesResponse;
import com.zimbra.soap.mail.message.GetFilterRulesRequest;
import com.zimbra.soap.mail.message.GetFilterRulesResponse;
import com.zimbra.soap.mail.message.GetFolderRequest;
import com.zimbra.soap.mail.message.GetFolderResponse;
import com.zimbra.soap.mail.message.GetIMAPRecentCutoffRequest;
import com.zimbra.soap.mail.message.GetIMAPRecentCutoffResponse;
import com.zimbra.soap.mail.message.GetIMAPRecentRequest;
import com.zimbra.soap.mail.message.GetIMAPRecentResponse;
import com.zimbra.soap.mail.message.GetLastItemIdInMailboxRequest;
import com.zimbra.soap.mail.message.GetLastItemIdInMailboxResponse;
import com.zimbra.soap.mail.message.GetModifiedItemsIDsRequest;
import com.zimbra.soap.mail.message.GetModifiedItemsIDsResponse;
import com.zimbra.soap.mail.message.GetOutgoingFilterRulesRequest;
import com.zimbra.soap.mail.message.GetOutgoingFilterRulesResponse;
import com.zimbra.soap.mail.message.IMAPCopyRequest;
import com.zimbra.soap.mail.message.IMAPCopyResponse;
import com.zimbra.soap.mail.message.ImportContactsRequest;
import com.zimbra.soap.mail.message.ImportContactsResponse;
import com.zimbra.soap.mail.message.ImportDataRequest;
import com.zimbra.soap.mail.message.ItemActionRequest;
import com.zimbra.soap.mail.message.ItemActionResponse;
import com.zimbra.soap.mail.message.ListIMAPSubscriptionsRequest;
import com.zimbra.soap.mail.message.ListIMAPSubscriptionsResponse;
import com.zimbra.soap.mail.message.ModifyContactRequest;
import com.zimbra.soap.mail.message.ModifyDataSourceRequest;
import com.zimbra.soap.mail.message.ModifyFilterRulesRequest;
import com.zimbra.soap.mail.message.ModifyOutgoingFilterRulesRequest;
import com.zimbra.soap.mail.message.NoOpRequest;
import com.zimbra.soap.mail.message.OpenIMAPFolderRequest;
import com.zimbra.soap.mail.message.OpenIMAPFolderResponse;
import com.zimbra.soap.mail.message.RecordIMAPSessionRequest;
import com.zimbra.soap.mail.message.RecordIMAPSessionResponse;
import com.zimbra.soap.mail.message.ResetRecentMessageCountRequest;
import com.zimbra.soap.mail.message.SaveIMAPSubscriptionsRequest;
import com.zimbra.soap.mail.message.TestDataSourceRequest;
import com.zimbra.soap.mail.message.TestDataSourceResponse;
import com.zimbra.soap.mail.type.ActionResult;
import com.zimbra.soap.mail.type.ActionSelector;
import com.zimbra.soap.mail.type.CalendarItemInfo;
import com.zimbra.soap.mail.type.ContactSpec;
import com.zimbra.soap.mail.type.Content;
import com.zimbra.soap.mail.type.Folder;
import com.zimbra.soap.mail.type.GetFolderSpec;
import com.zimbra.soap.mail.type.IMAPItemInfo;
import com.zimbra.soap.mail.type.ImapCursorInfo;
import com.zimbra.soap.mail.type.ImapMessageInfo;
import com.zimbra.soap.mail.type.ImportContact;
import com.zimbra.soap.mail.type.Invitation;
import com.zimbra.soap.mail.type.ModifyContactSpec;
import com.zimbra.soap.mail.type.NewContactAttr;
import com.zimbra.soap.mail.type.NewContactGroupMember;
import com.zimbra.soap.mail.type.NewSearchFolderSpec;
import com.zimbra.soap.mail.type.TestDataSource;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.type.AccountWithModifications;
import com.zimbra.soap.type.CalDataSource;
import com.zimbra.soap.type.DataSource;
import com.zimbra.soap.type.ImapDataSource;
import com.zimbra.soap.type.Pop3DataSource;
import com.zimbra.soap.type.RssDataSource;
import com.zimbra.soap.type.SearchSortBy;
import com.zimbra.soap.type.ShareInfo;

public class ZMailbox implements ToZJSONObject, MailboxStore {
    public final static int MAX_NUM_CACHED_SEARCH_PAGERS = 5;
    public final static int MAX_NUM_CACHED_SEARCH_CONV_PAGERS = 5;
    public final static int MAX_NUM_CACHED_MESSAGES = LC.zmailbox_message_cachesize.intValue();
    public final static int MAX_NUM_CACHED_CONTACTS = 25;
    private static final int ADMIN_PORT = LC.zimbra_admin_service_port.intValue();
    public static final String APPOINTMENT_IMPORT_TYPE_ICS= "ics";

    public final static String PATH_SEPARATOR = "/";

    public final static char PATH_SEPARATOR_CHAR = '/';

    private static final int CALENDAR_FOLDER_ALL = -1;
    /* Generally set "accountId" explicitly when using another user's auth token, as don't have GetAccountInfo
     * capability which is what is normally used to get the account ID.  Note that not always set.
     */
    private String accountId = null;
    /* As above, generally set "name" explicitly when using another user's auth token */
    private String name = null;
    private String authName = null;
    private static final Pattern sAttachmentId = Pattern.compile("\\d+,'.*','(.*)'");
    private static final Pattern sCOMMA = Pattern.compile(",");

    /** include items in the Trash folder */
    public static final String TC_INCLUDE_TRASH = "t";
    /** include items in the Spam/Junk folder */
    public static final String TC_INCLUDE_JUNK = "j";
    /** include items in the Sent folder */
    public static final String TC_INCLUDE_SENT = "s";

    /** include items in any other folder */
    public static final String TC_INCLUDE_OTHER = "o";
    public static final String CONTACT_IMPORT_TYPE_CSV = "csv";

    private static Set<InfoSection> NOT_ZIMLETS = Collections.unmodifiableSet(
            EnumSet.complementOf(EnumSet.of(InfoSection.zimlets)));

    private ZAuthToken mAuthToken;
    private String mCsrfToken;
    private String mTrustedToken;
    private SoapHttpTransport mTransport;
    private SessionPreference mNotifyPreference;
    private Map<String, ZTag> mNameToTag;
    private ItemCache mItemCache;
    private ZGetInfoResult mGetInfoResult;
    private ZFolder mUserRoot;
    private ZSearchPagerCache mSearchPagerCache;
    private ZSearchPagerCache mSearchConvPagerCache;
    private ZApptSummaryCache mApptSummaryCache;
    private Map<String, CachedMessage> mMessageCache;
    private Map<String, ZContact> mContactCache;
    private ZFilterRules incomingRules;
    private ZFilterRules outgoingRules;
    private ZAuthResult mAuthResult;
    private String mClientIp;
    private List<ZPhoneAccount> mPhoneAccounts;
    private Map<String, ZPhoneAccount> mPhoneAccountMap;
    private Element mVoiceStorePrincipal;
    private long mSize;
    private boolean mNoTagCache;
    private boolean alwaysRefreshFolders;
    private ZContactByPhoneCache mContactByPhoneCache;
    private final ZMailboxLock lock;
    private LastChange lastChange = new LastChange();
    private NotificationFormat mNotificationFormat = NotificationFormat.DEFAULT;
    private String mCurWaitSetID = null;

    private final List<ZEventHandler> mHandlers = new ArrayList<ZEventHandler>();

    static {
        SocketFactories.registerProtocols();
    }

    public static final class Fetch {
        public static final Fetch none              = new Fetch("none");
        public static final Fetch first                = new Fetch("first");
        public static final Fetch hits                 = new Fetch("hits");
        public static final Fetch all                 = new Fetch("all");
        public static final Fetch unread            = new Fetch("unread");
        public static final Fetch u1                = new Fetch("u1");
        public static final Fetch first_msg         = new Fetch("!");
        public static final Fetch hits_or_first_msg = new Fetch("hits!");
        public static final Fetch u_or_first_msg    = new Fetch("u!");
        public static final Fetch u1_or_first_msg   = new Fetch("u1!");

        private final String name;

        private static final Map<String, Fetch> MAP = ImmutableMap.<String, Fetch>builder()
                .put(none.name, none)
                .put(first.name, first)
                .put(unread.name, unread)
                .put(u1.name, u1)
                .put(hits.name, hits)
                .put(all.name, all)
                .put(first_msg.name, first_msg)
                .put(hits_or_first_msg.name,hits_or_first_msg)
                .put(u_or_first_msg.name,u_or_first_msg)
                .put(u1_or_first_msg.name,u1_or_first_msg)
                .build();

        public Fetch(String name){
            this.name = name;
        }

        public static Fetch fromString(String s) throws ServiceException {
            Fetch result = MAP.get(s);
            if (result != null) {
                return result;
            }
            else {
                String[] ids = s.split(",");
                for (String id: ids) {
                    try {
                        Integer.parseInt(id);
                    }
                    catch (NumberFormatException e) {
                        throw ZClientException.CLIENT_ERROR("invalid fetch: "+s, e);
                    }
                }
                return new Fetch(s);
            }
        }

        public String name(){
            return name;
        }
    }

    public static final class LastChange {
        public int id = 0;
        private long time = 0;

        public void setId(int newLastChangeId) {
            if ((id != newLastChangeId) || (time == 0)) {
                time = System.currentTimeMillis();
            }
            id = newLastChangeId;
        }

        public int getId() {
            return id;
        }

        public long getTime() {
            return time;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("lastChangeId=").append(id).append(" since=");
            sb.append(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(time)));
            return sb.toString();
        }
    }

    private enum SessionPreference {
        nosession, full;

        static SessionPreference fromOptions(Options options) {
            if (options == null) {
                return full;
            } else if (options.getNoSession()) {
                return nosession;
            } else {
                return full;
            }
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
        private String mUserAgentName;
        private String mUserAgentVersion;
        private String mOriginalUserAgent;
        private int mTimeout = -1;
        private int mRetryCount = -1;
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
        private boolean mCsrfSupported; // Used by AuthRequest
        private Map<String, String> mCustomHeaders;
        private String mTwoFactorCode;
        private boolean mAppSpecificPasswordsSupported;
        private boolean mTrustedDevice;
        private String mTrustedDeviceToken;
        private String mDeviceId;
        private boolean mGenerateDeviceId;
        private SoapTransport.NotificationFormat notificationFormat = SoapTransport.NotificationFormat.DEFAULT;
        private boolean alwaysRefreshFolders;

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

        public Options(ZAuthToken authToken, String uri, boolean forceAuth, boolean csrfSupported) {
            mAuthToken = authToken;
            mAuthAuthToken = forceAuth;
            mCsrfSupported = csrfSupported;
            setUri(uri);
        }

        public String getClientIp() { return mClientIp; }
        public Options setClientIp(String clientIp) { mClientIp = clientIp;  return this; }

        public String getAccount() { return mAccount; }
        public Options setAccount(String account) { mAccount = account;  return this; }

        public AccountBy getAccountBy() { return mAccountBy; }
        public Options setAccountBy(AccountBy accountBy) { mAccountBy = accountBy;  return this; }

        public String getTargetAccount() { return mTargetAccount; }
        public Options setTargetAccount(String targetAccount) { mTargetAccount = targetAccount;  return this; }

        public AccountBy getTargetAccountBy() { return mTargetAccountBy; }
        public Options setTargetAccountBy(AccountBy targetAccountBy) { mTargetAccountBy = targetAccountBy;  return this; }

        public String getPassword() { return mPassword; }
        public Options setPassword(String password) { mPassword = password;  return this; }

        public String getNewPassword() { return mNewPassword; }
        public Options setNewPassword(String newPassword) { mNewPassword = newPassword;  return this; }

        public String getVirtualHost() { return mVirtualHost; }
        public Options setVirtualHost(String virtualHost) { mVirtualHost = virtualHost;  return this; }

        public ZAuthToken getAuthToken() { return mAuthToken; }
        public Options setAuthToken(ZAuthToken authToken) { mAuthToken = authToken;  return this; }

        public boolean getAlwaysRefreshFolders() { return alwaysRefreshFolders; }
        public Options setAlwaysRefreshFolders(boolean alwaysRefresh) { alwaysRefreshFolders = alwaysRefresh; return this; }

        public SoapTransport.NotificationFormat getNotificationFormat() {
            return notificationFormat;
        }

        public Options setNotificationFormat(SoapTransport.NotificationFormat notificationFormat) {
            this.notificationFormat = notificationFormat;
            return this;
        }

        // AP-TODO-8: retire
        public Options setAuthToken(String authToken) { mAuthToken = new ZAuthToken(null, authToken, null);  return this; }

        public String getUri() { return mUri; }
        public Options setUri(String uri) {
            setUri(uri, false);
            return this;
        }

        public Options setUri(String uri, boolean isAdmin) {
            try {
                mUri = resolveUrl(uri, isAdmin);
            } catch (ZClientException e) {
                mUri = uri;
            }
            return this;
        }

        public String getUserAgentName() { return mUserAgentName; }
        public String getUserAgentVersion() { return mUserAgentVersion; }
        public Options setUserAgent(String name, String version) {
            mUserAgentName = name;
            mUserAgentVersion = version;
            return this;
        }

        public String getOriginalUserAgent() { return mOriginalUserAgent; }
        public Options setOriginalUserAgent(String userAgent) {
            this.mOriginalUserAgent = userAgent;
            return this;
        }

        public int getTimeout() { return mTimeout; }
        public Options setTimeout(int timeout) { mTimeout = timeout;  return this; }

        public int getRetryCount() { return mRetryCount; }
        public Options setRetryCount(int retryCount) { mRetryCount = retryCount;  return this; }

        public SoapTransport.DebugListener getDebugListener() { return mDebugListener; }
        public Options setDebugListener(SoapTransport.DebugListener listener) { mDebugListener = listener;  return this; }

        public SoapHttpTransport.HttpDebugListener getHttpDebugListener() { return mHttpDebugListener; }
        public Options setHttpDebugListener(SoapHttpTransport.HttpDebugListener listener) { mHttpDebugListener = listener;  return this; }

        public boolean getNoSession() { return mNoSession; }
        public Options setNoSession(boolean noSession) { mNoSession = noSession;  return this; }

        public boolean getAuthAuthToken() { return mAuthAuthToken; }
        /** @param authAuthToken set to true if you want to send an AuthRequest to valid the auth token */
        public Options setAuthAuthToken(boolean authAuthToken) { mAuthAuthToken = authAuthToken;  return this; }

        public ZEventHandler getEventHandler() { return mHandler; }
        public Options setEventHandler(ZEventHandler handler) { mHandler = handler;  return this; }

        public List<String> getPrefs() { return mPrefs; }
        public Options setPrefs(List<String> prefs) { mPrefs = prefs;  return this; }

        public List<String> getAttrs() { return mAttrs; }
        public Options setAttrs(List<String> attrs) { mAttrs = attrs;  return this; }

        public String getRequestedSkin() { return mRequestedSkin; }
        public Options setRequestedSkin(String skin) { mRequestedSkin = skin;  return this; }

        public boolean getCsrfSupported() { return mCsrfSupported; }
        public Options setCsrfSupported(boolean csrfSupported) { mCsrfSupported = csrfSupported;  return this; }

        public String getTwoFactorCode() { return mTwoFactorCode; }
        public Options setTwoFactorCode(String code) { mTwoFactorCode = code; return this; }

        public boolean getAppSpecificPasswordsSupported() { return mAppSpecificPasswordsSupported; }
        public Options setAppSpecificPasswordsSupported(boolean bool) { mAppSpecificPasswordsSupported = bool; return this; }

        public boolean getTrustedDevice() { return mTrustedDevice; }
        public Options setTrustedDevice(boolean bool) { mTrustedDevice = bool; return this; }

        public String getTrustedDeviceToken() { return mTrustedDeviceToken; }
        public Options setTrustedDeviceToken(String token) { mTrustedDeviceToken = token; return this; }

        public String getDeviceId() { return mDeviceId; }
        public Options setDeviceId(String deviceId) {  mDeviceId = deviceId; return this; }

        public boolean getGenerateDeviceId() { return mGenerateDeviceId; }
        public Options setGenerateDeviceId(boolean bool) { mGenerateDeviceId = bool; return this; }

        public Map<String, String> getCustomHeaders() {
            if (mCustomHeaders == null) {
                mCustomHeaders = new HashMap<String, String>();
            }
            return mCustomHeaders;
        }
    }

    public static enum TrustedStatus {
        trusted, not_trusted;
    }

    private static class ItemCache {
        private final Map<String /* id */, ZItem> idMap;
        private final Map<String /* uuid */, ZItem> uuidMap;
        private final ZMailbox zmbx;
        private String acctId = null;

        public ItemCache(ZMailbox zmbx) {
            this.zmbx = zmbx;
            idMap = new HashMap<String, ZItem>();
            uuidMap = new HashMap<String, ZItem>();
        }

        public void clear() {
            idMap.clear();
            uuidMap.clear();
        }

        public void put(ZItem item) {
            putWithId(item.getId(), item);
        }

        public void putWithId(String id, ZItem item) {
            idMap.put(key(id), item);
            if (item.getUuid() != null) {
                uuidMap.put(item.getUuid(), item);
            }
        }

        public ZItem getById(String id) {
            return idMap.get(key(id));
        }

        public ZItem getByUuid(String uuid) {
            return uuidMap.get(uuid);
        }

        public ZItem removeById(String id) {
            ZItem removed = idMap.remove(key(id));
            if (removed != null && removed.getUuid() != null) {
                uuidMap.remove(removed.getUuid());
            }
            return removed;
        }

        private String key(String id) {
            if (acctId == null) {  /* zmbox.getAccountId() may not succeed early on for on behalf of case */
                try {
                    acctId = zmbx.getAccountId();
                } catch (Exception ex) {
                }
            }

            try {
                return ItemIdentifier.asSimplestString(id, acctId);
            } catch (ServiceException e) {
                // This can be raised when constructing an ItemIdentifier with a folder name; e.g., "INBOX"
                return id;
            }
        }
    }

    public static ZMailbox getMailbox(Options options) throws ServiceException {
        return new ZMailbox(options);
    }

    /**
     * for use with changePassword
     */
    private ZMailbox() {
        lock = new ZMailboxLock(
                LC.zimbra_mailbox_lock_max_waiting_threads.intValue(),
                LC.zimbra_mailbox_lock_timeout.intValue());
    }

    /**
     * change password. You must pass in an options with an account, password, newPassword, and Uri.
     * @param options uri/name/pass/newPass
     * @throws ServiceException on error
     */
    public static ZChangePasswordResult changePassword(Options options) throws ServiceException {
        ZMailbox mailbox = new ZMailbox();
        mailbox.mClientIp = options.getClientIp();
        mailbox.mNotifyPreference = SessionPreference.fromOptions(options);
        mailbox.initPreAuth(options);
        return mailbox.changePassword(options.getAccount(), options.getAccountBy(), options.getPassword(), options.getNewPassword(), options.getVirtualHost());
    }

    public static ZMailbox getByName(String name, String password, String uri) throws ServiceException {
        return new ZMailbox(new Options(name, AccountBy.name, password, uri));
    }

    public static ZMailbox getByAuthToken(String authToken, String uri) throws ServiceException {
        return new ZMailbox(new Options(authToken, uri));
    }

    public static ZMailbox getByAuthToken(ZAuthToken authToken, String uri, boolean forceAuth, boolean csrfSupported) throws ServiceException {
        return new ZMailbox(new Options(authToken, uri, forceAuth, csrfSupported));
    }

    public ZMailbox(Options options) throws ServiceException {
        this();
        mHandlers.add(new InternalEventHandler());
        mSearchPagerCache = new ZSearchPagerCache(MAX_NUM_CACHED_SEARCH_PAGERS, true);
        mHandlers.add(mSearchPagerCache);
        mSearchConvPagerCache = new ZSearchPagerCache(MAX_NUM_CACHED_SEARCH_CONV_PAGERS, false);
        mHandlers.add(mSearchConvPagerCache);
        mMessageCache = MapUtil.newLruMap(MAX_NUM_CACHED_MESSAGES);
        mContactCache = MapUtil.newLruMap(MAX_NUM_CACHED_CONTACTS);
        mApptSummaryCache = new ZApptSummaryCache();
        mHandlers.add(mApptSummaryCache);
        if (options.getEventHandler() != null) {
            mHandlers.add(options.getEventHandler());
        }
        mNotificationFormat = options.getNotificationFormat();
        mNotifyPreference = SessionPreference.fromOptions(options);
        alwaysRefreshFolders = options.getAlwaysRefreshFolders();

        mClientIp = options.getClientIp();

        initPreAuth(options);
        if (options.getTargetAccount() != null) {
            /* ZCS-4341 NOT initTargetAccountForTransport - that can cause problems because
             * the server requires an auth token in context if account is specified and that isn't
             * always the case. */
            initTargetAccountInfo(options.getTargetAccount(), options.getTargetAccountBy());
        }
        if (options.getAuthToken() != null) {
            if (options.getAuthAuthToken()) {
                mAuthResult = authByAuthToken(options);
                initCsrfToken(mAuthResult.getCsrfToken());
                initAuthToken(mAuthResult.getAuthToken());
                initTrustedToken(mAuthResult.getTrustedToken());
            } else {
                initAuthToken(options.getAuthToken());
            }
        } else if (options.getAccount() != null) {
            String password;
            if (options.getNewPassword() != null) {
                changePassword(options.getAccount(), options.getAccountBy(), options.getPassword(), options.getNewPassword(), options.getVirtualHost());
                password = options.getNewPassword();
            } else {
                password = options.getPassword();
            }
            mAuthResult = authByPassword(options, password);
            initAuthToken(mAuthResult.getAuthToken());
            initCsrfToken(mAuthResult.getCsrfToken());
            initTrustedToken(mAuthResult.getTrustedToken());
        }
        if (options.getTargetAccount() != null) {
            /* ZCS-4341 do this only AFTER finished authenticating */
            initTargetAccountForTransport(options.getTargetAccount(), options.getTargetAccountBy());
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

    public void initAuthToken(ZAuthToken authToken){
        mAuthToken = authToken;
        mTransport.setAuthToken(mAuthToken);
    }

    public void initCsrfToken(String csrfToken){
        mCsrfToken = csrfToken;
        mTransport.setCsrfToken(mCsrfToken);
    }

    public void initTrustedToken(String trustedToken) {
        mTrustedToken = trustedToken;
        mTransport.setTrustedToken(mTrustedToken);
    }

    private void initPreAuth(Options options) {
        mItemCache = new ItemCache(this);
        setSoapURI(options);
        if (options.getDebugListener() != null) {
            mTransport.setDebugListener(options.getDebugListener());
        } else if (options.getHttpDebugListener() != null) {
            mTransport.setHttpDebugListener(options.getHttpDebugListener());
        }
    }

    private void initTargetAccountInfo(String key, AccountBy by) {
        if (AccountBy.id.equals(by)) {
            accountId = key;
        } else if (AccountBy.name.equals(by)) {
            name = key;
        }
    }

    private void initTargetAccountForTransport(String key, AccountBy by) {
        if (AccountBy.id.equals(by)) {
            mTransport.setTargetAcctId(key);
        } else if (AccountBy.name.equals(by)) {
            mTransport.setTargetAcctName(key);
        }
    }

    public Element newRequestElement(QName name) {
        if (mTransport.getRequestProtocol() == SoapProtocol.SoapJS) {
            return new JSONElement(name);
        } else {
            return new XMLElement(name);
        }
    }

    private ZChangePasswordResult changePassword(String key, AccountBy by, String oldPassword, String newPassword, String virtualHost) throws ServiceException {
        if (mTransport == null) {
            throw ZClientException.CLIENT_ERROR("must call setURI before calling changePassword", null);
        }

        AccountSelector account = new AccountSelector(SoapConverter.TO_SOAP_ACCOUNT_BY.apply(by), key);
        ChangePasswordRequest req = new ChangePasswordRequest(account, oldPassword, newPassword);
        req.setVirtualHost(virtualHost);

        ChangePasswordResponse res = invokeJaxb(req);
        return new ZChangePasswordResult(res);
    }

    private void addAttrsAndPrefs(AuthRequest req, Options options) {
        List<String> prefs = options.getPrefs();
        if (!ListUtil.isEmpty(prefs)) {
            for (String p : prefs) {
                req.addPref(p);
            }
        }
        List<String> attrs = options.getAttrs();
        if (!ListUtil.isEmpty(attrs)) {
            for (String a : attrs) {
                req.addAttr(a);
            }
        }
    }

    public ZAuthResult authByPassword(Options options, String password) throws ServiceException {
        if (mTransport == null) {
            throw ZClientException.CLIENT_ERROR("must call setURI before calling authenticate", null);
        }

        AccountSelector account = new AccountSelector(com.zimbra.soap.type.AccountBy.name, options.getAccount());
        AuthRequest auth = new AuthRequest(account, password);
        auth.setPassword(password);
        auth.setTwoFactorCode(options.getTwoFactorCode());
        auth.setVirtualHost(options.getVirtualHost());
        auth.setRequestedSkin(options.getRequestedSkin());
        auth.setCsrfSupported(options.getCsrfSupported());
        auth.setDeviceTrusted(options.getTrustedDevice());
        if (options.getTrustedDevice()) {
            auth.setDeviceTrusted(true);
        }
        if (options.getAuthToken() != null) {
            auth.setAuthToken(new AuthToken(options.getAuthToken().getValue(), false));
        }
        if (options.getDeviceId() != null) {
            auth.setDeviceId(options.getDeviceId());
        }
        if (options.getTrustedDeviceToken() != null) {
            auth.setTrustedDeviceToken(options.getTrustedDeviceToken());
        }
        if (options.getGenerateDeviceId()) {
            auth.setGenerateDeviceId(true);
        }
        addAttrsAndPrefs(auth, options);

        AuthResponse authRes = invokeJaxb(auth);
        ZAuthResult r = new ZAuthResult(authRes);
        r.setSessionId(mTransport.getSessionId());
        return r;
    }

    public ZAuthResult authByAuthToken(Options options) throws ServiceException {
        if (mTransport == null) {
            throw ZClientException.CLIENT_ERROR("must call setURI before calling authenticate", null);
        }

        AuthRequest req = new AuthRequest();
        ZAuthToken zat = options.getAuthToken(); // cannot be null here
        req.setAuthToken(new AuthToken(zat.getValue(), false));
        req.setTwoFactorCode(options.getTwoFactorCode());
        req.setRequestedSkin(options.getRequestedSkin());
        req.setCsrfSupported(options.getCsrfSupported());
        req.setDeviceTrusted(options.getTrustedDevice());
        addAttrsAndPrefs(req, options);

        AuthResponse res = invokeJaxb(req);
        ZAuthResult r = new ZAuthResult(res);
        r.setSessionId(mTransport.getSessionId());
        return r;
    }

    public ZAuthResult getAuthResult() {
        return mAuthResult;
    }

    public ZAuthToken getAuthToken() {
        return mAuthToken;
    }

    public String getCsrfToken() {
        return mCsrfToken;
    }

    public String getTrustedToken() {
        return mTrustedToken;
    }

    /**
     * @param uri URI of server we want to talk to
     * @param timeout timeout for HTTP connection or 0 for no timeout
     * @param retryCount max number of times to retry the call on connection failure
     */
    private void setSoapURI(Options options) {
        if (mTransport != null) {
            mTransport.shutdown();
        }
        mTransport = new SoapHttpTransport(options.getUri());
        if (options.getUserAgentName() == null) {
            mTransport.setUserAgent("zclient", SystemUtil.getProductVersion());
        } else {
            mTransport.setUserAgent(options.getUserAgentName(), options.getUserAgentVersion());
        }
        if (options.getOriginalUserAgent() != null) {
            mTransport.setOriginalUserAgent(options.getOriginalUserAgent());
        }
        mTransport.setMaxNotifySeq(0);
        mTransport.setClientIp(mClientIp);
        if (options.getTimeout() > -1) {
            mTransport.setTimeout(options.getTimeout());
        }
        if (options.getRetryCount() != -1) {
            mTransport.setRetryCount(options.getRetryCount());
        }
        if (mAuthToken != null) {
            mTransport.setAuthToken(mAuthToken);
        }
        if (mCsrfToken != null) {
            mTransport.setCsrfToken(mCsrfToken);
        }
        for (Map.Entry<String, String> entry : options.getCustomHeaders().entrySet()) {
            mTransport.getCustomHeaders().put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * It is sometimes useful to use JAXB for the request but get the response as an Element
     * as a first stage in complete migration to JAXB
     */
    public Element invokeJaxbToElement(Object jaxbObject) throws ServiceException {
        Element req = JaxbUtil.jaxbToElement(jaxbObject);
        return invoke(req);
    }

    @SuppressWarnings("unchecked")
    public <T> T invokeJaxb(Object jaxbObject) throws ServiceException {
        Element res = invokeJaxbToElement(jaxbObject);
        return (T) JaxbUtil.elementToJaxb(res);
    }

    @SuppressWarnings("unchecked")
    public <T> T invokeJaxbOnTargetAccount(Object jaxbObject, String requestedAccountId) throws ServiceException {
        Element req = JaxbUtil.jaxbToElement(jaxbObject);
        Element res = invoke(req, requestedAccountId);
        return (T) JaxbUtil.elementToJaxb(res);
    }

    public Element invoke(Element request) throws ServiceException {
        return invoke(request, null);
    }

    public boolean isUsingSession() {
        return (mNotifyPreference != SessionPreference.nosession);
    }

    public Element invoke(Element request, String requestedAccountId) throws ServiceException {
        lock();
        try {
            try {
                boolean nosession = mNotifyPreference == SessionPreference.nosession;
                if(nosession) {
                    return mTransport.invoke(request, false, nosession, requestedAccountId);
                } else {
                    return mTransport.invoke(request, nosession, requestedAccountId, this.mNotificationFormat, this.mCurWaitSetID);
                }
            } catch (SoapFaultException e) {
                throw e; // for now, later, try to map to more specific exception
            } catch (IOException e) {
                doConnectionAndSSLFailures(e);
                throw ZClientException.IO_ERROR(e.getMessage(), e);
            } catch (Exception e) {
                doConnectionAndSSLFailures(e);
                throw ServiceException.FAILURE(e.getMessage(), e);
            } finally {
                Element context = mTransport.getZimbraContext();
                mTransport.clearZimbraContext();
                handleResponseContext(context);
            }
        } finally {
            unlock();
        }
    }

    private void doConnectionAndSSLFailures(Exception e) throws RemoteServiceException {
        Throwable t = SystemUtil.getInnermostException(e);
        RemoteServiceException.doConnectionFailures(mTransport.getURI(), t);
        RemoteServiceException.doSSLFailures(t.getMessage(), t);
    }

    private void handleResponseContext(Element context) throws ServiceException {
        if (context == null) {
            return;
        }
        // handle refresh blocks
        Element refresh = context.getOptionalElement(ZimbraNamespace.E_REFRESH);
        if (refresh != null) {
            handleRefresh(refresh);
        }

        // handle refresh blocks
        Element change = context.getOptionalElement(HeaderConstants.E_CHANGE);
        if (change != null) {
            lastChange.setId(change.getAttributeInt(HeaderConstants.A_CHANGE_ID));
        }

        for (Element notify : context.listElements(ZimbraNamespace.E_NOTIFY)) {
            mTransport.setMaxNotifySeq(
                    Math.max(mTransport.getMaxNotifySeq(),
                            notify.getAttributeLong(HeaderConstants.A_SEQNO, 0)));
            // MUST DO IN THIS ORDER!
            handleDeleted(notify.getOptionalElement(ZimbraNamespace.E_DELETED));
            handleCreated(notify.getOptionalElement(ZimbraNamespace.E_CREATED));
            handleModified(notify.getOptionalElement(ZimbraNamespace.E_MODIFIED));
            handlePendingModifications(notify.getOptionalElement(MailConstants.E_A));
        }
    }

    private void handleRefresh(Element refresh) throws ServiceException {
        for (Element mbx : refresh.listElements(MailConstants.E_MAILBOX)) {
            // FIXME: logic should be different if ZMailbox points at another user's mailbox
            if (mbx.getAttribute(HeaderConstants.A_ACCOUNT_ID, null) == null) {
                mSize = mbx.getAttributeLong(MailConstants.A_SIZE);
            }
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
        for (ZEventHandler handler : mHandlers) {
            handler.handleRefresh(event, this);
        }
        incomingRules = null;
        outgoingRules = null;
    }

    private void handleModified(Element modified) throws ServiceException {
        if (modified == null) {
            return;
        }
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
            if (event != null) {
                handleEvent(event);
            }
        }
    }

    private void handleEvent(ZModifyEvent event) throws ServiceException {
        for (ZEventHandler handler : mHandlers) {
            handler.handleModify(event, this);
        }
    }

    private List<ZFolder> parentCheck(List<ZFolder> list, ZFolder f, ZFolder parent) {
        if (parent != null) {
            parent.addChild(f);
        } else {
            if (list == null) {
                list = new ArrayList<ZFolder>();
            }
            list.add(f);
        }
        return list;
    }

    private void handleCreated(Element created) throws ServiceException {
        if (created == null) {
            return;
        }
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
                if (events == null) {
                    events = new ArrayList<ZCreateEvent>();
                }
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
                for (ZEventHandler handler : mHandlers) {
                    handler.handleCreate(event, this);
                }
            }
        }
    }

    private void handlePendingModifications(Element pendingMods) throws ZClientException {
        if(pendingMods == null) {
            return;
        }
        try {
            AccountWithModifications accountMods = JaxbUtil.elementToJaxb(pendingMods, AccountWithModifications.class);
            for (ZEventHandler handler : mHandlers) {
                handler.handlePendingModification(lastChange.getId(), accountMods);
            }
        } catch (ServiceException e) {
            throw ZClientException.CLIENT_ERROR("unable to parse pending modifications", e);
        }
    }

    private void handleDeleted(Element deleted) throws ServiceException {
        if (deleted == null) {
            return;
        }
        String ids = deleted.getAttribute(MailConstants.A_ID, null);
        if (ids == null) {
            return;
        }
        ZDeleteEvent de = new ZDeleteEvent(ids);
        for (ZEventHandler handler : mHandlers) {
            handler.handleDelete(de, this);
        }
    }

    private void addIdMappings(ZFolder folder) {
        if (folder == null) {
            return;
        }
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
        @Override
        public synchronized void handleRefresh(ZRefreshEvent event, ZMailbox mailbox) {
            ZFolder root = event.getUserRoot();
            List<ZTag> tags = event.getTags();

            mItemCache.clear();
            mMessageCache.clear();
            mContactCache.clear();
            mTransport.setMaxNotifySeq(0);
            mSize = event.getSize();
            if (root != null) {
                try {
                    // skip the cache update if invalid auth/zmailbox instance
                    mailbox.getAccountId();
                } catch (ServiceException e) {
                    ZimbraLog.cache.error("Unable to refresh mailbox item id mappings due to missing auth info.");
                    return;
                }
                mUserRoot = root;
                addIdMappings(mUserRoot);
            }
            if (tags != null) {
                if (mNameToTag == null) {
                    mNameToTag = new HashMap<String, ZTag>();
                } else {
                    mNameToTag.clear();
                }
                for (ZTag tag : tags) {
                    addTag(tag);
                }
            }
        }

        @Override
        public synchronized void handleCreate(ZCreateEvent event, ZMailbox mailbox) {
            // do nothing
        }

        @Override
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
                    if (newParentId != null && !newParentId.equals(f.getParentId())) {
                        reparent(f, newParentId);
                    }
                    f.modifyNotification(event);
                }
            } else if (event instanceof ZModifyMailboxEvent) {
                ZModifyMailboxEvent mme = (ZModifyMailboxEvent) event;
                // FIXME: logic should be different if ZMailbox points at another user's mailbox
                if (mme.getOwner(null) == null) {
                    mSize = mme.getSize(mSize);
                }
            } else if (event instanceof ZModifyMessageEvent) {
                ZModifyMessageEvent mme = (ZModifyMessageEvent) event;
                CachedMessage cm = mMessageCache.get(mme.getId());
                if (cm != null) {
                    cm.zm.modifyNotification(mme);
                }
            } else if (event instanceof ZModifyContactEvent) {
                ZModifyContactEvent mce = (ZModifyContactEvent) event;
                ZContact contact = mContactCache.get(mce.getId());
                if (contact != null) {
                    contact.modifyNotification(mce);
                }
            }
        }

        @Override
        public synchronized void handleDelete(ZDeleteEvent event, ZMailbox mailbox) {
            for (String id : event.toList()) {
                mMessageCache.remove(id);
                mContactCache.remove(id);
                ZItem item = mItemCache.getById(id);
                if (item instanceof ZMountpoint) {
                    ZMountpoint sl = (ZMountpoint) item;
                    if (sl.getParent() != null) {
                        sl.getParent().removeChild(sl);
                    }
                    mItemCache.removeById(sl.getCanonicalRemoteId());
                } else if (item instanceof ZFolder) {
                    ZFolder sf = (ZFolder) item;
                    if (sf.getParent() != null) {
                        sf.getParent().removeChild(sf);
                    }
                } else if ((item instanceof ZTag) && (mNameToTag != null)) {
                    mNameToTag.remove(((ZTag) item).getName());
                }
                if (item != null) {
                    mItemCache.removeById(item.getId());
                }
            }
        }
    }

    private void addTag(ZTag tag) {
        if (mNameToTag != null) {
            mNameToTag.put(tag.getName(), tag);
        }
        addItemIdMapping(tag);
    }

    protected void addItemIdMapping(ZItem item) {
        mItemCache.put(item);
    }

    protected void addRemoteItemIdMapping(String remoteId, ZItem item) {
        mItemCache.putWithId(remoteId, item);
    }

    private void reparent(ZFolder f, String newParentId) throws ServiceException {
        ZFolder parent = f.getParent();
        if (parent != null) {
            parent.removeChild(f);
        }
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
        if (path.equals(PATH_SEPARATOR)) {
            return PATH_SEPARATOR;
        }
        if (path.charAt(0) != PATH_SEPARATOR_CHAR) {
            throw ServiceException.INVALID_REQUEST("path must be absolute: "+path, null);
        }
        if (path.charAt(path.length()-1) == PATH_SEPARATOR_CHAR) {
            path = path.substring(0, path.length()-1);
        }
        int index = path.lastIndexOf(PATH_SEPARATOR_CHAR);
        path = path.substring(0, index);
        if (path.length() == 0) {
            return PATH_SEPARATOR;
        } else {
            return path;
        }
    }

    /**
     * returns the base folder path. First removes a trailing {@link #PATH_SEPARATOR} if one is present, then
     * returns the value of the path trailing the last {@link #PATH_SEPARATOR} in the path.
     * @throws ServiceException if an error occurs
     * @return base path
     * @param path the path we are getting the base from
     */
    public static String getBasePath(String path) throws ServiceException {
        if (path.equals(PATH_SEPARATOR)) {
            return PATH_SEPARATOR;
        }
        if (path.charAt(0) != PATH_SEPARATOR_CHAR) {
            throw ServiceException.INVALID_REQUEST("path must be absolute: "+path, null);
        }
        if (path.charAt(path.length()-1) == PATH_SEPARATOR_CHAR) {
            path = path.substring(0, path.length()-1);
        }
        int index = path.lastIndexOf(PATH_SEPARATOR_CHAR);
        return path.substring(index+1);
    }

    /**
     * @return current size of mailbox in bytes
     * @throws com.zimbra.common.service.ServiceException on error
     */
    @Override
    public long getSize() throws ServiceException {
        populateFolderCache();
        return mSize;
    }

    /**
     * @return account name of mailbox
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public String getName() throws ServiceException {
        if (name != null) {
            return name;
        }
        return getAccountInfo(false).getName();
    }

    public ZMailbox setName(String name) {
        this.name = name;
        return this;
    }

    public ZMailbox setAuthName(String authName) {
        this.authName = authName;
        return this;
    }

    @Override
    public String getAccountId() throws ServiceException {
        if (accountId != null) {
            return accountId;
        }
        return getAccountInfo(false).getId();
    }

    public ZMailbox setAccountId(String accountId) {
        this.accountId = accountId;
        return this;
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

    /**
     * @return the last ID assigned to an item successfully created in the mailbox
     * @throws ServiceException
     */
    public int getLastItemIdInMailbox() throws ServiceException {
        GetLastItemIdInMailboxRequest req = new GetLastItemIdInMailboxRequest();
        GetLastItemIdInMailboxResponse resp = invokeJaxb(req);
        return resp.getId();

    }

    public ZLicenses getLicenses() throws ServiceException {
        return getLicenses(false);
    }

    public ZLicenses getLicenses(boolean refresh) throws ServiceException {
        return getAccountInfo(refresh).getLicenses();
    }

    public ZGetInfoResult getAccountInfo(boolean refresh) throws ServiceException {
        if (mGetInfoResult == null || refresh) {
            GetInfoRequest req = new GetInfoRequest(NOT_ZIMLETS);
            GetInfoResponse res = invokeJaxb(req);
            mGetInfoResult = new ZGetInfoResult(res);
        }
        return mGetInfoResult;
    }

    public int getTimeout() {
        return mTransport.getTimeout();
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
    public List<ZTag> getAllTags() throws ServiceException {
        populateTagCache();
        List<ZTag> result = new ArrayList<ZTag>(mNameToTag.values());
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
        ZItem item = mItemCache.getById(id);
        if (item instanceof ZTag) {
            return (ZTag) item;
        } else {
            return null;
        }
    }

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
                if (tag != null) {
                    tags.add(tag);
                }
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
        if (color != null) {
            if (color == ZTag.Color.rgbColor) {
                tagEl.addAttribute(MailConstants.A_RGB, color.getRgbColor());
            } else {
                tagEl.addAttribute(MailConstants.A_COLOR, color.getValue());
            }
        }
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
        if (color != null) {
            if (color == ZTag.Color.rgbColor) {
                action.addAttribute(MailConstants.A_RGB, color.getRgbColor());
            } else {
                action.addAttribute(MailConstants.A_COLOR, color.getValue());
            }
        }
        if (name != null && name.length() > 0) {
            action.addAttribute(MailConstants.A_NAME, name);
        }
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
        if (color == ZTag.Color.rgbColor) {
            return doAction(tagAction("color", id).addAttribute(MailConstants.A_RGB, color.getRgbColor()));
        } else {
            return doAction(tagAction("color", id).addAttribute(MailConstants.A_COLOR, color.getValue()));
        }
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
        if (optFolderId != null) {
            req.addAttribute(MailConstants.A_FOLDER, optFolderId);
        }
        if (sortBy != null) {
            req.addAttribute(MailConstants.A_SORTBY, sortBy.name());
        }
        if (sync) {
            req.addAttribute(MailConstants.A_SYNC, sync);
        }
        req.addAttribute(MailConstants.A_WANT_IMAP_UID, true);

        if (attrs != null) {
            for (String name : attrs) {
                req.addNonUniqueElement(MailConstants.E_ATTRIBUTE).addAttribute(MailConstants.A_ATTRIBUTE_NAME, name);
            }
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

        @Override
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
        return createContact(folderId, tags, attrs, null, null);
    }
    /**
     * Creates a new contact.
     * @param folderId the new contact's folder id
     * @param tags tags to set on the contact, or <tt>null</tt>
     * @param attrs contact attributes (key/value)
     * @param members members of a contact group
     * @return the new contact
     * @throws ServiceException
     */
    public ZContact createContactWithMembers(String folderId, String tags, Map<String, String> attrs, Map<String, String> members) throws ServiceException {
        return createContact(folderId, tags, attrs, null, members);
    }


    public ZContact createContact(String folderId, String tags, Map<String, String> attrs, Map<String, ZAttachmentInfo> attachments) throws ServiceException {
        return createContact(folderId, tags, attrs, attachments, null);
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
    public ZContact createContact(String folderId, String tags, Map<String, String> attrs,
            Map<String, ZAttachmentInfo> attachments, Map<String, String> members)
    throws ServiceException {
        ContactSpec contactSpec = new ContactSpec();
        CreateContactRequest ccReq = new CreateContactRequest(contactSpec);
        ccReq.setWantImapUid(true);
        ccReq.setWantModifiedSequence(true);
        if (folderId != null) {
            contactSpec.setFolder(folderId);
        }
        if (tags != null) {
            contactSpec.setTags(tags);
        }
        addAttrsAndAttachmentsToContact(contactSpec, attrs, attachments);
        if (members != null) {
            for (Map.Entry<String, String> entry : members.entrySet()) {
                contactSpec.addContactGroupMemberWithTypeAndValue(entry.getValue(), entry.getKey());
            }
        }
        return new ZContact(invokeJaxbToElement(ccReq).getElement(MailConstants.E_CONTACT), this);
    }

    private static void addAttrsAndAttachmentsToContact(
            SpecifyContact<? extends NewContactAttr, ? extends NewContactGroupMember> contactSpec,
            Map<String, String> attrs, Map<String, ZAttachmentInfo> attachments) {
        if (attrs != null) {
            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                contactSpec.addAttrWithNameAndValue(entry.getKey(), entry.getValue().trim());
            }
        }
        if (attachments != null) {
            for (String name : attachments.keySet()) {
                ZAttachmentInfo info = attachments.get(name);
                NewContactAttr attach = contactSpec.addAttrWithName(name);
                if (info.getAttachmentId() != null) {
                    attach.setAttachId(info.getAttachmentId());
                } else if (info.getItemId() != null) {
                    attach.setId(Integer.parseInt(info.getItemId()));
                    attach.setPart(info.getPartName());
                } else if (info.getPartName() != null) {
                    attach.setPart(info.getPartName());
                }
            }
        }
    }

    /**
     * @param id of contact
     * @param replace if true, replace all attrs with specified attrs, otherwise merge with existing
     * @param attrs modified attrs
     * @param members members of a contact group
     * @return updated contact
     * @throws ServiceException on error
     */
    public ZContact modifyContactWithMembers(String id, boolean replace, Map<String, String> attrs,  Map<String, String> members) throws ServiceException {
        return modifyContact(id, replace, attrs, null, members);
    }

    /**
     * @param id of contact
     * @param replace if true, replace all attrs with specified attrs, otherwise merge with existing
     * @param attrs modified attrs
     * @return updated contact
     * @throws ServiceException on error
     */
    public ZContact modifyContact(String id, boolean replace, Map<String, String> attrs) throws ServiceException {
        return modifyContact(id, replace, attrs, null, null);
    }

    /**
     * @param id of contact
     * @param replace if true, replace all attrs with specified attrs, otherwise merge with existing
     * @param attrs modified attrs
     * @param attachments contact attachments (key/upload id) or <tt>null</tt>
     * @return updated contact
     * @throws ServiceException on error
     */
    public ZContact modifyContact(String id, boolean replace, Map<String, String> attrs, Map<String, ZAttachmentInfo> attachments) throws ServiceException {
        return modifyContact(id, replace, attrs, attachments, null);
    }

    /**
     * @param id of contact
     * @param replace if true, replace all attrs with specified attrs, otherwise merge with existing
     * @param attrs modified attrs, or <tt>null</tt>
     * @param attachments modified attachments , or <tt>null</tt>
     * @param members members of a contact group
     * @return updated contact
     * @throws ServiceException on error
     */
    public ZContact modifyContact(String id, boolean replace, Map<String, String> attrs, Map<String, ZAttachmentInfo> attachments, Map<String, String> members)
            throws ServiceException {
        ModifyContactSpec contactSpec = ModifyContactSpec.createForId(Integer.valueOf(id));
        ModifyContactRequest mcReq = new ModifyContactRequest(contactSpec);
        mcReq.setWantImapUid(true);
        mcReq.setWantModifiedSequence(true);
        mcReq.setReplace(replace);
        addAttrsAndAttachmentsToContact(contactSpec, attrs, attachments);
        if (members != null) {
            for (Map.Entry<String, String> entry : members.entrySet()) {
                contactSpec.addContactGroupMemberWithTypeAndValue(entry.getValue(), entry.getKey());
            }
        }
        return new ZContact(invokeJaxbToElement(mcReq).getElement(MailConstants.E_CONTACT), this);
    }

    /**
     * Fetch contacts for a given folder or contacts ids
     * @param folderid folder id of the contact folder
     * @param ids comma-separated list of contact ids
     * @param attrs limit attrs returns to given list
     * @param sortBy sort results (null for no sorting)
     * @param sync if true, return modified date on contacts
     * @return list of contacts
     * @throws ServiceException on error
     */
    public List<ZContact> getContactsForFolder(String folderid, String ids, ContactSortBy sortBy, boolean sync, List<String> attrs) throws ServiceException {
        Element req = newRequestElement(MailConstants.GET_CONTACTS_REQUEST);

        if (!StringUtil.isNullOrEmpty(folderid)) {
            req.addAttribute(MailConstants.A_FOLDER, folderid);
        }
        if (sortBy != null) {
            req.addAttribute(MailConstants.A_SORTBY, sortBy.name());
        }
        if (sync) {
            req.addAttribute(MailConstants.A_SYNC, sync);
        }
        req.addAttribute(MailConstants.A_WANT_IMAP_UID, true);
        if (!StringUtil.isNullOrEmpty(ids)) {
            req.addAttribute(MailConstants.A_DEREF_CONTACT_GROUP_MEMBER, true);
            req.addNonUniqueElement(MailConstants.E_CONTACT).addAttribute(MailConstants.A_ID, ids);
        }
        if (attrs != null) {
            for (String name : attrs) {
                req.addNonUniqueElement(MailConstants.E_ATTRIBUTE).addAttribute(MailConstants.A_ATTRIBUTE_NAME, name);
            }
        }
        List<ZContact> result = new ArrayList<ZContact>();
        for (Element cn : invoke(req).listElements(MailConstants.E_CONTACT)) {
            result.add(new ZContact(cn, this));
        }
        return result;
    }

    public List<ZContact> getContacts(String ids, ContactSortBy sortBy, boolean sync, List<String> attrs) throws ServiceException {
        return getContactsForFolder(null, ids, sortBy, sync, attrs);
    }

    public ZContact getContact(ItemIdentifier itemId) throws ServiceException {
        try {
        return getContact(itemId.toString());
        } catch (SoapFaultException sfe) {
            if (sfe.getMessage().startsWith("no such contact")) {
                throw ZClientException.NO_SUCH_CONTACT(itemId.id, sfe);
            }
            throw sfe;
        }
    }

    /**
     *
     * @param id single contact id to fetch
     * @return fetched contact
     * @throws ServiceException on error
     */
    public ZContact getContact(String id) throws ServiceException {
        ZContact result = mContactCache.get(id);
        if (result == null || result.isDirty()) {
            Element req = newRequestElement(MailConstants.GET_CONTACTS_REQUEST);
            req.addAttribute(MailConstants.A_SYNC, true);
            req.addAttribute(MailConstants.A_WANT_IMAP_UID, true);
            req.addNonUniqueElement(MailConstants.E_CONTACT).addAttribute(MailConstants.A_ID, id);
            req.addAttribute(MailConstants.A_DEREF_CONTACT_GROUP_MEMBER, true);
            result = new ZContact(invoke(req).getElement(MailConstants.E_CONTACT), this);
            /* Note: that ZContact.getImapUid() relies on cache hits having Imap UID information available.
             *       At present, this is the only put to the cache, so that should be fine. */
            mContactCache.put(id, result);
        }
        return result;
    }

    public ZContact getContactFromCache(String id) {
        lock();
        try {
            return mContactCache.get(id);
        } finally {
            unlock();
        }
    }

    public List<ZAutoCompleteMatch> autoComplete(String query, int limit) throws ServiceException {
        Element req = newRequestElement(MailConstants.AUTO_COMPLETE_REQUEST);
        req.addAttribute(MailConstants.A_LIMIT, limit);
        req.addAttribute(MailConstants.A_INCLUDE_GAL, getFeatures().getGalAutoComplete());
        req.addUniqueElement(MailConstants.E_NAME).setText(query);
        Element response = invoke(req);
        List<ZAutoCompleteMatch> matches = new ArrayList<ZAutoCompleteMatch>();
        for (Element match : response.listElements(MailConstants.E_MATCH)) {
            matches.add(new ZAutoCompleteMatch(match, this));
        }
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

    @Deprecated
    public synchronized ZContact getMyCard() {
        return null;
    }

    @Deprecated
    public boolean getIsMyCard(String ids) {
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
        if (destFolderId != null && destFolderId.length() > 0) {
            actionEl.addAttribute(MailConstants.A_FOLDER, destFolderId);
        }
        if (tagList != null) {
            actionEl.addAttribute(MailConstants.A_TAGS, tagList);
        }
        if (flags != null) {
            actionEl.addAttribute(MailConstants.A_FLAGS, flags);
        }
        return doAction(actionEl);
    }

    public static class ZImportContactsResult {

        private final String mIds;
        private final long mCount;

        public ZImportContactsResult(Element response) throws ServiceException {
            mIds = response.getAttribute(MailConstants.A_ID, null);
            mCount = response.getAttributeLong(MailConstants.A_NUM);
        }

        public ZImportContactsResult(ImportContactsResponse res) {
            ImportContact impCntct = res.getContact();
            mIds = impCntct.getListOfCreatedIds();
            mCount = impCntct.getNumImported();
        }

        public String getIds() {
            return mIds;
        }

        public long getCount() {
            return mCount;
        }
    }

    public ZImportContactsResult importContacts(String folderId, String type, String attachmentId) throws ServiceException {
        ImportContactsRequest request = new ImportContactsRequest();
        request.setContentType(type);
        request.setFolderId(folderId);
        Content importContent = new Content();
        importContent.setAttachUploadId(attachmentId);
        request.setContent(importContent);
        ImportContactsResponse res = this.invokeJaxb(request);
        return new ZImportContactsResult(res);
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

    private Element convAction(String op, String id, String constraints) {
        Element req = newRequestElement(MailConstants.CONV_ACTION_REQUEST);
        Element actionEl = req.addUniqueElement(MailConstants.E_ACTION);
        actionEl.addAttribute(MailConstants.A_ID, id);
        actionEl.addAttribute(MailConstants.A_OPERATION, op);
        if (constraints != null) {
            actionEl.addAttribute(MailConstants.A_TARGET_CONSTRAINT, constraints);
        }
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
        if (destFolderId != null && destFolderId.length() > 0) {
            actionEl.addAttribute(MailConstants.A_FOLDER, destFolderId);
        }
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
        if (constraints != null) {
            actionEl.addAttribute(MailConstants.A_TARGET_CONSTRAINT, constraints);
        }
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
     * permanently delete item(s) from the dumpster
     *
     * @param ids list of item ids to act on
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult dumpsterDeleteItem(String ids) throws ServiceException {
        return doAction(itemAction("dumpsterdelete", ids, null));
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
        if (destFolderId != null && destFolderId.length() > 0) {
            actionEl.addAttribute(MailConstants.A_FOLDER, destFolderId);
        }
        if (tagList != null) {
            actionEl.addAttribute(MailConstants.A_TAGS, tagList);
        }
        if (flags != null) {
            actionEl.addAttribute(MailConstants.A_FLAGS, flags);
        }
        return doAction(actionEl);
    }

    public void recordImapSession(ItemIdentifier folderId) throws ServiceException {
        RecordIMAPSessionRequest req = new RecordIMAPSessionRequest(folderId.toString());
        RecordIMAPSessionResponse resp = invokeJaxb(req);
        String folderUuid = resp.getFolderUuid();
        int lastItemId = resp.getLastItemId();
        ZFolder folder = this.getFolderByUuid(folderUuid);
        if (folder != null) {
            folder.updateImapRECENTCutoff(lastItemId);
        }
    }

    /**
     * recover items from the dumpster to the specified folder
     *
     * @param ids list of item ids to act on
     * @param destFolderId id of destination folder
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult recoverItem(String ids, String destFolderId) throws ServiceException {
        return doAction(itemAction("recover", ids, null).addAttribute(MailConstants.A_FOLDER, destFolderId));
    }

    /* ------------------------------------------------- */

    /**
     * Uploads files to <tt>FileUploadServlet</tt>.
     * @return the attachment id
     */
    public String uploadAttachments(File[] files, int msTimeout) throws ServiceException {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            String contentType = URLConnection.getFileNameMap().getContentTypeFor(file.getName());
            if (contentType != null) {
                builder.addBinaryBody("upfile", file, ContentType.create(contentType, "UTF-8"), file.getName());
            } else {
                builder.addBinaryBody("upfile", file, ContentType.DEFAULT_BINARY, file.getName());
            }
        }

        return uploadAttachments(builder, msTimeout);
    }

    /**
     * Uploads a byte array to <tt>FileUploadServlet</tt>.
     * @return the attachment id
     */
    public String uploadAttachment(String name, byte[] content, String contentType, int msTimeout) throws ServiceException {

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("upfile",content, ContentType.create(contentType), name);
        return uploadAttachments(builder, msTimeout);
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
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        for (String name : attachments.keySet()) {
            byte[] content = attachments.get(name);
            String contentType = URLConnection.getFileNameMap().getContentTypeFor(name);
            if (contentType != null) {
                builder.addBinaryBody(name, content, ContentType.create(contentType), name);
            } else {
                builder.addBinaryBody(name, content, ContentType.DEFAULT_BINARY, name);
            }
        }

        return uploadAttachments(builder, msTimeout);
    }

    /**
     * Creates an <tt>HttpClient FilePart</tt> from the given filename and content.
     */
    public ByteArrayBody createAttachmentPart(String filename, byte[] content) {
        String contentType = URLConnection.getFileNameMap().getContentTypeFor(filename);
        ByteArrayBody part = new ByteArrayBody(content, ContentType.create(contentType), filename);
        return part;
    }

    /**
     * Uploads HTTP post parts to <tt>FileUploadServlet</tt>.
     * @return the attachment id
     */
    public String uploadAttachments(MultipartEntityBuilder builder, int msTimeout) throws ServiceException {
        String aid = null;

        URI uri = getUploadURI();
        // make the post
        HttpPost post = new HttpPost(uri.toString());

        HttpClientBuilder clientBuilder = getHttpClientBuilder(uri);
        SocketConfig config = SocketConfig.custom().setSoTimeout(msTimeout).build();
        clientBuilder.setDefaultSocketConfig(config);
        HttpClient client = clientBuilder.build();
        int statusCode;
        try {
            if (mCsrfToken != null) {
                post.addHeader(Constants.CSRF_TOKEN, mCsrfToken);
            }
            HttpEntity entity = builder.build();
            post.setEntity(entity);
            HttpResponse response = HttpClientUtil.executeMethod(client, post);
            statusCode = response.getStatusLine().getStatusCode();

            // parse the response
            if (statusCode == HttpServletResponse.SC_OK) {
                String content = EntityUtils.toString(response.getEntity());
                aid = getAttachmentId(content);
            } else if (statusCode == HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE) {
                throw ZClientException.UPLOAD_SIZE_LIMIT_EXCEEDED("upload size limit exceeded", null);
            } else {
                throw ZClientException.UPLOAD_FAILED("Attachment post failed, status=" + statusCode, null);
            }
        } catch (IOException | HttpException e) {
            throw ZClientException.IO_ERROR(e.getMessage(), e);
        } finally {
            post.releaseConnection();
        }
        return aid;
    }



    public String uploadContentAsStream(String name, InputStream in, String contentType, long contentLength, int msTimeout)
            throws ServiceException {
        return uploadContentAsStream(name, in, contentType, contentLength, msTimeout, false);
    }

    public String uploadContentAsStream(String name, InputStream in, String contentType, long contentLength, int msTimeout, boolean limitByFileUploadMaxSize)
            throws ServiceException {
        String aid = null;
        if (name != null) {
            contentType += "; name=" + name;
        }

        URI uri = getUploadURI(limitByFileUploadMaxSize);
        // make the post
        HttpPost post = new HttpPost(uri.toString());
        HttpClientBuilder clientBuilder = getHttpClientBuilder(uri);
        SocketConfig config = SocketConfig.custom().setSoTimeout(msTimeout).build();
        clientBuilder.setDefaultSocketConfig(config);
        clientBuilder.setRetryHandler(new InputStreamRequestHttpRetryHandler());
        HttpClient client = clientBuilder.build();
        int statusCode;
        try {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("upfile", in, ContentType.DEFAULT_BINARY, name);
            HttpEntity httpEntity = builder.build();
            post.setEntity(httpEntity);

            if (mCsrfToken != null) {
                post.addHeader(Constants.CSRF_TOKEN, mCsrfToken);
            }

            HttpResponse response = HttpClientUtil.executeMethod(client, post);
            statusCode = response.getStatusLine().getStatusCode();

            // parse the response
            if (statusCode == HttpServletResponse.SC_OK) {
                String content = EntityUtils.toString(response.getEntity());
                aid = getAttachmentId(content);
            } else if (statusCode == HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE) {
                throw ZClientException.UPLOAD_SIZE_LIMIT_EXCEEDED("upload size limit exceeded", null);
            } else {
                throw ZClientException.UPLOAD_FAILED("Attachment post failed, status=" + statusCode, null);
            }
        } catch (IOException | HttpException e) {
            throw ZClientException.IO_ERROR(e.getMessage(), e);
        } finally {
            post.releaseConnection();
        }
        return aid;
    }

    public URI getUploadURI() throws ServiceException {
        return  getUploadURI(false);
    }

    private URI getUploadURI(boolean limitByFileUploadMaxSize)  throws ServiceException {
        try {
            URI uri = new URI(mTransport.getURI());
            return  uri.resolve("/service/upload?fmt=raw" + (limitByFileUploadMaxSize ? "&lbfums" : ""));
        } catch (URISyntaxException e) {
            throw ZClientException.CLIENT_ERROR("unable to parse URI: "+mTransport.getURI(), e);
        }
    }

    public static String getAttachmentId(String result) throws ZClientException {
        if (result.startsWith(HttpServletResponse.SC_OK+"")) {
            Matcher m = sAttachmentId.matcher(result);
            return m.find() ? m.group(1) : null;
        } else if (result.startsWith(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE+"")) {
            throw ZClientException.UPLOAD_SIZE_LIMIT_EXCEEDED("upload size limit exceeded", null);
        }
        throw ZClientException.UPLOAD_FAILED("upload failed, response: " + result, null);
    }

    public HttpClient getHttpClient(URI uri) {
        boolean isAdmin = uri.getPort() == LC.zimbra_admin_service_port.intValue();
        BasicCookieStore initialState = HttpClientUtil.newHttpState(getAuthToken(), uri.getHost(), isAdmin);
        HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        clientBuilder.setDefaultCookieStore(initialState);

        RequestConfig reqConfig = RequestConfig.copy(
            ZimbraHttpConnectionManager.getInternalHttpConnMgr().getZimbraConnMgrParams().getReqConfig())
            .setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).build();

        clientBuilder.setDefaultRequestConfig(reqConfig);
        return clientBuilder.build();
    }

    public HttpClientBuilder getHttpClientBuilder(URI uri) {
        boolean isAdmin = uri.getPort() == LC.zimbra_admin_service_port.intValue();
        BasicCookieStore initialState = HttpClientUtil.newHttpState(getAuthToken(), uri.getHost(), isAdmin);
        HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        clientBuilder.setDefaultCookieStore(initialState);

        RequestConfig reqConfig = RequestConfig.copy(
            ZimbraHttpConnectionManager.getInternalHttpConnMgr().getZimbraConnMgrParams().getReqConfig())
            .setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).build();

        clientBuilder.setDefaultRequestConfig(reqConfig);
        return clientBuilder;
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
    public String addMessage(String folderId, String flags, String tags, long receivedDate, String content, boolean noICal)
            throws ServiceException {
        return addMessage(folderId, flags, tags, receivedDate, content, noICal, false);
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
     * @param filterSent if TRUE, then do outgoing message filtering
     * @return ID of newly created message
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public String addMessage(
            String folderId, String flags, String tags, long receivedDate, String content, boolean noICal, boolean filterSent)
                    throws ServiceException {
        Element req = newRequestElement(MailConstants.ADD_MSG_REQUEST);
        if (filterSent) {
            req.addAttribute(MailConstants.A_FILTER_SENT, filterSent);
        }
        Element m = req.addUniqueElement(MailConstants.E_MSG);
        m.addAttribute(MailConstants.A_FOLDER, folderId);
        if (flags != null && flags.length() > 0) {
            m.addAttribute(MailConstants.A_FLAGS, flags);
        }
        if (tags != null && tags.length() > 0) {
            m.addAttribute(MailConstants.A_TAGS, tags);
        }
        if (receivedDate != 0) {
            m.addAttribute(MailConstants.A_DATE, receivedDate);
        }
        m.addAttribute(MailConstants.A_NO_ICAL, noICal);
        m.addNonUniqueElement(MailConstants.E_CONTENT).setText(content);
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
        return addMessage(folderId, flags, TagSpecifier.tagById(tags), receivedDate, in, contentLength, noICal);
    }

    /**
     * @param folderId (required) folderId of folder to add message to
     * @param flags non-comma-separated list of flags, e.g. "sf" for "sent by me and flagged",
     *        or <tt>null</tt>
     * @param tags TagSpecifier instance, or <tt>null</tt>
     * @param receivedDate time the message was originally received, in MILLISECONDS since the epoch,
     *        or <tt>0</tt> for the current time
     * @param in content stream
     * @param contentLength number of bytes in the content stream
     * @param noICal if TRUE, then don't process iCal attachments.
     * @return ID of newly created message
     * @throws ServiceException on error
     */
    public String addMessage(String folderId, String flags, TagSpecifier tags, long receivedDate,
            InputStream in, long contentLength, boolean noICal)
                    throws ServiceException {
        // first, upload the content via the FileUploadServlet
        String aid = uploadContentAsStream("message", in, "message/rfc822", contentLength, 5000);

        // now, use the returned upload ID to do the message send
        Element req = newRequestElement(MailConstants.ADD_MSG_REQUEST);
        Element m = req.addUniqueElement(MailConstants.E_MSG);
        m.addAttribute(MailConstants.A_FOLDER, folderId);
        if (flags != null && flags.length() > 0) {
            m.addAttribute(MailConstants.A_FLAGS, flags);
        }
        if (tags != null) {
            tags.encodeTags(m);
        }
        if (receivedDate > 0) {
            m.addAttribute(MailConstants.A_DATE, receivedDate);
        }
        m.addAttribute(MailConstants.A_ATTACHMENT_ID, aid);
        m.addAttribute(MailConstants.A_NO_ICAL, noICal);
        return invoke(req).getElement(MailConstants.E_MSG).getAttribute(MailConstants.A_ID);
    }


    static class CachedMessage {
        protected ZGetMessageParams params;
        protected ZMessage zm;
    }

    public ZMessage getMessage(ZGetMessageParams params) throws ServiceException {
        return getMessage(params, false);
    }

    public ZMessage getMessage(ZGetMessageParams params, boolean inCacheOnly) throws ServiceException {
        lock();
        try {
            CachedMessage cm = mMessageCache.get(params.getId());
            if (cm == null || !cm.params.equals(params)) {
                if (inCacheOnly) {
                    return null;
                }
                Element req = newRequestElement(MailConstants.GET_MSG_REQUEST);
                Element msgEl = req.addUniqueElement(MailConstants.E_MSG);
                msgEl.addAttribute(MailConstants.A_ID, params.getId());
                if (params.getPart() != null) {
                    msgEl.addAttribute(MailConstants.A_PART, params.getPart());
                }
                msgEl.addAttribute(MailConstants.A_MARK_READ, params.isMarkRead());
                msgEl.addAttribute(MailConstants.A_WANT_HTML, params.isWantHtml());
                msgEl.addAttribute(MailConstants.A_NEUTER, params.isNeuterImages());
                msgEl.addAttribute(MailConstants.A_RAW, params.isRawContent());
                msgEl.addAttribute(MailConstants.A_WANT_IMAP_UID, true);
                msgEl.addAttribute(MailConstants.A_WANT_MODIFIED_SEQUENCE, true);
                Integer max = params.getMax();
                if (max != null) {
                    msgEl.addAttribute(MailConstants.A_MAX_INLINED_LENGTH, max);
                    if (params.isRawContent() && (max == 0)) {
                        msgEl.addAttribute(MailConstants.A_USE_CONTENT_URL, true);
                    }
                }
                //header request bug:33054
                String reqHdrs = params.getReqHeaders();
                if (reqHdrs != null && reqHdrs.length() > 0) {
                    for (String hdrName : reqHdrs.split(",")) {
                        Element headerEl = msgEl.addNonUniqueElement(MailConstants.A_HEADER);
                        headerEl.addAttribute(MailConstants.A_ATTRIBUTE_NAME, hdrName);
                    }
                }
                ZMessage zm = new ZMessage(invoke(req).getElement(MailConstants.E_MSG), this);
                cm = new CachedMessage();
                cm.zm = zm;
                cm.params = params;
                /* Note: that ZMessage.getImapUid() relies on cache hits having Imap UID information available.
                 *       At present, this is the only put to the cache, so that should be fine. */
                mMessageCache.put(params.getId(), cm);
            } else {
                if (params.isMarkRead() && cm.zm.isUnread()) {
                    markMessageRead(cm.zm.getId(), true);
                }
            }
            return cm.zm;
        } finally {
            unlock();
        }
    }

    public ZMessage getMessageById(String id, boolean raw, Integer max) throws ServiceException {
        ZGetMessageParams params = new ZGetMessageParams();
        params.setId(id);
        params.setRawContent(raw);
        params.setMax(max);
        return getMessage(params);
    }

    private ZMessage getMessageFromCache(String id, boolean raw, Integer max) throws ServiceException {
        ZGetMessageParams params = new ZGetMessageParams();
        params.setId(id);
        params.setRawContent(raw);
        params.setMax(max);
        return getMessage(params, true);
    }

    public ZMessage getMessageFromCache(String id) throws ServiceException {
        return getMessageFromCache(id, false, null);
    }

    public List<ZMessage> getMessagesByIds(String ids) throws ServiceException {
        List<ZMessage> list = new ArrayList<ZMessage>();
        if (!StringUtil.isNullOrEmpty(ids)) {
            String[] idsArray = ids.split(",");
            for (String id : idsArray) {
                ZMessage msg = null;
                try {
                    msg = getMessageById(id, false, null);
                } catch (ServiceException se) {
                    ZimbraLog.sync.debug("Exception occured while fetching msg for id = %s", id);
                    ZimbraLog.sync.debug(se);
                }
                if (msg != null) {
                    list.add(msg);
                }
            }
        }
        return list;
    }

    public ZMessage getMessageById(String id) throws ServiceException {
        return getMessageById(id, false, null);
    }

    public ZMessage getMessageById(ItemIdentifier itemId, boolean raw, Integer max) throws ServiceException {
        try {
            return getMessageById(itemId.toString(), raw, max);
        } catch (SoapFaultException sfe) {
            if (sfe.getMessage().startsWith("no such message")) {
                throw ZClientException.NO_SUCH_MSG(itemId.id, sfe);
            }
            throw sfe;
        }
    }

    public ZMessage getMessageById(ItemIdentifier itemId) throws ServiceException {
        return getMessageById(itemId, false, null);
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

    @Override
    public void flagItemAsRead(OpContext octxt, ItemIdentifier itemId, MailItemType type) throws ServiceException {
        /* TODO: will this work for contacts - and does it even need to? */
        markMessageRead(itemId.toString(), true);
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
        if (destFolderId != null && destFolderId.length() > 0) {
            actionEl.addAttribute(MailConstants.A_FOLDER, destFolderId);
        }
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
        if (destFolderId != null && destFolderId.length() > 0) {
            actionEl.addAttribute(MailConstants.A_FOLDER, destFolderId);
        }
        if (tagList != null) {
            actionEl.addAttribute(MailConstants.A_TAGS, tagList);
        }
        if (flags != null) {
            actionEl.addAttribute(MailConstants.A_FLAGS, flags);
        }
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

    public List<ShareInfo> getSharesFromOwnerWithName(String ownerName) throws ServiceException {
        GetShareInfoRequest req = GetShareInfoRequest.create(AccountSelector.fromName(ownerName),
                null /* grantee ID */, false /* includeSelf */);
        GetShareInfoResponse resp = invokeJaxb(req);
        return resp.getShares();
    }

    public List<ZFolder> getFolderSharedFromOwner(String ownerName) throws ServiceException {
        List<ShareInfo> shares = getSharesFromOwnerWithName(ownerName);
        if (shares == null) {
            return Collections.emptyList();
        }
        List<ZFolder> zfolders = Lists.newArrayListWithExpectedSize(shares.size());
        for (ShareInfo share : shares) {
            zfolders.add(getSharedFolderById(
                        ItemIdentifier.fromAccountIdAndItemId(share.getOwnerId(), share.getFolderId()).toString()));
        }
        return zfolders;
    }

    public ZSharedFolder getFolderSharedFromOwnerWithPath(String ownerName, String path) throws ServiceException {
        List<ShareInfo> shares = getSharesFromOwnerWithName(ownerName);
        if (shares == null) {
            return null;
        }
        for (ShareInfo share : shares) {
            String folderPath = share.getFolderPath();
            if (StringUtils.startsWithIgnoreCase(path, folderPath)) {
                ZSharedFolder sFolder = getSharedFolderById(
                        ItemIdentifier.fromAccountIdAndItemId(share.getOwnerId(), share.getFolderId()).toString());
                if (path.equalsIgnoreCase(folderPath)) {
                    return sFolder;
                }
                String rest = path.substring(folderPath.length());
                if (rest.startsWith(ZMailbox.PATH_SEPARATOR)) {
                    ZFolder zf = sFolder.getSubFolderByPath(rest.substring(1));
                    if (zf instanceof ZSharedFolder) {
                        return (ZSharedFolder)zf;
                    }
                }
                return null;
            }
        }
        return null;
    }

    /**
     * find the folder with the specified path, starting from the user root.
     * @param path path of folder. Must start with {@link #PATH_SEPARATOR}.
     * @return ZFolder if found, null otherwise.
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public ZFolder getFolderByPath(String path) throws ServiceException {
        populateFolderCache();
        if (!path.startsWith(ZMailbox.PATH_SEPARATOR)) {
            path = ZMailbox.PATH_SEPARATOR + path;
        }
        if (mUserRoot == null) {
            return null;
        }
        return mUserRoot.getSubFolderByPath(path.substring(1));
    }

    @Override
    public FolderStore getFolderByPath(OpContext octxt, String path) throws ServiceException {
        return getFolderByPath(path);
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
        ZItem item = mItemCache.getById(id);
        if (item == null && mUserRoot != null) {
            // try rebuilding folder cache
            addIdMappings(mUserRoot);
        }
        item = mItemCache.getById(id);
        if (item == null) {
            /* sometimes when working on behalf of, we're getting just by item id but the
             * cache has fully qualified keys.  Or sometimes it is the other way around.
             */
            try {
                String ident = ItemIdentifier.asSimplestString(id, this.getAccountId());
                if (!id.equals(ident)) {
                    item = mItemCache.getById(ident);
                }
            } catch (ServiceException e) {
                // This can be raised when constructing an ItemIdentifier with a folder name; e.g., "INBOX"
                if (!(e.getCause() instanceof NumberFormatException)) {
                    throw e;
                }
            }
        }
        if (!(item instanceof ZFolder)) {
            return null;
        }
        ZFolder folder = (ZFolder) item;
        return folder.isHierarchyPlaceholder() ? null : folder;
    }

    /**
     * @param id - String representation of integer ID of the folder in its mailbox
     * @return FolderStore or null
     */
    @Override
    public FolderStore getFolderById(OpContext octxt, String id) throws ServiceException {
        return getFolderById(id);
    }

    /**
     * find the folder with the specified UUID.
     * @param uuid UUID of folder
     * @return ZFolder if found, null otherwise.
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public ZFolder getFolderByUuid(String uuid) throws ServiceException {
        populateFolderCache();
        ZItem item = mItemCache.getByUuid(uuid);
        if (!(item instanceof ZFolder)) {
            return null;
        }
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
        req.addNonUniqueElement(MailConstants.E_FOLDER).addAttribute(MailConstants.A_FOLDER, id);

        Element response = invoke(req);
        Element eFolder = response.getOptionalElement(MailConstants.E_FOLDER);
        if (eFolder == null) {
            eFolder = response.getOptionalElement(MailConstants.E_MOUNT);
        }
        if (eFolder == null) {
            return null;
        }

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

    /** always bypass caching and issues a GetFolderRequest */
    public ZSharedFolder getSharedFolderById(String id) throws ServiceException {
        GetFolderRequest req = new GetFolderRequest(GetFolderSpec.forID(id));
        req.setVisible(true);
        GetFolderResponse resp = invokeJaxb(req);
        Folder folderInfo = resp.getFolder();
        ZSharedFolder folder = new ZSharedFolder(folderInfo, null /* parent */, id, this);
        return folder.isHierarchyPlaceholder() ? null : folder;
    }

    /**
     * Returns all folders and subfolders in this mailbox.
     * @throws ServiceException on error
     * @return all folders and subfolders in this mailbox
     */
    public List<ZFolder> getAllFolders() throws ServiceException {
        populateFolderCache();
        List<ZFolder> allFolders = new ArrayList<ZFolder>();
        if (getUserRoot() != null) {
            addSubFolders(getUserRoot(), allFolders);
        }
        return allFolders;
    }

    @Override
    public List<FolderStore> getUserRootSubfolderHierarchy(OpContext ctxt) throws ServiceException {
        List<FolderStore> folders = Lists.newArrayList(getAllFolders());
        return folders;
    }

    private void addSubFolders(ZFolder folder, List<ZFolder> folderList) throws ServiceException {
        if (!folder.isHierarchyPlaceholder()) {
            folderList.add(folder);
        }
        for (ZFolder subFolder : folder.getSubFolders()) {
            addSubFolders(subFolder, folderList);
        }
    }

    public URI getTransportURI(String relativePath) throws ServiceException {
        String pathPrefix = (relativePath.startsWith("/")) ? "" : "/";
        try {
            URI uri = new URI(mTransport.getURI());
            return  uri.resolve(pathPrefix + relativePath);
        } catch (URISyntaxException e) {
            throw ZClientException.CLIENT_ERROR("unable to parse URI: "+mTransport.getURI(), e);
        }
    }

    public InputStream getResource(URI uri) throws ServiceException {
        return getResource(uri, getTimeout());
    }

    private InputStream getResource(URI uri, int msecTimeout)
    throws ServiceException {
        HttpGet get = null;
        try {

            get = new HttpGet(uri.toString());
            HttpClientBuilder clientBuilder = getHttpClientBuilder(uri);
            if (msecTimeout > -1) {
                SocketConfig config = SocketConfig.custom().setSoTimeout(msecTimeout).build();
                clientBuilder.setDefaultSocketConfig(config);
            }
            HttpClient client = clientBuilder.build();
            HttpResponse response = HttpClientUtil.executeMethod(client, get);
            int statusCode = response.getStatusLine().getStatusCode();

            // parse the response
            if (statusCode == HttpServletResponse.SC_OK) {
                return new GetMethodInputStream(response.getEntity().getContent());
            } else {
                String msg = String.format("GET from %s failed, status=%d.  %s", uri, statusCode, response.getStatusLine().getReasonPhrase());
                throw ServiceException.FAILURE(msg, null);
            }
        } catch (IOException | HttpException e) {
            String msg = String.format("Unable to get resource from '%s' : %s", uri, e.getMessage());
            throw ZClientException.IO_ERROR(msg, e);
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
        String relPathWithParams = relativePath;
        URI uri = null;
        try {
            if (startTimeArg != null) {
                String encodedArg = URLEncoder.encode(startTimeArg, "UTF-8");
                if (!relPathWithParams.contains("?")) {
                    relPathWithParams = relPathWithParams + "?start=" + encodedArg;
                } else {
                    relPathWithParams = relPathWithParams + "&start=" + encodedArg;
                }
            }
            if (endTimeArg != null) {
                String encodedArg = URLEncoder.encode(endTimeArg, "UTF-8");
                if (!relPathWithParams.contains("?")) {
                    relPathWithParams = relPathWithParams + "?end=" + encodedArg;
                } else {
                    relPathWithParams = relPathWithParams + "&end=" + encodedArg;
                }
            }
        } catch (UnsupportedEncodingException e) {
            String msg = String.format("Unable to get REST resource relativePath=%s start=%s end=%s: %s",
                    relPathWithParams, startTimeArg, endTimeArg, e.getMessage());
            throw ZClientException.IO_ERROR(msg, e);
        }
        uri = getRestURI(relPathWithParams, alternateUrl);
        return getResource(uri, msecTimeout);
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
     * @param contentType optional content-type header value (defaults to "binary")
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
        HttpPost post = null;

        try {
            if (ignoreAndContinueOnError) {
                if (!relativePath.contains("?")) {
                    relativePath = relativePath + "?ignore=1";
                } else {
                    relativePath = relativePath + "&ignore=1";
                }
            }
            if (preserveAlarms) {
                if (!relativePath.contains("?")) {
                    relativePath = relativePath + "?preserveAlarms=1";
                } else {
                    relativePath = relativePath + "&preserveAlarms=1";
                }
            }
            URI uri = getRestURI(relativePath, alternateUrl);
            post = new HttpPost(uri.toString());
            HttpClientBuilder clientBuilder = getHttpClientBuilder(uri);
            if (msecTimeout > -1) {
                SocketConfig config = SocketConfig.custom().setSoTimeout(msecTimeout).build();
                clientBuilder.setDefaultSocketConfig(config);
            }
            clientBuilder.setRetryHandler(new InputStreamRequestHttpRetryHandler());
            HttpClient client = clientBuilder.build();
            int statusCode;

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("upfile", is, ContentType.DEFAULT_BINARY, "file");
            HttpEntity httpEntity = builder.build();
            post.setEntity(httpEntity);
            HttpResponse response = HttpClientUtil.executeMethod(client, post);

            statusCode =response.getStatusLine().getStatusCode();
            // parse the response
            if (statusCode != HttpServletResponse.SC_OK) {
                throw ServiceException.FAILURE("POST failed, status=" + statusCode+" "+ response.getStatusLine().getStatusCode(), null);
            }
        } catch (IOException | HttpException e) {
            throw ZClientException.IO_ERROR(e.getMessage(), e);
        } finally {
            if (closeIs) {
                ByteUtil.closeStream(is);
            }
            if (post != null) {
                post.releaseConnection();
            }
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
        ZItem item = mItemCache.getById(id);
        if (item instanceof ZSearchFolder) {
            return (ZSearchFolder) item;
        } else {
            return null;
        }
    }

    /**
     * find the mountpoint with the specified id.
     * @param id id of mountpoint
     * @return ZMountpoint if found, null otherwise.
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public ZMountpoint getMountpointById(String id) throws ServiceException {
        populateFolderCache();
        ZItem item = mItemCache.getById(id);
        if (item instanceof ZMountpoint) {
            return (ZMountpoint) item;
        } else {
            return null;
        }
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
        if (defaultView != null) {
            folderEl.addAttribute(MailConstants.A_DEFAULT_VIEW, defaultView.name());
        }
        if (color != null) {
            if (StringUtil.equal(color.getName(), Color.RGBCOLOR)) {
                folderEl.addAttribute(MailConstants.A_RGB, color.getRgbColorValue());
            } else {
                folderEl.addAttribute(MailConstants.A_COLOR, color.getValue());
            }
        }
        if (flags != null) {
            folderEl.addAttribute(MailConstants.A_FLAGS, flags);
        }
        if (url != null && url.length() > 0) {
            folderEl.addAttribute(MailConstants.A_URL, url);
        }
        Element newFolderEl = invoke(req).getElement(MailConstants.E_FOLDER);
        ZFolder newFolder = getFolderById(newFolderEl.getAttribute(MailConstants.A_ID));
        return newFolder != null ? newFolder : new ZFolder(newFolderEl, null, this);
    }

    @Override
    public void createFolderForMsgs(OpContext octxt, String path) throws ServiceException {
        createFolder(null, path, ZFolder.View.message, ZFolder.Color.DEFAULTCOLOR, null, null);
    }

    /**
     * create a new sub folder of the specified parent folder.
     *
     * @param parentId parent folder id
     * @param name name of new folder
     * @param query search query (required)
     * @param types comma-sep list of types to search for.  Use null for default value.
     * @param sortBy how to sort the result. Use null for default value.
     * @see {@link ZSearchParams#TYPE_MESSAGE}
     * @return newly created search folder
     * @throws ServiceException on error
     * @param color color of folder
     */
    public ZSearchFolder createSearchFolder(String parentId, String name,
            String query, String types, SearchSortBy sortBy, ZFolder.Color color) throws ServiceException {
        NewSearchFolderSpec spec = NewSearchFolderSpec.forNameQueryAndFolder(name, query, parentId);
        if (color != null) {
            if (StringUtil.equal(color.getName(), Color.RGBCOLOR)) {
                spec.setRgb(color.getRgbColorValue());
            } else {
                spec.setColor((byte)color.getValue());
            }
        }
        spec.setSearchTypes(types);
        if (sortBy != null) {
            spec.setSortBy(sortBy.name());
        }
        CreateSearchFolderResponse resp = this.invokeJaxb(new CreateSearchFolderRequest(spec));
        ZSearchFolder newSearch = getSearchFolderById(resp.getSearchFolder().getId());
        return newSearch != null ? newSearch : new ZSearchFolder(resp.getSearchFolder(), null, this);
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
        if (query != null) {
            folderEl.addAttribute(MailConstants.A_QUERY, query);
        }
        if (types != null) {
            folderEl.addAttribute(MailConstants.A_SEARCH_TYPES, types);
        }
        if (sortBy != null) {
            folderEl.addAttribute(MailConstants.A_SORTBY, sortBy.name());
        }
        invoke(req);
        // this assumes notifications will modify the search folder
        return getSearchFolderById(id);
    }

    public static class ZActionResult {
        private final String mIds;
        private final Element mResponse;

        public ZActionResult(Element response) throws ServiceException {
            String ids = response.getElement(MailConstants.E_ACTION).getAttribute(MailConstants.A_ID);
            if (ids == null) {
                ids = "";
            }
            mIds = ids;
            mResponse = response;
        }

        public String getIds() {
            return mIds;
        }

        public String[] getIdsAsArray() {
            return mIds.split(",");
        }

        @Override
        public String toString() {
            return String.format("[ZActionResult %s]", mIds);
        }

        protected Element getResponse() {
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

    @Override
    public void deleteFolder(OpContext octxt, String itemId) throws ServiceException {
        deleteFolder(itemId);
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
        return doAction(folderAction("empty", ids).addAttribute(MailConstants.A_RECURSIVE, subfolders));
    }

    @Override
    public void emptyFolder(OpContext octxt, String folderId, boolean removeSubfolders)
    throws ServiceException {
        emptyFolder(folderId, removeSubfolders);
    }

    /** empties the dumpster
     *
     * @throws ServiceException
     */
    public void emptyDumpster() throws ServiceException {
        Element req = newRequestElement(MailConstants.EMPTY_DUMPSTER_REQUEST);
        invoke(req);
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

    @Override
    public void renameFolder(OpContext octxt, FolderStore folder, String path) throws ServiceException {
        renameFolder(folder.getFolderIdAsString(), path);
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
     * @param folderId to modify
     * @param granteeType type of grantee
     * @param name of grantee
     * @param perms permission mask ("rwid")
     * @param args extra args
     */
    public ZActionResult modifyFolderGrant(String folderId, GranteeType granteeType, String name,
            String perms, String args) throws ServiceException {
        return modifyFolderGrant(folderId, granteeType, false, name, perms, args);
    }

    /**
     * @param folderId to modify
     * @param granteeType type of grantee
     * @param useId - false if granteeId is actually a name/email address
     * @param granteeId of grantee
     * @param perms permission mask ("rwid")
     * @param args extra args
     */
    public ZActionResult modifyFolderGrant(String folderId, GranteeType granteeType, boolean useId, String granteeId,
            String perms, String args) throws ServiceException {
        Element action = folderAction("grant", folderId);
        Element grant = action.addUniqueElement(MailConstants.E_GRANT);
        grant.addAttribute(MailConstants.A_RIGHTS, perms);
        if (useId) {
            grant.addAttribute(MailConstants.A_ZIMBRA_ID, granteeId);
        } else {
            grant.addAttribute(MailConstants.A_DISPLAY, granteeId);
        }
        grant.addAttribute(MailConstants.A_DISPLAY, granteeId);
        grant.addAttribute(MailConstants.A_GRANT_TYPE, granteeType.name());
        if (args != null) {
            if (granteeType == GranteeType.key) {
                grant.addAttribute(MailConstants.A_ACCESSKEY, args);
            } else {
                grant.addAttribute(MailConstants.A_ARGS, args);
            }
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
                        g.getGranteeId().equals(granteeId)) {
                    String key = null;
                    Element eAction = r.getResponse().getOptionalElement(MailConstants.E_ACTION);
                    if (eAction != null) {
                        key = eAction.getAttribute(MailConstants.A_ACCESSKEY, null);
                    }
                    if (key != null) {
                        g.setAccessKey(key);
                    }
                    break;
                }
            }
        }

        return r;
    }

    /**
     * @param perms permission mask ("rwid")
     * @param args extra args
     */
    @Override
    public void modifyFolderGrant(OpContext ctxt, FolderStore folder,
            com.zimbra.common.mailbox.GrantGranteeType granteeType, String granteeId, String perms, String args)
    throws ServiceException {
        modifyFolderGrant(folder.getFolderIdAsString(), GranteeType.fromCommon(granteeType), true,
                granteeId, perms, args);
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

    @Override
    public void modifyFolderRevokeGrant(OpContext ctxt, String folderId, String granteeId) throws ServiceException
    {
        modifyFolderRevokeGrant(folderId, granteeId);
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

    public ZActionResult updateFolder(String id, String name, String parentId, Color newColor, String rgbColor, String flags, List<ZGrant> acl) throws ServiceException {
        Element action = folderAction("update", id);
        if (name != null && name.length() > 0) {
            action.addAttribute(MailConstants.A_NAME, name);
        }
        if (parentId != null && parentId.length() > 0) {
            action.addAttribute(MailConstants.A_FOLDER, parentId);
        }
        if (newColor != null) {
            action.addAttribute(MailConstants.A_COLOR, newColor.getValue());
        }
        if (rgbColor != null) {
            action.addAttribute(MailConstants.A_RGB, rgbColor);
        }
        if (flags != null) {
            action.addAttribute(MailConstants.A_FLAGS, flags);
        }
        if (acl != null) {
            Element aclEl = action.addNonUniqueElement(MailConstants.E_ACL);
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

    private ZSearchResult internalSearch(String convId, ZSearchParams params, boolean nest) throws ServiceException {
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
            req.addNonUniqueElement(AccountConstants.E_NAME).setText(params.getQuery());
            //req.addAttribute(MailConstants.A_SORTBY, SearchSortBy.nameAsc.name());

        }

        req.addAttribute(MailConstants.A_CONV_ID, convId);
        if (nest) {
            req.addAttribute(MailConstants.A_NEST_MESSAGES, true);
        }
        if (params.getLimit() != 0) {
            req.addAttribute(MailConstants.A_QUERY_LIMIT, params.getLimit());
        }
        if (params.getOffset() != 0) {
            req.addAttribute(MailConstants.A_QUERY_OFFSET, params.getOffset());
        }
        if (params.getSortBy() != null) {
            req.addAttribute(MailConstants.A_SORTBY, params.getSortBy().name());
        }
        if (params.getTypes() != null) {
            req.addAttribute(MailConstants.A_SEARCH_TYPES, params.getTypes());
        }
        if (params.getFetch() != null && params.getFetch() != Fetch.none) {
            // use "1" for "first" for backward compat until DF is updated
            req.addAttribute(MailConstants.A_FETCH, params.getFetch() == Fetch.first ? "1" : params.getFetch().name());
        }
        if (params.getCalExpandInstStart() != 0) {
            req.addAttribute(MailConstants.A_CAL_EXPAND_INST_START, params.getCalExpandInstStart());
        }
        if (params.getCalExpandInstEnd() != 0) {
            req.addAttribute(MailConstants.A_CAL_EXPAND_INST_END, params.getCalExpandInstEnd());
        }

        if (params.isPreferHtml()) {
            req.addAttribute(MailConstants.A_WANT_HTML, params.isPreferHtml());
        }
        if (params.isMarkAsRead()) {
            req.addAttribute(MailConstants.A_MARK_READ, params.isMarkAsRead());
        }
        if (params.isRecipientMode()) {
            req.addAttribute(MailConstants.A_RECIPIENTS, params.isRecipientMode());
        }
        if (params.getField() != null) {
            req.addAttribute(MailConstants.A_FIELD, params.getField());
        }
        if (params.getInDumpster()) {
            req.addAttribute(MailConstants.A_IN_DUMPSTER, true);
        }
        if (params.getIncludeTagDeleted()) {
            req.addAttribute(MailConstants.A_INCLUDE_TAG_DELETED, true);
        }
        if (params.getZimbraFetchMode() != null) {
            req.addAttribute(MailConstants.A_RESULT_MODE, params.getZimbraFetchMode().toString());
        }
        if (!params.getPrefetch()) {
            req.addAttribute(MailConstants.A_PREFETCH, false);
        }

        req.addAttribute(MailConstants.E_QUERY, params.getQuery(), Element.Disposition.CONTENT);

        if (params.getCursor() != null) {
            Cursor cursor = params.getCursor();
            Element cursorEl = req.addNonUniqueElement(MailConstants.E_CURSOR);
            if (cursor.getPreviousId() != null) {
                cursorEl.addAttribute(MailConstants.A_ID, cursor.getPreviousId());
            }
            if (cursor.getPreviousSortValue() != null) {
                cursorEl.addAttribute(MailConstants.A_SORTVAL, cursor.getPreviousSortValue());
            }
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
    public ZSearchResult search(ZSearchParams params) throws ServiceException {
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
    public ZSearchPagerResult search(ZSearchParams params, int page, boolean useCache, boolean useCursor) throws ServiceException {
        lock();
        try {
            return mSearchPagerCache.search(this, params, page, useCache, useCursor);
        } finally {
            unlock();
        }
    }

    /**
     *
     * @param type if non-null, clear only cached searches of the specified tape
     */
    public void clearSearchCache(String type) {
        lock();
        try {
            mSearchPagerCache.clear(type);
        } finally {
            unlock();
        }
    }

    /**
     *  do a search conv
     * @param convId id of conversation to search
     * @param params convId onversation id
     * @return search result
     * @throws ServiceException on error
     */
    public ZSearchResult searchConversation(String convId, ZSearchParams params) throws ServiceException {
        if (convId == null) {
            throw ZClientException.CLIENT_ERROR("conversation id must not be null", null);
        }
        return internalSearch(convId, params, true);
    }

    public ZSearchPagerResult searchConversation(String convId, ZSearchParams params, int page, boolean useCache, boolean useCursor) throws ServiceException {
        lock();
        try {
            if (params.getConvId() == null) {
                params.setConvId(convId);
            }
            return mSearchConvPagerCache.search(this, params, page, useCache, useCursor);
        } finally {
            unlock();
        }
    }

    private void populateFolderCache() throws ServiceException {
        if (mUserRoot != null) {
            if(alwaysRefreshFolders) {
                noOp();
            }
            return;
        }

        if (mNotifyPreference == null || mNotifyPreference == SessionPreference.full) {
            noOp();
            if (mUserRoot != null) {
                return;
            }
        }

        GetFolderRequest req = new GetFolderRequest(null, true);
        GetFolderResponse res = invokeJaxb(req);
        Folder root = res.getFolder();
        ZFolder userRoot = (root != null ? new ZFolder(root, null, this) : null);

        ZRefreshEvent event = new ZRefreshEvent(mSize, userRoot, null);
        for (ZEventHandler handler : mHandlers) {
            handler.handleRefresh(event, this);
        }
    }

    private void populateTagCache() throws ServiceException {
        if (mNameToTag != null) {
            return;
        }
        if (mNotifyPreference == null || mNotifyPreference == SessionPreference.full) {
            noOp();
            if (mNameToTag != null) {
                return;
            }
        }

        List<ZTag> tagList = new ArrayList<ZTag>();
        if (!mNoTagCache) {
            try {
                Element response = invoke(newRequestElement(MailConstants.GET_TAG_REQUEST));
                for (Element t : response.listElements(MailConstants.E_TAG)) {
                    tagList.add(new ZTag(t, this));
                }
            } catch (SoapFaultException sfe) {
                if (!sfe.getCode().equals(ServiceException.PERM_DENIED)) {
                    throw sfe;
                }
            }
        }

        ZRefreshEvent event = new ZRefreshEvent(mSize, null, tagList);
        for (ZEventHandler handler : mHandlers) {
            handler.handleRefresh(event, this);
        }
    }

    /**
     * A request that does nothing and always returns nothing. Used to keep a session alive, and return
     * any pending notifications.
     *
     * @throws ServiceException on error
     */
    @Override
    public void noOp() throws ServiceException {
        invokeJaxb(new NoOpRequest());
    }

    /**
     * A blocking NoOpRequest which waits up to the specified timeout
     *
     */
    public void noOp(long timeout) throws ServiceException {
        invokeJaxb(NoOpRequest.createWithWaitAndTimeout(true, timeout));
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
     * create a new mountpoint in the specified parent folder.
     *
     * @param parentId parent folder id
     * @param name name of new folder
     * @param defaultView default view of new folder.
     * @param color
     * @param flags
     * @param ownerBy used to specify whether owner is an id or account name (email address)
     * @param owner either the id or name of the owner
     * @param itemBy used to specify whether sharedItem is an id or path to the shared item
     * @param sharedItem either the id or path of the item
     * @param reminderEnabled whether client should show reminders on appointments/tasks
     *
     * @return newly created folder
     * @throws ServiceException on error
     * @param color initial color
     * @param flags initial flags
     */
    public ZMountpoint createMountpoint(String parentId, String name,
            ZFolder.View defaultView, ZFolder.Color color, String flags,
            OwnerBy ownerBy, String owner, SharedItemBy itemBy, String sharedItem,
            boolean reminderEnabled) throws ServiceException {
        Element req = newRequestElement(MailConstants.CREATE_MOUNTPOINT_REQUEST);
        Element linkEl = req.addUniqueElement(MailConstants.E_MOUNT);
        linkEl.addAttribute(MailConstants.A_NAME, name);
        linkEl.addAttribute(MailConstants.A_FOLDER, parentId);
        if (defaultView != null) {
            linkEl.addAttribute(MailConstants.A_DEFAULT_VIEW, defaultView.name());
        }
        if (color != null) {
            if (StringUtil.equal(color.getName(), Color.RGBCOLOR)) {
                linkEl.addAttribute(MailConstants.A_RGB, color.getRgbColorValue());
            } else {
                linkEl.addAttribute(MailConstants.A_COLOR, color.getValue());
            }
        }
        if (flags != null) {
            linkEl.addAttribute(MailConstants.A_FLAGS, flags);
        }
        linkEl.addAttribute(ownerBy == OwnerBy.BY_ID ? MailConstants.A_ZIMBRA_ID : MailConstants.A_OWNER_NAME, owner);
        linkEl.addAttribute(itemBy == SharedItemBy.BY_ID ? MailConstants.A_REMOTE_ID: MailConstants.A_PATH, sharedItem);
        linkEl.addAttribute(MailConstants.A_REMINDER, reminderEnabled);
        Element newMountEl = invoke(req).getElement(MailConstants.E_MOUNT);
        ZMountpoint newMount = getMountpointById(newMountEl.getAttribute(MailConstants.A_ID));
        return newMount != null ? newMount : new ZMountpoint(newMountEl, null, this);
    }

    /**
     * enable/disable displaying reminder for shared appointments/tasks
     * @param mountpointId
     * @param reminderEnabled
     * @throws ServiceException
     */
    public void enableSharedReminder(String mountpointId, boolean reminderEnabled) throws ServiceException {
        Element req = newRequestElement(MailConstants.ENABLE_SHARED_REMINDER_REQUEST);
        Element linkEl = req.addUniqueElement(MailConstants.E_MOUNT);
        linkEl.addAttribute(MailConstants.A_ID, mountpointId);
        linkEl.addAttribute(MailConstants.A_REMINDER, reminderEnabled);
        invoke(req);
    }

    /**
     * Sends an iCalendar REPLY object
     * @param ical iCalendar data
     * @throws ServiceException on error
     */
    public void iCalReply(String ical, String sender) throws ServiceException {
        Element req = newRequestElement(MailConstants.ICAL_REPLY_REQUEST);
        Element icalElem = req.addUniqueElement(MailConstants.E_CAL_ICAL);
        icalElem.setText(ical);
        if (sender != null) {
            icalElem.addAttribute(MailConstants.E_CAL_ATTENDEE, sender);
        }
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
        private String mMessageId;
        private String mDraftMessageId;
        private String mReplyType;
        private String mIdentityId;


        public static class AttachedMessagePart {
            private String mMessageId;
            private String mPartName;
            private String mContentId;
            private String mAttachmentId;

            public AttachedMessagePart(String messageId, String partName, String contentId) {
                mMessageId = messageId;
                mPartName = partName;
                mContentId = contentId;
            }

            public AttachedMessagePart(String attachmentId, String contentId) {
                mAttachmentId = attachmentId;
                mContentId = contentId;
            }

            public String getMessageId()  {return mMessageId; }
            public void setMessageId(String messageId) { mMessageId = messageId; }

            public String getContentId()  {return mContentId; }
            public void setContentId(String contentId) { mContentId = contentId; }

            public String getPartName() { return mPartName; }
            public void setPartName(String partName) { mPartName = partName; }

            public String getAttachmentId() { return mAttachmentId; }
            public void setAttachmentId(String attachmentId) { mAttachmentId = attachmentId; }
        }

        public static class MessagePart {
            private String mContentType;
            private final String mContent;
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
                for (MessagePart sub : parts) {
                    mSubParts.add(sub);
                }
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
                Element mpEl = parent.addNonUniqueElement(MailConstants.E_MIMEPART);
                mpEl.addAttribute(MailConstants.A_CONTENT_TYPE, mContentType);
                if (mContent != null) {
                    mpEl.addNonUniqueElement(MailConstants.E_CONTENT).setText(mContent);
                }
                if (mSubParts != null) {
                    for (MessagePart subPart : mSubParts) {
                        subPart.toElement(mpEl);
                    }
                }

                if (mAttachSubParts != null) {

                    for (AttachedMessagePart subPart : mAttachSubParts) {
                        Element e = parent.addNonUniqueElement(MailConstants.E_MIMEPART);
                        e.addAttribute(MailConstants.A_CONTENT_ID, subPart.getContentId());
                        Element attach = e.addNonUniqueElement(MailConstants.E_ATTACH);
                        if (subPart.getMessageId() != null) {
                            Element el = attach.addNonUniqueElement(MailConstants.E_MIMEPART);
                            el.addAttribute(MailConstants.A_MESSAGE_ID,subPart.getMessageId());
                            el.addAttribute(MailConstants.A_PART,subPart.getPartName());
                        } else {
                            attach.addAttribute(MailConstants.A_ATTACHMENT_ID,subPart.getAttachmentId());
                        }
                    }
                }
                return mpEl;
            }
        }

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

        public String getMessageId() { return mMessageId; }
        public void setMessageId(String messageId) { mMessageId = messageId; }

        public String getDraftMessageId() { return mDraftMessageId; }
        public void setDraftMessageId(String draftMessageId) { mDraftMessageId = draftMessageId; }

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

        public String getIdentityId() { return mIdentityId; }
        public void setIdentityId(String id) { mIdentityId = id; }

        public List<String> getDocIdsToAttach() { return mDocIdsToAttach; }
        public void setDocIdsToAttach(List<String> docIdsToAttach) { mDocIdsToAttach = docIdsToAttach; }


        public List<AttachedMessagePart> getInlineMessagePartsToAttach() {
            List<AttachedMessagePart> attachments = new ArrayList<AttachedMessagePart>();
            if (!ListUtil.isEmpty(mMessagePartsToAttach)) {
                for (AttachedMessagePart part: mMessagePartsToAttach) {
                    if (part.getContentId() != null && !part.getContentId().equals("")) {
                        attachments.add(part);
                    }
                }
            }
            return attachments;
        }
    }

    public Element getMessageElement(Element req, ZOutgoingMessage message, ZMountpoint mountpoint) {
        Element m = req.addNonUniqueElement(MailConstants.E_MSG);

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

        String msgId = message.getMessageId();
        if (msgId != null) {
            m.addAttribute(MailConstants.A_ID, msgId);
        }

        String draftId = message.getDraftMessageId();
        if (draftId != null) {
            m.addAttribute(MailConstants.A_DRAFT_ID, draftId);
        }

        if (message.getReplyType() != null) {
            m.addAttribute(MailConstants.A_REPLY_TYPE, message.getReplyType());
        }

        if (message.getAddresses() != null) {
            for (ZEmailAddress addr : message.getAddresses()) {
                if (mountpoint != null && addr.getType().equals(ZEmailAddress.EMAIL_TYPE_FROM)) {
                    //  For on behalf of messages, replace the from: and add a sender:
                    Element e = m.addNonUniqueElement(MailConstants.E_EMAIL);
                    e.addAttribute(MailConstants.A_TYPE, ZEmailAddress.EMAIL_TYPE_SENDER);
                    e.addAttribute(MailConstants.A_ADDRESS, addr.getAddress());

                    e = m.addNonUniqueElement(MailConstants.E_EMAIL);
                    e.addAttribute(MailConstants.A_TYPE, ZEmailAddress.EMAIL_TYPE_FROM);
                    e.addAttribute(MailConstants.A_ADDRESS, mountpoint.getOwnerDisplayName());
                } else {
                    Element e = m.addNonUniqueElement(MailConstants.E_EMAIL);
                    e.addAttribute(MailConstants.A_TYPE, addr.getType());
                    e.addAttribute(MailConstants.A_ADDRESS, addr.getAddress());
                    e.addAttribute(MailConstants.A_PERSONAL, addr.getPersonal());
                }
            }
        }

        if (message.getSubject() != null) {
            m.addNonUniqueElement(MailConstants.E_SUBJECT).setText(message.getSubject());
        }

        if (message.getPriority() != null && message.getPriority().length() != 0) {
            m.addAttribute(MailConstants.A_FLAGS, message.getPriority());
        }

        if (message.getInReplyTo() != null) {
            m.addNonUniqueElement(MailConstants.E_IN_REPLY_TO).setText(message.getInReplyTo());
        }

        if (message.getMessagePart() != null) {
            message.getMessagePart().toElement(m);
        }

        Element attach = null;

        if (message.getAttachmentUploadId() != null) {
            attach = m.addNonUniqueElement(MailConstants.E_ATTACH);
            attach.addAttribute(MailConstants.A_ATTACHMENT_ID, message.getAttachmentUploadId());
        }

        if (message.getMessageIdsToAttach() != null) {
            if (attach == null) {
                attach = m.addNonUniqueElement(MailConstants.E_ATTACH);
            }
            for (String mid: message.getMessageIdsToAttach()) {
                attach.addNonUniqueElement(MailConstants.E_MSG).addAttribute(MailConstants.A_ID, mid);
            }
        }
        if (message.getDocIdsToAttach() != null) {
            if (attach == null) {
                attach = m.addNonUniqueElement(MailConstants.E_ATTACH);
            }
            for (String did: message.getDocIdsToAttach()) {
                attach.addNonUniqueElement(MailConstants.E_DOC).addAttribute(MailConstants.A_ID, did);
            }
        }
        if (message.getContactIdsToAttach() != null) {
            if (attach == null) attach = m.addNonUniqueElement(MailConstants.E_ATTACH);
            for (String cid: message.getContactIdsToAttach()) {
                attach.addNonUniqueElement(MailConstants.E_CONTACT).addAttribute(MailConstants.A_ID, cid);
            }
        }
        if (message.getMessagePartsToAttach() != null) {
            if (attach == null) {
                attach = m.addNonUniqueElement(MailConstants.E_ATTACH);
            }
            for (AttachedMessagePart part: message.getMessagePartsToAttach()) {
                if(part.getContentId() == null || part.getContentId().equals("")) {
                    attach.addNonUniqueElement(MailConstants.E_MIMEPART).addAttribute(MailConstants.A_MESSAGE_ID, part.getMessageId()).addAttribute(MailConstants.A_PART, part.getPartName());
                }
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

        if (sendUid != null && sendUid.length() > 0) {
            req.addAttribute(MailConstants.A_SEND_UID, sendUid);
        }

        if (needCalendarSentByFixup) {
            req.addAttribute(MailConstants.A_NEED_CALENDAR_SENTBY_FIXUP, needCalendarSentByFixup);
        }

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
    public ZMessage saveDraft(ZOutgoingMessage message, String existingDraftId, String folderId)
            throws ServiceException {
        return saveDraft(message, existingDraftId, folderId, 0);
    }

    /**
     * Saves a message draft.
     *
     * @param message the message
     * @param existingDraftId id of existing draft or <tt>null</tt>
     * @param folderId folder to save to or <tt>null</tt> to save to the <tt>Drafts</tt> folder
     * @param autoSendTime time in UTC millis at which the draft should be auto-sent by the server.
     *                     zero value implies a normal draft, i.e. no auto-send intended.
     *
     * @return the message
     */
    public ZMessage saveDraft(
            ZOutgoingMessage message, String existingDraftId, String folderId, long autoSendTime)
                    throws ServiceException {
        lock();
        try {
            Element req = newRequestElement(MailConstants.SAVE_DRAFT_REQUEST);

            ZMountpoint mountpoint = getMountpoint(message);
            Element m = getMessageElement(req, message, mountpoint);

            if (existingDraftId != null && existingDraftId.length() > 0) {
                mMessageCache.remove(existingDraftId);
                m.addAttribute(MailConstants.A_ID, existingDraftId);
            }

            if (folderId != null) {
                m.addAttribute(MailConstants.A_FOLDER, folderId);
            }

            if (autoSendTime != 0) {
                m.addAttribute(MailConstants.A_AUTO_SEND_TIME, autoSendTime);
            }

            if (message.getIdentityId() != null) {
                m.addAttribute(MailConstants.A_IDENTITY_ID, message.getIdentityId());
            }
            m.addAttribute(MailConstants.A_WANT_IMAP_UID, true);

            String requestedAccountId = mountpoint == null ? null : mGetInfoResult.getId();
            return new ZMessage(invoke(req, requestedAccountId).getElement(MailConstants.E_MSG), this);
        } finally {
            unlock();
        }
    }

    public CheckSpellingResponse checkSpelling(String text) throws ServiceException {
        return checkSpelling(text, null, null);
    }

    public CheckSpellingResponse checkSpelling(String text, String dictionary)
            throws ServiceException {
        return checkSpelling(text, dictionary, null);
    }

    public CheckSpellingResponse checkSpelling(String text, String dictionary, List<String> ignore)
            throws ServiceException {
        String ignoreList = (ignore == null ? null : StringUtil.join(",", ignore));
        CheckSpellingRequest req = new CheckSpellingRequest(dictionary, ignoreList, text);
        return invokeJaxb(req);
    }

    public void createIdentity(ZIdentity identity) throws ServiceException {
        Element req = newRequestElement(AccountConstants.CREATE_IDENTITY_REQUEST);
        identity.toElement(req);
        invoke(req);
    }

    public List<ZIdentity> getIdentities() throws ServiceException {
        GetIdentitiesResponse res = invokeJaxb(new GetIdentitiesRequest());
        return ListUtil.newArrayList(res.getIdentities(), SoapConverter.FROM_SOAP_IDENTITY);
    }

    public void deleteIdentity(String name) throws ServiceException {
        deleteIdentity(Key.IdentityBy.name, name);
    }

    public void deleteIdentity(Key.IdentityBy by, String key) throws ServiceException {
        Element req = newRequestElement(AccountConstants.DELETE_IDENTITY_REQUEST);
        if (by == Key.IdentityBy.name) {
            req.addUniqueElement(AccountConstants.E_IDENTITY).addAttribute(AccountConstants.A_NAME, key);
        } else if (by == Key.IdentityBy.id) {
            req.addUniqueElement(AccountConstants.E_IDENTITY).addAttribute(AccountConstants.A_ID, key);
        }
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
        CreateDataSourceRequest req = new CreateDataSourceRequest();
        DataSource jaxbObj = source.toJaxb();
        req.setDataSource(jaxbObj);
        CreateDataSourceResponse resp = (CreateDataSourceResponse)invokeJaxb(req);
        return resp.getDataSource().getId();
    }

    /**
     * Tests a data source.
     *
     * @return <tt>null</tt> on success, or the error string on failure
     */
    public String testDataSource(ZDataSource source) throws ServiceException {
        TestDataSourceRequest req = new TestDataSourceRequest();
        DataSource jaxbObj = source.toJaxb();
        req.setDataSource(jaxbObj);
        TestDataSourceResponse resp = (TestDataSourceResponse)invokeJaxb(req);
        List<TestDataSource> dataSources = resp.getDataSources();
        int success = 0;
        if(dataSources.size() > 0 && dataSources.get(0) != null) {
            TestDataSource ds = dataSources.get(0);
            success = ds.getSuccess();
            if(success < 1) {
                return ds.getError();
            } else {
                return null;
            }
        }
        return null;
    }

    public List<ZDataSource> getAllDataSources() throws ServiceException {
        GetDataSourcesResponse res = invokeJaxb(new GetDataSourcesRequest());
        List<ZDataSource> result = new ArrayList<ZDataSource>();
        for (DataSource ds : res.getDataSources()) {
            if (ds instanceof Pop3DataSource) {
                result.add(new ZPop3DataSource((Pop3DataSource) ds));
            } else if (ds instanceof ImapDataSource) {
                result.add(new ZImapDataSource((ImapDataSource) ds));
            } else if (ds instanceof CalDataSource) {
                result.add(new ZCalDataSource((CalDataSource) ds));
            } else if (ds instanceof RssDataSource) {
                result.add(new ZRssDataSource((RssDataSource) ds));
            } else {
                result.add(new ZDataSource(ds));
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
            if (ds.getId() != null && ds.getId().equals(id)) {
                return ds;
            }
        }
        return null;
    }

    /**
     * Gets a data source by name.
     * @return the data source, or <tt>null</tt> if no data source with the
     * given name exists
     */
    public ZDataSource getDataSourceByName(String name) throws ServiceException {
        for (ZDataSource ds : getAllDataSources()) {
            if (ds.getName() != null && ds.getName().equals(name)) {
                return ds;
            }
        }
        return null;
    }

    public void modifyDataSource(ZDataSource source) throws ServiceException {
        ModifyDataSourceRequest req = new ModifyDataSourceRequest();
        req.setDataSource(source.toJaxb());
        invokeJaxb(req);
    }

    public void deleteDataSource(ZDataSource source) throws ServiceException {
        DeleteDataSourceRequest req = new DeleteDataSourceRequest();
        req.addDataSource(source.toJaxbNameOrId());
        invokeJaxb(req);
    }

    public ZFilterRules getIncomingFilterRules() throws ServiceException {
        return getIncomingFilterRules(false);
    }

    public ZFilterRules getOutgoingFilterRules() throws ServiceException {
        return getOutgoingFilterRules(false);
    }

    public ZFilterRules getIncomingFilterRules(boolean refresh) throws ServiceException {
        if (incomingRules == null || refresh) {
            GetFilterRulesResponse resp = invokeJaxb(new GetFilterRulesRequest());
            incomingRules = new ZFilterRules(resp.getFilterRules());
        }
        return new ZFilterRules(incomingRules);
    }

    public ZFilterRules getOutgoingFilterRules(boolean refresh) throws ServiceException {
        if (outgoingRules == null || refresh) {
            GetOutgoingFilterRulesResponse resp = invokeJaxb(new GetOutgoingFilterRulesRequest());
            outgoingRules = new ZFilterRules(resp.getFilterRules());
        }
        return new ZFilterRules(outgoingRules);
    }

    public void saveIncomingFilterRules(ZFilterRules rules) throws ServiceException {
        ModifyFilterRulesRequest req = new ModifyFilterRulesRequest();
        req.addFilterRules(rules.toJAXB());
        invokeJaxb(req);
        incomingRules = new ZFilterRules(rules);
    }

    public void saveOutgoingFilterRules(ZFilterRules rules) throws ServiceException {
        ModifyOutgoingFilterRulesRequest req = new ModifyOutgoingFilterRulesRequest();
        req.addFilterRule(rules.toJAXB());
        invokeJaxb(req);
        outgoingRules = new ZFilterRules(rules);
    }

    public void deleteDataSource(Key.DataSourceBy by, String key) throws ServiceException {
        Element req = newRequestElement(MailConstants.DELETE_DATA_SOURCE_REQUEST);
        if (by == Key.DataSourceBy.name) {
            req.addUniqueElement(MailConstants.E_DS).addAttribute(MailConstants.A_NAME, key);
        } else if (by == Key.DataSourceBy.id) {
            req.addUniqueElement(MailConstants.E_DS).addAttribute(MailConstants.A_ID, key);
        } else {
            throw ServiceException.INVALID_REQUEST("must specify data source by id or name", null);
        }
        invoke(req);
    }

    public void importData(List<ZDataSource> sources) throws ServiceException {
        ImportDataRequest req = new ImportDataRequest();
        for (ZDataSource src : sources) {
            req.addDataSource(src.toJaxbNameOrId());
        }
        invokeJaxb(req);
    }

    public static class ZImportStatus {
        private final String mType;
        private final boolean mIsRunning;
        private final boolean mSuccess;
        private final String mError;
        private final String mId;

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
        return createDocument(folderId, name, attachmentId, false);
    }

    public String createDocument(String folderId, String name, String attachmentId, boolean isNote)
            throws ServiceException {
        Element req = newRequestElement(MailConstants.SAVE_DOCUMENT_REQUEST);
        Element doc = req.addUniqueElement(MailConstants.E_DOC);
        doc.addAttribute(MailConstants.A_NAME, name);
        doc.addAttribute(MailConstants.A_FOLDER, folderId);
        if (isNote) {
            doc.addAttribute(MailConstants.A_FLAGS, ZItem.Flag.NOTE.toString());
        }
        Element upload = doc.addNonUniqueElement(MailConstants.E_UPLOAD);
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

    /**
     * modify prefs. The key in the map is the pref name, and the value should be a String[],
     * a Collection of String objects, or a single String/Object.toString.
     * @param prefs prefs to modify
     * @throws ServiceException on error
     */
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
                @SuppressWarnings("rawtypes")
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
            if (name != null) {
                result.add(name);
            }
        }
        Collections.sort(result);
        return result;
    }

    public List<String> getAvailableLocales() throws ServiceException {
        Element req = newRequestElement(AccountConstants.GET_AVAILABLE_LOCALES_REQUEST);
        Element resp = invoke(req);
        List<String> result = new ArrayList<String>();
        for (Element locale : resp.listElements(AccountConstants.E_LOCALE)) {
            String id = locale.getAttribute(AccountConstants.A_ID, null);
            if (id != null) {
                result.add(id);
            }
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
        private final boolean mMore;
        private final List<ZContact> mContacts;
        private final String mQuery;
        private final GalEntryType mType;

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

    public void applyConditions(ArrayList<Object> conditions, Element parentCondition)
    {
        for(Object condition: conditions)
        {
            if(condition instanceof ArrayList)
            {
                // Conditions
                Element element = parentCondition.addNonUniqueElement(AccountConstants.E_ENTRY_SEARCH_FILTER_MULTICOND);
                element.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OR, true);
                applyConditions((ArrayList<Object>)condition, element);
            }
            if(condition instanceof String[])
            {
                String conditionAttr[] = (String [])condition;
                Element conditionElem = parentCondition.addNonUniqueElement(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND);
                conditionElem.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR, conditionAttr[0]);
                conditionElem.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OP, conditionAttr[1]);
                conditionElem.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE, conditionAttr[2]);
            }
        }
    }

    public ZSearchGalResult searchGal(String query, ArrayList<Object> conditions, GalEntryType type) throws ServiceException {
        Element req = newRequestElement(AccountConstants.SEARCH_GAL_REQUEST);
        if (type != null) {
            req.addAttribute(AccountConstants.A_TYPE, type.name());
        }
        req.addNonUniqueElement(AccountConstants.E_NAME).setText(query);

        if(conditions.size() > 0)
        {
            Element searchFilterElem = req.addNonUniqueElement(AccountConstants.E_ENTRY_SEARCH_FILTER);
            Element condsElement = searchFilterElem.addNonUniqueElement(AccountConstants.E_ENTRY_SEARCH_FILTER_MULTICOND);
            condsElement.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OR, false);

            applyConditions(conditions, condsElement);
        }

        Element resp = invoke(req);
        List<ZContact> contacts = new ArrayList<ZContact>();
        for (Element contact : resp.listElements(MailConstants.E_CONTACT)) {
            contacts.add(new ZContact(contact, true, this));
        }
        return new ZSearchGalResult(contacts, resp.getAttributeBool(AccountConstants.A_MORE, false), query, type);
    }

    public ZSearchGalResult autoCompleteGal(String query, GalEntryType type, int limit) throws ServiceException {
        Element req = newRequestElement(AccountConstants.AUTO_COMPLETE_GAL_REQUEST);
        if (type != null) {
            req.addAttribute(AccountConstants.A_TYPE, type.name());
        }
        req.addAttribute(AccountConstants.A_LIMIT, limit);
        req.addNonUniqueElement(AccountConstants.E_NAME).setText(query);
        Element resp = invoke(req);
        List<ZContact> contacts = new ArrayList<ZContact>();
        for (Element contact : resp.listElements(MailConstants.E_CONTACT)) {
            contacts.add(new ZContact(contact, true, this));
        }
        return new ZSearchGalResult(contacts, resp.getAttributeBool(AccountConstants.A_MORE, false), query, type);
    }

    public static class ZApptSummaryResult {
        private final String mFolderId;
        private final List<ZAppointmentHit> mAppointments;
        private final long mStart;
        private final long mEnd;
        private final TimeZone mTimeZone;
        private final String mQuery;

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
    public void clearApptSummaryCache() {
        lock();
        try {
            mApptSummaryCache.clear();
        } finally {
            unlock();
        }
    }

    public static class ZGetMiniCalResult {
        private final Set<String> mDates;
        private final List<ZMiniCalError> mErrors;

        public ZGetMiniCalResult(Set<String> dates, List<ZMiniCalError> errors) {
            mDates = dates;
            mErrors = errors;
        }

        public Set<String> getDates() { return mDates; }
        public List<ZMiniCalError> getErrors() { return mErrors; }
    }

    public static class ZMiniCalError {
        private final String mFolderId;
        private final String mErrCode;
        private final String mErrMsg;
        public ZMiniCalError(String folderId, String errcode, String errmsg) {
            mFolderId = folderId;
            mErrCode = errcode;
            mErrMsg = errmsg;
        }
        public String getFolderId() { return mFolderId; }
        public String getErrCode() { return mErrCode; }
        public String getErrMsg() { return mErrMsg; }
    }

    public ZGetMiniCalResult getMiniCal(long startMsec, long endMsec, String folderIds[]) throws ServiceException {
        lock();
        try {
            Set<String> dates = mApptSummaryCache.getMiniCal(startMsec, endMsec, folderIds);
            List<ZMiniCalError> errors = null;

            if (dates == null) {
                Element req = newRequestElement(MailConstants.GET_MINI_CAL_REQUEST);
                req.addAttribute(MailConstants.A_CAL_START_TIME, startMsec);
                req.addAttribute(MailConstants.A_CAL_END_TIME, endMsec);
                for (String folderId : folderIds) {
                    Element folderElem = req.addNonUniqueElement(MailConstants.E_FOLDER);
                    folderElem.addAttribute(MailConstants.A_ID, folderId);
                }
                Element resp = invoke(req);
                dates = new HashSet<String>();
                for (Element date : resp.listElements(MailConstants.E_CAL_MINICAL_DATE)) {
                    dates.add(date.getTextTrim());
                }
                mApptSummaryCache.putMiniCal(dates, startMsec, endMsec, folderIds);
                for (Element error : resp.listElements(MailConstants.E_ERROR)) {
                    String fid = error.getAttribute(MailConstants.A_ID);
                    String code = error.getAttribute(MailConstants.A_CAL_CODE);
                    String msg = error.getTextTrim();
                    if (errors == null) {
                        errors = new ArrayList<ZMiniCalError>();
                    }
                    errors.add(new ZMiniCalError(fid, code, msg));
                }
            }
            return new ZGetMiniCalResult(dates, errors);
        } finally {
            unlock();
        }
    }

    /**
     * Validates the given set of folder ids.  If a folder id corresponds to a mountpoint
     * that is not accessible, that id is omitted from the returned list.
     */
    public String getValidFolderIds(String ids) throws ServiceException {
        lock();
        try {
            if (StringUtil.isNullOrEmpty(ids)) {
                return "";
            }

            // 1. Separate Local FolderIds and Remote FolderIds
            // sbResult is a list of valid folderIds
            // sbRemote is a list of mountpoints
            Set<String> mountpointIds = new HashSet<String>();
            Set<String> validIds = new HashSet<String>();

            for (String id : ids.split(",")) {
                ZFolder f = getFolderById(id);
                if(f instanceof ZMountpoint) {
                    mountpointIds.add(id);
                }
                else {
                    validIds.add(id);
                }
            }

            //2. Send a batch request GetFolderRequest with sbRemote as input
            try {
                Element batch = newRequestElement(ZimbraNamespace.E_BATCH_REQUEST);
                //Element resp;
                for (String id : mountpointIds) {
                    Element folderrequest = batch.addNonUniqueElement(MailConstants.GET_FOLDER_REQUEST);
                    Element e = folderrequest.addNonUniqueElement(MailConstants.E_FOLDER);
                    e.addAttribute(MailConstants.A_FOLDER, id);
                }

                Element resp = mTransport.invoke(batch);
                //3. Parse the response and add valid folderIds to sbResult.
                for (Element e : resp.listElements()) {
                    if (e.getName().equals(MailConstants.GET_FOLDER_RESPONSE.getName())) {
                        boolean isBrokenMountpoint = e.getElement(MailConstants.E_MOUNT).getAttributeBool(MailConstants.A_BROKEN, false);
                        if (!isBrokenMountpoint) {
                            String id = e.getElement(MailConstants.E_MOUNT).getAttribute(MailConstants.A_ID);
                            validIds.add(id);
                        }
                    }
                }

                return StringUtil.join(",", validIds);
            } catch (IOException e) {
                throw ZClientException.IO_ERROR("invoke "+e.getMessage(), e);
            }
        } finally {
            unlock();
        }
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
    public List<ZApptSummaryResult> getApptSummaries(String query, long startMsec, long endMsec, String folderIds[], TimeZone timeZone, String types) throws ServiceException {
        lock();
        try {
            if (types == null) {
                types = ZSearchParams.TYPE_APPOINTMENT;
            }
            if (query == null) {
                query = "";
            }
            if (folderIds == null || folderIds.length == 0) {
                folderIds = new String[] { ZFolder.ID_CALENDAR };
            }

            List<ZApptSummaryResult> summaries = new ArrayList<ZApptSummaryResult>();
            List<String> idsToFetch = new ArrayList<String>(folderIds.length);

            for (String folderId : folderIds) {
                if (folderId == null) {
                    folderId = ZFolder.ID_CALENDAR;
                }
                ZApptSummaryResult cached = mApptSummaryCache.get(startMsec, endMsec, folderId, timeZone, query);
                if (cached == null) {
                    idsToFetch.add(folderId);
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
                    if (searchQuery.length() > 1) {
                        searchQuery.append(" or ");
                    }
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
                            if (fid == null) {
                                fid = as.getFolderId();
                            }
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
                for (ZApptSummaryResult r : folder2List.values()) {
                    mApptSummaryCache.add(r, timeZone);
                }
            }
            return summaries;
        } finally {
            unlock();
        }
    }

    public static class ZAppointmentResult {

        private final String mCalItemId;
        private final String mInviteId;

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

        if (flags != null) {
            mEl.addAttribute(MailConstants.A_FLAGS, flags);
        }

        if (folderId != null) {
            mEl.addAttribute(MailConstants.A_FOLDER, folderId);
        }

        Element invEl = invite.toElement(mEl);
        if (optionalUid != null) {
            invEl.addAttribute(MailConstants.A_UID, optionalUid);
        }

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

        if (optionalUid != null) {
            invEl.addAttribute(MailConstants.A_UID, optionalUid);
        }

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

        if (tz != null) {
            tz.toElement(req);
        }

        if (instance != null) {
            Element instEl = instance.toElement(MailConstants.E_INSTANCE, req);
            if (range != null) {
                instEl.addAttribute(MailConstants.A_CAL_RANGE, range.name());
            }
        }

        if (message != null) {
            getMessageElement(req, message, null);
        }

        mMessageCache.remove(id);

        invoke(req);
    }

    public static class ZSendInviteReplyResult {

        public static final String STATUS_OK = "OK";
        public static final String STATUS_OLD = "OLD";
        public static final String STATUS_ALREADY_REPLIED = "ALREADY-REPLIED";
        public static final String STATUS_FAIL = "FAIL";

        private final String mStatus;

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

        if (tz != null) {
            tz.toElement(req);
        }

        if (instance != null) {
            instance.toElement(MailConstants.E_CAL_EXCEPTION_ID, req);
        }

        if (message != null) {
            getMessageElement(req, message, null);
        }

        mMessageCache.remove(id);

        return new ZSendInviteReplyResult(invoke(req));
    }

    public ZAppointment getAppointment(String id) throws ServiceException {
        Element req = newRequestElement(MailConstants.GET_APPOINTMENT_REQUEST);
        req.addAttribute(MailConstants.A_ID, id);
        req.addAttribute(MailConstants.A_SYNC, true);
        return new ZAppointment(invoke(req).getElement(MailConstants.E_APPOINTMENT));
    }

    public com.zimbra.soap.mail.type.CalendarItemInfo getRemoteCalItemByUID(String requestedAccountId, String uid,
            boolean includeInvites, boolean includeContent)
                    throws ServiceException {
        GetAppointmentResponse resp = invokeJaxbOnTargetAccount(
                GetAppointmentRequest.createForUidInvitesContent(uid, includeInvites, includeContent),
                requestedAccountId);
        return resp == null ? null : resp.getItem();
    }

    public com.zimbra.soap.mail.type.CalendarItemInfo getRemoteCalItemByID(String requestedAccountId, String id,
            boolean includeInvites, boolean includeContent, boolean sync)
                    throws ServiceException {
        GetAppointmentResponse resp = invokeJaxbOnTargetAccount(
                GetAppointmentRequest.createForIdInvitesContent(id, includeInvites, includeContent, sync),
                requestedAccountId);
        return resp == null ? null : resp.getItem();
    }

    public void clearMessageCache() {
        mMessageCache.clear();
    }

    public void clearContactCache() {
        mContactCache.clear();
    }

    public static class ZImportAppointmentsResult {

        private final String mIds;
        private final long mCount;

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

    public ZImportAppointmentsResult importAppointments(String folderId, String type, String attachmentId) throws ServiceException {
        Element req = newRequestElement(MailConstants.IMPORT_APPOINTMENTS_REQUEST);
        req.addAttribute(MailConstants.A_CONTENT_TYPE, type);
        req.addAttribute(MailConstants.A_FOLDER, folderId);
        Element content = req.addNonUniqueElement(MailConstants.E_CONTENT);
        content.addAttribute(MailConstants.A_ATTACHMENT_ID, attachmentId);
        return new ZImportAppointmentsResult(invoke(req).getElement(MailConstants.E_APPOINTMENT));
    }

    public static class ZGetFreeBusyResult {
        private final String mId;
        private final List<ZFreeBusyTimeSlot> mTimeSlots;

        public ZGetFreeBusyResult(String id, List<ZFreeBusyTimeSlot> timeSlots) {
            mId = id;
            mTimeSlots = timeSlots;
        }

        public String getId() { return mId; }
        public List<ZFreeBusyTimeSlot> getTimeSlots() { return mTimeSlots; }
    }

    public enum ZFreeBusySlotType {
        FREE, BUSY, TENTATIVE, UNAVAILABLE, NODATA;

        public static ZFreeBusySlotType fromString(String s) throws ServiceException {
            try {
                return ZFreeBusySlotType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid free busy slot type: "+s+", valid values: "+Arrays.asList(ZFreeBusySlotType.values()), e);
            }
        }
    }

    public static class ZFreeBusyTimeSlot {
        private final ZFreeBusySlotType mType;
        private final long mStart;
        private final long mEnd;

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
        Element userElem = req.addNonUniqueElement(MailConstants.E_FREEBUSY_USER);
        userElem.addAttribute(MailConstants.A_NAME, email);
        if (folder != CALENDAR_FOLDER_ALL) {
            userElem.addAttribute(MailConstants.A_FOLDER, folder);
        }
        Element resp = invoke(req);
        List<ZGetFreeBusyResult> result = new ArrayList<ZGetFreeBusyResult>();
        for (Element user : resp.listElements(MailConstants.E_FREEBUSY_USER)) {
            String userId = user.getAttribute(MailConstants.A_ID);
            List<ZFreeBusyTimeSlot> slots = new ArrayList<ZFreeBusyTimeSlot>();
            for (Element slot : user.listElements()) {
                ZFreeBusySlotType type;
                if (slot.getName().equals(MailConstants.E_FREEBUSY_BUSY)) {
                    type = ZFreeBusySlotType.BUSY;
                } else if (slot.getName().equals(MailConstants.E_FREEBUSY_BUSY_TENTATIVE)) {
                    type = ZFreeBusySlotType.TENTATIVE;
                } else if (slot.getName().equals(MailConstants.E_FREEBUSY_BUSY_UNAVAILABLE)) {
                    type = ZFreeBusySlotType.UNAVAILABLE;
                } else if (slot.getName().equals(MailConstants.E_FREEBUSY_NODATA)) {
                    type = ZFreeBusySlotType.NODATA;
                } else {
                    type = ZFreeBusySlotType.FREE;
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
            case NODATA:
                result.add(new ZAppointmentHit(slot));
                break;
            default:
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

        if (flags != null) {
            mEl.addAttribute(MailConstants.A_FLAGS, flags);
        }

        if (folderId != null) {
            mEl.addAttribute(MailConstants.A_FOLDER, folderId);
        }

        Element invEl = invite.toElement(mEl);
        if (optionalUid != null) {
            invEl.addAttribute(MailConstants.A_UID, optionalUid);
        }

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

        if (optionalUid != null) {
            invEl.addAttribute(MailConstants.A_UID, optionalUid);
        }

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

        if (tz != null) {
            tz.toElement(req);
        }

        if (instance != null) {
            Element instEl = instance.toElement(MailConstants.E_INSTANCE, req);
            if (range != null) {
                instEl.addAttribute(MailConstants.A_CAL_RANGE, range.name());
            }
        }

        if (message != null) {
            getMessageElement(req, message, null);
        }

        mMessageCache.remove(id);

        invoke(req);
    }

    public List<ZPhoneAccount> getAllPhoneAccounts() throws ServiceException {
        lock();
        try {
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
        } finally {
            unlock();
        }
    }

    private void setVoiceStorePrincipal(Element req) {
        req.addNonUniqueElement(mVoiceStorePrincipal.clone());
    }

    public ZPhoneAccount getPhoneAccount(String name) throws ServiceException {
        getAllPhoneAccounts(); // Make sure they're loaded.
        return mPhoneAccountMap.get(name);
    }

    public String uploadVoiceMail(String phone, String id) throws ServiceException {
        Element req = newRequestElement(VoiceConstants.UPLOAD_VOICE_MAIL_REQUEST);
        setVoiceStorePrincipal(req);
        Element actionEl = req.addNonUniqueElement(VoiceConstants.E_VOICEMSG);
        actionEl.addAttribute(MailConstants.A_ID, id);
        actionEl.addAttribute(VoiceConstants.A_PHONE, phone);
        Element response = invoke(req);
        return response.getElement(VoiceConstants.E_UPLOAD).getAttribute(MailConstants.A_ID);
    }

    public void loadCallFeatures(ZCallFeatures features) throws ServiceException {
        Element req = newRequestElement(VoiceConstants.GET_VOICE_FEATURES_REQUEST);
        setVoiceStorePrincipal(req);
        Element phoneEl = req.addNonUniqueElement(VoiceConstants.E_PHONE);
        phoneEl.addAttribute(MailConstants.A_NAME, features.getPhone().getName());
        Collection<ZCallFeature> featureList = features.getSubscribedFeatures();
        for (ZCallFeature feature : featureList) {
            phoneEl.addNonUniqueElement(feature.getName());
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
        Element phoneEl = req.addNonUniqueElement(VoiceConstants.E_PHONE);
        phoneEl.addAttribute(MailConstants.A_NAME, newFeatures.getPhone().getName());
        Collection<ZCallFeature> list = newFeatures.getAllFeatures();
        for (ZCallFeature newFeature : list) {
            Element element = phoneEl.addNonUniqueElement(newFeature.getName());
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
        Element phoneEl = req.addNonUniqueElement(VoiceConstants.E_PHONE);
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
        Element actionEl = req.addNonUniqueElement(MailConstants.E_ACTION);
        actionEl.addAttribute(MailConstants.A_ID, id);
        actionEl.addAttribute(MailConstants.A_OPERATION, op);
        actionEl.addAttribute(VoiceConstants.A_PHONE, phone);
        if (folderId != 0) {
            actionEl.addAttribute(MailConstants.A_FOLDER, Integer.toString(folderId) + '-' + phone);
        }
        return actionEl;
    }

    public ZContactByPhoneCache.ContactPhone getContactByPhone(String phone) throws ServiceException {
        lock();
        try {
            if (mContactByPhoneCache == null) {
                mContactByPhoneCache = new ZContactByPhoneCache();
                mHandlers.add(mContactByPhoneCache);
            }
            return mContactByPhoneCache.getByPhone(phone, this);
        } finally {
            unlock();
        }
    }

    private void updateSigs() {
        try {
            if (mGetInfoResult != null) {
                mGetInfoResult.setSignatures(getSignatures());
            }
        } catch (ServiceException e) {
            /* ignore */
        }
    }
    public List<String> saveAttachmentsToBriefcase(String mid, String[] partIds, String folderId ) throws ServiceException {
        if(partIds == null || partIds.length <= 0 ){
            return null;
        }
        List<String> docIds = new ArrayList<String>();
        for(String pid: partIds){               //!TODO We should do batch request for performance
            Element req = newRequestElement(MailConstants.SAVE_DOCUMENT_REQUEST);
            Element doc = req.addNonUniqueElement(MailConstants.E_DOC).addAttribute(MailConstants.A_FOLDER,folderId);
            Element m = doc.addNonUniqueElement(MailConstants.E_MSG).addAttribute(MailConstants.A_ID,mid);
            m.addAttribute(MailConstants.A_PART,pid);
            Element rDoc = invoke(req).getElement(MailConstants.E_DOC);
            if(rDoc == null){
                continue;
            }
            docIds.add(rDoc.getAttribute(MailConstants.A_ID));
        }
        return docIds;
    }
    public String createSignature(ZSignature signature) throws ServiceException {
        Element req = newRequestElement(AccountConstants.CREATE_SIGNATURE_REQUEST);
        signature.toElement(req);
        String id = invoke(req).getElement(AccountConstants.E_SIGNATURE).getAttribute(AccountConstants.A_ID);
        updateSigs();
        return id;
    }

    public List<ZSignature> getSignatures() throws ServiceException {
        GetSignaturesResponse res = invokeJaxb(new GetSignaturesRequest());
        return ListUtil.newArrayList(res.getSignatures(), SoapConverter.FROM_SOAP_SIGNATURE);
    }

    public void deleteSignature(String id) throws ServiceException {
        Element req = newRequestElement(AccountConstants.DELETE_SIGNATURE_REQUEST);
        req.addNonUniqueElement(AccountConstants.E_SIGNATURE).addAttribute(AccountConstants.A_ID, id);
        invoke(req);
        updateSigs();
    }

    public void modifySignature(ZSignature signature) throws ServiceException {
        Element req = newRequestElement(AccountConstants.MODIFY_SIGNATURE_REQUEST);
        signature.toElement(req);
        invoke(req);
        updateSigs();
    }

    public List<ZAce> getRights(String[] rights) throws ServiceException {
        Element req = newRequestElement(AccountConstants.GET_RIGHTS_REQUEST);
        if (rights != null && rights.length > 0) {
            for (String right : rights) {
                req.addNonUniqueElement(AccountConstants.E_ACE).addAttribute(AccountConstants.A_RIGHT, right);
            }
        }
        Element resp = invoke(req);
        List<ZAce> result = new ArrayList<ZAce>();
        for (Element ace : resp.listElements(AccountConstants.E_ACE)) {
            result.add(new ZAce(ace));
        }
        return result;
    }

    public List<ZAce> grantRight(ZAce ace) throws ServiceException {
        Element req = newRequestElement(AccountConstants.GRANT_RIGHTS_REQUEST);
        ace.toElement(req);
        Element resp = invoke(req);
        List<ZAce> result = new ArrayList<ZAce>();
        for (Element a : resp.listElements(AccountConstants.E_ACE)) {
            result.add(new ZAce(a));
        }
        return result;
    }

    public List<ZAce> revokeRight(ZAce ace) throws ServiceException {
        Element req = newRequestElement(AccountConstants.REVOKE_RIGHTS_REQUEST);
        ace.toElement(req);
        Element resp = invoke(req);
        List<ZAce> result = new ArrayList<ZAce>();
        for (Element a : resp.listElements(AccountConstants.E_ACE)) {
            result.add(new ZAce(a));
        }
        return result;
    }

    public boolean checkRights(String name, List<String> rights) throws ServiceException {
        Element req = newRequestElement(AccountConstants.CHECK_RIGHTS_REQUEST);
        Element eTarget = req.addNonUniqueElement(AccountConstants.E_TARGET);
        eTarget.addAttribute(AccountConstants.A_TARGET_TYPE, "account");
        eTarget.addAttribute(AccountConstants.A_TARGET_BY, "name");
        eTarget.addAttribute(AccountConstants.A_KEY, name);

        for (String right : rights) {
            Element eRight = eTarget.addNonUniqueElement(AccountConstants.E_RIGHT);
            eRight.setText(right);
        }
        Element resp = invoke(req);
        Element eTargetResp = resp.getElement(AccountConstants.E_TARGET);
        boolean allow = eTargetResp.getAttributeBool(AccountConstants.A_ALLOW);

        return allow;
    }

    @Override
    public String toString() {
        String main = "";
        String aux = authName != null ? " auth=" + authName : "";
        try {
            main = getName();
        } catch (ServiceException e) {
            if (mTransport != null) {
                String targ = mTransport.getTargetAcctName();
                if (targ == null) {
                    targ = mTransport.getTargetAcctId();
                }
                if (targ != null) {
                    main = "targ=" + targ;
                }
            }
        }
        return String.format("[ZMailbox %s%s]", main, aux);
    }

    @Override
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

    public void logout() throws ZClientException {
        if(mAuthToken == null) {
           return;
        }
        EndSessionRequest logout = new EndSessionRequest();
        logout.setLogOff(true);
        try {
            invokeJaxb(logout);
        } catch (ServiceException e) {
            //do not thrown an exception if the authtoken has already expired as when user us redirected to logout tag when authtoken expires.
            if(!ServiceException.AUTH_EXPIRED.equals(e.getCode())) {
                throw ZClientException.CLIENT_ERROR("Failed to log out", e);
            }
        }
    }
    public static String resolveUrl(String url, boolean isAdmin) throws ZClientException {
        String resolvedUrl = url;
        try {
            URI uri = new URI(resolvedUrl);

            if (isAdmin && uri.getPort() == -1) {
                uri = new URI("https", uri.getUserInfo(), uri.getHost(), ADMIN_PORT, uri.getPath(), uri.getQuery(), uri.getFragment());
                resolvedUrl = uri.toString();
            }

            String service = (uri.getPort() == ADMIN_PORT) ? AdminConstants.ADMIN_SERVICE_URI : AccountConstants.USER_SERVICE_URI;
            if (uri.getPath() == null || uri.getPath().length() <= 1) {
                if (resolvedUrl.charAt(resolvedUrl.length()-1) == '/') {
                    resolvedUrl = resolvedUrl.substring(0, resolvedUrl.length()-1) + service;
                } else {
                    resolvedUrl = resolvedUrl + service;
                }
            }
            return resolvedUrl;
        } catch (URISyntaxException e) {
            throw ZClientException.CLIENT_ERROR("invalid URL: "+resolvedUrl, e);
        }
    }

    /**
     * Given a path, resolves as much of the path as possible and returns the folder and the unmatched part.
     *
     * E.G. if the path is "/foo/bar/baz/gub" and this mailbox has a Folder at "/foo/bar" -- this API returns
     * a Pair containing that Folder and the unmatched part "baz/gub".
     *
     * If the returned folder is a ZMountpoint, then it can be assumed that the remaining part is a subfolder in
     * the remote mailbox.
     *
     * @param baseFolderItemId Folder to start from (pass ZFolder.ID_USER_ROOT as String to start from the root)
     * @throws ServiceException if the folder with {@code startingFolderId} does not exist or {@code path} is
     * {@code null} or empty.
     */
    public Pair<ZFolder, String> getFolderByPathLongestMatch(String baseFolderItemId, String path)
            throws ServiceException {
        if (Strings.isNullOrEmpty(path)) {
            throw ServiceException.INVALID_REQUEST("no such folder " + path, null);
        }
        ZFolder folder = getFolderById(baseFolderItemId);
        assert(folder != null);
        String trimmedPath = CharMatcher.is('/').trimFrom(path); // trim leading and trailing '/'
        if (trimmedPath.isEmpty()) { // relative root to the base folder
            return new Pair<ZFolder, String>(folder, null);
        }
        String unmatched = null;
        String[] segments = trimmedPath.split("/");
        for (int i = 0; i < segments.length; i++) {
            ZFolder subfolder = folder.getSubFolderByPath(segments[i]);
            if (subfolder == null) {
                unmatched = StringUtil.join("/", segments, i, segments.length - i);
                break;
            }
            folder = subfolder;
            if (folder instanceof ZMountpoint) {
                if ((i + 1) < segments.length) {
                    unmatched = StringUtil.join("/", segments, i + 1, segments.length - (i + 1));
                }
                break;
            }
        }
        return new Pair<ZFolder, String>(folder, unmatched);
    }

    /**
     * Given a path, resolves as much of the path as possible and returns the folder and the unmatched part.
     *
     * For path e.g. "/foo/bar/baz/gub" where a mailbox has a Folder at "/foo/bar" but NOT one at "/foo/bar/baz"
     * this class can encapsulate this information where:
     *     parentFolderStore is the folder at path "/foo/bar"
     *     unmatchedPart = "baz/gub".
     *
     * If the returned folder is a mountpoint, then perhaps the remaining part is a subfolder in the remote mailbox.
     */
    @Override
    public ExistingParentFolderStoreAndUnmatchedPart getParentFolderStoreAndUnmatchedPart(OpContext octxt, String path)
    throws ServiceException {
        if (Strings.isNullOrEmpty(path)) {
            return new ExistingParentFolderStoreAndUnmatchedPart(getFolderById(ZFolder.ID_USER_ROOT), "");
        }
        try {
            Pair<ZFolder, String> pair = getFolderByPathLongestMatch(ZFolder.ID_USER_ROOT, path);
            if (pair != null) {
                return new ExistingParentFolderStoreAndUnmatchedPart(pair.getFirst(), pair.getSecond());
            }
        } catch (ServiceException e) {}
        return new ExistingParentFolderStoreAndUnmatchedPart(getFolderById(ZFolder.ID_USER_ROOT), path);
    }

    public EnableTwoFactorAuthResponse enableTwoFactorAuth(String password, TOTPAuthenticator auth) throws ServiceException {
        EnableTwoFactorAuthRequest req = new EnableTwoFactorAuthRequest();
        req.setName(getName());
        req.setPassword(password);
        EnableTwoFactorAuthResponse resp = invokeJaxb(req);
        String secret = resp.getSecret();
        long timestamp = System.currentTimeMillis() / 1000;
        String totp = auth.generateCode(secret, timestamp, Encoding.BASE32);
        req.setTwoFactorCode(totp);
        req.setAuthToken(resp.getAuthToken());
        resp = invokeJaxb(req);
        resp.setSecret(secret);
        initAuthToken(new ZAuthToken(resp.getAuthToken().getValue()));
        return resp;
    }

    public DisableTwoFactorAuthResponse disableTwoFactorAuth(String password) throws ServiceException {
        DisableTwoFactorAuthRequest req = new DisableTwoFactorAuthRequest();
        return invokeJaxb(req);
    }

    public SoapHttpTransport getTransport() {
        return mTransport;
    }

    public ItemActionResponse copyItemAction(ItemIdentifier targetFolder, List<ItemIdentifier> idList)
    throws ServiceException {
        String ids = Joiner.on(',').join(idList);
        ActionSelector action = ActionSelector.createForIdsAndOperation(ids, MailConstants.OP_COPY);
        action.setFolder(targetFolder.toString(this.getAccountId()));
        action.setNewlyCreatedIds(true);
        ItemActionRequest req = new ItemActionRequest(action);
        return invokeJaxb(req);
    }

    /**
     * Copies the items identified in {@link idlist} to folder {@link targetFolder}
     * @param idlist - list of item ids for items to copy
     * @param targetFolder - Destination folder
     */
    @Override
    public List<String> copyItemAction(OpContext ctxt, ItemIdentifier targetFolder, List<ItemIdentifier> idlist)
    throws ServiceException {
        ItemActionResponse resp = copyItemAction(targetFolder, idlist);
        return Lists.newArrayList(resp.getAction().getNewlyCreatedIds());
    }

    public ItemActionResponse resetImapUid(List<Integer> idList) throws ServiceException {
        String ids = Joiner.on(",").join(idList);
        ActionSelector action = ActionSelector.createForIdsAndOperation(ids, MailConstants.OP_RESET_IMAP_UID);
        ItemActionRequest req = new ItemActionRequest(action);
        return invokeJaxb(req);
    }

    /**
     * @return the list of new IMAP UIDs corresponding to the specified items
     */
    @Override
    public List<Integer> resetImapUid(OpContext octxt, List<Integer> itemIds) throws ServiceException {
        if ((null == itemIds) || itemIds.isEmpty()) {
            return Collections.emptyList();
        }
        resetImapUid(itemIds);
        // TODO:  ItemAction returns the original IDs.  However, the underlying mailbox
        //        operation on the server returns the new UIDs - i.e. ItemAction throws away that information.
        //        If performance is a problem probably worth creating a dedicated SOAP request.
        List<ZimbraMailItem> zmis =
                getItemsById(octxt, ItemIdentifier.fromAccountIdAndItemIds(getAccountId(), itemIds));
        if ((null == zmis) || zmis.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> newImapUIDs = Lists.newArrayListWithExpectedSize(zmis.size());
        for (ZimbraMailItem zmi : zmis) {
            newImapUIDs.add(zmi.getImapUid());
        }
        return newImapUIDs;
    }

    @Override
    public void flagFolderAsSubscribed(OpContext ctxt, FolderStore folder) throws ServiceException {
        if (folder instanceof ZFolder && !folder.isIMAPSubscribed()) {
            ZFolder zFolder = (ZFolder)folder;
            String flags = zFolder.getFlags() == null ? "" : zFolder.getFlags();
            flags = flags + String.valueOf(ZFolder.Flag.imapSubscribed.getFlagChar());
            updateFolder(zFolder.getFolderIdAsString(), null, null, null, null, flags, null);
        }
    }

    @Override
    public void flagFolderAsUnsubscribed(OpContext ctxt, FolderStore folder) throws ServiceException {
        if (folder instanceof ZFolder && folder.isIMAPSubscribed()) {
            ZFolder zFolder = (ZFolder)folder;
            String flags = zFolder.getFlags().replace(
                String.valueOf(ZFolder.Flag.imapSubscribed.getFlagChar()),
                ""
            );
            updateFolder(zFolder.getFolderIdAsString(), null, null, null, null, flags, null);
        }
    }

    /**
     * Delete <tt>MailItem</tt>s with given ids.  If there is no <tt>MailItem</tt> for a given id, that id is ignored.
     *
     * @param octxt operation context or {@code null}
     * @param itemIds item ids
     * @param nonExistingItems If not null, This gets populated with the item IDs of nonExisting items
     */
    @Override
    public void delete(OpContext octxt, List<Integer> itemIds, List<Integer> nonExistingItems) throws ServiceException {
         String ids = Joiner.on(",").join(itemIds);
         ActionSelector action = ActionSelector.createForIdsAndOperation(ids, MailConstants.OP_HARD_DELETE);
         action.setNonExistentIds(true);
         ItemActionRequest req = new ItemActionRequest(action);
         ItemActionResponse resp = invokeJaxb(req);
         ActionResult ar = resp.getAction();

         for (String id: ar.getNonExistentIds().split(",")) {
             if (id.length() > 0) {
                 nonExistingItems.add(Integer.parseInt(id));
             }
         }
    }

    /** Resets the mailbox's "recent message count" to 0.  A message is considered "recent" if:
     *     (a) it's not a draft or a sent message, and
     *     (b) it was added since the last write operation associated with any SOAP session. */
    @Override
    public void resetRecentMessageCount(OpContext octxt) throws ServiceException {
        ResetRecentMessageCountRequest req = new ResetRecentMessageCountRequest();
        invokeJaxb(req);
    }

    private void lock() {
        //ZMailboxLock doesn't need to differentiate between read and write locks
        lock(false);
    }

    /** Acquire an in process lock relevant for this type of MailboxStore */
    @Override
    public void lock(boolean write) {
        lock.lock();
    }

    /** Release an in process lock relevant for this type of MailboxStore */
    @Override
    public void unlock() {
        lock.release();
    }

    /**
     *  Will not return modified folders or tags
     * @return a List of IDs of all caller-visible MailItems modified since the checkpoint
     */
    @Override
    public List<Integer> getIdsOfModifiedItemsInFolder(OpContext octxt, int lastSync, int folderId)
    throws ServiceException {
        GetModifiedItemsIDsRequest req = new GetModifiedItemsIDsRequest(String.valueOf(folderId), lastSync);
        GetModifiedItemsIDsResponse resp = invokeJaxb(req);
        return resp.getMids();
    }

    /**
     *  Will not return modified folders or tags
     * @return a List of IDs of all caller-visible MailItems modified and deleted since the checkpoint
     */
    public Pair<List<Integer>, List<Integer>> getIdsOfModifiedAndDeletedItemsInFolder(int lastSync, int folderId)
    throws ServiceException {
        GetAppointmentIdsSinceRequest req = new GetAppointmentIdsSinceRequest(String.valueOf(folderId), lastSync);
        GetAppointmentIdsSinceResponse resp = invokeJaxb(req);
        return new Pair<List<Integer>, List<Integer>>(resp.getMids(), resp.getDids());
    }

    public Set<String> listIMAPSubscriptions() throws ServiceException {
        ListIMAPSubscriptionsRequest req = new ListIMAPSubscriptionsRequest();
        ListIMAPSubscriptionsResponse resp = invokeJaxb(req);
        return resp.getSubscriptions();
    }

    public void saveIMAPsubscriptions(Set<String> subscriptions) throws ServiceException {
        SaveIMAPSubscriptionsRequest req = new SaveIMAPSubscriptionsRequest(subscriptions);
        invokeJaxb(req);
    }

    /**
     * Note that only messages and contacts can be returned by this currently.
     */
    @Override
    public ZimbraMailItem getItemById(OpContext octxt, ItemIdentifier id, MailItemType type) throws ServiceException {
        switch (type) {
        case CHAT:
            // ImapHandler suggests that chats have item type of message
            return getMessageById(id);
        case CONTACT:
            return getContact(id);
        case MESSAGE:
            return getMessageById(id);
        case UNKNOWN:
            try {
                return getContact(id);
            } catch (ServiceException se) {
            }
            return getMessageById(id);
        default:
            break;
        }
        throw ZClientException.NO_SUCH_ITEM(id.id);
    }

    @Override
    public List<ZimbraMailItem> getItemsById(OpContext octxt, Collection<ItemIdentifier> ids) throws ServiceException {
        List<ZimbraMailItem> zmis = Lists.newArrayListWithExpectedSize(ids.size());
        for (ItemIdentifier id : ids) {
            zmis.add(getItemById(octxt, id, MailItemType.UNKNOWN));
        }
        return zmis;
    }

    @Override
    public void alterTag(OpContext octxt, Collection<ItemIdentifier> ids, String tagName, boolean addTag)
            throws ServiceException {
        String idStr = itemIdsToString(ids);
        doAction(itemAction(addTag ? "tag" : "!tag", idStr, null).addAttribute(MailConstants.A_TAG_NAMES, tagName));
    }

    @Override
    public void setTags(OpContext octxt, Collection<ItemIdentifier> itemIds, int flags, Collection<String> tags)
            throws ServiceException {
        String idStr = itemIdsToString(itemIds);
        Element actionEl = itemAction("update", idStr, null);
        String tagList = Joiner.on(",").join(tags);
        actionEl.addAttribute(MailConstants.A_TAG_NAMES, tagList);
        actionEl.addAttribute(MailConstants.A_FLAGS, String.valueOf(flags));
        doAction(actionEl);
    }

    private static String itemIdsToString(Collection<ItemIdentifier> ids) {
        List<String> itemIds = new ArrayList<String>();
        for (ItemIdentifier itemIdentifer: ids) {
            itemIds.add(itemIdentifer.toString());
        }
        return Joiner.on(",").join(itemIds);
    }

    @Override
    public ZimbraQueryHitResults searchImap(OpContext octx, ZimbraSearchParams params)
    throws ServiceException {
        ZSearchParams zparams = new ZSearchParams((ZSearchParams) params);
        SearchSortBy origSortBy = zparams.getSortBy();
        if (origSortBy == null || SearchSortBy.none == origSortBy) {
            zparams.setSortBy(SearchSortBy.idAsc);  /* need something for cursor to work */
        }
        ZSearchResult result = search(zparams);
        List<ZImapSearchHit> imapSearchHits = result.getImapHits();
        List<ZImapSearchHit> imapHits = new ArrayList<>(imapSearchHits);
        while (result.hasMore() && imapSearchHits.size() > 0) {
            ZImapSearchHit last = imapSearchHits.get(imapSearchHits.size() - 1);
            last.getSortField();
            Cursor cursor = new Cursor(last.getId(), last.getSortField());
            zparams.setCursor(cursor);
            result = search(zparams);
            imapSearchHits = result.getImapHits();
            imapHits.addAll(imapSearchHits);
        }
        return new ZRemoteQueryHitResults(imapHits);
    }

    @Override
    public ZimbraSearchParams createSearchParams(String queryString) {
        return new ZSearchParams(queryString);
    }

    /**
     * Returns the change sequence number for the most recent transaction.  This will be either the change number
     * for the current transaction or, if no database changes have yet been made in this transaction, the sequence
     * number for the last committed change.
     */
    @Override
    public int getLastChangeID() {
        return lastChange.getId();
    }

    public LastChange getLastChange() {
        return lastChange;
    }

    @VisibleForTesting
    public OpenIMAPFolderResponse fetchImapFolderChunk(OpenIMAPFolderParams params) throws ServiceException {
        OpenIMAPFolderRequest req = new OpenIMAPFolderRequest();
        req.setFolderId(params.getFolderId().toString());
        req.setLimit(params.getLimit());
        if (params.getCursorId() != null) {
            req.setCursor(new ImapCursorInfo(params.getCursorId()));
        }
        return invokeJaxb(req);
    }

    public void beginTrackingImap() throws ServiceException {
        if (!getAccountInfo(false).getIsImapTracked()) {
            invokeJaxb(new BeginTrackingIMAPRequest());
        }
    }

    /**
     * Iterate OpenImapFolderRequests until all results have been returned
     * @param params
     * @return
     * @throws ServiceException
     */
    public List<ImapMessageInfo> openImapFolder(ItemIdentifier folderId, int chunkSize) throws ServiceException {
        List<ImapMessageInfo> msgs = new ArrayList<ImapMessageInfo>();
        String cursorId = null;
        OpenIMAPFolderResponse folderInfo = null;
        do {
            OpenIMAPFolderParams params = new OpenIMAPFolderParams(folderId);
            params.setLimit(chunkSize);
            if (cursorId != null) {
                params.setCursorId(cursorId);
            }
            folderInfo = fetchImapFolderChunk(params);
            List<ImapMessageInfo> results = folderInfo.getImapMessageInfo();
            if (results.isEmpty()) {
                break;
            } else {
                ImapCursorInfo cursor = folderInfo.getCursor();
                if (cursor == null) {
                    //fall back to last message of this results page
                    cursorId = String.valueOf(results.get(results.size() - 1).getId());
                } else {
                    cursorId = cursor.getId();
                }
                msgs.addAll(results);
            }
        } while (folderInfo.getHasMore());
        return msgs;
    }

    public static class OpenIMAPFolderParams {

        private static final int DEFAULT_LIMIT = 1000;
        private final ItemIdentifier folderId;
        private int limit;
        private String cursorId;

        public OpenIMAPFolderParams(ItemIdentifier folderIdent) {
            this.folderId = folderIdent;
            this.limit = DEFAULT_LIMIT;
            this.cursorId = null;
        }

        public ItemIdentifier getFolderId() { return folderId; }

        public void setLimit(int limit) { this.limit = limit; }
        public int getLimit() { return limit; }

        public void setCursorId(String id) { this.cursorId =id; }
        public String getCursorId() { return cursorId; }
    }

    public int getImapRECENT(String folderId) throws ServiceException {
        GetIMAPRecentResponse resp = invokeJaxb(new GetIMAPRecentRequest(folderId));
        return resp.getNum();
    }

    public int getImapRECENTCutoff(String folderId) throws ServiceException {
        GetIMAPRecentCutoffResponse resp = invokeJaxb(new GetIMAPRecentCutoffRequest(folderId));
        return resp.getCutoff();
    }

    /** @return List of IMAP UIDs */
    public List<Integer> imapCopy(int[] itemIds, MailItemType type, int folderId) throws ServiceException {
        List<Integer> result = Lists.newArrayList();
        IMAPCopyResponse resp = invokeJaxb(new IMAPCopyRequest(Ints.join(",", itemIds), type.toString(), folderId));
        List<IMAPItemInfo> items = resp.getItems();
        for(IMAPItemInfo item : items) {
            result.add(item.getImapUid());
        }
        return result;
    }

    public static class TagSpecifier {
        private final String identifier;
        private final boolean isIds;

        private TagSpecifier(String identifier, boolean isIds) {
            this.identifier = identifier;
            this.isIds = isIds;
        }

        public static TagSpecifier tagById(String tagId) {
            return new TagSpecifier(tagId, true);
        }

        public static TagSpecifier tagByName(String tagName) {
            return new TagSpecifier(tagName, false);
        }

        public void encodeTags(Element el) {
            if (identifier != null && identifier.length() > 0) {
                el.addAttribute(isIds ? MailConstants.A_TAGS : MailConstants.A_TAG_NAMES, identifier);
            }
        }
    }

    public void setCurWaitSetID(String id) {
        mCurWaitSetID = id;
    }

    public void unsetCurWaitSetID() {
        mCurWaitSetID = null;
    }

    public Invitation getDefaultInvite(CalendarItemInfo calItemInfo) throws ServiceException {
        if (calItemInfo == null) {
            return null;
        }
        List<Invitation> list =  calItemInfo.getInvites();
        if (list == null || list.isEmpty()) {
            return null;
        }
        Invitation invitation = list.get(0);
        if (list.size() > 1 && invitation.getInviteComponent().getRecurrence() == null) {
            for (Invitation inv : list) {
                if (inv.getInviteComponent().getRecurrence() != null) {
                    invitation = inv;
                    break;
                }
            }
        }
        return invitation;
    }
}
