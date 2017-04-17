/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Contact.Attachment;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.util.TagUtil;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @since May 26, 2004
 * @author schemers
 */
public class ContactAction extends ItemAction {

    private static final Set<String> CONTACT_OPS = ImmutableSet.of(OP_UPDATE);

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Element action = request.getElement(MailConstants.E_ACTION);
        String operation = action.getAttribute(MailConstants.A_OPERATION).toLowerCase();

        if (operation.endsWith(OP_READ) || operation.endsWith(OP_SPAM)) {
            throw ServiceException.INVALID_REQUEST("invalid operation on contact: " + operation, null);
        }

        ItemActionResult successes;
        if (CONTACT_OPS.contains(operation)) {
            successes = handleContact(context, request, operation);
        } else {
            successes = handleCommon(context, request, MailItem.Type.CONTACT);
        }
        Element response = zsc.createElement(MailConstants.CONTACT_ACTION_RESPONSE);
        Element actionOut = response.addUniqueElement(MailConstants.E_ACTION);
        actionOut.addAttribute(MailConstants.A_ID, Joiner.on(",").join(successes.getSuccessIds()));
        actionOut.addAttribute(MailConstants.A_OPERATION, operation);
        return response;
    }

    private ItemActionResult handleContact(Map<String,Object> context, Element request, String operation)
    throws ServiceException, SoapFaultException {
        Element action = request.getElement(MailConstants.E_ACTION);

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        // figure out which items are local and which ones are remote, and proxy accordingly
        ArrayList<Integer> local = new ArrayList<Integer>();
        HashMap<String, StringBuilder> remote = new HashMap<String, StringBuilder>();
        partitionItems(zsc, action.getAttribute(MailConstants.A_ID), local, remote);
        ItemActionResult successes = proxyRemoteItems(action, remote, request, context);

        if (!local.isEmpty()) {
            ItemActionResult localResults;
            if (operation.equals(OP_UPDATE)) {
                // duplicating code from ItemAction.java for now...
                String folderId = action.getAttribute(MailConstants.A_FOLDER, null);
                ItemId iidFolder = new ItemId(folderId == null ? "-1" : folderId, zsc);
                if (!iidFolder.belongsTo(mbox)) {
                    throw ServiceException.INVALID_REQUEST("cannot move item between mailboxes", null);
                } else if (folderId != null && iidFolder.getId() <= 0) {
                    throw MailServiceException.NO_SUCH_FOLDER(iidFolder.getId());
                }
                String flags = action.getAttribute(MailConstants.A_FLAGS, null);
                String[] tags = TagUtil.parseTags(action, mbox, octxt);
                Color color = getColor(action);
                ParsedContact pc = null;
                if (!action.listElements(MailConstants.E_ATTRIBUTE).isEmpty()) {
                    Contact cn = local.size() == 1 ? mbox.getContactById(octxt, local.get(0)) : null;
                    Pair<Map<String,Object>, List<Attachment>> cdata = CreateContact.parseContact(action, zsc, octxt, cn);
                    pc = new ParsedContact(cdata.getFirst(), cdata.getSecond());
                }

                localResults = ContactActionHelper.UPDATE(zsc, octxt, mbox, local, iidFolder, flags, tags, color, pc).getResult();
            } else {
                throw ServiceException.INVALID_REQUEST("unknown operation: " + operation, null);
            }
            successes.appendSuccessIds(localResults.getSuccessIds());
        }

        return successes;
    }
}
