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

import java.util.Map;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.WriteOpDocumentHandler;

/**
 * @author schemers
 */
public class CreateTag extends WriteOpDocumentHandler  {

	public Element handle(Element request, Map context) throws ServiceException {
		ZimbraSoapContext lc = getZimbraSoapContext(context);
		// FIXME: need to check that account and mailbox exist
        Mailbox mbox = getRequestedMailbox(lc);

        Element t = request.getElement(MailService.E_TAG);
        String name = t.getAttribute(MailService.A_NAME);
        byte color = (byte) t.getAttributeLong(MailService.A_COLOR, Tag.DEFAULT_COLOR);
        
        Tag tag = mbox.createTag(null, name, color);
        
        Element response = lc.createElement(MailService.CREATE_TAG_RESPONSE);
        if (tag != null)
        	ToXML.encodeTag(response, lc, tag);
        return response;
	}
}
