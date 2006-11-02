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

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.zclient.ZGrant.GranteeType;
import com.zimbra.cs.zclient.ZSearchParams.Cursor;
import com.zimbra.soap.Element;
import com.zimbra.soap.Element.XMLElement;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.SoapHttpTransport;
import com.zimbra.soap.SoapTransport;
import com.zimbra.soap.ZimbraNamespace;
import com.zimbra.soap.ZimbraSoapContext;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZMailbox {

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
        private String mAuthToken;
        private String mUri;
        private SoapTransport.DebugListener mDebugListener;
        private String mTargetAccount;
        private AccountBy mTargetAccountBy = AccountBy.name;
        private boolean mNoSession;
        private boolean mNoNotify;

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

        public AccountBy getTaretAccountBy() { return mTargetAccountBy; }
        public void setTargetAccountBy(AccountBy targetAccountBy) { mTargetAccountBy = targetAccountBy; }

        public String getPassword() { return mPassword; }
        public void setPassword(String password) { mPassword = password; }

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

    }

    private String mAuthToken;
    private SoapHttpTransport mTransport;

    private Map<String, ZTag> mNameToTag;
    private Map<String, ZItem> mIdToItem;
    private boolean mProcessedRefresh;
    private ZGetInfoResult mGetInfoResult;
    private ZFolder mUserRoot;
    private SoapTransport.DebugListener mListener;
    
    private long mSize;

    public static ZMailbox getMailbox(Options options) throws ServiceException {
        return new ZMailbox(options);
    }
    
    public ZMailbox(Options options) throws ServiceException {
        initPreAuth(options.getUri(), options.getDebugListener());
        if (options.getAuthToken() != null) {
            initAuthToken(options.getAuthToken());
        } else {
            initAuthToken(auth(options.getAccount(), options.getAccountBy(), options.getPassword()).getAuthToken());
        }
        if (options.getTargetAccount() != null) {
            initTargetAccount(options.getTargetAccount(), options.getTaretAccountBy());
        }
    }

    public void initAuthToken(String authToken) throws ServiceException {
        mAuthToken = authToken;
        mTransport.setAuthToken(mAuthToken);
        getAccountInfo(true);
    }

    public void initPreAuth(String uri, SoapTransport.DebugListener listener) {
        mNameToTag = new HashMap<String, ZTag>();
        mIdToItem = new HashMap<String, ZItem>();                
        mListener = listener;
        setSoapURI(uri);
        if (listener != null) mTransport.setDebugListener(listener);
    }

    private void initTargetAccount(String key, AccountBy by) {
        if (AccountBy.id.equals(by))
            mTransport.setTargetAcctId(key);
        else if (AccountBy.name.equals(by))
            mTransport.setTargetAcctName(key);
    }

    private ZAuthResult auth(String key, AccountBy by, String password) throws ServiceException {
        if (mTransport == null) throw ZClientException.CLIENT_ERROR("must call setURI before calling asuthenticate", null);
        XMLElement req = new XMLElement(AccountService.AUTH_REQUEST);
        Element account = req.addElement(AccountService.E_ACCOUNT);
        account.addAttribute(AccountService.A_BY, by.name());
        account.setText(key);
        req.addElement(AccountService.E_PASSWORD).setText(password);
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
        mTransport.setUserAgent("zclient", "1.0");
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
        if (refresh != null) refreshHandler(refresh);
        for (Element notify : context.listElements(ZimbraNamespace.E_NOTIFY)) {
            mTransport.setMaxNoitfySeq(
                    Math.max(mTransport.getMaxNotifySeq(),
                             notify.getAttributeLong(ZimbraSoapContext.A_SEQNO, 0)));
            // MUST DO IN THIS ORDER?
            handleDeleted(notify.getOptionalElement(ZimbraNamespace.E_DELETED));
            handleCreated(notify.getOptionalElement(ZimbraNamespace.E_CREATED));
            handleModified(notify.getOptionalElement(ZimbraNamespace.E_MODIFIED));
        }
    }

    private void handleModified(Element modified) throws ServiceException {
        if (modified == null) return;
        for (Element e : modified.listElements()) {
            if (e.getName().equals(MailService.E_TAG)) {
                ZTag tag = getTagById(e.getAttribute(MailService.A_ID));
                if (tag != null) {
                    String oldName = tag.getName();
                    tag.modifyNotification(e);
                    if (!tag.getName().equalsIgnoreCase(oldName)) {
                        mNameToTag.remove(oldName);
                        mNameToTag.put(tag.getName(), tag);
                    }
                }
            } else if (e.getName().equals(MailService.E_SEARCH) || e.getName().equals(MailService.E_FOLDER) || e.getName().equals(MailService.E_MOUNT)) {
                ZFolder f = getFolderById(e.getAttribute(MailService.A_ID));
                if (f != null)
                    f.modifyNotification(e, this);
            } else if (e.getName().equals(MailService.E_MAILBOX)) {
                mSize = e.getAttributeLong(MailService.A_SIZE);                
            }
        }
    }

    private void handleCreated(Element created) throws ServiceException {
        if (created == null) return;
        for (Element e : created.listElements()) {
            if (e.getName().equals(MailService.E_FOLDER)) {
                String parentId = e.getAttribute(MailService.A_FOLDER);
                ZFolder parent = getFolderById(parentId);
                new ZFolder(e, parent, this);
            } else if (e.getName().equals(MailService.E_MOUNT)) {
                String parentId = e.getAttribute(MailService.A_FOLDER);
                ZFolder parent = getFolderById(parentId);
                new ZMountpoint(e, parent, this);
            } else if (e.getName().equals(MailService.E_SEARCH)) {
                String parentId = e.getAttribute(MailService.A_FOLDER);
                ZFolder parent = getFolderById(parentId);
                new ZSearchFolder(e, parent, this);
            } else if (e.getName().equals(MailService.E_TAG)) {
                addTag(new ZTag(e));
            }
        }
    }

    private void handleDeleted(Element deleted) {
        if (deleted == null) return;
        String ids = deleted.getAttribute(MailService.A_ID, null);
        if (ids == null) return;
        for (String id : ids.split(",")) {
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

    private void addTag(ZTag tag) {
        mNameToTag.put(tag.getName(), tag);
        addItemIdMapping(tag);
    }

    /**
     * handle a &lt;refresh&gt; block
     * @param refresh refresh block element
     * @throws ServiceException on error
     */
    private void refreshHandler(Element refresh) throws ServiceException {
        mProcessedRefresh = true;
        mNameToTag.clear();
        mIdToItem.clear();
        mTransport.setMaxNoitfySeq(0);
        Element mbx = refresh.getOptionalElement(MailService.E_MAILBOX);
        if (mbx != null) mSize = mbx.getAttributeLong(MailService.A_SIZE);
        Element tags = refresh.getOptionalElement(ZimbraNamespace.E_TAGS);
        if (tags != null) {
            for (Element t : tags.listElements(MailService.E_TAG))
                addTag(new ZTag(t));
        }
        Element folder = refresh.getOptionalElement(MailService.E_FOLDER);
        refreshFolders(folder);
    }

    void addItemIdMapping(ZItem item) {
        mIdToItem.put(item.getId(), item);
    }

    void addRemoteItemIdMapping(String remoteId, ZItem item) {
        mIdToItem.put(remoteId, item);
    }

    private void refreshFolders(Element folderEl) throws ServiceException {
        mUserRoot = new ZFolder(folderEl, null, this);
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
     */
    public long getSize() {
        return mSize;
    }

    /**
     * @return account name of mailbox
     * @throws com.zimbra.cs.service.ServiceException on error
     */
    public String getName() throws ServiceException {
        return getAccountInfo(false).getName();
    }
   
    
    public ZGetInfoResult getAccountInfo(boolean refresh) throws ServiceException {
        if (mGetInfoResult == null || refresh) {
            XMLElement req = new XMLElement(AccountService.GET_INFO_REQUEST);
            mGetInfoResult = new ZGetInfoResult(invoke(req));            
        }
        return mGetInfoResult;       
    }
    
    //  ------------------------
    
    /**
     * @return current List of all tags in the mailbox
     */
    @SuppressWarnings("unchecked")    
    public List<ZTag> getAllTags() {
        List result = new ArrayList<ZTag>(mNameToTag.values());
        Collections.sort(result);
        return result;      
    }
    
    /**
     * @return current list of all tags names in the mailbox, sorted
     */
    public List<String> getAllTagNames() {
        ArrayList<String> names = new ArrayList<String>(mNameToTag.keySet());
        Collections.sort(names);
        return names;   
    }
    
    /**
     * @return current list of all tags ids in the mailbox
     */
    public List<String> getAllTagIds() {
        ArrayList<String> ids = new ArrayList<String>(mNameToTag.size());
        for (ZTag tag: mNameToTag.values()) {
            ids.add(tag.getId());
        }
        return ids;
    }
    
    /**
     * returns the tag the specified name, or null if no such tag exists.
     * 
     * @param name tag name
     * @return the tag, or null if tag not found
     */
    public ZTag getTagByName(String name) {
        return mNameToTag.get(name);
    }

    /**
     * returns the tag with the specified id, or null if no such tag exists.
     * 
     * @param id the tag id
     * @return tag with given id, or null
     */
    public ZTag getTagById(String id) {
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
     * @throws com.zimbra.cs.service.ServiceException if an error occurs
     *
     */
    public ZTag createTag(String name, ZTag.Color color) throws ServiceException {
        XMLElement req = new XMLElement(MailService.CREATE_TAG_REQUEST);
        Element tagEl = req.addElement(MailService.E_TAG);
        tagEl.addAttribute(MailService.A_NAME, name);
        if (color != null) tagEl.addAttribute(MailService.A_COLOR, color.getValue());
        String id = invoke(req).getElement(MailService.E_TAG).getAttribute(MailService.A_ID);
        // this assumes notifications will create the tag
        return getTagById(id);      
    }

    /**
     * modifies the tag's color
     * @return action result
     * @param id id of tag to modify
     * @param color color of tag to modify
     * @throws com.zimbra.cs.service.ServiceException on error
     */
    public ZActionResult modifyTagColor(String id, ZTag.Color color) throws ServiceException {
        return doAction(tagAction("color", id).addAttribute(MailService.A_COLOR, color.getValue()));        
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
        return doAction(tagAction("rename", id).addAttribute(MailService.A_NAME, name));
    }

    private Element tagAction(String op, String id) {
        XMLElement req = new XMLElement(MailService.TAG_ACTION_REQUEST);
        Element actionEl = req.addElement(MailService.E_ACTION);
        actionEl.addAttribute(MailService.A_ID, id);
        actionEl.addAttribute(MailService.A_OPERATION, op);
        return actionEl;
    }
    
    private ZActionResult doAction(Element actionEl) throws ServiceException {
        Element response = invoke(actionEl.getParent());
        return new ZActionResult(response.getElement(MailService.E_ACTION).getAttribute(MailService.A_ID));
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
        XMLElement req = new XMLElement(MailService.GET_CONTACTS_REQUEST);
        if (optFolderId != null) 
            req.addAttribute(MailService.A_FOLDER, optFolderId);
        if (sortBy != null) 
            req.addAttribute(MailService.A_SORTBY, sortBy.name());
        if (sync)
            req.addAttribute(MailService.A_SYNC, sync);

        if (attrs != null) {
            for (String name : attrs)
                req.addElement(MailService.E_ATTRIBUTE).addAttribute(MailService.A_ATTRIBUTE_NAME, name);
        }
        
        Element response = invoke(req);
        List<ZContact> result = new ArrayList<ZContact>();
        for (Element cn : response.listElements(MailService.E_CONTACT)) {
            result.add(new ZContact(cn));
        }
        return result;
    }
    
    public String createContact(String folderId, String tags, Map<String, String> attrs) throws ServiceException {
        XMLElement req = new XMLElement(MailService.CREATE_CONTACT_REQUEST);
        Element cn = req.addElement(MailService.E_CONTACT);
        if (folderId != null) 
            cn.addAttribute(MailService.A_FOLDER, folderId);
        if (tags != null) 
            cn.addAttribute(MailService.A_TAGS, tags);
        for (Map.Entry<String, String> entry : attrs.entrySet()) {
            Element a = cn.addElement(MailService.E_ATTRIBUTE);
            a.addAttribute(MailService.A_ATTRIBUTE_NAME, entry.getKey());
            a.setText(entry.getValue());
        }
        return invoke(req).getElement(MailService.E_CONTACT).getAttribute(MailService.A_ID);
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
        XMLElement req = new XMLElement(MailService.MODIFY_CONTACT_REQUEST);
        if (replace) 
            req.addAttribute(MailService.A_REPLACE, replace);
        Element cn = req.addElement(MailService.E_CONTACT);
        cn.addAttribute(MailService.A_ID, id);
        for (Map.Entry<String, String> entry : attrs.entrySet()) {
            Element a = cn.addElement(MailService.E_ATTRIBUTE);
            a.addAttribute(MailService.A_ATTRIBUTE_NAME, entry.getKey());
            a.setText(entry.getValue());
        }
        return invoke(req).getElement(MailService.E_CONTACT).getAttribute(MailService.A_ID);
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
        XMLElement req = new XMLElement(MailService.GET_CONTACTS_REQUEST);

        if (sortBy != null) 
            req.addAttribute(MailService.A_SORTBY, sortBy.name());
        if (sync)
            req.addAttribute(MailService.A_SYNC, sync);        
        req.addElement(MailService.E_CONTACT).addAttribute(MailService.A_ID, ids);
        if (attrs != null) {
            for (String name : attrs)
                req.addElement(MailService.E_ATTRIBUTE).addAttribute(MailService.A_ATTRIBUTE_NAME, name);
        }
        List<ZContact> result = new ArrayList<ZContact>();
        for (Element cn : invoke(req).listElements(MailService.E_CONTACT)) {
            result.add(new ZContact(cn));
        }
        return result;
    }   
    
    private Element contactAction(String op, String id) {
        XMLElement req = new XMLElement(MailService.CONTACT_ACTION_REQUEST);
        Element actionEl = req.addElement(MailService.E_ACTION);
        actionEl.addAttribute(MailService.A_ID, id);
        actionEl.addAttribute(MailService.A_OPERATION, op);
        return actionEl;
    }
    
    public ZActionResult moveContact(String ids, String destFolderId) throws ServiceException {
        return doAction(contactAction("move", ids).addAttribute(MailService.A_FOLDER, destFolderId));        
    }
    
    public ZActionResult deleteContact(String ids) throws ServiceException {
        return doAction(contactAction("delete", ids));        
    }
    
    public ZActionResult flagContact(String ids, boolean flag) throws ServiceException {
        return doAction(contactAction(flag ? "flag" : "!flag", ids));
    }
    
    public ZActionResult tagContact(String ids, String tagId, boolean tag) throws ServiceException {
        return doAction(contactAction(tag ? "tag" : "!tag", ids).addAttribute(MailService.A_TAG, tagId));
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
        if (destFolderId != null && destFolderId.length() > 0) actionEl.addAttribute(MailService.A_FOLDER, destFolderId);
        if (tagList != null) actionEl.addAttribute(MailService.A_TAGS, tagList);
        if (flags != null) actionEl.addAttribute(MailService.A_FLAGS, flags);
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
        XMLElement req = new XMLElement(MailService.GET_CONV_REQUEST);
        Element convEl = req.addElement(MailService.E_CONV);
        convEl.addAttribute(MailService.A_ID, id);
        if (fetch != null && fetch != Fetch.none && fetch != Fetch.hits) {
            // use "1" for "first" for backward compat until DF is updated
            convEl.addAttribute(MailService.A_FETCH, fetch == Fetch.first ? "1" : fetch.name());
        }        
        
        Map<String,ZEmailAddress> cache = new HashMap<String, ZEmailAddress>();
        return new ZConversation(invoke(req).getElement(MailService.E_CONV), cache);
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
        XMLElement req = new XMLElement(MailService.CONV_ACTION_REQUEST);
        Element actionEl = req.addElement(MailService.E_ACTION);
        actionEl.addAttribute(MailService.A_ID, id);
        actionEl.addAttribute(MailService.A_OPERATION, op);
        if (constraints != null) actionEl.addAttribute(MailService.A_TARGET_CONSTRAINT, constraints);
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
        return doAction(convAction(tag ? "tag" : "!tag", ids, targetConstraints).addAttribute(MailService.A_TAG, tagId));        
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
        return doAction(convAction("move", ids, targetConstraints).addAttribute(MailService.A_FOLDER, destFolderId));
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
        if (destFolderId != null && destFolderId.length() > 0) actionEl.addAttribute(MailService.A_FOLDER, destFolderId);
        return doAction(actionEl);
    }

    private Element messageAction(String op, String id) {
        XMLElement req = new XMLElement(MailService.MSG_ACTION_REQUEST);
        Element actionEl = req.addElement(MailService.E_ACTION);
        actionEl.addAttribute(MailService.A_ID, id);
        actionEl.addAttribute(MailService.A_OPERATION, op);
        return actionEl;
    }

    // ------------------------

    private Element itemAction(String op, String id, String constraints) {
        XMLElement req = new XMLElement(MailService.ITEM_ACTION_REQUEST);
        Element actionEl = req.addElement(MailService.E_ACTION);
        actionEl.addAttribute(MailService.A_ID, id);
        actionEl.addAttribute(MailService.A_OPERATION, op);
        if (constraints != null) actionEl.addAttribute(MailService.A_TARGET_CONSTRAINT, constraints);
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
        return doAction(itemAction(tag ? "tag" : "!tag", ids, targetConstraints).addAttribute(MailService.A_TAG, tagId));
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
        return doAction(itemAction("move", ids, targetConstraints).addAttribute(MailService.A_FOLDER, destFolderId));
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
        if (destFolderId != null && destFolderId.length() > 0) actionEl.addAttribute(MailService.A_FOLDER, destFolderId);
        if (tagList != null) actionEl.addAttribute(MailService.A_TAGS, tagList);
        if (flags != null) actionEl.addAttribute(MailService.A_FLAGS, flags);
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

        return doUpload(parts, msTimeout);
    }


    public String uploadAttachment(String name, byte[] content, String contentType, int msTimeout) throws ServiceException {
        FilePart part = new FilePart(name, new ByteArrayPartSource(name, content));
        part.setContentType(contentType);

        return doUpload(new Part[] { part }, msTimeout);
    }

    private String doUpload(Part[] parts, int msTimeout) throws ServiceException {
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
                if (aid == null)
                    throw ServiceException.FAILURE("Attachment post failed, unable to parse response: " + response, null);
            } else {
                throw ServiceException.FAILURE("Attachment post failed, status=" + statusCode, null);
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

    private String getAttachmentId(String result) {
        Matcher m = sAttachmentId.matcher(result);
        return m.find() ? m.group(1) : null;
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
     * @throws com.zimbra.cs.service.ServiceException on error
     */
    public String addMessage(String folderId, String flags, String tags, long receivedDate, String content, boolean noICal) throws ServiceException {
        XMLElement req = new XMLElement(MailService.ADD_MSG_REQUEST);
        Element m = req.addElement(MailService.E_MSG);        
        m.addAttribute(MailService.A_FOLDER, folderId);
        if (flags != null && flags.length() > 0) 
            m.addAttribute(MailService.A_FLAGS, flags);
        if (tags != null && tags.length() > 0) 
            m.addAttribute(MailService.A_TAGS, tags);
        if (receivedDate != 0)
            m.addAttribute(MailService.A_DATE, receivedDate);
        m.addElement(MailService.E_CONTENT).setText(content);
        return invoke(req).getElement(MailService.E_MSG).getAttribute(MailService.A_ID);
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
        XMLElement req = new XMLElement(MailService.ADD_MSG_REQUEST);
        Element m = req.addElement(MailService.E_MSG);        
        m.addAttribute(MailService.A_FOLDER, folderId);
        if (flags != null && flags.length() > 0) 
            m.addAttribute(MailService.A_FLAGS, flags);
        if (tags != null && tags.length() > 0) 
            m.addAttribute(MailService.A_TAGS, tags);
        if (receivedDate > 0)
            m.addAttribute(MailService.A_DATE, receivedDate);
        m.addAttribute(MailService.A_ATTACHMENT_ID, aid);
        return invoke(req).getElement(MailService.E_MSG).getAttribute(MailService.A_ID);
    }
     
    public ZMessage getMessage(String id, boolean markRead, boolean wantHtml, boolean neuterImages, boolean rawContent, String part) throws ServiceException {
        XMLElement req = new XMLElement(MailService.GET_MSG_REQUEST);
        Element msgEl = req.addElement(MailService.E_MSG);
        msgEl.addAttribute(MailService.A_ID, id);
        if (part != null) msgEl.addAttribute(MailService.A_PART, part);
        msgEl.addAttribute(MailService.A_MARK_READ, markRead);
        msgEl.addAttribute(MailService.A_WANT_HTML, wantHtml);
        msgEl.addAttribute(MailService.A_NEUTER, neuterImages);
        msgEl.addAttribute(MailService.A_RAW, rawContent);
        Map<String,ZEmailAddress> cache = new HashMap<String, ZEmailAddress>();
        return new ZMessage(invoke(req).getElement(MailService.E_MSG), cache);
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
        if (destFolderId != null && destFolderId.length() > 0) actionEl.addAttribute(MailService.A_FOLDER, destFolderId);
        return doAction(actionEl);        
    }
    
    /** flag/unflag message(s)
     *
     * @return action result
     * @param ids of messages to flag
     * @param flag flag on /off
     * @throws com.zimbra.cs.service.ServiceException on error
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
        return doAction(messageAction(tag ? "tag" : "!tag", ids).addAttribute(MailService.A_TAG, tagId));
    }
    
    /** move message(s)
     * @param ids list of ids to move
     * @param destFolderId destination folder id
     * @return action result
     * @throws ServiceException on error 
     */
    public ZActionResult moveMessage(String ids, String destFolderId) throws ServiceException {
        return doAction(messageAction("move", ids).addAttribute(MailService.A_FOLDER, destFolderId));
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
        if (destFolderId != null && destFolderId.length() > 0) actionEl.addAttribute(MailService.A_FOLDER, destFolderId);
        if (tagList != null) actionEl.addAttribute(MailService.A_TAGS, tagList);
        if (flags != null) actionEl.addAttribute(MailService.A_FLAGS, flags);
        return doAction(actionEl);        
    }

    // ------------------------
    
    /**
     * return the root user folder
     * @return user root folder
     */
    public ZFolder getUserRoot() {
        return mUserRoot; 
    }

    /**
     * find the folder with the pecified path, starting from the user root.
     * @param path path of folder. Must start with {@link #PATH_SEPARATOR}.
     * @return ZFolder if found, null otherwise.
     */
    public ZFolder getFolderByPath(String path) {
        if (!path.startsWith(ZMailbox.PATH_SEPARATOR)) 
            path = ZMailbox.PATH_SEPARATOR + path;
        return getUserRoot().getSubFolderByPath(path.substring(1));
    }
    
    /**
     * find the folder with the specified id.
     * @param id id of  folder
     * @return ZFolder if found, null otherwise.
     */
    public ZFolder getFolderById(String id) {
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
     */
    public ZSearchFolder getSearchFolderById(String id) {
        ZItem item = mIdToItem.get(id);
        if (item instanceof ZSearchFolder) return (ZSearchFolder) item;
        else return null;
    }

    /**
     * find the mountpoint with the specified id.
     * @param id id of mountpoint
     * @return ZMountpoint if found, null otherwise.
     */
    public ZMountpoint getMountpointById(String id) {
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
     */
    public ZFolder createFolder(String parentId, String name, ZFolder.View defaultView, ZFolder.Color color, String flags) throws ServiceException {
        XMLElement req = new XMLElement(MailService.CREATE_FOLDER_REQUEST);
        Element folderEl = req.addElement(MailService.E_FOLDER);
        folderEl.addAttribute(MailService.A_NAME, name);
        folderEl.addAttribute(MailService.A_FOLDER, parentId);
        if (defaultView != null) folderEl.addAttribute(MailService.A_DEFAULT_VIEW, defaultView.name());
        if (color != null) folderEl.addAttribute(MailService.A_COLOR, color.getValue());
        if (flags != null) folderEl.addAttribute(MailService.A_FLAGS, flags);
        String id = invoke(req).getElement(MailService.E_FOLDER).getAttribute(MailService.A_ID);
        // this assumes notifications will create the folder
        return getFolderById(id);
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
        XMLElement req = new XMLElement(MailService.CREATE_SEARCH_FOLDER_REQUEST);
        Element folderEl = req.addElement(MailService.E_SEARCH);
        folderEl.addAttribute(MailService.A_NAME, name);
        folderEl.addAttribute(MailService.A_FOLDER, parentId);
        folderEl.addAttribute(MailService.A_QUERY, query);
        if (color != null) folderEl.addAttribute(MailService.A_COLOR, color.getValue());
        if (types != null) folderEl.addAttribute(MailService.A_SEARCH_TYPES, types);
        if (sortBy != null) folderEl.addAttribute(MailService.A_SORTBY, sortBy.name());
        String id = invoke(req).getElement(MailService.E_SEARCH).getAttribute(MailService.A_ID);
        // this assumes notifications will create the folder
        return getSearchFolderById(id);
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
        XMLElement req = new XMLElement(MailService.MODIFY_SEARCH_FOLDER_REQUEST);
        Element folderEl = req.addElement(MailService.E_SEARCH);
        folderEl.addAttribute(MailService.A_ID, id);
        if (query != null) folderEl.addAttribute(MailService.A_QUERY, query);
        if (types != null) folderEl.addAttribute(MailService.A_SEARCH_TYPES, types);
        if (sortBy != null) folderEl.addAttribute(MailService.A_SORTBY, sortBy.name());
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
        XMLElement req = new XMLElement(MailService.FOLDER_ACTION_REQUEST);
        Element actionEl = req.addElement(MailService.E_ACTION);
        actionEl.addAttribute(MailService.A_ID, ids);
        actionEl.addAttribute(MailService.A_OPERATION, op);
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
        return doAction(folderAction("color", ids).addAttribute(MailService.A_COLOR, color.getValue()));
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
        return doAction(folderAction("import", id).addAttribute(MailService.A_URL, url));
    }
    
    /** move the folder to be a child of {target-folder}
     * @param id folder id to move
     * @param targetFolderId id of target folder
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult moveFolder(String id, String targetFolderId) throws ServiceException {
        return doAction(folderAction("move", id).addAttribute(MailService.A_FOLDER, targetFolderId));
    }
    
    /** change the folder's name; if new name  begins with '/', the folder is moved to the new path and any missing path elements are created
     * @param id id of folder to rename
     * @param name new name
     * @return action result
     * @throws ServiceException on error
     */
    public ZActionResult renameFolder(String id, String name) throws ServiceException {
        return doAction(folderAction("rename", id).addAttribute(MailService.A_NAME, name));
    }

    /** sets or unsets the folder's exclude from free busy state
     * @param ids folder id
     * @param state exclude/not-exclude
     * @throws ServiceException on error
     * @return action result
     */
    public ZActionResult modifyFolderExcludeFreeBusy(String ids, boolean state) throws ServiceException {
        return doAction(folderAction("fb", ids).addAttribute(MailService.A_EXCLUDE_FREEBUSY, state));
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
        Element grant = action.addElement(MailService.E_GRANT);
        grant.addAttribute(MailService.A_INHERIT, inherit);
        grant.addAttribute(MailService.A_RIGHTS, perms);
        grant.addAttribute(MailService.A_DISPLAY, grantreeId);
        grant.addAttribute(MailService.A_GRANT_TYPE, granteeType.name());
        if (args != null) grant.addAttribute(MailService.A_ARGS, args);
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
        action.addAttribute(MailService.A_ZIMBRA_ID, grantreeId);
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
        return doAction(folderAction("url", id).addAttribute(MailService.A_URL, url));
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
    
    private ZSearchResult internalSearch(String convId, ZSearchParams params) throws ServiceException {
        XMLElement req = new XMLElement(convId == null ? MailService.SEARCH_REQUEST : MailService.SEARCH_CONV_REQUEST);

        req.addAttribute(MailService.A_CONV_ID, convId);
        if (params.getLimit() != 0) req.addAttribute(MailService.A_QUERY_LIMIT, params.getLimit());
        if (params.getOffset() != 0) req.addAttribute(MailService.A_QUERY_OFFSET, params.getOffset());
        if (params.getSortBy() != null) req.addAttribute(MailService.A_SORTBY, params.getSortBy().name());
        if (params.getTypes() != null) req.addAttribute(MailService.A_SEARCH_TYPES, params.getTypes());
        if (params.getFetch() != null && params.getFetch() != Fetch.none) {
            // use "1" for "first" for backward compat until DF is updated
            req.addAttribute(MailService.A_FETCH, params.getFetch() == Fetch.first ? "1" : params.getFetch().name());
        }
        if (params.isPreferHtml()) req.addAttribute(MailService.A_WANT_HTML, params.isPreferHtml());
        if (params.isMarkAsRead()) req.addAttribute(MailService.A_MARK_READ, params.isMarkAsRead());
        if (params.isRecipientMode()) req.addAttribute(MailService.A_RECIPIENTS, params.isRecipientMode());
        
        req.addElement(MailService.E_QUERY).setText(params.getQuery());
        
        if (params.getCursor() != null) {
            Cursor cursor = params.getCursor();
            Element cursorEl = req.addElement(MailService.E_CURSOR);
            if (cursor.getPreviousId() != null) cursorEl.addAttribute(MailService.A_ID, cursor.getPreviousId());
            if (cursor.getPreviousSortValue() != null) cursorEl.addAttribute(MailService.A_SORTVAL, cursor.getPreviousSortValue());
        }
        
        return new ZSearchResult(invoke(req));
    }
 
    /**
     * do a search 
     * @param params search prams
     * @return search result
     * @throws ServiceException on error
     */
    public ZSearchResult search(ZSearchParams params) throws ServiceException {
        return internalSearch(null, params);
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
        return internalSearch(convId, params);
    }
    
    /**
     * A request that does nothing and always returns nothing. Used to keep a session alive, and return
     * any pending notifications.
     *
     * @throws ServiceException on error
     */
    public void noOp() throws ServiceException {
        invoke(new XMLElement(MailService.NO_OP_REQUEST));
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
        XMLElement req = new XMLElement(MailService.CREATE_MOUNTPOINT_REQUEST);
        Element linkEl = req.addElement(MailService.E_MOUNT);
        linkEl.addAttribute(MailService.A_NAME, name);
        linkEl.addAttribute(MailService.A_FOLDER, parentId);
        if (defaultView != null) linkEl.addAttribute(MailService.A_DEFAULT_VIEW, defaultView.name());
        if (color != null) linkEl.addAttribute(MailService.A_COLOR, color.getValue());
        if (flags != null) linkEl.addAttribute(MailService.A_FLAGS, flags);
        linkEl.addAttribute(ownerBy == OwnerBy.BY_ID ? MailService.A_ZIMBRA_ID : MailService.A_OWNER_NAME, owner);
        linkEl.addAttribute(itemBy == SharedItemBy.BY_ID ? MailService.A_REMOTE_ID: MailService.A_PATH, sharedItem);
        String id = invoke(req).getElement(MailService.E_MOUNT).getAttribute(MailService.A_ID);
        return getMountpointById(id);
    }
    
    /**
     * Sends an iCalendar REPLY object
     * @param ical iCalendar data
     * @throws ServiceException on error
     */
    public void iCalReply(String ical) throws ServiceException {
        XMLElement req = new XMLElement(MailService.ICAL_REPLY_REQUEST);
        Element icalElem = req.addElement(MailService.E_APPT_ICAL);        
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

    public static class ZSendMessagePart {
        private String mMessageId;
        private String mPartName;

        public ZSendMessagePart(String messageId) {
            mMessageId = messageId;
        }

        public String getMessageId() {
            return mMessageId;
        }

        public void setMessageId(String messageId) {
            mMessageId = messageId;
        }

        public String getPartName() {
            return mPartName;
        }

        public void setPartName(String partName) {
            mPartName = partName;
        }
    }

    /**
     * send a message.
     * 
     * @param addrs list of addresses message is to be sent to
     * @param subject subject of message
     * @param origMessageIdHeader Message-ID header for message being replied to
     * @param contentType content type of message body (normally text/plain)
     * @param content content of message body
     * @param attachmentUploadId the id of attachments uploaded previously
     * @param messageIdsToAttach list of additional messages to attach to this message, or null.
     * @param messagePartsToAttach list of additional attachments (id/part) to attach to this message, or null.
     * @param contactIdsToAttach list of contact ids to attach to this message, or null.
     * @return SendMessageResponse. id is set in response only if message was saved to a sent folder.
     * @throws com.zimbra.cs.service.ServiceException on error
     */
    public ZSendMessageResponse sendMessage(List<ZEmailAddress> addrs, String subject, String origMessageIdHeader, 
            String contentType, String content, String attachmentUploadId, 
            List<String> messageIdsToAttach, List<ZSendMessagePart> messagePartsToAttach, List<String> contactIdsToAttach) throws ServiceException {
        XMLElement req = new XMLElement(MailService.SEND_MSG_REQUEST);
        Element m = req.addElement(MailService.E_MSG);
        for (ZEmailAddress addr : addrs) {
            Element e = m.addElement(MailService.E_EMAIL);
            e.addAttribute(MailService.A_TYPE, addr.getType());
            e.addAttribute(MailService.A_ADDRESS, addr.getAddress());
            e.addAttribute(MailService.A_PERSONAL, addr.getPersonal());
        }
        if (subject != null) m.addElement(MailService.E_SUBJECT).setText(subject);
        if (origMessageIdHeader != null) m.addElement(MailService.E_IN_REPLY_TO).setText(origMessageIdHeader);
        Element mp = m.addElement(MailService.E_MIMEPART);
        mp.addAttribute(MailService.A_CONTENT_TYPE, contentType);
        mp.addElement(MailService.E_CONTENT).setText(content);
        if (attachmentUploadId != null || messageIdsToAttach != null || messagePartsToAttach != null || contactIdsToAttach != null) {
            Element attach = m.addElement(MailService.E_ATTACH);
            if (attachmentUploadId != null)
                attach.addAttribute(MailService.A_ATTACHMENT_ID, attachmentUploadId);
            if (messageIdsToAttach != null) {
                for (String mid: messageIdsToAttach) {
                    attach.addElement(MailService.E_MSG).addAttribute(MailService.A_ID, mid);
                }
            }
            if (messagePartsToAttach != null) {
                for (ZSendMessagePart part: messagePartsToAttach) {
                    attach.addElement(MailService.E_MIMEPART).addAttribute(MailService.A_ID, part.getMessageId()).addAttribute(MailService.A_PART, part.getPartName());    
                }
            }
        }
        Element resp = invoke(req);
        Element msg = resp.getOptionalElement(MailService.E_MSG);
        String id = msg == null ? null : msg.getAttribute(MailService.A_ID, null);
        return new ZSendMessageResponse(id);
    }

    public void CreateIdentityRequest(ZIdentity identity) throws ServiceException {
        XMLElement req = new XMLElement(AccountService.CREATE_IDENTITY_REQUEST);
        identity.toElement(req);
        invoke(req);
    }

    public void DeleteIdentityRequest(String name) throws ServiceException {
        XMLElement req = new XMLElement(AccountService.DELETE_IDENTITY_REQUEST);
        req.addElement(AccountService.E_IDENTITY).addAttribute(AccountService.A_NAME, name);
        invoke(req);
    }

    public void ModifyIdentityRequest(ZIdentity identity) throws ServiceException {
        XMLElement req = new XMLElement(AccountService.MODIFY_IDENTITY_REQUEST);
        identity.toElement(req);
        invoke(req);
    }

    public String CreateDataSourceRequest(ZDataSource source) throws ServiceException {
        XMLElement req = new XMLElement(MailService.CREATE_DATA_SOURCE_REQUEST);
        source.toElement(req);
        return invoke(req).listElements().get(0).getAttribute(MailService.A_ID);
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
    public String TestPop3DataSource(String host, int port, String username, String password) throws ServiceException {
        XMLElement req = new XMLElement(MailService.TEST_DATA_SOURCE_REQUEST);
        Element pop3 = req.addElement(MailService.E_DS_POP3);
        pop3.addAttribute(MailService.A_DS_HOST, host);
        pop3.addAttribute(MailService.A_DS_PORT, port);
        pop3.addAttribute(MailService.A_DS_USERNAME, username);
        pop3.addAttribute(MailService.A_DS_PASSWORD, password);
        Element resp = invoke(req);
        boolean success = resp.getAttributeBool(MailService.A_DS_SUCCESS, false);
        if (!success) {
            return resp.getAttribute(MailService.A_DS_ERROR, "error");
        } else {
            return null;
        }
    }

    public List<ZDataSource> getAllDataSources() throws ServiceException {
        XMLElement req = new XMLElement(MailService.GET_DATA_SOURCES_REQUEST);
        Element response = invoke(req);
        List<ZDataSource> result = new ArrayList<ZDataSource>();
        for (Element ds : response.listElements()) {
            if (ds.getName().equals(MailService.E_DS_POP3)) {
                result.add(new ZPop3DataSource(ds));
            }
        }
        return result;
    }

    public void ModifyDataSource(ZDataSource source) throws ServiceException {
        XMLElement req = new XMLElement(MailService.MODIFY_DATA_SOURCE_REQUEST);
        source.toElement(req);
        invoke(req);
    }

    public void DeleteDataSource(ZDataSource source) throws ServiceException {
        XMLElement req = new XMLElement(MailService.DELETE_DATA_SOURCE_REQUEST);
        source.toIdElement(req);
        invoke(req);
    }

    public void ImportData(List<ZDataSource> sources) throws ServiceException {
        XMLElement req = new XMLElement(MailService.IMPORT_DATA_REQUEST);
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
            mIsRunning = e.getAttributeBool(MailService.A_DS_IS_RUNNING, false);
            mSuccess = e.getAttributeBool(MailService.A_DS_SUCCESS, true);
            mError = e.getAttribute(MailService.A_DS_ERROR, null);
        }

        public String getType() { return mType; }
        public boolean isRunning() { return mIsRunning; }
        public boolean getSuccess() { return mSuccess; }
        public String getError() { return mError; }
    }

    public List<ZImportStatus> getImportStatus() throws ServiceException {
        XMLElement req = new XMLElement(MailService.GET_IMPORT_STATUS_REQUEST);
        Element response = invoke(req);
        List<ZImportStatus> result = new ArrayList<ZImportStatus>();
        for (Element status : response.listElements()) {
            result.add(new ZImportStatus(status));
        }
        return result;
    }

}

