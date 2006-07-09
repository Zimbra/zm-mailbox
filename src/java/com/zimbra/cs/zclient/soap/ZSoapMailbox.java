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

package com.zimbra.cs.zclient.soap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.zclient.ZConversation;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMountpoint;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMessage;
import com.zimbra.cs.zclient.ZSearchFolder;
import com.zimbra.cs.zclient.ZSearchHit;
import com.zimbra.cs.zclient.ZSearchParams;
import com.zimbra.cs.zclient.ZSearchResult;
import com.zimbra.cs.zclient.ZTag;
import com.zimbra.cs.zclient.ZTag.Color;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.SoapHttpTransport;
import com.zimbra.soap.SoapTransport;
import com.zimbra.soap.ZimbraNamespace;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.Element.XMLElement;

public class ZSoapMailbox extends ZMailbox {

    private String mAuthToken;
    private long mAuthTokenLifetime;
    private long mAuthTokenExpiration;
    private SoapHttpTransport mTransport;

    private Map<String, ZSoapTag> mNameToTag;
    private Map<String, ZSoapItem> mIdToItem;

    private ZSoapFolder mUserRoot;

    private long mSize;

    ZSoapMailbox() {
        mNameToTag = new HashMap<String, ZSoapTag>();
        mIdToItem = new HashMap<String, ZSoapItem>();        
    }

    /**
     * @param uri URI of server we want to talk to
     */
    void setSoapURI(String uri) {
        if (mTransport != null) mTransport.shutdown();
        mTransport = new SoapHttpTransport(uri);
        mTransport.setMaxNoitfySeq(0);
        if (mAuthToken != null)
            mTransport.setAuthToken(mAuthToken);
    }    

    void authRequest(String key, AccountBy by, String password) throws ServiceException {
        if (mTransport == null) throw SoapFaultException.CLIENT_ERROR("must call setURI before calling adminAuthenticate", null);
        XMLElement req = new XMLElement(AccountService.AUTH_REQUEST);
        Element account = req.addElement(AccountService.E_ACCOUNT);
        account.addAttribute(AccountService.A_BY, by.name());
        account.setText(key);
        req.addElement(AccountService.E_PASSWORD).setText(password);
        Element response = invoke(req);
        mAuthToken = response.getElement(AccountService.E_AUTH_TOKEN).getText();
        mAuthTokenLifetime = response.getAttributeLong(AccountService.E_LIFETIME);
        mAuthTokenExpiration = System.currentTimeMillis() + mAuthTokenLifetime;
        mTransport.setAuthToken(mAuthToken);
        // TODO: handle <refer>
    }

    synchronized Element invoke(Element request) throws ServiceException {
        try {
            return mTransport.invoke(request);
        } catch (SoapFaultException e) {
            throw e; // for now, later, try to map to more specific exception
        } catch (IOException e) {
            throw SoapFaultException.IO_ERROR("invoke "+e.getMessage(), e);
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
                ZSoapTag tag = (ZSoapTag) getTagById(e.getAttribute(MailService.A_ID));
                if (tag != null) {
                    String oldName = tag.getName();
                    tag.modifyNotification(e);
                    if (!tag.getName().equalsIgnoreCase(oldName)) {
                        mNameToTag.remove(oldName);
                        mNameToTag.put(tag.getName(), tag);
                    }
                }
            } else if (e.getName().equals(MailService.E_SEARCH) || e.getName().equals(MailService.E_FOLDER)) {
                ZSoapFolder f = (ZSoapFolder) getFolderById(e.getAttribute(MailService.A_ID));
                if (f != null)
                    f.modifyNotification(e);
            }
        }
    }

    private void handleCreated(Element created) throws ServiceException {
        if (created == null) return;
        for (Element e : created.listElements()) {
            if (e.getName().equals(MailService.E_FOLDER)) {
                String parentId = e.getAttribute(MailService.A_FOLDER);
                ZSoapFolder parent = (ZSoapFolder) getFolderById(parentId);
                new ZSoapFolder(e, parent, this);
            } else if (e.getName().equals(MailService.E_MOUNT)) {
                String parentId = e.getAttribute(MailService.A_FOLDER);
                ZSoapFolder parent = (ZSoapFolder) getFolderById(parentId);
                new ZSoapMountpoint(e, parent, this);
            } else if (e.getName().equals(MailService.E_SEARCH)) {
                String parentId = e.getAttribute(MailService.A_FOLDER);
                ZSoapFolder parent = (ZSoapFolder) getFolderById(parentId);
                new ZSoapSearchFolder(e, parent, this);
            } else if (e.getName().equals(MailService.E_TAG)) {
                addTag(new ZSoapTag(e));
            }
        }
    }

    private void handleDeleted(Element deleted) {
        if (deleted == null) return;
        String ids = deleted.getAttribute(MailService.A_ID, null);
        if (ids == null) return;
        for (String id : ids.split(",")) {
            ZSoapItem item = mIdToItem.get(id);
            if (item instanceof ZSoapFolder) {
                ZSoapFolder sf = (ZSoapFolder) item;
                if (sf.getParent() != null) 
                    ((ZSoapFolder)sf.getParent()).removeChild(sf);
            } else if (item instanceof ZSoapMountpoint) {
                ZSoapMountpoint sl = (ZSoapMountpoint) item;
                if (sl.getParent() != null) 
                    ((ZSoapFolder)sl.getParent()).removeChild(sl);
            } else if (item instanceof ZSoapTag) {
                mNameToTag.remove(((ZSoapTag) item).getName());
            }
            if (item != null) mIdToItem.remove(item.getId());
        }
    }

    private void addTag(ZSoapTag tag) {
        mNameToTag.put(tag.getName(), tag);
        addItemIdMapping(tag);
    }

    /**
     * handle a &lt;refresh&gt; block
     * @param refresh
     * @throws ServiceException
     */
    private void refreshHandler(Element refresh) throws ServiceException {
        mNameToTag.clear();
        mIdToItem.clear();
        mTransport.setMaxNoitfySeq(0);
        Element mbx = refresh.getElement(MailService.E_MAILBOX);
        if (mbx != null) mSize = mbx.getAttributeLong(MailService.A_SIZE);
        Element tags = refresh.getElement(ZimbraNamespace.E_TAGS);
        if (tags != null) {
            for (Element t : tags.listElements(MailService.E_TAG))
                addTag(new ZSoapTag(t));
        }
        Element folder = refresh.getElement(MailService.E_FOLDER);
        refreshFolders(folder);
    }

    void addItemIdMapping(ZSoapItem item) {   mIdToItem.put(item.getId(), item); }

    private void refreshFolders(Element folderEl) throws ServiceException {
        mUserRoot = new ZSoapFolder(folderEl, null, this);
    }

    public static ZMailbox getMailbox(String accountName, String password, String uri) throws ServiceException {
        return getMailbox(accountName, password, uri, null);
    }

    public static ZMailbox getMailbox(String accountName, String password, String uri, SoapTransport.DebugListener listener) throws ServiceException {
        ZSoapMailbox zmbx = new ZSoapMailbox();
        zmbx.setSoapURI(uri);
        if (listener != null) zmbx.mTransport.setDebugListener(listener);
        zmbx.authRequest(accountName, AccountBy.name, password);
        return zmbx;
    }
    
    @Override
    public ZFolder getUserRoot() {
        return mUserRoot;
    }
    
    @Override
    public long getSize() {
        return mSize;
    }

    @Override
    public List<ZTag> getAllTags() {
        return new ArrayList<ZTag>(mNameToTag.values());
    }

    @Override
    public List<String> getAllTagIds() {
        ArrayList<String> ids = new ArrayList<String>(mNameToTag.size());
        for (ZTag tag: mNameToTag.values()) {
            ids.add(tag.getId());
        }
        return ids;
    }

    @Override
    public List<String> getAllTagNames() {
        ArrayList<String> names = new ArrayList<String>(mNameToTag.keySet());
        Collections.sort(names);
        return names;
    }

    @Override
    public ZTag getTagById(String id) {
        ZSoapItem item = mIdToItem.get(id);
        if (item instanceof ZTag) return (ZTag) item;
        else return null;
    }

    @Override
    public ZTag getTagByName(String name) {
        return mNameToTag.get(name);
    }

    @Override
    public ZSearchResult search(ZSearchParams params) throws ServiceException {
        return internalSearch(null, params);
    }

    @Override
    public ZSearchResult searchConversation(String convId, ZSearchParams params) throws ServiceException {
        if (convId == null) throw SoapFaultException.CLIENT_ERROR("conversation id must not be null", null);
        return internalSearch(convId, params);
    }
    
    private ZSearchResult internalSearch(String convId, ZSearchParams params) throws ServiceException {
        XMLElement req = new XMLElement(convId == null ? MailService.SEARCH_REQUEST : MailService.SEARCH_CONV_REQUEST);

        req.addAttribute(MailService.A_CONV_ID, convId);
        if (params.getLimit() != 0) req.addAttribute(MailService.A_QUERY_LIMIT, params.getLimit());
        if (params.getOffset() != 0) req.addAttribute(MailService.A_QUERY_OFFSET, params.getOffset());
        if (params.getSortBy() != null) req.addAttribute(MailService.A_SORTBY, params.getSortBy().name());
        if (params.getTypes() != null) req.addAttribute(MailService.A_SEARCH_TYPES, params.getTypes());
        if (params.isFetchFirstMessage()) req.addAttribute(MailService.A_FETCH, params.isFetchFirstMessage());
        if (params.isPreferHtml()) req.addAttribute(MailService.A_WANT_HTML, params.isPreferHtml());
        if (params.isMarkAsRead()) req.addAttribute(MailService.A_MARK_READ, params.isMarkAsRead());
        if (params.isRecipientMode()) req.addAttribute(MailService.A_RECIPIENTS, params.isRecipientMode());
        
        req.addElement(MailService.E_QUERY).setText(params.getQuery());
        
        if (params.getCursorPreviousId() != null || params.getCursorPreviousSortValue() != null) {
            Element cursor = req.addElement(MailService.E_CURSOR);
            if (params.getCursorPreviousId() != null) cursor.addAttribute(MailService.A_ID, params.getCursorPreviousId());
            if (params.getCursorPreviousSortValue() != null) cursor.addAttribute(MailService.A_SORTVAL, params.getCursorPreviousSortValue());
        }
        
        return new ZSoapSearchResult(invoke(req));
    }

    @Override
    public void noOp() throws ServiceException {
        invoke(new XMLElement(MailService.NO_OP_REQUEST));
    }

    @Override
    public ZFolder createFolder(String parentId, String name, ZFolder.View defaultView) throws ServiceException {
        XMLElement req = new XMLElement(MailService.CREATE_FOLDER_REQUEST);
        Element folderEl = req.addElement(MailService.E_FOLDER);
        folderEl.addAttribute(MailService.A_NAME, name);
        folderEl.addAttribute(MailService.A_FOLDER, parentId);
        if (defaultView != null) folderEl.addAttribute(MailService.A_DEFAULT_VIEW, defaultView.name());
        String id = invoke(req).getElement(MailService.E_FOLDER).getAttribute(MailService.A_ID);
        // this assumes notifications will create the folder
        return getFolderById(id);
    }

    @Override
    public ZSearchFolder createSearchFolder(String parentId, String name, String query, String types, SortBy sortBy) throws ServiceException {
        XMLElement req = new XMLElement(MailService.CREATE_SEARCH_FOLDER_REQUEST);
        Element folderEl = req.addElement(MailService.E_SEARCH);
        folderEl.addAttribute(MailService.A_NAME, name);
        folderEl.addAttribute(MailService.A_FOLDER, parentId);
        folderEl.addAttribute(MailService.A_QUERY, query);
        if (types != null) folderEl.addAttribute(MailService.A_SEARCH_TYPES, types);
        if (sortBy != null) folderEl.addAttribute(MailService.A_SORTBY, sortBy.name());
        String id = invoke(req).getElement(MailService.E_SEARCH).getAttribute(MailService.A_ID);
        // this assumes notifications will create the folder
        return getSearchFolderById(id);
    }

    @Override
    public ZSearchFolder modifySearchFolder(String id, String query, String types, SortBy sortBy) throws ServiceException {
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
    
    @Override
    public ZTag createTag(String name, Color color) throws ServiceException {
        XMLElement req = new XMLElement(MailService.CREATE_TAG_REQUEST);
        Element tagEl = req.addElement(MailService.E_TAG);
        tagEl.addAttribute(MailService.A_NAME, name);
        tagEl.addAttribute(MailService.A_COLOR, color.getValue());
        String id = invoke(req).getElement(MailService.E_TAG).getAttribute(MailService.A_ID);
        // this assumes notifications will create the tag
        return getTagById(id);
    }

    @Override
    public ZFolder getFolderById(String id) {
        ZSoapItem item = mIdToItem.get(id);
        if (item instanceof ZFolder) return (ZFolder) item;
        else return null;
    }

    @Override
    public ZMountpoint getMountpointById(String id) {
        ZSoapItem item = mIdToItem.get(id);
        if (item instanceof ZMountpoint) return (ZMountpoint) item;
        else return null;
    }

    @Override
    public ZSearchFolder getSearchFolderById(String id) {
        ZSoapItem item = mIdToItem.get(id);
        if (item instanceof ZSearchFolder) return (ZSearchFolder) item;
        else return null;
    }

    @Override
    public ZFolder getFolderByPath(String path) throws ServiceException {
        if (!path.startsWith(ZMailbox.PATH_SEPARATOR)) 
            throw SoapFaultException.CLIENT_ERROR("path must start with "+ZMailbox.PATH_SEPARATOR, null);
        return getUserRoot().getSubFolderByPath(path.substring(1));
    }

    private Element folderAction(String op, String ids) throws ServiceException {
        XMLElement req = new XMLElement(MailService.FOLDER_ACTION_REQUEST);
        Element actionEl = req.addElement(MailService.E_ACTION);
        actionEl.addAttribute(MailService.A_ID, ids);
        actionEl.addAttribute(MailService.A_OPERATION, op);
        return actionEl;
    }
    
    private ZActionResult doAction(Element actionEl) throws ServiceException {
        Element response = invoke(actionEl.getParent());
        return new ZActionResult(response.getElement(MailService.E_ACTION).getAttribute(MailService.A_ID));
    }

    @Override
    public ZActionResult deleteFolder(String ids) throws ServiceException {
        return doAction(folderAction("delete", ids));
    }

    @Override
    public ZActionResult emptyFolder(String ids) throws ServiceException {
        return doAction(folderAction("empty", ids));        
    }

    @Override
    public ZActionResult importURLIntoFolder(String id, String url) throws ServiceException {
        return doAction(folderAction("import", id).addAttribute(MailService.A_URL, url));
    }

    @Override
    public ZActionResult markFolderRead(String ids) throws ServiceException {
        return doAction(folderAction("read", ids));                
    }

    @Override
    public ZActionResult moveFolder(String id, String targetFolderId) throws ServiceException {
        return doAction(folderAction("move", id).addAttribute(MailService.A_FOLDER, targetFolderId));
    }

    @Override
    public ZActionResult renameFolder(String id, String name) throws ServiceException {
        return doAction(folderAction("rename", id).addAttribute(MailService.A_NAME, name));
    }

    @Override
    public ZActionResult modifyFolderChecked(String ids, boolean checked) throws ServiceException {
        return doAction(folderAction(checked ? "check" : "!check", ids));
    }

    @Override
    public ZActionResult modifyFolderColor(String ids, ZFolder.Color color) throws ServiceException {
        return doAction(folderAction("color", ids).addAttribute(MailService.A_COLOR, color.getValue()));
    }

    @Override
    public ZActionResult modifyFolderExcludeFreeBusy(String ids, boolean state) throws ServiceException {
        return doAction(folderAction("fb", ids).addAttribute(MailService.A_EXCLUDE_FREEBUSY, state));
    }

    @Override
    public ZActionResult modifyFolderURL(String id, String url) throws ServiceException {
        return doAction(folderAction("url", id).addAttribute(MailService.A_URL, url));
    }

    @Override
    public ZActionResult syncFolder(String ids) throws ServiceException {
        return doAction(folderAction("sync", ids));
    }

    private Element tagAction(String op, String id) throws ServiceException {
        XMLElement req = new XMLElement(MailService.TAG_ACTION_REQUEST);
        Element actionEl = req.addElement(MailService.E_ACTION);
        actionEl.addAttribute(MailService.A_ID, id);
        actionEl.addAttribute(MailService.A_OPERATION, op);
        return actionEl;
    }

    @Override
    public ZActionResult deleteTag(String id) throws ServiceException {
        return doAction(tagAction("delete", id));
    }

    @Override
    public ZActionResult markTagRead(String id) throws ServiceException {
        return doAction(tagAction("read", id));
    }

    @Override
    public ZActionResult renameTag(String id, String name) throws ServiceException {
        return doAction(tagAction("rename", id).addAttribute(MailService.A_NAME, name));
    }

    @Override
    public ZActionResult modifyTagColor(String id, Color color) throws ServiceException {
        return doAction(tagAction("color", id).addAttribute(MailService.A_COLOR, color.getValue()));        
    }

    private Element convAction(String op, String id, String constraints) throws ServiceException {
        XMLElement req = new XMLElement(MailService.CONV_ACTION_REQUEST);
        Element actionEl = req.addElement(MailService.E_ACTION);
        actionEl.addAttribute(MailService.A_ID, id);
        actionEl.addAttribute(MailService.A_OPERATION, op);
        if (constraints != null) actionEl.addAttribute(MailService.A_TARGET_CONSTRAINT, constraints);
        return actionEl;
    }

    @Override
    public ZActionResult deleteConversation(String ids, String targetConstraints) throws ServiceException {
        return doAction(convAction("delete", ids, targetConstraints));
    }

    @Override
    public ZActionResult flagConversation(String ids, boolean flag, String targetConstraints) throws ServiceException {
        return doAction(convAction(flag ? "flag" : "!flag", ids, targetConstraints));
    }

    @Override
    public ZActionResult markConversationRead(String ids, boolean read, String targetConstraints) throws ServiceException {
        return doAction(convAction(read ? "read" : "!read", ids, targetConstraints));
    }

    @Override
    public ZActionResult markConversationSpam(String id, boolean spam, String destFolderId, String targetConstraints) throws ServiceException {
        Element actionEl = convAction(spam ? "spam" : "!spam", id, targetConstraints);
        if (destFolderId != null && destFolderId.length() > 0) actionEl.addAttribute(MailService.A_FOLDER, destFolderId);
        return doAction(actionEl);
    }

    @Override
    public ZActionResult moveConversation(String ids, String destFolderId, String targetConstraints) throws ServiceException {
        return doAction(convAction("move", ids, targetConstraints).addAttribute(MailService.A_FOLDER, destFolderId));
    }

    @Override
    public ZActionResult tagConversation(String ids, String tagId, boolean tag, String targetConstraints) throws ServiceException {
        return doAction(convAction(tag ? "tag" : "!tag", ids, targetConstraints).addAttribute(MailService.A_TAG, tagId));        
    }

    private Element messageAction(String op, String id) throws ServiceException {
        XMLElement req = new XMLElement(MailService.MSG_ACTION_REQUEST);
        Element actionEl = req.addElement(MailService.E_ACTION);
        actionEl.addAttribute(MailService.A_ID, id);
        actionEl.addAttribute(MailService.A_OPERATION, op);
        return actionEl;
    }

    @Override
    public ZActionResult deleteMessage(String ids) throws ServiceException {
        return doAction(messageAction("delete", ids));
    }

    @Override
    public ZActionResult flagMessage(String ids, boolean flag) throws ServiceException {
        return doAction(messageAction(flag ? "flag" : "!flag", ids));
    }

    @Override
    public ZActionResult markMessageRead(String ids, boolean read) throws ServiceException {
        return doAction(messageAction(read ? "read" : "!read", ids));
    }

    @Override
    public ZActionResult markMessageSpam(String id, boolean spam, String destFolderId) throws ServiceException {
        Element actionEl = messageAction(spam ? "spam" : "!spam", id);
        if (destFolderId != null && destFolderId.length() > 0) actionEl.addAttribute(MailService.A_FOLDER, destFolderId);
        return doAction(actionEl);
    }

    @Override
    public ZActionResult moveMessage(String ids, String destFolderId) throws ServiceException {
        return doAction(messageAction("move", ids).addAttribute(MailService.A_FOLDER, destFolderId));
    }

    @Override
    public ZActionResult tagMessage(String ids, String tagId, boolean tag) throws ServiceException {
        return doAction(messageAction(tag ? "tag" : "!tag", ids).addAttribute(MailService.A_TAG, tagId));
    }

    @Override
    public ZActionResult updateMessage(String ids, String destFolderId, String tagList, String flags) throws ServiceException {
        Element actionEl = messageAction("update", ids);
        if (destFolderId != null && destFolderId.length() > 0) actionEl.addAttribute(MailService.A_FOLDER, destFolderId);
        if (tagList != null) actionEl.addAttribute(MailService.A_TAGS, tagList);
        if (flags != null) actionEl.addAttribute(MailService.A_FLAGS, flags);
        return doAction(actionEl);        
    }

    private Element itemAction(String op, String id, String constraints) throws ServiceException {
        XMLElement req = new XMLElement(MailService.ITEM_ACTION_REQUEST);
        Element actionEl = req.addElement(MailService.E_ACTION);
        actionEl.addAttribute(MailService.A_ID, id);
        actionEl.addAttribute(MailService.A_OPERATION, op);
        if (constraints != null) actionEl.addAttribute(MailService.A_TARGET_CONSTRAINT, constraints);
        return actionEl;
    }

    @Override
    public ZActionResult deleteItem(String ids, String targetConstraints) throws ServiceException {
        return doAction(itemAction("delete", ids, targetConstraints));        
    }

    @Override
    public ZActionResult flagItem(String ids, boolean flag, String targetConstraints) throws ServiceException {
        return doAction(itemAction(flag ? "flag" : "!flag", ids, targetConstraints));
    }

    @Override
    public ZActionResult markItemRead(String ids, boolean read, String targetConstraints) throws ServiceException {
        return doAction(itemAction(read ? "read" : "!read", ids, targetConstraints));
    }

    @Override
    public ZActionResult moveItem(String ids, String destFolderId, String targetConstraints) throws ServiceException {
        return doAction(itemAction("move", ids, targetConstraints).addAttribute(MailService.A_FOLDER, destFolderId));
    }

    @Override
    public ZActionResult tagItem(String ids, String tagId, boolean tag, String targetConstraints) throws ServiceException {
        return doAction(itemAction(tag ? "tag" : "!tag", ids, targetConstraints).addAttribute(MailService.A_TAG, tagId));
    }

    @Override
    public ZActionResult updateItem(String ids, String destFolderId, String tagList, String flags, String targetConstraints) throws ServiceException {
        Element actionEl = itemAction("update", ids, targetConstraints);
        if (destFolderId != null && destFolderId.length() > 0) actionEl.addAttribute(MailService.A_FOLDER, destFolderId);
        if (tagList != null) actionEl.addAttribute(MailService.A_TAGS, tagList);
        if (flags != null) actionEl.addAttribute(MailService.A_FLAGS, flags);
        return doAction(actionEl);
    }

    @Override
    public ZMountpoint createMountpoint(String parentId, String name, ZFolder.View defaultView, OwnerBy ownerBy, String owner, SharedItemBy itemBy, String sharedItem) throws ServiceException {
        XMLElement req = new XMLElement(MailService.CREATE_MOUNTPOINT_REQUEST);
        Element linkEl = req.addElement(MailService.E_MOUNT);
        linkEl.addAttribute(MailService.A_NAME, name);
        linkEl.addAttribute(MailService.A_FOLDER, parentId);
        if (defaultView != null) linkEl.addAttribute(MailService.A_DEFAULT_VIEW, defaultView.name());
        linkEl.addAttribute(ownerBy == OwnerBy.BY_ID ? MailService.A_ZIMBRA_ID : MailService.A_DISPLAY, owner);
        linkEl.addAttribute(itemBy == SharedItemBy.BY_ID ? MailService.A_REMOTE_ID: MailService.A_PATH, sharedItem);
        String id = invoke(req).getElement(MailService.E_MOUNT).getAttribute(MailService.A_ID);
        return getMountpointById(id);
    }

    @Override
    public ZConversation getConversation(String id) throws ServiceException {
        XMLElement req = new XMLElement(MailService.GET_CONV_REQUEST);
        Element convEl = req.addElement(MailService.E_CONV);
        convEl.addAttribute(MailService.A_ID, id);
        Map<String,ZSoapEmailAddress> cache = new HashMap<String, ZSoapEmailAddress>();
        return new ZSoapConversation(invoke(req).getElement(MailService.E_CONV), cache);
    }

    public static void main(String args[]) throws ServiceException {
        Zimbra.toolSetup();
        ZMailbox mbox = getMailbox("user1", "test123", "http://localhost:7070/service/soap");
        System.out.println(mbox.getSize());
        System.out.println(mbox.getAllTags());
        System.out.println(mbox.getUserRoot());
        ZSearchParams sp = new ZSearchParams("my pictures");
        //ZSearchParams sp = new ZSearchParams("in:inbox");        
        sp.setLimit(20);
        sp.setTypes(ZSearchParams.TYPE_CONVERSATION);
        System.out.println(mbox.search(sp));
        System.out.println(mbox.searchConversation("346", sp));
        
        ZFolder inbox = mbox.getFolderByPath("/inBOX");
        System.out.println(inbox);
        System.out.println(mbox.getFolderById(inbox.getId()));
        mbox.markFolderRead(inbox.getId());
        mbox.modifyFolderChecked(inbox.getId(), true);
        mbox.modifyFolderChecked(inbox.getId(), false);        
        mbox.modifyFolderColor(inbox.getId(), ZFolder.Color.blue);
        ZFolder dork = mbox.createFolder(inbox.getId(), "dork", ZFolder.View.message);
        System.out.println("---------- created dork -----------");                
        System.out.println(dork);
        System.out.println(inbox);
        mbox.deleteFolder(dork.getId());
        System.out.println("---------- deleted dork -----------");
        System.out.println(inbox);
        ZTag zippy = mbox.createTag("zippy", Color.purple);
        System.out.println(zippy);
        System.out.println(mbox.modifyTagColor(zippy.getId(), Color.orange));
        System.out.println(mbox.markTagRead(zippy.getId()));
        System.out.println(mbox.renameTag(zippy.getId(), "zippy2"));
        System.out.println(mbox.getAllTags());
        System.out.println(mbox.deleteTag(zippy.getId()));
        System.out.println(mbox.getAllTags());        
        ZSearchFolder flagged = mbox.createSearchFolder(mbox.getUserRoot().getId(), "is-it-flagged", "is:flagged", null, null);
        System.out.println(flagged);
        System.out.println(mbox.renameFolder(flagged.getId(), "flagged-and-unread"));        
        System.out.println(mbox.modifyFolderColor(flagged.getId(), ZFolder.Color.red));
        System.out.println(mbox.modifySearchFolder(flagged.getId(), "is:flagged is:unread", null, SortBy.dateAsc));
        
        mbox.deleteFolder(flagged.getId());
        mbox.noOp();        
        System.out.println(mbox.flagConversation("346", false, null));
        System.out.println(mbox.getConversation("346"));
        System.out.println(mbox.getMessage("375", false, false, false, null, null));
        //ZLink user2 = mbox.createMountpoint(mbox.getUserRoot(), "user2", ZFolder.VIEW_APPOINTMENT, OwnerBy.BY_NAME, "user2", SharedItemBy.BY_PATH, "/Calendar");
        //System.out.println(user2);        
        //mbox.deleteItem(user2.getId(), null);

    }

    @Override
    public ZMessage getMessage(String id, boolean markRead, boolean defangedHtml, boolean rawContent, String part, String subId) throws ServiceException {
        XMLElement req = new XMLElement(MailService.GET_MSG_REQUEST);
        Element msgEl = req.addElement(MailService.E_MSG);
        msgEl.addAttribute(MailService.A_ID, id);
        Map<String,ZSoapEmailAddress> cache = new HashMap<String, ZSoapEmailAddress>();
        return new ZSoapMessage(invoke(req).getElement(MailService.E_MSG), cache);
    }

    /*
 
 <AuthRequest xmlns="urn:zimbraAccount">
 <ChangePasswordRequest>
 <GetPrefsRequest>
 <GetInfoRequest>
 <GetAccountInfoRequest>
 <GetAvailableSkinsRequest/>
 <SearchGalRequest [type="{type}"]>
 <AutoCompleteGalRequest [type="{type}"] limit="limit-returned">
 <SyncGalRequest token="[{previous-token}]"/>
 <SearchCalendarResourcesRequest
 <ModifyPrefsRequest>
 
 *<SearchRequest>  - TODO: partially done
 *<SearchConvRequest>
 
 <BrowseRequest browseBy="{browse-by}"/>
 <GetFolderRequest>
 *<GetConvRequest>
 *<GetMsgRequest>

 *<CreateFolderRequest>

 *<ItemActionRequest>
 *<MsgActionRequest>
 *<ConvActionRequest>

 *<FolderActionRequest>   - TODO: grant

 *<GetTagRequest/> (NOT NEEDED?)
 *<CreateTagRequest>
 *<TagActionRequest>

 *<GetSearchFolderRequest> (NOT NEEDED?)
 *<CreateSearchFolderRequest>
 *<ModifySearchFolderRequest>
 *<CreateMountpointRequest>

 <SendMsgRequest>
 <SaveDraftRequest>
 <AddMsgRequest>

 <CreateContactRequest>
 <ModifyContactRequest replace="{replace-mode}">
 <GetContactsRequest [sortBy="nameAsc|nameDesc"] [sync="1"] [l="{folder-id}"]>
 <ContactActionRequest>
 <ImportContactsRequest ct="{content-type}" [l="{folder-id}"]>
 <ExportContactsRequest ct="{content-type}" [l="{folder-id}"]/>

 *<NoOpRequest/>

 <GetRulesRequest>
 <GetRulesResponse>
 <SaveRulesRequest>
 <SaveRulesResponse>

 <CheckSpellingRequest>
 <GetAllLocalesRequest/>
   
     */
}
