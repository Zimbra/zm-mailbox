/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mime.ParsedDocument;

public final class WikiItem extends Document {

    WikiItem(Mailbox mbox, UnderlyingData data) throws ServiceException {
        this(mbox, data, false);
    }

    WikiItem(Mailbox mbox, UnderlyingData data, boolean skipCache) throws ServiceException {
        super(mbox, data, skipCache);
    }

    WikiItem(Account acc, UnderlyingData data, int mailboxId)  throws ServiceException {
        super(acc, data, mailboxId);
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
