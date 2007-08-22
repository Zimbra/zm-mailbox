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
 * Portions created by Zimbra are Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import com.zimbra.common.service.ServiceException;
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
        DbMailItem.create(mbox, data);

        WikiItem wiki = new WikiItem(mbox, data);
        wiki.finishCreation(null);
        pd.setVersion(wiki.getVersion());
        return wiki;
    }
}
