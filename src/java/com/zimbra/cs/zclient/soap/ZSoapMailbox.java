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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZSearchHit;
import com.zimbra.cs.zclient.ZSearchParams;
import com.zimbra.cs.zclient.ZSearchResult;
import com.zimbra.cs.zclient.ZTag;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.SoapHttpTransport;
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
    
    static class AuthResult {
        String mToken;
        String mSessionId;
        String mRefer;
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
            // TODO: save max seq number!
            mTransport.setMaxNoitfySeq(
                    Math.max(mTransport.getMaxNotifySeq(),
                             notify.getAttributeLong(ZimbraSoapContext.A_SEQNO, 0)));
            // MUST DO IN THIS ORDER
            handleDeleted(notify.getOptionalElement(ZimbraNamespace.E_DELETED));
            handleCreated(notify.getOptionalElement(ZimbraNamespace.E_CREATED));
            handleModified(notify.getOptionalElement(ZimbraNamespace.E_MODIFIED));
        }
    }

    private void handleModified(Element modified) {
        if (modified == null) return;
        // TODO Auto-generated method stub
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
                new ZSoapLink(e, parent, this);
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
            } else if (item instanceof ZSoapLink) {
                ZSoapLink sl = (ZSoapLink) item;
                if (sl.getParent() != null) 
                    ((ZSoapFolder)sl.getParent()).removeChild(sl);
            } else if (item instanceof ZSoapSearchFolder) {
                ZSoapSearchFolder sf = (ZSoapSearchFolder) item;
                if (sf.getParent() != null) 
                    ((ZSoapFolder)sf.getParent()).removeChild(sf);
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
        ZSoapMailbox zmbx = new ZSoapMailbox();
        zmbx.setSoapURI(uri);
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
    public Collection<ZSoapTag> getAllTags() {
        return mNameToTag.values();
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
        XMLElement req = new XMLElement(MailService.SEARCH_REQUEST);

        if (params.getLimit() != 0) req.addAttribute(MailService.A_QUERY_LIMIT, params.getLimit());
        if (params.getOffset() != 0) req.addAttribute(MailService.A_QUERY_OFFSET, params.getOffset());
        if (params.getSortBy() != null) req.addAttribute(MailService.A_SORTBY, params.getSortBy());
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
        
        Element response = invoke(req);
        
        String sortBy = response.getAttribute(MailService.A_SORTBY);
        boolean hasMore = response.getAttributeBool(MailService.A_QUERY_MORE);
        int offset = (int) response.getAttributeLong(MailService.A_QUERY_OFFSET);
        List<ZSearchHit> hits = new ArrayList<ZSearchHit>();
        Map<String,ZSoapEmailAddress> cache = new HashMap<String, ZSoapEmailAddress>();
        for (Element e: response.listElements()) {
            if (e.getName().equals(MailService.E_CONV)) {
                hits.add(new ZSoapConversationHit(e, cache));
            } else if (e.getName().equals(MailService.E_MSG)) {
                hits.add(new ZSoapMessageHit(e, cache));
            }
        }
        return new ZSearchResult(hits, hasMore, sortBy, offset);
    }

    @Override
    public ZFolder createFolder(ZFolder parent, String name, String defaultView) throws ServiceException {
        XMLElement req = new XMLElement(MailService.CREATE_FOLDER_REQUEST);
        Element folderEl = req.addElement(MailService.E_FOLDER);
        folderEl.addAttribute(MailService.A_NAME, name);
        folderEl.addAttribute(MailService.A_FOLDER, parent.getId());
        if (defaultView != null) folderEl.addAttribute(MailService.A_DEFAULT_VIEW, defaultView);
        String id = invoke(req).getElement(MailService.E_FOLDER).getAttribute(MailService.A_ID);
        // this assumes notifications will create the folder
        return getFolderById(id);
    }

    @Override
    public ZTag createTag(String name, int color) throws ServiceException {
        XMLElement req = new XMLElement(MailService.CREATE_TAG_REQUEST);
        Element tagEl = req.addElement(MailService.E_TAG);
        tagEl.addAttribute(MailService.A_NAME, name);
        tagEl.addAttribute(MailService.A_COLOR, color);
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
    public ZFolder getFolderByPath(String path) throws ServiceException {
        if (!path.startsWith(ZMailbox.PATH_SEPARATOR)) 
            throw SoapFaultException.CLIENT_ERROR("path must start with "+ZMailbox.PATH_SEPARATOR, null);
        return getUserRoot().getSubFolderByPath(path.substring(1));
    }

    public static void main(String args[]) throws ServiceException {
        Zimbra.toolSetup();
        ZMailbox mbox = getMailbox("user1", "test123", "http://localhost:7070/service/soap");
        System.out.println(mbox.getSize());
        System.out.println(mbox.getAllTags());
        System.out.println(mbox.getUserRoot());
        //ZSearchParams sp = new ZSearchParams("StringBuffer");
        ZSearchParams sp = new ZSearchParams("in:inbox");        
        sp.setLimit(20);
        sp.setTypes(ZSearchParams.TYPE_MESSAGE);
        System.out.println(mbox.search(sp));
        ZFolder inbox = mbox.getFolderByPath("/inBOX");
        System.out.println(inbox);
        System.out.println(mbox.getFolderById(inbox.getId()));
        mbox.markFolderAsRead(inbox.getId());
        mbox.setFolderChecked(inbox.getId(), true);
        mbox.setFolderChecked(inbox.getId(), false);        
        mbox.setFolderColor(inbox.getId(), 1);
        ZFolder dork = mbox.createFolder(inbox, "dork",ZFolder.VIEW_MESSAGE);
        System.out.println("---------- created dork -----------");                
        System.out.println(dork);
        System.out.println(inbox);
        mbox.deleteFolder(dork.getId());
        System.out.println("---------- deleted dork -----------");
        System.out.println(inbox);
        System.out.println(mbox.createTag("zippy", 6));
        System.out.println(mbox.getAllTags());
    }

    Element initFolderAction(String op, String ids) throws ServiceException {
        XMLElement req = new XMLElement(MailService.FOLDER_ACTION_REQUEST);
        Element actionEl = req.addElement(MailService.E_ACTION);
        actionEl.addAttribute(MailService.A_ID, ids);
        actionEl.addAttribute(MailService.A_OPERATION, op);
        return actionEl;
    }

    ZFolderActionResult doFolderAction(Element actionEl) throws ServiceException {
        Element response = invoke(actionEl.getParent());
        return new ZFolderActionResult(response.getElement(MailService.E_ACTION).getAttribute(MailService.A_ID));
    }

    @Override
    public ZFolderActionResult deleteFolder(String ids) throws ServiceException {
        return doFolderAction(initFolderAction("delete", ids));
    }

    @Override
    public ZFolderActionResult emptyFolder(String ids) throws ServiceException {
        return doFolderAction(initFolderAction("empty", ids));        
    }

    @Override
    public ZFolderActionResult importURLIntoFolder(String id, String url) throws ServiceException {
        return doFolderAction(initFolderAction("import", id).addAttribute(MailService.A_URL, url));
    }

    @Override
    public ZFolderActionResult markFolderAsRead(String ids) throws ServiceException {
        return doFolderAction(initFolderAction("read", ids));                
    }

    @Override
    public ZFolderActionResult moveFolder(String id, String targetFolderId) throws ServiceException {
        return doFolderAction(initFolderAction("move", id).addAttribute(MailService.A_FOLDER, targetFolderId));
    }

    @Override
    public ZFolderActionResult renameFolder(String id, String name) throws ServiceException {
        return doFolderAction(initFolderAction("rename", id).addAttribute(MailService.A_NAME, name));
    }

    @Override
    public ZFolderActionResult setFolderChecked(String ids, boolean checked) throws ServiceException {
        return doFolderAction(initFolderAction(checked ? "check" : "!check", ids));
    }

    @Override
    public ZFolderActionResult setFolderColor(String ids, int color) throws ServiceException {
        return doFolderAction(initFolderAction("color", ids).addAttribute(MailService.A_COLOR, color));
    }

    @Override
    public ZFolderActionResult setFolderExcludeFreeBusy(String ids, boolean state) throws ServiceException {
        return doFolderAction(initFolderAction("fb", ids).addAttribute(MailService.A_EXCLUDE_FREEBUSY, state));
    }

    @Override
    public ZFolderActionResult setFolderURL(String id, String url) throws ServiceException {
        return doFolderAction(initFolderAction("url", id).addAttribute(MailService.A_URL, url));
    }

    @Override
    public ZFolderActionResult syncFolder(String ids) throws ServiceException {
        return doFolderAction(initFolderAction("sync", ids));
    }

}
