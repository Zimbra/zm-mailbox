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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.*;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.EasySSLProtocolSocketFactory;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DataSourceBy;
import com.zimbra.cs.account.Provisioning.IdentityBy;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.zclient.ZGrant.GranteeType;
import com.zimbra.cs.zclient.ZMailbox.ZOutgoingMessage.AttachedMessagePart;
import com.zimbra.cs.zclient.ZMailbox.ZOutgoingMessage.MessagePart;
import com.zimbra.cs.zclient.ZSearchParams.Cursor;
import com.zimbra.cs.zclient.event.ZCreateContactEvent;
import com.zimbra.cs.zclient.event.ZCreateConversationEvent;
import com.zimbra.cs.zclient.event.ZCreateEvent;
import com.zimbra.cs.zclient.event.ZCreateFolderEvent;
import com.zimbra.cs.zclient.event.ZCreateMessageEvent;
import com.zimbra.cs.zclient.event.ZCreateMountpointEvent;
import com.zimbra.cs.zclient.event.ZCreateSearchFolderEvent;
import com.zimbra.cs.zclient.event.ZCreateTagEvent;
import com.zimbra.cs.zclient.event.ZDeleteEvent;
import com.zimbra.cs.zclient.event.ZEventHandler;
import com.zimbra.cs.zclient.event.ZModifyContactEvent;
import com.zimbra.cs.zclient.event.ZModifyConversationEvent;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import com.zimbra.cs.zclient.event.ZModifyFolderEvent;
import com.zimbra.cs.zclient.event.ZModifyMailboxEvent;
import com.zimbra.cs.zclient.event.ZModifyMessageEvent;
import com.zimbra.cs.zclient.event.ZModifyMountpointEvent;
import com.zimbra.cs.zclient.event.ZModifySearchFolderEvent;
import com.zimbra.cs.zclient.event.ZModifyTagEvent;
import com.zimbra.cs.zclient.event.ZRefreshEvent;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapTransport;
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

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZMailbox {

    static {
        if (LC.ssl_allow_untrusted_certs.booleanValue())
            EasySSLProtocolSocketFactory.init();
    }

    public final static int MAX_NUM_CACHED_SEARCH_PAGERS = 5;
    public final static int MAX_NUM_CACHED_SEARCH_CONV_PAGERS = 5;
    public final static int MAX_NUM_CACHED_MESSAGES = 5;
    public final static int MAX_NUM_CACHED_CONTACTS = 25;    
    
    public final static String PATH_SEPARATOR = "/";
    
    public final static char PATH_SEPARATOR_CHAR = '/';

    public enum SearchSortBy {
        dateDesc, dateAsc, subjDesc, subjAsc, nameDesc, nameAsc;

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
    
    public static class Options {
        private String mAccount;
        private AccountBy mAccountBy = AccountBy.name;
        private String mPassword;
        private String mNewPassword;
        private String mAuthToken;
        private String mUri;
        private SoapTransport.DebugListener mDebugListener;
        private String mTargetAccount;
        private AccountBy mTargetAccountBy = AccountBy.name;
        private boolean mNoSession;
        private boolean mNoNotify;
        private ZEventHandler mHandler;

        public Options() {
        }
        
        public Options(String account, AccountBy accountBy, String password, String uri) {
            mAccount = account;
            mAccountBy = accountBy;
            mPassword = password;
            mUri = uri;
        }

        public Options(String authToken, String uri) {
            mAuthToken = authToken;
            mUri = uri;
        }

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

        public String getAuthToken() { return mAuthToken; }
        public void setAuthToken(String authToken) { mAuthToken = authToken; }

        public String getUri() { return mUri; }
        public void setUri(String uri) { mUri = uri; }

        public SoapTransport.DebugListener getDebugListener() { return mDebugListener; }
        public void setDebugListener(SoapTransport.DebugListener liistener) { mDebugListener = liistener; }

        public boolean getNoSession() { return mNoSession; }
        public void setNoSession(boolean noSession) { mNoSession = noSession; }

        public boolean getNoNotify() { return mNoNotify; }
        public void setNoNotify(boolean noNotify) { mNoNotify = noNotify; }
        
        public ZEventHandler getEventHandler() { return mHandler; }
        public void setEventHandler(ZEventHandler handler) { mHandler = handler; }

    }

    private String mAuthToken;
    private SoapHttpTransport mTransport;
    private Map<String, ZTag> mNameToTag;
    private Map<String, ZItem> mIdToItem;
    private ZGetInfoResult mGetInfoResult;
    private ZFolder mUserRoot;
    private boolean mNeedsRefresh = true;
    private ZSearchPagerCache mSearchPagerCache;
    private ZSearchPagerCache mSearchConvPagerCache;
    private LRUMap mMessageCache;
    private LRUMap mContactCache;

    private long mSize;

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
    public static void changePassword(Options options) throws ServiceException {
        ZMailbox mailbox = new ZMailbox();
        mailbox.initPreAuth(options.getUri(), options.getDebugListener());
        mailbox.changePassword(options.getAccount(), options.getAccountBy(), options.getPassword(), options.getNewPassword());
    }
    
    public ZMailbox(Options options) throws ServiceException {
    	mHandlers.add(new InternalEventHandler());
    	mSearchPagerCache = new ZSearchPagerCache(MAX_NUM_CACHED_SEARCH_PAGERS, true);
        mHandlers.add(mSearchPagerCache);
        mSearchConvPagerCache = new ZSearchPagerCache(MAX_NUM_CACHED_SEARCH_CONV_PAGERS, false);
        mHandlers.add(mSearchConvPagerCache);
        mMessageCache = new LRUMap(MAX_NUM_CACHED_MESSAGES);
        mContactCache = new LRUMap(MAX_NUM_CACHED_CONTACTS);        
        
        if (options.getEventHandler() != null)
    		mHandlers.add(options.getEventHandler());
        initPreAuth(options.getUri(), options.getDebugListener());
        if (options.getAuthToken() != null) {
            initAuthToken(options.getAuthToken());
        } else {
            String password;
            if (options.getNewPassword() != null) {
                changePassword(options.getAccount(), options.getAccountBy(), options.getPassword(), options.getNewPassword());
                password = options.getNewPassword();
            } else {
                password = options.getPassword();
            }
            initAuthToken(auth(options.getAccount(), options.getAccountBy(), password).getAuthToken());
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

    public void initAuthToken(String authToken){
        mAuthToken = authToken;
        mTransport.setAuthToken(mAuthToken);
    }

    public void initPreAuth(String uri, SoapTransport.DebugListener listener) {
        mNameToTag = new HashMap<String, ZTag>();
        mIdToItem = new HashMap<String, ZItem>();                
        setSoapURI(uri);
        if (listener != null) mTransport.setDebugListener(listener);
    }

    private void initTargetAccount(String key, AccountBy by) {
        if (AccountBy.id.equals(by))
            mTransport.setTargetAcctId(key);
        else if (AccountBy.name.equals(by))
            mTransport.setTargetAcctName(key);
    }

    private void changePassword(String key, AccountBy by, String oldPassword, String newPassword) throws ServiceException {
        if (mTransport == null) throw ZClientException.CLIENT_ERROR("must call setURI before calling changePassword", null);
        XMLElement req = new XMLElement(AccountConstants.CHANGE_PASSWORD_REQUEST);
        Element account = req.addElement(AccountConstants.E_ACCOUNT);
        account.addAttribute(AccountConstants.A_BY, by.name());
        account.setText(key);
        req.addElement(AccountConstants.E_OLD_PASSWORD).setText(oldPassword);
        req.addElement(AccountConstants.E_PASSWORD).setText(newPassword);
        invoke(req);
    }

    private ZAuthResult auth(String key, AccountBy by, String password) throws ServiceException {
        if (mTransport == null) throw ZClientException.CLIENT_ERROR("must call setURI before calling asuthenticate", null);
        XMLElement req = new XMLElement(AccountConstants.AUTH_REQUEST);
        Element account = req.addElement(AccountConstants.E_ACCOUNT);
        account.addAttribute(AccountConstants.A_BY, by.name());
        account.setText(key);
        req.addElement(AccountConstants.E_PASSWORD).setText(password);
        return new ZAuthResult(invoke(req));
    }

    public String getAuthToken() {
        return mAuthToken;
    }

    /**
     * @param uri URI of server we want to talk to
     */
    private void setSoapURI(String uri) {
        if (mTransport != null) mTransport.shutdown();
        mTransport = new SoapHttpTransport(uri);
        mTransport.setUserAgent("zclient", BuildInfo.VERSION);
        mTransport.setMaxNoitfySeq(0);
        if (mAuthToken != null)
            mTransport.setAuthToken(mAuthToken);
    }    

    synchronized Element invoke(Element request) throws ServiceException {
        try {
            return mTransport.invoke(request);
        } catch (SoapFaultException e) {
            throw e; // for now, later, try to map to more specific exception
        } catch (IOException e) {
            throw ZClientException.IO_ERROR("invoke "+e.getMessage(), e);
        } finally {
            handleResponseContext(mTransport.getZimbraContext());
        }
    }

    private void handleResponseContext(Element context) throws ServiceException {
        if (context == null) return;
        // handle refresh blocks
        Element refresh = context.getOptionalElement(ZimbraNamespace.E_REFRESH);
        if (refresh != null) 
        	handleRefresh(refresh);

        for (Element notify : context.listElements(ZimbraNamespace.E_NOTIFY)) {
            mTransport.setMaxNoitfySeq(
                    Math.max(mTransport.getMaxNotifySeq(),
                             notify.getAttributeLong(HeaderConstants.A_SEQNO, 0)));
            // MUST DO IN THIS ORDER!
            handleDeleted(notify.getOptionalElement(ZimbraNamespace.E_DELETED));
            handleCreated(notify.getOptionalElement(ZimbraNamespace.E_CREATED));
            handleModified(notify.getOptionalElement(ZimbraNamespace.E_MODIFIED));
        }
    }
    
    private void handleRefresh(Element refresh) throws ServiceException {
        Element mbx = refresh.getOptionalElement(MailConstants.E_MAILBOX);
        if (mbx != null) mSize = mbx.getAttributeLong(MailConstants.A_SIZE);
        
        Element tags = refresh.getOptionalElement(ZimbraNamespace.E_TAGS);
        List<ZTag> tagList = new ArrayList<ZTag>();
        if (tags != null) {
            for (Element t : tags.listElements(MailConstants.E_TAG)) {
            	ZTag tag = new ZTag(t);
            	tagList.add(tag);
            }
        }
        Element folderEl = refresh.getOptionalElement(MailConstants.E_FOLDER);
        ZFolder userRoot = new ZFolder(folderEl, null);
        ZRefreshEvent event = new ZRefreshEvent(mSize, userRoot, tagList);
        for (ZEventHandler handler : mHandlers)
        	handler.handleRefresh(event, this);
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
            }
            if (event != null)
            	for (ZEventHandler handler : mHandlers)
            		handler.handleModify(event, this);
        }
    }

    private void handleCreated(Element created) throws ServiceException {
        if (created == null) return;
        for (Element e : created.listElements()) {
        	ZCreateEvent event = null;
            if (e.getName().equals(MailConstants.E_CONV)) {
                event = new ZCreateConversationEvent(e);
            } else if (e.getName().equals(MailConstants.E_MSG)) {
                event = new ZCreateMessageEvent(e);
            } else if (e.getName().equals(MailConstants.E_CONTACT)) {
                event = new ZCreateContactEvent(e);
            } else if (e.getName().equals(MailConstants.E_FOLDER)) {
                String parentId = e.getAttribute(MailConstants.A_FOLDER);
                ZFolder parent = getFolderById(parentId);
                ZFolder child = new ZFolder(e, parent);
                event = new ZCreateFolderEvent(child);
                parent.addChild(child);
            } else if (e.getName().equals(MailConstants.E_MOUNT)) {
                String parentId = e.getAttribute(MailConstants.A_FOLDER);
                ZFolder parent = getFolderById(parentId);
                ZMountpoint child = new ZMountpoint(e, parent);
                event = new ZCreateMountpointEvent(child);
                parent.addChild(child);
            } else if (e.getName().equals(MailConstants.E_SEARCH)) {
                String parentId = e.getAttribute(MailConstants.A_FOLDER);
                ZFolder parent = getFolderById(parentId);
                ZSearchFolder child = new ZSearchFolder(e, parent);
                event = new ZCreateSearchFolderEvent(child);
                parent.addChild(child);                
            } else if (e.getName().equals(MailConstants.E_TAG)) {
                event = new ZCreateTagEvent(new ZTag(e));
            }
            if (event != null)
            	for (ZEventHandler handler : mHandlers)
            		handler.handleCreate(event, this);            
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
            mNameToTag.clear();
            mIdToItem.clear();
            mMessageCache.clear();
            mContactCache.clear();
            mTransport.setMaxNoitfySeq(0);
            mSize = event.getSize();
            mUserRoot = event.getUserRoot();
            mNeedsRefresh = false;
            addIdMappings(mUserRoot);
            for (ZTag tag: event.getTags())
            	addTag(tag);
        }

        public synchronized void handleCreate(ZCreateEvent event, ZMailbox mailbox) {
            if (event instanceof ZCreateTagEvent) {
                addTag(((ZCreateTagEvent)event).getTag());
            } else if (event instanceof ZCreateSearchFolderEvent) {
            	mailbox.addItemIdMapping(((ZCreateSearchFolderEvent)event).getSearchFolder());
            } else if (event instanceof ZCreateMountpointEvent) {
            	ZMountpoint mp =  ((ZCreateMountpointEvent)event).getMountpoint();
            	mailbox.addItemIdMapping(mp);
            	mailbox.addRemoteItemIdMapping(mp.getCanonicalRemoteId(), mp);
            } else if (event instanceof ZCreateFolderEvent) {            	
            	mailbox.addItemIdMapping(((ZCreateFolderEvent)event).getFolder());
            }
        }

        public synchronized void handleModify(ZModifyEvent event, ZMailbox mailbox) throws ServiceException {
            if (event instanceof ZModifyTagEvent) {
                ZModifyTagEvent tagEvent = (ZModifyTagEvent) event;
                ZTag tag = getTagById(tagEvent.getId());
                if (tag != null) {
                    String oldName = tag.getName();
                    tag.modifyNotification(tagEvent);
                    if (!tag.getName().equalsIgnoreCase(oldName)) {
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
            	mSize = ((ZModifyMailboxEvent)event).getSize(mSize);
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
        
        public synchronized void handleDelete(ZDeleteEvent event, ZMailbox mailbox) throws ServiceException {
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
                    mNameToTag.remove(((ZTag) item).getName());
                }
                if (item != null) mIdToItem.remove(item.getId());
            }
        }
    }

    private void addTag(ZTag tag) {
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
            throw ServiceException.INVALID_REQUEST("path must be absoliute: "+path, null);
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
            throw ServiceException.INVALID_REQUEST("path must be absoliute: "+path, null);
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
        if (mNeedsRefresh) noOp();
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
            XMLElement req = new XMLElement(AccountConstants.GET_INFO_REQUEST);
            mGetInfoResult = new ZGetInfoResult(invoke(req));            
        }
        return mGetInfoResult;       
    }
    
    //  ------------------------
    
    /**
     * @return current List of all tags in the mailbox
     * @throws com.zimbra.common.service.ServiceException on error
     */
    @SuppressWarnings("unchecked")    
    public List<ZTag> getAllTags() throws ServiceException {
        if (mNeedsRefresh) noOp(); 
        List result = new ArrayList<ZTag>(mNameToTag.values());
        Collections.sort(result);
        return result;      
    }
    
    /**
     * @return current list of all tags names in the mailbox, sorted
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public List<String> getAllTagNames() throws ServiceException {
        if (mNeedsRefresh) noOp();
        ArrayList<String> names = new ArrayList<String>(mNameToTag.keySet());
        Collections.sort(names);
        return names;   
    }

    /**
     * returns the tag the specified name, or null if no such tag exists.
     * 
     * @param name tag name
     * @return the tag, or null if tag not found
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public ZTag getTagByName(String name) throws ServiceException {
        if (mNeedsRefresh) noOp();
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
        if (mNeedsRefresh) noOp();         
        ZItem item = mIdToItem.get(id);
        if (item instanceof ZTag) return (ZTag) item;
        else return null;       
    }

    /**
     * create a new tag with the specified color.
     * 
     * @return newly created tag
     * @param name name of the tag
     * @param color color of the tag
     * @throws com.zimbra.common.service.ServiceException if an error occurs
     *
     */
    public ZTag createTag(String name, ZTag.Color color) throws ServiceException {
        XMLElement req = new XMLElement(MailConstants.CREATE_TAG_REQUEST);
        Element tagEl = req.addElement(MailConstants.E_TAG);
        tagEl.addAttribute(MailConstants.A_NAME, name);
        if (color != null) tagEl.addAttribute(MailConstants.A_COLOR, color.getValue());
        Element createdTagEl = invoke(req).getElement(MailConstants.E_TAG);
        ZTag tag = getTagById(createdTagEl.getAttribute(MailConstants.A_ID));
        return tag != null ? tag : new ZTag(createdTagEl);
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
        XMLElement req = new XMLElement(MailConstants.TAG_ACTION_REQUEST);
        Element actionEl = req.addElement(MailConstants.E_ACTION);
        actionEl.addAttribute(MailConstants.A_ID, id);
        actionEl.addAttribute(MailConstants.A_OPERATION, op);
        return actionEl;
    }
    
    private ZActionResult doAction(Element actionEl) throws ServiceException {
        Element response = invoke(actionEl.getParent());
        return new ZActionResult(response.getElement(MailConstants.E_ACTION).getAttribute(MailConstants.A_ID));
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
        XMLElement req = new XMLElement(MailConstants.GET_CONTACTS_REQUEST);
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
            result.add(new ZContact(cn));
        }
        return result;
    }
    
    public String createContact(String folderId, String tags, Map<String, String> attrs) throws ServiceException {
        XMLElement req = new XMLElement(MailConstants.CREATE_CONTACT_REQUEST);
        Element cn = req.addElement(MailConstants.E_CONTACT);
        if (folderId != null) 
            cn.addAttribute(MailConstants.A_FOLDER, folderId);
        if (tags != null) 
            cn.addAttribute(MailConstants.A_TAGS, tags);
        for (Map.Entry<String, String> entry : attrs.entrySet()) {
            Element a = cn.addElement(MailConstants.E_ATTRIBUTE);
            a.addAttribute(MailConstants.A_ATTRIBUTE_NAME, entry.getKey());
            a.setText(entry.getValue());
        }
        return invoke(req).getElement(MailConstants.E_CONTACT).getAttribute(MailConstants.A_ID);
    }
 
    /**
     * 
     * @param id of contact
     * @param replace if true, replace all attrs with specified attrs, otherwise merge with existing
     * @param attrs modified attrs
     * @return updated contact
     * @throws ServiceException on error
     */
    public String modifyContact(String id, boolean replace, Map<String, String> attrs) throws ServiceException {
        XMLElement req = new XMLElement(MailConstants.MODIFY_CONTACT_REQUEST);
        if (replace) 
            req.addAttribute(MailConstants.A_REPLACE, replace);
        Element cn = req.addElement(MailConstants.E_CONTACT);
        cn.addAttribute(MailConstants.A_ID, id);
        for (Map.Entry<String, String> entry : attrs.entrySet()) {
            Element a = cn.addElement(MailConstants.E_ATTRIBUTE);
            a.addAttribute(MailConstants.A_ATTRIBUTE_NAME, entry.getKey());
            a.setText(entry.getValue());
        }
        return invoke(req).getElement(MailConstants.E_CONTACT).getAttribute(MailConstants.A_ID);
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
        XMLElement req = new XMLElement(MailConstants.GET_CONTACTS_REQUEST);

        if (sortBy != null) 
            req.addAttribute(MailConstants.A_SORTBY, sortBy.name());
        if (sync)
            req.addAttribute(MailConstants.A_SYNC, sync);
        req.addElement(MailConstants.E_CONTACT).addAttribute(MailConstants.A_ID, ids);
        if (attrs != null) {
            for (String name : attrs)
                req.addElement(MailConstants.E_ATTRIBUTE).addAttribute(MailConstants.A_ATTRIBUTE_NAME, name);
        }
        List<ZContact> result = new ArrayList<ZContact>();
        for (Element cn : invoke(req).listElements(MailConstants.E_CONTACT)) {
            result.add(new ZContact(cn));
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
            XMLElement req = new XMLElement(MailConstants.GET_CONTACTS_REQUEST);
            req.addAttribute(MailConstants.A_SYNC, true);
            req.addElement(MailConstants.E_CONTACT).addAttribute(MailConstants.A_ID, id);
            result = new ZContact(invoke(req).getElement(MailConstants.E_CONTACT));
            mContactCache.put(id, result);
        }
        return result;
    }

    private Element contactAction(String op, String id) {
        XMLElement req = new XMLElement(MailConstants.CONTACT_ACTION_REQUEST);
        Element actionEl = req.addElement(MailConstants.E_ACTION);
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
    
    public ZActionResult flagContact(String ids, boolean flag) throws ServiceException {
        return doAction(contactAction(flag ? "flag" : "!flag", ids));
    }
    
    public ZActionResult tagContact(String ids, String tagId, boolean tag) throws ServiceException {
        return doAction(contactAction(tag ? "tag" : "!tag", ids).addAttribute(MailConstants.A_TAG, tagId));
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
   

    /**
     * 
     * @param id conversation id 
     * @param fetch Whether or not fetch none/first/all messages in conv.
     * @return conversation
     * @throws ServiceException on error
     */
    public ZConversation getConversation(String id, Fetch fetch) throws ServiceException {
        XMLElement req = new XMLElement(MailConstants.GET_CONV_REQUEST);
        Element convEl = req.addElement(MailConstants.E_CONV);
        convEl.addAttribute(MailConstants.A_ID, id);
        if (fetch != null && fetch != Fetch.none && fetch != Fetch.hits) {
            // use "1" for "first" for backward compat until DF is updated
            convEl.addAttribute(MailConstants.A_FETCH, fetch == Fetch.first ? "1" : fetch.name());
        }        
        return new ZConversation(invoke(req).getElement(MailConstants.E_CONV));
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
        XMLElement req = new XMLElement(MailConstants.CONV_ACTION_REQUEST);
        Element actionEl = req.addElement(MailConstants.E_ACTION);
        actionEl.addAttribute(MailConstants.A_ID, id);
        actionEl.addAttribute(MailConstants.A_OPERATION, op);
        if (constraints != null) actionEl.addAttribute(MailConstants.A_TARGET_CONSTRAINT, constraints);
        return actionEl;
    }

    /**
     * hard delete conversation(s).
     * 
     * @param ids list of conversation ids to act on
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items in a conversation. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.  
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult deleteConversation(String ids, String targetConstraints) throws ServiceException {
        return doAction(convAction("delete", ids, targetConstraints));
    }

    /**
     * mark conversation as read/unread
     * 
     * @param ids list of conversation ids to act on
     * @param read mark read (TRUE) or unread (FALSE)
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
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
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
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
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
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
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
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
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
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
        XMLElement req = new XMLElement(MailConstants.MSG_ACTION_REQUEST);
        Element actionEl = req.addElement(MailConstants.E_ACTION);
        actionEl.addAttribute(MailConstants.A_ID, id);
        actionEl.addAttribute(MailConstants.A_OPERATION, op);
        return actionEl;
    }

    // ------------------------

    private Element itemAction(String op, String id, String constraints) {
        XMLElement req = new XMLElement(MailConstants.ITEM_ACTION_REQUEST);
        Element actionEl = req.addElement(MailConstants.E_ACTION);
        actionEl.addAttribute(MailConstants.A_ID, id);
        actionEl.addAttribute(MailConstants.A_OPERATION, op);
        if (constraints != null) actionEl.addAttribute(MailConstants.A_TARGET_CONSTRAINT, constraints);
        return actionEl;
    }
    
    /**
     * hard delete item(s).
     * 
     * @param ids list of item ids to act on
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.  
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult deleteItem(String ids, String targetConstraints) throws ServiceException {
        return doAction(itemAction("delete", ids, targetConstraints));
    }

    /**
     * mark item as read/unread
     * 
     * @param ids list of ids to act on
     * @param read mark read (TRUE) or unread (FALSE)
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
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
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
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
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
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
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
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
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
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

    public String uploadAttachment(String name, byte[] content, String contentType, int msTimeout) throws ServiceException {
        FilePart part = new FilePart(name, new ByteArrayPartSource(name, content));
        part.setContentType(contentType);

        return uploadAttachments(new Part[] { part }, msTimeout);
    }

    public String uploadAttachments(Part[] parts, int msTimeout) throws ServiceException {
        String aid = null;
        
        URI uri = getUploadURI();
        HttpClient client = getHttpClient(uri);
        
        // make the post
        PostMethod post = new PostMethod(uri.toString());
        client.getHttpConnectionManager().getParams().setConnectionTimeout(msTimeout);
        int statusCode;
        try {
            post.setRequestEntity( new MultipartRequestEntity(parts, post.getParams()) );
            statusCode = client.executeMethod(post);

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

    private URI getUploadURI()  throws ServiceException {
        try {
            URI uri = new URI(mTransport.getURI());
            return  uri.resolve("/service/upload?fmt=raw");
        } catch (URISyntaxException e) {
            throw ZClientException.CLIENT_ERROR("unable to parse URI: "+mTransport.getURI(), e);
        }
    }
     
    Pattern sAttachmentId = Pattern.compile("\\d+,'.*','(.*)'");

    private String getAttachmentId(String result) throws ZClientException {
        if (result.startsWith(HttpServletResponse.SC_OK+"")) {
            Matcher m = sAttachmentId.matcher(result);
            return m.find() ? m.group(1) : null;
        } else if (result.startsWith(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE+"")) {
            throw ZClientException.UPLOAD_SIZE_LIMIT_EXCEEDED("upload size limit exceeded", null);
        }
        throw ZClientException.UPLOAD_FAILED("upload failed, response: " + result, null);
    }

    private void addAuthCookoie(String name, URI uri, HttpState state) {
        Cookie cookie = new Cookie(uri.getHost(), name, getAuthToken(), "/", -1, false);    
        state.addCookie(cookie);
    }

    HttpClient getHttpClient(URI uri) {
        boolean isAdmin = uri.getPort() == 7071; // TODO???
        HttpState initialState = new HttpState();        
        if (isAdmin) 
            addAuthCookoie(ZimbraServlet.COOKIE_ZM_ADMIN_AUTH_TOKEN, uri, initialState);
        addAuthCookoie(ZimbraServlet.COOKIE_ZM_AUTH_TOKEN, uri, initialState); 
        HttpClient client = new HttpClient();
        client.setState(initialState);
        client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        return client;
    }
    
    /**
     * @param folderId (required) folderId of folder to add message to
     * @param flags non-comma-separated list of flags, e.g. "sf" for "sent by me and flagged"
     * @param tags coma-spearated list of tags, or null for no tags
     * @param receivedDate (optional) time the message was originally received, in MILLISECONDS since the epoch 
     * @param content message content
     * @param noICal if TRUE, then don't process iCal attachments.
     * @return ID of newly created message
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public String addMessage(String folderId, String flags, String tags, long receivedDate, String content, boolean noICal) throws ServiceException {
        XMLElement req = new XMLElement(MailConstants.ADD_MSG_REQUEST);
        Element m = req.addElement(MailConstants.E_MSG);
        m.addAttribute(MailConstants.A_FOLDER, folderId);
        if (flags != null && flags.length() > 0) 
            m.addAttribute(MailConstants.A_FLAGS, flags);
        if (tags != null && tags.length() > 0) 
            m.addAttribute(MailConstants.A_TAGS, tags);
        if (receivedDate != 0)
            m.addAttribute(MailConstants.A_DATE, receivedDate);
        m.addElement(MailConstants.E_CONTENT).setText(content);
        return invoke(req).getElement(MailConstants.E_MSG).getAttribute(MailConstants.A_ID);
    }

    /**
     * @param folderId (required) folderId of folder to add message to
     * @param flags non-comma-separated list of flags, e.g. "sf" for "sent by me and flagged"
     * @param tags coma-spearated list of tags, or null for no tags
     * @param receivedDate (optional) time the message was originally received, in MILLISECONDS since the epoch 
     * @param content message content
     * @param noICal if TRUE, then don't process iCal attachments.
     * @return ID of newly created message
     * @throws ServiceException on error
     */
    public String addMessage(String folderId, String flags, String tags, long receivedDate, byte[] content, boolean noICal) throws ServiceException {
        // first, upload the content via the FileUploadServlet
        String aid = uploadAttachment("message", content, "message/rfc822", 5000);

        // now, use the returned upload ID to do the message send
        XMLElement req = new XMLElement(MailConstants.ADD_MSG_REQUEST);
        Element m = req.addElement(MailConstants.E_MSG);
        m.addAttribute(MailConstants.A_FOLDER, folderId);
        if (flags != null && flags.length() > 0) 
            m.addAttribute(MailConstants.A_FLAGS, flags);
        if (tags != null && tags.length() > 0) 
            m.addAttribute(MailConstants.A_TAGS, tags);
        if (receivedDate > 0)
            m.addAttribute(MailConstants.A_DATE, receivedDate);
        m.addAttribute(MailConstants.A_ATTACHMENT_ID, aid);
        return invoke(req).getElement(MailConstants.E_MSG).getAttribute(MailConstants.A_ID);
    }

    static class CachedMessage {
        ZGetMessageParams params;
        ZMessage zm;
    }

    public synchronized ZMessage getMessage(ZGetMessageParams params) throws ServiceException {
        CachedMessage cm = (CachedMessage) mMessageCache.get(params.getId());
        if (cm == null || !cm.params.equals(params)) {
            XMLElement req = new XMLElement(MailConstants.GET_MSG_REQUEST);
            Element msgEl = req.addElement(MailConstants.E_MSG);
            msgEl.addAttribute(MailConstants.A_ID, params.getId());
            if (params.getPart() != null) msgEl.addAttribute(MailConstants.A_PART, params.getPart());
            msgEl.addAttribute(MailConstants.A_MARK_READ, params.isMarkRead());
            msgEl.addAttribute(MailConstants.A_WANT_HTML, params.isWantHtml());
            msgEl.addAttribute(MailConstants.A_NEUTER, params.isNeuterImages());
            msgEl.addAttribute(MailConstants.A_RAW, params.isRawContent());
            ZMessage zm = new ZMessage(invoke(req).getElement(MailConstants.E_MSG));
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
        if (mNeedsRefresh) noOp();
        return mUserRoot; 
    }

    /**
     * find the folder with the specified path, starting from the user root.
     * @param path path of folder. Must start with {@link #PATH_SEPARATOR}.
     * @return ZFolder if found, null otherwise.
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public ZFolder getFolderByPath(String path) throws ServiceException {
        if (mNeedsRefresh) noOp();
        if (!path.startsWith(ZMailbox.PATH_SEPARATOR)) 
            path = ZMailbox.PATH_SEPARATOR + path;
        return getUserRoot().getSubFolderByPath(path.substring(1));
    }
    
    /**
     * find the folder with the specified id.
     * @param id id of  folder
     * @return ZFolder if found, null otherwise.
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public ZFolder getFolderById(String id) throws ServiceException {
        if (mNeedsRefresh) noOp();
        ZItem item = mIdToItem.get(id);
        if (item instanceof ZFolder) return (ZFolder) item;
        else return null;
    }

    /**
     * returns a rest URL relative to this mailbox. 
     * @param relativePath a relative path (i.e., "/Calendar", "Inbox?fmt=rss", etc).
     * @return URI of path
     * @throws ServiceException on error
     */
    public URI getRestURI(String relativePath) throws ServiceException {
        try {
            URI uri = new URI(mTransport.getURI());
            return  uri.resolve("/home/" + getName() + (relativePath.startsWith("/") ? "" : "/") + relativePath);
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
     * @throws ServiceException on error
     */
    @SuppressWarnings({"EmptyCatchBlock"})
    public void getRESTResource(String relativePath, OutputStream os, boolean closeOs, int msecTimeout) throws ServiceException {
        GetMethod get = null;
        InputStream is = null;
        
        int statusCode;
        try {
            URI uri = getRestURI(relativePath);
            HttpClient client = getHttpClient(uri);
            
            if (msecTimeout > 0)
                client.getHttpConnectionManager().getParams().setConnectionTimeout(msecTimeout);

            get = new GetMethod(uri.toString());
            
            statusCode = client.executeMethod(get);
            // parse the response
            if (statusCode == 200) {
                is = get.getResponseBodyAsStream();
                ByteUtil.copy(is, false, os, false);
            } else {
                throw ServiceException.FAILURE("GET failed, status=" + statusCode+" "+get.getStatusText(), null);
            }
        } catch (IOException e) {
            throw ZClientException.IO_ERROR(e.getMessage(), e);
        } finally {
            if (is != null)
                try { is.close(); } catch (IOException e) { }
            if (closeOs && os != null)
                try { os.close(); } catch (IOException e) { }
            if (get != null)
                get.releaseConnection();
        }
    }
        
    /**
     * 
     * @param relativePath a relative path (i.e., "/Calendar", "Inbox?fmt=rss", etc).
     * @param is the input stream to post
     * @param closeIs whether to close the input stream when done
     * @param length length of inputstream, or 0/-1 if length is unknown.
     * @param contentType optional content-type header value (defaults to "application/octect-stream")
     * @param msecTimeout connection timeout
     * @throws ServiceException on error
     */
    @SuppressWarnings({"EmptyCatchBlock"})
    public void postRESTResource(String relativePath, InputStream is, boolean closeIs, long length, String contentType, int msecTimeout) throws ServiceException {
        PostMethod post = null;
        
        try {
            URI uri = getRestURI(relativePath);
            HttpClient client = getHttpClient(uri);

            if (msecTimeout > 0)
                client.getHttpConnectionManager().getParams().setConnectionTimeout(msecTimeout);

            post = new PostMethod(uri.toString());            
            RequestEntity entity = (length > 0) ? 
                    new InputStreamRequestEntity(is, length, contentType != null ? contentType:  "application/octet-stream") :
                    new InputStreamRequestEntity(is, contentType);
            post.setRequestEntity(entity);
           int statusCode = client.executeMethod(post);
            // parse the response
            if (statusCode == 200) {
                //
            } else {
                throw ServiceException.FAILURE("POST failed, status=" + statusCode+" "+post.getStatusText(), null);
            }
        } catch (IOException e) {
            throw ZClientException.IO_ERROR(e.getMessage(), e);
        } finally {
            if (is != null && closeIs) try { is.close(); } catch (IOException e) {}
            if (post != null) post.releaseConnection();
        }
    }

    
    /**
     * find the search folder with the specified id.
     * @param id id of  folder
     * @return ZSearchFolder if found, null otherwise.
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public ZSearchFolder getSearchFolderById(String id) throws ServiceException {
        if (mNeedsRefresh) noOp();
        ZItem item = mIdToItem.get(id);
        if (item instanceof ZSearchFolder) return (ZSearchFolder) item;
        else return null;
    }

    /**
     * find the mountpoint with the specified id.
     * @param id id of mountpoint
     * @return ZMountpoint if found, null otherwise.
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public ZMountpoint getMountpointById(String id) throws ServiceException {
        if (mNeedsRefresh) noOp();        
        ZItem item = mIdToItem.get(id);
        if (item instanceof ZMountpoint) return (ZMountpoint) item;
        else return null;
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
        XMLElement req = new XMLElement(MailConstants.CREATE_FOLDER_REQUEST);
        Element folderEl = req.addElement(MailConstants.E_FOLDER);
        folderEl.addAttribute(MailConstants.A_NAME, name);
        folderEl.addAttribute(MailConstants.A_FOLDER, parentId);
        if (defaultView != null) folderEl.addAttribute(MailConstants.A_DEFAULT_VIEW, defaultView.name());
        if (color != null) folderEl.addAttribute(MailConstants.A_COLOR, color.getValue());
        if (flags != null) folderEl.addAttribute(MailConstants.A_FLAGS, flags);
        if (url != null && url.length() > 0) folderEl.addAttribute(MailConstants.A_URL, url);
        Element newFolderEl = invoke(req).getElement(MailConstants.E_FOLDER);
        ZFolder newFolder = getFolderById(newFolderEl.getAttribute(MailConstants.A_ID));
        return newFolder != null ? newFolder : new ZFolder(newFolderEl, null); 
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
        XMLElement req = new XMLElement(MailConstants.CREATE_SEARCH_FOLDER_REQUEST);
        Element folderEl = req.addElement(MailConstants.E_SEARCH);
        folderEl.addAttribute(MailConstants.A_NAME, name);
        folderEl.addAttribute(MailConstants.A_FOLDER, parentId);
        folderEl.addAttribute(MailConstants.A_QUERY, query);
        if (color != null) folderEl.addAttribute(MailConstants.A_COLOR, color.getValue());
        if (types != null) folderEl.addAttribute(MailConstants.A_SEARCH_TYPES, types);
        if (sortBy != null) folderEl.addAttribute(MailConstants.A_SORTBY, sortBy.name());
        Element newSearchEl = invoke(req).getElement(MailConstants.E_SEARCH);
        ZSearchFolder newSearch = getSearchFolderById(newSearchEl.getAttribute(MailConstants.A_ID));
        return newSearch != null ? newSearch : new ZSearchFolder(newSearchEl, null);
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
        XMLElement req = new XMLElement(MailConstants.MODIFY_SEARCH_FOLDER_REQUEST);
        Element folderEl = req.addElement(MailConstants.E_SEARCH);
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
        
        public ZActionResult(String ids) {
            if (ids == null) ids = "";
            mIds = ids;
        }
        
        public String getIds() {
            return mIds;
        }
        
        public String[] getIdsAsArray() {
            return mIds.split(",");
        }
        
        public String toString() {
            return String.format("actionResult: { ids: %s }", mIds);
        }
    }

    private Element folderAction(String op, String ids) {
        XMLElement req = new XMLElement(MailConstants.FOLDER_ACTION_REQUEST);
        Element actionEl = req.addElement(MailConstants.E_ACTION);
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

    /** hard delete all items in folder and sub folders (doesn't delete the folder itself)
     * @param ids ids of folders to empty
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult emptyFolder(String ids) throws ServiceException {
        return doAction(folderAction("empty", ids));        
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
        return doAction(folderAction("rename", id).addAttribute(MailConstants.A_NAME, name));
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
     * @param inherit inherited bit
     * @return action result
     * @throws ServiceException on error 
     */
    public ZActionResult modifyFolderGrant(
            String folderId, GranteeType granteeType, String grantreeId, 
            String perms, String args, boolean inherit) throws ServiceException {
        Element action = folderAction("grant", folderId);
        Element grant = action.addElement(MailConstants.E_GRANT);
        grant.addAttribute(MailConstants.A_INHERIT, inherit);
        grant.addAttribute(MailConstants.A_RIGHTS, perms);
        grant.addAttribute(MailConstants.A_DISPLAY, grantreeId);
        grant.addAttribute(MailConstants.A_GRANT_TYPE, granteeType.name());
        if (args != null) grant.addAttribute(MailConstants.A_ARGS, args);
        return doAction(action);
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
    /**
     * sync the folder's contents to the remote feed specified by the folders URL
     * @param ids folder id
     * @throws ServiceException on error
     * @return action result
     */
    public ZActionResult syncFolder(String ids) throws ServiceException {
        return doAction(folderAction("sync", ids));
    }

    // ------------------------
    
    private ZSearchResult internalSearch(String convId, ZSearchParams params, boolean nest) throws ServiceException {
        XMLElement req = new XMLElement(convId == null ? MailConstants.SEARCH_REQUEST : MailConstants.SEARCH_CONV_REQUEST);

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
        if (params.isPreferHtml()) req.addAttribute(MailConstants.A_WANT_HTML, params.isPreferHtml());
        if (params.isMarkAsRead()) req.addAttribute(MailConstants.A_MARK_READ, params.isMarkAsRead());
        if (params.isRecipientMode()) req.addAttribute(MailConstants.A_RECIPIENTS, params.isRecipientMode());
        if (params.getField() != null) req.addAttribute(MailConstants.A_FIELD, params.getField());
        
        req.addElement(MailConstants.E_QUERY).setText(params.getQuery());
        
        if (params.getCursor() != null) {
            Cursor cursor = params.getCursor();
            Element cursorEl = req.addElement(MailConstants.E_CURSOR);
            if (cursor.getPreviousId() != null) cursorEl.addAttribute(MailConstants.A_ID, cursor.getPreviousId());
            if (cursor.getPreviousSortValue() != null) cursorEl.addAttribute(MailConstants.A_SORTVAL, cursor.getPreviousSortValue());
        }
        
        return new ZSearchResult(invoke(req), nest);
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
        return mSearchPagerCache.search(this, params, page, useCache, useCursor);
    }

    /**
     *
     * @param type if non-null, clear only cached searches of the specified tape
     */
    public void clearSearchCache(String type) {
        mSearchPagerCache.clear(type);
    }
 
    /**
     *  do a search conv
     * @param convId id of conversation to search 
     * @param params convId onversation id
     * @return search result
     * @throws ServiceException on error  
     */
    public ZSearchResult searchConversation(String convId, ZSearchParams params) throws ServiceException {
        if (convId == null) throw ZClientException.CLIENT_ERROR("conversation id must not be null", null);
        return internalSearch(convId, params, true);
    }

    public ZSearchPagerResult searchConversation(String convId, ZSearchParams params, int page, boolean useCache, boolean useCursor) throws ServiceException {
        if (params.getConvId() == null)
            params.setConvId(convId);
        return mSearchConvPagerCache.search(this, params, page, useCache, useCursor);
    }

    /**
     * A request that does nothing and always returns nothing. Used to keep a session alive, and return
     * any pending notifications.
     *
     * @throws ServiceException on error
     */
    public void noOp() throws ServiceException {
        invoke(new XMLElement(MailConstants.NO_OP_REQUEST));
    }    

    public enum OwnerBy { BY_ID, BY_NAME }
    
    public enum SharedItemBy { BY_ID, BY_PATH }
    
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
        XMLElement req = new XMLElement(MailConstants.CREATE_MOUNTPOINT_REQUEST);
        Element linkEl = req.addElement(MailConstants.E_MOUNT);
        linkEl.addAttribute(MailConstants.A_NAME, name);
        linkEl.addAttribute(MailConstants.A_FOLDER, parentId);
        if (defaultView != null) linkEl.addAttribute(MailConstants.A_DEFAULT_VIEW, defaultView.name());
        if (color != null) linkEl.addAttribute(MailConstants.A_COLOR, color.getValue());
        if (flags != null) linkEl.addAttribute(MailConstants.A_FLAGS, flags);
        linkEl.addAttribute(ownerBy == OwnerBy.BY_ID ? MailConstants.A_ZIMBRA_ID : MailConstants.A_OWNER_NAME, owner);
        linkEl.addAttribute(itemBy == SharedItemBy.BY_ID ? MailConstants.A_REMOTE_ID: MailConstants.A_PATH, sharedItem);
        Element newMountEl = invoke(req).getElement(MailConstants.E_MOUNT);
        ZMountpoint newMount = getMountpointById(newMountEl.getAttribute(MailConstants.A_ID));
        return newMount != null ? newMount : new ZMountpoint(newMountEl, null);
    }
    
    /**
     * Sends an iCalendar REPLY object
     * @param ical iCalendar data
     * @throws ServiceException on error
     */
    public void iCalReply(String ical) throws ServiceException {
        XMLElement req = new XMLElement(MailConstants.ICAL_REPLY_REQUEST);
        Element icalElem = req.addElement(MailConstants.E_CAL_ICAL);
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

            public AttachedMessagePart(String messageId, String partName) {
                mMessageId = messageId;
                mPartName = partName;
            }

            public String getMessageId()  {return mMessageId; }
            public void setMessageId(String messageId) { mMessageId = messageId; }

            public String getPartName() { return mPartName; }
            public void setPartName(String partName) { mPartName = partName; }
        }
        
        public static class MessagePart {
            private String mContentType;
            private String mContent;

            public MessagePart(String contentType, String content) {
                mContent = content;
                mContentType = contentType;
            }

            public String getContentType() { return mContentType; }
            public void setContentType(String contentType) { mContentType = contentType; }

            public String getContent() { return mContent; }

            public void setContent(String content) { mContentType = content; }
        }
        
        private List<ZEmailAddress> mAddresses;
        private String mSubject;
        private String mInReplyTo;
        private List<MessagePart> mMessageParts;
        private String mAttachmentUploadId;
        private List<AttachedMessagePart> mMessagePartsToAttach;
        private List<String> mContactIdsToAttach;
        private List<String> mMessageIdsToAttach;
        private String mOriginalMessageId;
        private String mReplyType;
        
        public List<ZEmailAddress> getAddresses() { return mAddresses; }
        public void setAddresses(List<ZEmailAddress> addresses) { mAddresses = addresses; }

        public String getAttachmentUploadId() { return mAttachmentUploadId; }
        public void setAttachmentUploadId(String attachmentUploadId) { mAttachmentUploadId = attachmentUploadId; }
        
        public List<String> getContactIdsToAttach() { return mContactIdsToAttach; }
        public void setContactIdsToAttach(List<String> contactIdsToAttach) { mContactIdsToAttach = contactIdsToAttach; }

        public List<MessagePart> getMessageParts() { return mMessageParts; }
        public void setMessageParts(List<MessagePart> messageParts) { mMessageParts = messageParts; }

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

        public List<String> getMessageIdsToAttach() { return mMessageIdsToAttach; }
        public void setMessageIdsToAttach(List<String> messageIdsToAttach) { mMessageIdsToAttach = messageIdsToAttach; }
    }

    public Element getMessageElement(Element req, ZOutgoingMessage message) {
        Element m = req.addElement(MailConstants.E_MSG);

        if (message.getOriginalMessageId() != null) 
            m.addAttribute(MailConstants.A_ORIG_ID, message.getOriginalMessageId());

        if (message.getReplyType() != null)
            m.addAttribute(MailConstants.A_REPLY_TYPE, message.getReplyType());

        if (message.getAddresses() != null) {
            for (ZEmailAddress addr : message.getAddresses()) {
                Element e = m.addElement(MailConstants.E_EMAIL);
                e.addAttribute(MailConstants.A_TYPE, addr.getType());
                e.addAttribute(MailConstants.A_ADDRESS, addr.getAddress());
                e.addAttribute(MailConstants.A_PERSONAL, addr.getPersonal());
            }
        }

        if (message.getSubject() != null) 
            m.addElement(MailConstants.E_SUBJECT).setText(message.getSubject());
        
        if (message.getInReplyTo() != null) 
            m.addElement(MailConstants.E_IN_REPLY_TO).setText(message.getInReplyTo());

        if (message.getMessageParts() != null) {
            for (MessagePart part : message.getMessageParts()) {
                Element mp = m.addElement(MailConstants.E_MIMEPART);
                mp.addAttribute(MailConstants.A_CONTENT_TYPE, part.getContentType());
                mp.addElement(MailConstants.E_CONTENT).setText(part.getContent());
            }
        }
        
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
        
        if (message.getMessagePartsToAttach() != null) {
            if (attach == null) attach = m.addElement(MailConstants.E_ATTACH);
            for (AttachedMessagePart part: message.getMessagePartsToAttach()) {
                attach.addElement(MailConstants.E_MIMEPART).addAttribute(MailConstants.A_MESSAGE_ID, part.getMessageId()).addAttribute(MailConstants.A_PART, part.getPartName());
            }            
        }
        return m;
    }
    
    public ZSendMessageResponse sendMessage(ZOutgoingMessage message, String sendUid, boolean needCalendarSentByFixup) throws ServiceException {
        XMLElement req = new XMLElement(MailConstants.SEND_MSG_REQUEST);
        
        if (sendUid != null)
            req.addAttribute(MailConstants.A_SEND_UID, sendUid);
        
        if (needCalendarSentByFixup) 
            req.addAttribute(MailConstants.A_NEED_CALENDAR_SENTBY_FIXUP, needCalendarSentByFixup);

        //noinspection UnusedDeclaration
        Element m = getMessageElement(req, message);

        Element resp = invoke(req);
        Element msg = resp.getOptionalElement(MailConstants.E_MSG);
        String id = msg == null ? null : msg.getAttribute(MailConstants.A_ID, null);
        return new ZSendMessageResponse(id);       
    }
 
    public ZMessage saveDraft(ZOutgoingMessage message, String existingDraftId, String folderId) throws ServiceException {
        XMLElement req = new XMLElement(MailConstants.SAVE_DRAFT_REQUEST);
        
        Element m = getMessageElement(req, message);

        if (existingDraftId != null && existingDraftId.length() > 0)
            m.addAttribute(MailConstants.A_ID, existingDraftId);

        if (folderId != null)
            m.addAttribute(MailConstants.A_FOLDER, folderId);

        return new ZMessage(invoke(req).getElement(MailConstants.E_MSG));
    }
    
    public void createIdentity(ZIdentity identity) throws ServiceException {
        XMLElement req = new XMLElement(AccountConstants.CREATE_IDENTITY_REQUEST);
        identity.toElement(req);
        invoke(req);
    }

    public List<ZIdentity> getIdentities() throws ServiceException {
        XMLElement req = new XMLElement(AccountConstants.GET_IDENTITIES_REQUEST);
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
        XMLElement req = new XMLElement(AccountConstants.DELETE_IDENTITY_REQUEST);
        if (by == IdentityBy.name)
            req.addElement(AccountConstants.E_IDENTITY).addAttribute(AccountConstants.A_NAME, key);
        else if (by == IdentityBy.id)
            req.addElement(AccountConstants.E_IDENTITY).addAttribute(AccountConstants.A_ID, key);
        invoke(req);
    }

    public void modifyIdentity(ZIdentity identity) throws ServiceException {
        XMLElement req = new XMLElement(AccountConstants.MODIFY_IDENTITY_REQUEST);
        identity.toElement(req);
        invoke(req);
    }

    public String createDataSource(ZDataSource source) throws ServiceException {
        XMLElement req = new XMLElement(MailConstants.CREATE_DATA_SOURCE_REQUEST);
        source.toElement(req);
        return invoke(req).listElements().get(0).getAttribute(MailConstants.A_ID);
    }

    /**
     *
     * @param host pop server hostname
     * @param port pop server port
     * @param username pop server username
     * @param password pop server password
     * @return null on success, or an error string on failure.
     * @throws ServiceException on error
     */
    public String testPop3DataSource(String host, int port, String username, String password) throws ServiceException {
        XMLElement req = new XMLElement(MailConstants.TEST_DATA_SOURCE_REQUEST);
        Element pop3 = req.addElement(MailConstants.E_DS_POP3);
        pop3.addAttribute(MailConstants.A_DS_HOST, host);
        pop3.addAttribute(MailConstants.A_DS_PORT, port);
        pop3.addAttribute(MailConstants.A_DS_USERNAME, username);
        pop3.addAttribute(MailConstants.A_DS_PASSWORD, password);
        Element resp = invoke(req);
        boolean success = resp.getAttributeBool(MailConstants.A_DS_SUCCESS, false);
        if (!success) {
            return resp.getAttribute(MailConstants.A_DS_ERROR, "error");
        } else {
            return null;
        }
    }

    public List<ZDataSource> getAllDataSources() throws ServiceException {
        XMLElement req = new XMLElement(MailConstants.GET_DATA_SOURCES_REQUEST);
        Element response = invoke(req);
        List<ZDataSource> result = new ArrayList<ZDataSource>();
        for (Element ds : response.listElements()) {
            if (ds.getName().equals(MailConstants.E_DS_POP3)) {
                result.add(new ZPop3DataSource(ds));
            }
        }
        return result;
    }

    public void modifyDataSource(ZDataSource source) throws ServiceException {
        XMLElement req = new XMLElement(MailConstants.MODIFY_DATA_SOURCE_REQUEST);
        source.toElement(req);
        invoke(req);
    }

    public void deleteDataSource(ZDataSource source) throws ServiceException {
        XMLElement req = new XMLElement(MailConstants.DELETE_DATA_SOURCE_REQUEST);
        source.toIdElement(req);
        invoke(req);
    }

    public ZFilterRules getFilterRules() throws ServiceException {
        XMLElement req = new XMLElement(MailConstants.GET_RULES_REQUEST);
        return new ZFilterRules(invoke(req).getElement(MailConstants.E_RULES));
    }

    public void saveFilterRules(ZFilterRules rules) throws ServiceException {
        XMLElement req = new XMLElement(MailConstants.SAVE_RULES_REQUEST);
        rules.toElement(req);
        invoke(req);
    }

    public void deleteDataSource(DataSourceBy by, String key) throws ServiceException {
        XMLElement req = new XMLElement(MailConstants.DELETE_DATA_SOURCE_REQUEST);
        if (by == DataSourceBy.name)
            req.addElement(MailConstants.E_DS).addAttribute(MailConstants.A_NAME, key);
        else if (by == DataSourceBy.id)
            req.addElement(MailConstants.E_DS).addAttribute(MailConstants.A_ID, key);
        else
            throw ServiceException.INVALID_REQUEST("must specify data source by id or name", null);
        invoke(req);
    }

    public void importData(List<ZDataSource> sources) throws ServiceException {
        XMLElement req = new XMLElement(MailConstants.IMPORT_DATA_REQUEST);
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

        ZImportStatus(Element e) throws ServiceException {
            mType = e.getName();
            mIsRunning = e.getAttributeBool(MailConstants.A_DS_IS_RUNNING, false);
            mSuccess = e.getAttributeBool(MailConstants.A_DS_SUCCESS, true);
            mError = e.getAttribute(MailConstants.A_DS_ERROR, null);
        }

        public String getType() { return mType; }
        public boolean isRunning() { return mIsRunning; }
        public boolean getSuccess() { return mSuccess; }
        public String getError() { return mError; }
    }

    public List<ZImportStatus> getImportStatus() throws ServiceException {
        XMLElement req = new XMLElement(MailConstants.GET_IMPORT_STATUS_REQUEST);
        Element response = invoke(req);
        List<ZImportStatus> result = new ArrayList<ZImportStatus>();
        for (Element status : response.listElements()) {
            result.add(new ZImportStatus(status));
        }
        return result;
    }

    public String createDocument(String folderId, String name, String attachmentId) throws ServiceException {
    	XMLElement req = new XMLElement(MailConstants.SAVE_DOCUMENT_REQUEST);
    	Element doc = req.addElement(MailConstants.E_DOC);
    	doc.addAttribute(MailConstants.A_NAME, name);
    	doc.addAttribute(MailConstants.A_FOLDER, folderId);
    	Element upload = doc.addElement(MailConstants.E_UPLOAD);
    	upload.addAttribute(MailConstants.A_ID, attachmentId);
    	return invoke(req).getElement(MailConstants.E_DOC).getAttribute(MailConstants.A_ID);
    }

    public String createWiki(String folderId, String name, String contents) throws ServiceException {
    	XMLElement req = new XMLElement(MailConstants.SAVE_WIKI_REQUEST);
    	Element doc = req.addElement(MailConstants.E_WIKIWORD);
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
    public void modifyPrefs(Map<String, ? extends Object> prefs) throws ServiceException {
        XMLElement req = new XMLElement(AccountConstants.MODIFY_PREFS_REQUEST);
        for (Map.Entry<String, ? extends Object> entry : prefs.entrySet()){
            Object vo = entry.getValue();
            if (vo instanceof String[]) {
                String[] values = (String[]) vo;
                for (String v : values) {
                    Element pref = req.addElement(AccountConstants.E_PREF);
                    pref.addAttribute(AccountConstants.A_NAME, entry.getKey());
                    pref.setText(v);
                }
            } else if (vo instanceof Collection) {
                Collection values = (Collection) vo;
                for (Object v : values) {
                    Element pref = req.addElement(AccountConstants.E_PREF);
                    pref.addAttribute(AccountConstants.A_NAME, entry.getKey());
                    pref.setText(v.toString());
                }
            } else {
                Element pref = req.addElement(AccountConstants.E_PREF);
                pref.addAttribute(AccountConstants.A_NAME, entry.getKey());
                pref.setText(vo.toString());
            }
        }
        invoke(req);
    }

    public List<String> getAvailableSkins() throws ServiceException {
        XMLElement req = new XMLElement(AccountConstants.GET_AVAILABLE_SKINS_REQUEST);
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
        XMLElement req = new XMLElement(AccountConstants.SEARCH_GAL_REQUEST);
        if (type != null)
        req.addAttribute(AccountConstants.A_TYPE, type.name());
        req.addElement(AccountConstants.E_NAME).setText(query);
        Element resp = invoke(req);
        List<ZContact> contacts = new ArrayList<ZContact>();
        for (Element contact : resp.listElements(MailConstants.E_CONTACT)) {
                contacts.add(new ZContact(contact));
        }
        return new ZSearchGalResult(contacts, resp.getAttributeBool(AccountConstants.A_MORE, false), query, type);
    }
}
