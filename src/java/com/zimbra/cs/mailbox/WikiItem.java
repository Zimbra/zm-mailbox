/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mime.ParsedDocument;

public final class WikiItem extends Document {

    WikiItem(Mailbox mbox, UnderlyingData data) throws ServiceException {
        super(mbox, data);
    }

    public String getWikiWord() {
        return getName();
    }

    public static final String WIKI_CONTENT_TYPE = "text/html; charset=utf-8";

    static WikiItem create(int id, String uuid, Folder folder, String wikiword, ParsedDocument pd, CustomMetadata custom)
            throws ServiceException {
        Metadata meta = new Metadata();
        UnderlyingData data = prepareCreate(Type.WIKI, id, uuid, folder, wikiword, WIKI_CONTENT_TYPE, pd, meta, custom, 0);

        Mailbox mbox = folder.getMailbox();
        data.contentChanged(mbox);
        ZimbraLog.mailop.info("Adding WikiItem %s: id=%d, folderId=%d, folderName=%s.",
            wikiword, data.id, folder.getId(), folder.getName());
        new DbMailItem(mbox).create(data);

        WikiItem wiki = new WikiItem(mbox, data);
        wiki.finishCreation(null);
        pd.setVersion(wiki.getVersion());
        return wiki;
    }
}
