/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.SearchParams.ExpandResults;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetConv extends MailDocumentHandler  {

    private static final String[] TARGET_CONV_PATH = new String[] { MailConstants.E_CONV, MailConstants.A_ID };
    @Override protected String[] getProxiedIdPath(Element request)  { return TARGET_CONV_PATH; }

    @Override public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        Element econv = request.getElement(MailConstants.E_CONV);
        ItemId iid = new ItemId(econv.getAttribute(MailConstants.A_ID), zsc);

        SearchParams params = new SearchParams();
        params.setInlineRule(ExpandResults.valueOf(econv.getAttribute(MailConstants.A_FETCH, null), zsc));
        if (params.getInlineRule() != ExpandResults.NONE) {
            params.setWantHtml(econv.getAttributeBool(MailConstants.A_WANT_HTML, false));
            params.setMaxInlinedLength((int) econv.getAttributeLong(MailConstants.A_MAX_INLINED_LENGTH, -1));
//          params.setMarkRead(econv.getAttributeBool(MailConstants.A_MARK_READ, false));
            for (Element eHdr : econv.listElements(MailConstants.A_HEADER))
                params.addInlinedHeader(eHdr.getAttribute(MailConstants.A_ATTRIBUTE_NAME));
        }

        Conversation conv = mbox.getConversationById(octxt, iid.getId());

        if (conv == null)
            throw MailServiceException.NO_SUCH_CONV(iid.getId());

        List<Message> msgs = mbox.getMessagesByConversation(octxt, conv.getId(), SortBy.DATE_ASCENDING);
        if (msgs.isEmpty() && zsc.isDelegatedRequest())
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions");

        Element response = zsc.createElement(MailConstants.GET_CONV_RESPONSE);
        ToXML.encodeConversation(response, ifmt, octxt, conv, msgs, params);
        return response;
    }
}
