/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mime.ParsedDocument;

public class WikiItem extends Document {
	
	WikiItem(Mailbox mbox, UnderlyingData data) throws ServiceException {
		super(mbox, data);
	}
	
	public String getWikiWord() {
		return getName();
	}

    public static final String WIKI_CONTENT_TYPE = "text/html; charset=utf-8";
	
    static WikiItem create(int id, Folder folder, short volumeId, String wikiword, ParsedDocument pd)
    throws ServiceException {
        Metadata meta = new Metadata();
        UnderlyingData data = prepareCreate(TYPE_WIKI, id, folder, volumeId, wikiword, WIKI_CONTENT_TYPE, pd, meta);

		Mailbox mbox = folder.getMailbox();
		data.contentChanged(mbox);
        ZimbraLog.mailop.info("Adding WikiItem %s: id=%d, folderId=%d, folderName=%s.",
            wikiword, data.id, folder.getId(), folder.getName());
        DbMailItem.create(mbox, data);

        WikiItem wiki = new WikiItem(mbox, data);
        wiki.finishCreation(null);
        pd.setVersion(wiki.getVersion());
        return wiki;
    }
}
