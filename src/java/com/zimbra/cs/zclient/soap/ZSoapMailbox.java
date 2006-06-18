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
import com.zimbra.cs.zclient.ZFolderAction;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZSearchHit;
import com.zimbra.cs.zclient.ZSearchParams;
import com.zimbra.cs.zclient.ZSearchResult;
import com.zimbra.cs.zclient.ZTag;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.SoapHttpTransport;
import com.zimbra.soap.ZimbraNamespace;
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
            // handle refresh blocks
            Element context = mTransport.getZimbraContext();
            if (context != null) {
                Element refresh = context.getOptionalElement(ZimbraNamespace.E_REFRESH);
                if (refresh != null) refreshHandler(refresh);
            }
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
        ZFolder folder = getFolderById(id);
        return folder;
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

    @Override
    public ZFolderAction.Result doAction(ZFolderAction action, ZFolder folder) throws ServiceException {
        return doAction(action, folder.getId());
    }

    @Override
    public ZFolderAction.Result doAction(ZFolderAction action, String ids) throws ServiceException {
        XMLElement req = new XMLElement(MailService.FOLDER_ACTION_REQUEST);
        Element actionEl = req.addElement(MailService.E_ACTION);
        
        actionEl.addAttribute(MailService.A_ID, ids);
        String opStr = null;
        switch (action.getOp()) {
        case CHECK:
            opStr = action.getChecked() ? "check" : "!check";
            break;
        case COLOR: 
            opStr = "color";
            actionEl.addAttribute(MailService.A_COLOR, action.getColor());
            break;            
        case DELETE:
            opStr = "delete";
            break;            
        case EXCLUDE_FREE_BUSY:
            opStr = "fb";
            actionEl.addAttribute(MailService.A_EXCLUDE_FREEBUSY, action.getExcludeFreeBusy());
            break;            
        case EMPTY:
            opStr = "empty";
            break;            
        case IMPORT:
            opStr = "import";
            actionEl.addAttribute(MailService.A_URL, action.getURL());
            break;            
        case MARK_AS_READ:
            opStr = "read";
            break;
        case MOVE:
            opStr = "move";
            actionEl.addAttribute(MailService.A_FOLDER, action.getName());
            break;
        case RENAME:
            opStr = "rename";
            actionEl.addAttribute(MailService.A_NAME, action.getName());
            break;    
        case SYNC:
            opStr = "sync";
            break;
        case URL:
            opStr = "url";
            actionEl.addAttribute(MailService.A_URL, action.getURL());
            break;                        
        default:
            throw SoapFaultException.CLIENT_ERROR("unsupported operation: "+action.getOp(), null);
        }
        
        actionEl.addAttribute(MailService.A_OPERATION, opStr);
        
        Element response = invoke(req);
        ZFolderAction.Result result = new ZFolderAction.Result(response.getElement(MailService.E_ACTION).getAttribute(MailService.A_ID));
        return result;
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
        mbox.doAction(ZFolderAction.markAsRead(), inbox);
        mbox.doAction(ZFolderAction.setChecked(true), inbox);
        mbox.doAction(ZFolderAction.setChecked(false), inbox);        
        mbox.doAction(ZFolderAction.setColor(1), inbox);
        mbox.createFolder(inbox, "dork",ZFolder.VIEW_MESSAGE);
                
    }

}
