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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient.soap;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;
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
    private Map<String, ZSoapTag> mIdToTag;
    
    private ZSoapFolder mUserRoot;
    
    private long mSize;

    ZSoapMailbox() {
        mNameToTag = new HashMap<String, ZSoapTag>();
        mIdToTag = new HashMap<String, ZSoapTag>();        
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
                Element refresh = context.getElement(ZimbraNamespace.E_REFRESH);
                if (refresh != null) refreshHandler(refresh);
            }
        }
    }

    private void addTag(ZSoapTag tag) {
        mNameToTag.put(tag.getName(), tag);
        mIdToTag.put(tag.getId(), tag);
    }
    
    /**
     * handle a &lt;refresh&gt; block
     * @param refresh
     * @throws ServiceException
     */
    private void refreshHandler(Element refresh) throws ServiceException {
        mNameToTag.clear();
        mIdToTag.clear();
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

    private void refreshFolders(Element folderEl) throws ServiceException {
        mUserRoot = new ZSoapFolder(folderEl, null);
    }

    public static ZMailbox getMailbox(String accountName, String password, String uri) throws ServiceException {
        ZSoapMailbox zmbx = new ZSoapMailbox();
        zmbx.setSoapURI(uri);
        zmbx.authRequest(accountName, AccountBy.name, password);
        return zmbx;
    }
    
    @Override
    public ZFolder getRoot() {
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

    public static void main(String args[]) throws ServiceException {
        Zimbra.toolSetup();
        ZMailbox mbox = getMailbox("user1", "test123", "http://localhost:7070/service/soap");
        System.out.println(mbox.getSize());
        System.out.println(mbox.getAllTags());
        System.out.println(mbox.getRoot());
    }

    @Override
    public ZTag getTagById(String id) {
        return mIdToTag.get(id);
    }

    @Override
    public ZTag getTagByName(String name) {
        return mNameToTag.get(name);
    }
}
