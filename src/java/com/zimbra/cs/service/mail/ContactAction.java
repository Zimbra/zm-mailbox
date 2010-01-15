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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Contact.Attachment;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class ContactAction extends ItemAction {

    private static final Set CONTACT_OPS = new HashSet<String>(Arrays.asList(new String[] {
        OP_UPDATE
    }));

	public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Element action = request.getElement(MailConstants.E_ACTION);
        String operation = action.getAttribute(MailConstants.A_OPERATION).toLowerCase();

        if (operation.endsWith(OP_READ) || operation.endsWith(OP_SPAM))
            throw ServiceException.INVALID_REQUEST("invalid operation on contact: " + operation, null);
        String successes;
        if (CONTACT_OPS.contains(operation))
            successes = handleContact(context, request, operation);
        else
            successes = handleCommon(context, request, operation, MailItem.TYPE_CONTACT);

        Element response = zsc.createElement(MailConstants.CONTACT_ACTION_RESPONSE);
        Element actionOut = response.addUniqueElement(MailConstants.E_ACTION);
        actionOut.addAttribute(MailConstants.A_ID, successes);
        actionOut.addAttribute(MailConstants.A_OPERATION, operation);
        return response;
    }

    private String handleContact(Map<String,Object> context, Element request, String operation)
    throws ServiceException, SoapFaultException {
        Element action = request.getElement(MailConstants.E_ACTION);

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        // figure out which items are local and which ones are remote, and proxy accordingly
        ArrayList<Integer> local = new ArrayList<Integer>();
        HashMap<String, StringBuffer> remote = new HashMap<String, StringBuffer>();
        partitionItems(zsc, action.getAttribute(MailConstants.A_ID), local, remote);
        StringBuffer successes = proxyRemoteItems(action, remote, request, context);

        if (!local.isEmpty()) {
            String localResults;
            if (operation.equals(OP_UPDATE)) {
                // duplicating code from ItemAction.java for now...
                String folderId = action.getAttribute(MailConstants.A_FOLDER, null);
                ItemId iidFolder = new ItemId(folderId == null ? "-1" : folderId, zsc);
                if (!iidFolder.belongsTo(mbox))
                    throw ServiceException.INVALID_REQUEST("cannot move item between mailboxes", null);
                else if (folderId != null && iidFolder.getId() <= 0)
                    throw MailServiceException.NO_SUCH_FOLDER(iidFolder.getId());
                String flags = action.getAttribute(MailConstants.A_FLAGS, null);
                String tags  = action.getAttribute(MailConstants.A_TAGS, null);
                MailItem.Color color = getColor(action);
                ParsedContact pc = null;
                if (!action.listElements(MailConstants.E_ATTRIBUTE).isEmpty()) {
                    Contact cn = local.size() == 1 ? mbox.getContactById(octxt, local.get(0)) : null;
                    Pair<Map<String,String>, List<Attachment>> cdata = CreateContact.parseContact(action, zsc, octxt, cn);
                    pc = new ParsedContact(cdata.getFirst(), cdata.getSecond());
                }

                localResults = ContactActionHelper.UPDATE(zsc, octxt, mbox,
                                                             local, iidFolder, flags, tags, color, pc).getResult();
            } else {
                throw ServiceException.INVALID_REQUEST("unknown operation: " + operation, null);
            }
            successes.append(successes.length() > 0 ? "," : "").append(localResults);
        }

        return successes.toString();
    }
}
