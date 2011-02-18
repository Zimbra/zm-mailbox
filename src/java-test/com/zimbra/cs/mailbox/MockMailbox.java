/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.cs.mailbox;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.store.MockStoreManager;

/**
 * Mock implementation of {@link Mailbox} for testing.
 *
 * @author ysasaki
 */
public class MockMailbox extends Mailbox {

    private Account account;
    private Map<String, Metadata> metadata = new HashMap<String, Metadata>();

    MockMailbox(Account account) {
        super(new MailboxData());
        this.account = account;
    }

    @Override
    public Metadata getConfig(OperationContext octxt, String section) {
        return metadata.get(section);
    }

    @Override
    public void setConfig(OperationContext octxt, String section,
            Metadata config) {
        metadata.put(section, config);
    }

    @Override
    public Account getAccount() {
        return account;
    }

    @Override
    public String getAccountId() {
        return account.getId();
    }

    @Override
    public Folder getFolderById(OperationContext octxt, int id)
        throws ServiceException {

        return getFolderById(id);
    }

    @Override
    public Folder getFolderById(int id) throws ServiceException {
        MailItem.UnderlyingData data = new MailItem.UnderlyingData();
        data.type = MailItem.Type.FOLDER.toByte();
        data.id = id;
        data.name = String.valueOf(id);
        return new Folder(this, data);
    }

    @Override
    public Tag getTagByName(String name) throws ServiceException {
        MailItem.UnderlyingData data = new MailItem.UnderlyingData();
        data.type = MailItem.Type.TAG.toByte();
        data.name = name;
        return new Tag(this, data);
    }

    @Override
    public Document getDocumentById(OperationContext octxt, int id) throws ServiceException {
        if (itemMap != null) {
            return (Document)itemMap.get(Integer.valueOf(id));
        }
        return null;
    }
    
    private HashMap<Integer,MailItem> itemMap;
    
    public void addMailItem(MailItem item) {
        if (itemMap == null) {
            itemMap = new HashMap<Integer,MailItem>();
        }
        itemMap.put(Integer.valueOf(item.getId()), item);
    }
    
    public void addDocument(int id, String name, String contentType, String content) throws ServiceException {
        MailItem.UnderlyingData data = new MailItem.UnderlyingData();
        data.id = id;
        data.type = MailItem.Type.DOCUMENT.toByte();
        data.name = name;
        data.subject = name;
        data.flags = Flag.BITMASK_UNCACHED;
        data.setBlobDigest("foo");
        data.metadata = new Metadata().put(Metadata.FN_MIME_TYPE, contentType).toString();
        Document doc = new Document(this, data);
        MockStoreManager.setBlob(doc, content);
        addMailItem(doc);
    }
    
    @Override
    public boolean dumpsterEnabled() {
        return false;
    }
}
