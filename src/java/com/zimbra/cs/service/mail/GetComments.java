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
package com.zimbra.cs.service.mail;

import java.util.HashSet;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Comment;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;

public class GetComments extends MailDocumentHandler {

    private static final String[] PARENT_ID_PATH = new String[] { MailConstants.E_COMMENT, MailConstants.A_PARENT_ID };
    
    @Override
    protected String[] getProxiedIdPath(Element request)     { return PARENT_ID_PATH; }
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        Element c = request.getElement(MailConstants.E_COMMENT);
        String itemId = c.getAttribute(MailConstants.A_PARENT_ID);

        ItemId iid = new ItemId(itemId, zsc);

        Element response = zsc.createElement(MailConstants.GET_COMMENTS_RESPONSE);
        HashSet<Account> accounts = new HashSet<Account>();
        for (Comment comment : mbox.getComments(octxt, iid.getId(), 0, -1)) {
            accounts.add(comment.getCreatorAccount());
            ToXML.encodeComment(response, ifmt, octxt, comment);
        }
        
        for (Account a : accounts) {
            Element user = response.addElement(MailConstants.A_USER);
            user.addAttribute(MailConstants.A_ID, a.getId());
            user.addAttribute(MailConstants.A_EMAIL, a.getName());
            user.addAttribute(MailConstants.A_NAME, a.getDisplayName());
        }
        return response;
    }
}
