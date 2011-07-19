/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

import java.io.File;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.HSQLDB;
import com.zimbra.cs.mime.MockMimeTypeInfo;
import com.zimbra.cs.mime.handler.MessageRFC822Handler;
import com.zimbra.cs.mime.handler.TextHtmlHandler;
import com.zimbra.cs.mime.handler.TextPlainHandler;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.soap.mail.type.RetentionPolicy;

public final class MailboxTestUtil {

    private MailboxTestUtil() {
    }

    /**
     * Initializes the database, index, store manager, and provisioning.
     */
    public static void initServer() throws Exception {
        System.setProperty("log4j.configuration", "log4j-test.properties");
        // Don't load from /opt/zimbra/conf
        System.setProperty("zimbra.config", "src/java-test/localconfig-test.xml");
        LC.reload();
        LC.zimbra_attrs_directory.setDefault("conf/attrs");
        LC.zimbra_rights_directory.setDefault("conf/rights");

        // Initialize provisioning and set up default MIME handlers for indexing.
        MockProvisioning prov = new MockProvisioning();
        for (Map.Entry<String, MockMimeTypeInfo> entry : getMimeHandlers().entrySet()) {
            prov.addMimeType(entry.getKey(), entry.getValue());
        }
        Provisioning.setInstance(prov);

        LC.zimbra_class_database.setDefault(HSQLDB.class.getName());
        DbPool.startup();
        HSQLDB.createDatabase();

        MailboxManager.setInstance(null);
        MailboxIndex.setIndexStoreFactory("lucene");

        LC.zimbra_class_store.setDefault(MockStoreManager.class.getName());
        StoreManager.getInstance().startup();
    }

    private static Map<String, MockMimeTypeInfo> getMimeHandlers() {
        Map<String, MockMimeTypeInfo> map = Maps.newHashMap();

        MockMimeTypeInfo plain = new MockMimeTypeInfo();
        plain.setHandlerClass(TextPlainHandler.class.getName());
        plain.setIndexingEnabled(true);
        map.put(MimeConstants.CT_TEXT_PLAIN, plain);

        MockMimeTypeInfo html = new MockMimeTypeInfo();
        html.setHandlerClass(TextHtmlHandler.class.getName());
        html.setIndexingEnabled(true);
        map.put(MimeConstants.CT_TEXT_HTML, html);

        MockMimeTypeInfo message = new MockMimeTypeInfo();
        message.setHandlerClass(MessageRFC822Handler.class.getName());
        message.setIndexingEnabled(true);
        map.put(MimeConstants.CT_MESSAGE_RFC822, message);

        return map;
    }

    /**
     * Clears the database and index.
     */
    public static void clearData() throws Exception {
        HSQLDB.clearDatabase();
        MailboxManager.getInstance().clearCache();
        MailboxIndex.shutdown();
        File index = new File("build/test/index");
        if (index.isDirectory()) {
            Files.deleteDirectoryContents(index);
        }
    }

    public static void setFlag(Mailbox mbox, int itemId, Flag.FlagInfo flag) throws ServiceException {
        MailItem item = mbox.getItemById(null, itemId, MailItem.Type.UNKNOWN);
        int flags = item.getFlagBitmask() | flag.toBitmask();
        mbox.setTags(null, itemId, item.getType(), flags, item.getTagBitmask(), null);
    }

    public static void unsetFlag(Mailbox mbox, int itemId, Flag.FlagInfo flag) throws ServiceException {
        MailItem item = mbox.getItemById(null, itemId, MailItem.Type.UNKNOWN);
        int flags = item.getFlagBitmask() & ~flag.toBitmask();
        mbox.setTags(null, itemId, item.getType(), flags, item.getTagBitmask(), null);
    }

    public static void index(Mailbox mbox) throws ServiceException {
        mbox.index.indexDeferredItems();
    }
}
