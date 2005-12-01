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
 * Created on May 29, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.SpamHandler;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraContext;
import com.zimbra.soap.WriteOpDocumentHandler;

/**
 * @author dkarp
 */
public class ItemAction extends WriteOpDocumentHandler {

    protected static final String[] OPERATION_PATH = new String[] { MailService.E_ACTION, MailService.A_OPERATION };
    protected static final String[] TARGET_ITEM_PATH = new String[] { MailService.E_ACTION, MailService.A_ID };
    protected static final String[] TARGET_FOLDER_PATH = new String[] { MailService.E_ACTION, MailService.A_FOLDER };
    protected String[] getProxiedIdPath(Element request) {
        String operation = getXPath(request, OPERATION_PATH);
        if (operation == null)
            return null;
        if (operation.startsWith("!"))
            operation = operation.substring(1);
        // move operation needs to be executed in the context of the target folder
        return (operation.toLowerCase().equals(OP_MOVE) ? TARGET_FOLDER_PATH : null);
    }

    public static final String OP_TAG         = "tag";
    public static final String OP_FLAG        = "flag";
    public static final String OP_READ        = "read";
    public static final String OP_COLOR       = "color";
    public static final String OP_HARD_DELETE = "delete";
    public static final String OP_MOVE        = "move";
    public static final String OP_SPAM        = "spam";
    public static final String OP_UPDATE      = "update";

    public Element handle(Element request, Map context) throws ServiceException, SoapFaultException {
        ZimbraContext lc = getZimbraContext(context);

        Element action = request.getElement(MailService.E_ACTION);
        String operation = action.getAttribute(MailService.A_OPERATION).toLowerCase();

        String successes = handleCommon(context, request, operation, MailItem.TYPE_UNKNOWN);

        Element response = lc.createElement(MailService.ITEM_ACTION_RESPONSE);
        Element act = response.addUniqueElement(MailService.E_ACTION);
        act.addAttribute(MailService.A_ID, successes);
        act.addAttribute(MailService.A_OPERATION, operation);
        return response;
    }

    String handleCommon(Map context, Element request, String opAttr, byte type) throws ServiceException, SoapFaultException {
        Element action = request.getElement(MailService.E_ACTION);
        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);

        // determine the requested operation
        String op;
        boolean flagValue = true;
        if (opAttr.length() > 1 && opAttr.startsWith("!")) {
            flagValue = false;
            op = opAttr.substring(1);
        } else
            op = opAttr;

        // figure out which items are local and which ones are remote, and proxy accordingly
        ArrayList<Integer> local = new ArrayList<Integer>();
        HashMap<String, StringBuffer> remote = new HashMap<String, StringBuffer>();
        partitionItems(lc, action.getAttribute(MailService.A_ID), local, remote);
        // we don't yet support moving from a remote mailbox
        if (op.equals(OP_MOVE) && !remote.isEmpty())
            throw ServiceException.INVALID_REQUEST("cannot move item between mailboxes", null);
        StringBuffer successes = proxyRemoteItems(action, remote, request, context);
        if (local.isEmpty())
            return successes.toString();

        OperationContext octxt = lc.getOperationContext();
        String constraint = action.getAttribute(MailService.A_TARGET_CONSTRAINT, null);
        TargetConstraint tcon = TargetConstraint.parseConstraint(mbox, constraint);

        // iterate over the local items and perform the requested operation
        for (int id: local) {
            if (op.equals(OP_FLAG))
                mbox.alterTag(octxt, id, type, Flag.ID_FLAG_FLAGGED, flagValue, tcon);
            else if (op.equals(OP_READ))
                mbox.alterTag(octxt, id, type, Flag.ID_FLAG_UNREAD, !flagValue, tcon);
            else if (op.equals(OP_TAG)) {
                int tagId = (int) action.getAttributeLong(MailService.A_TAG);
                mbox.alterTag(octxt, id, type, tagId, flagValue, tcon);
            } else if (op.equals(OP_COLOR)) {
                byte color = (byte) action.getAttributeLong(MailService.A_COLOR);
                mbox.setColor(octxt, id, type, color);
            } else if (op.equals(OP_HARD_DELETE))
                mbox.delete(octxt, id, type, tcon);
            else if (op.equals(OP_MOVE)) {
                ItemId iidFolder = new ItemId(action.getAttribute(MailService.A_FOLDER), lc);
                mbox.move(octxt, id, type, iidFolder.getId(), tcon);
            } else if (op.equals(OP_SPAM)) {
                int defaultFolder = flagValue ? Mailbox.ID_FOLDER_SPAM : Mailbox.ID_FOLDER_INBOX;
                int folderId = (int) action.getAttributeLong(MailService.A_FOLDER, defaultFolder);
                mbox.move(octxt, id, type, folderId, tcon);
                SpamHandler.getInstance().handle(mbox, id, type, flagValue);
            } else if (op.equals(OP_UPDATE)) {
                ItemId iidFolder = new ItemId(action.getAttribute(MailService.A_FOLDER, GetFolder.DEFAULT_FOLDER_ID), lc);
                if (!iidFolder.belongsTo(mbox))
                    throw ServiceException.INVALID_REQUEST("cannot move item between mailboxes", null);
                String flags = action.getAttribute(MailService.A_FLAGS, null);
                String tags  = action.getAttribute(MailService.A_TAGS, null);
                byte color = (byte) action.getAttributeLong(MailService.A_COLOR, -1);

                if (iidFolder.getId() > 0)
                    mbox.move(octxt, id, type, iidFolder.getId(), tcon);
                if (tags != null || flags != null)
                    mbox.setTags(octxt, id, type, flags, tags, tcon);
                if (color >= 0)
                    mbox.setColor(octxt, id, type, color);
            } else
                throw ServiceException.INVALID_REQUEST("unknown operation: " + op, null);

            successes.append(successes.length() > 0 ? "," : "").append(lc.formatItemId(id));
        }

        return successes.toString();
    }

    private void partitionItems(ZimbraContext lc, String ids, ArrayList<Integer> local, HashMap<String, StringBuffer> remote) throws ServiceException {
        Account acct = getRequestedAccount(lc);
        String targets[] = ids.split(",");
        for (int i = 0; i < targets.length; i++) {
            ItemId iid = new ItemId(targets[i], lc);
            if (iid.belongsTo(acct))
                local.add(iid.getId());
            else {
                StringBuffer sb = remote.get(iid.getAccountId());
                if (sb == null)
                    remote.put(iid.getAccountId(), new StringBuffer(iid.toString()));
                else
                    sb.append(',').append(iid.toString());
            }
        }
    }

    private StringBuffer proxyRemoteItems(Element action, Map remote, Element request, Map context)
    throws ServiceException, SoapFaultException {
        StringBuffer successes = new StringBuffer();
        for (Iterator it = remote.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            action.addAttribute(MailService.A_ID, entry.getValue().toString());
            Element response = proxyRequest(request, context, entry.getKey().toString());
            String completed = extractSuccesses(response);
            successes.append(completed.length() > 0 && successes.length() > 0 ? "," : "").append(completed);
        }
        return successes;
    }

    String extractSuccesses(Element response) {
        try {
            return response.getElement(MailService.E_ACTION).getAttribute(MailService.A_ID);
        } catch (ServiceException e) {
            ZimbraLog.misc.warn("could not extract ItemAction successes from proxied response", e);
            return "";
        }
    }
}
