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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetSearchFolder extends DocumentHandler  {

	public Element handle(Element request, Map context) throws ServiceException {
		ZimbraSoapContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
		
        Element response = lc.createElement(MailService.GET_SEARCH_FOLDER_RESPONSE);
        List searches = mbox.getItemList(lc.getOperationContext(), MailItem.TYPE_SEARCHFOLDER);
        if (searches != null)
            for (Iterator mi = searches.iterator(); mi.hasNext(); ) {
                SearchFolder q = (SearchFolder) mi.next();
                ToXML.encodeSearchFolder(response, lc, q);
            }
        return response;
	}
}
