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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.ContactActionOperation;
import com.zimbra.cs.operation.Operation.Requester;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
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

        Element action = request.getElement(MailService.E_ACTION);
        String operation = action.getAttribute(MailService.A_OPERATION).toLowerCase();

        if (operation.endsWith(OP_READ) || operation.endsWith(OP_SPAM))
            throw ServiceException.INVALID_REQUEST("invalid operation on contact: " + operation, null);
        String successes;
        if (CONTACT_OPS.contains(operation))
            successes = handleContact(context, request, operation);
        else
            successes = handleCommon(context, request, operation, MailItem.TYPE_CONTACT);

        Element response = zsc.createElement(MailService.CONTACT_ACTION_RESPONSE);
        Element actionOut = response.addUniqueElement(MailService.E_ACTION);
        actionOut.addAttribute(MailService.A_ID, successes);
        actionOut.addAttribute(MailService.A_OPERATION, operation);
        return response;
    }

    private String handleContact(Map<String,Object> context, Element request, String operation)
    throws ServiceException, SoapFaultException {
        Element action = request.getElement(MailService.E_ACTION);

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        SoapSession session = (SoapSession) zsc.getSession(SessionCache.SESSION_SOAP);
        OperationContext octxt = zsc.getOperationContext();


        // figure out which items are local and which ones are remote, and proxy accordingly
        ArrayList<Integer> local = new ArrayList<Integer>();
        HashMap<String, StringBuffer> remote = new HashMap<String, StringBuffer>();
        partitionItems(zsc, action.getAttribute(MailService.A_ID), local, remote);
        StringBuffer successes = proxyRemoteItems(action, remote, request, context);

        if (!local.isEmpty()) {
            String localResults;
            if (operation.equals(OP_UPDATE)) {
                // duplicating code from ItemAction.java for now...
                String folderId = action.getAttribute(MailService.A_FOLDER, "-1");
                ItemId iidFolder = new ItemId(folderId == null ? "-1" : folderId, zsc);
                if (!iidFolder.belongsTo(mbox))
                    throw ServiceException.INVALID_REQUEST("cannot move item between mailboxes", null);
                else if (folderId != null && iidFolder.getId() <= 0)
                    throw MailServiceException.NO_SUCH_FOLDER(iidFolder.getId());
                String flags = action.getAttribute(MailService.A_FLAGS, null);
                String tags  = action.getAttribute(MailService.A_TAGS, null);
                byte color = (byte) action.getAttributeLong(MailService.A_COLOR, -1);
                Map<String, String> fields = ModifyContact.parseFields(action.listElements(MailService.E_ATTRIBUTE));

                localResults = ContactActionOperation.UPDATE(zsc, session, octxt, mbox, Requester.SOAP,
                                                             local, iidFolder, flags, tags, color, fields).getResult();
            } else {
                throw ServiceException.INVALID_REQUEST("unknown operation: " + operation, null);
            }
            successes.append(successes.length() > 0 ? "," : "").append(localResults);
        }

        return successes.toString();
    }
}
