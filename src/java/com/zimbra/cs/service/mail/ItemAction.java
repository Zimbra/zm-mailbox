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
 * Created on May 29, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.operation.ItemActionOperation;
import com.zimbra.cs.operation.Operation.Requester;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author dkarp
 */
public class ItemAction extends MailDocumentHandler {

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

    public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Element action = request.getElement(MailService.E_ACTION);
        String operation = action.getAttribute(MailService.A_OPERATION).toLowerCase();

        String successes = handleCommon(context, request, operation, MailItem.TYPE_UNKNOWN);

        Element response = zsc.createElement(MailService.ITEM_ACTION_RESPONSE);
        Element act = response.addUniqueElement(MailService.E_ACTION);
        act.addAttribute(MailService.A_ID, successes);
        act.addAttribute(MailService.A_OPERATION, operation);
        return response;
    }

    protected String handleCommon(Map<String,Object> context, Element request, String opAttr, byte type) throws ServiceException, SoapFaultException {
        Element action = request.getElement(MailService.E_ACTION);
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        SoapSession session = (SoapSession) zsc.getSession(SessionCache.SESSION_SOAP);

        // determine the requested operation
        String opStr;
        boolean flagValue = true;
        if (opAttr.length() > 1 && opAttr.startsWith("!")) {
            flagValue = false;
            opStr = opAttr.substring(1);
        } else
            opStr = opAttr;

        // figure out which items are local and which ones are remote, and proxy accordingly
        ArrayList<Integer> local = new ArrayList<Integer>();
        HashMap<String, StringBuffer> remote = new HashMap<String, StringBuffer>();
        partitionItems(zsc, action.getAttribute(MailService.A_ID), local, remote);
        
        // we don't yet support moving from a remote mailbox
        if (opStr.equals(OP_MOVE) && !remote.isEmpty())
            throw ServiceException.INVALID_REQUEST("cannot move item between mailboxes", null);
        StringBuffer successes = proxyRemoteItems(action, remote, request, context);

        if (!local.isEmpty()) {
            OperationContext octxt = zsc.getOperationContext();
        	String constraint = action.getAttribute(MailService.A_TARGET_CONSTRAINT, null);
        	TargetConstraint tcon = TargetConstraint.parseConstraint(mbox, constraint);
        	
        	String localResults;
        	
        	// set additional parameters (depends on op type)
        	if (opStr.equals(OP_TAG)) {
        		int tagId = (int) action.getAttributeLong(MailService.A_TAG);
        		localResults = ItemActionOperation.TAG(zsc, session, octxt, mbox,
        					Requester.SOAP, local, type, flagValue, tcon, tagId).getResult();
        	} else if (opStr.equals(OP_FLAG)) {
        		localResults = ItemActionOperation.FLAG(zsc, session, octxt, mbox,
        					Requester.SOAP, local, type, flagValue, tcon).getResult();
        	} else if (opStr.equals(OP_READ)) {
        		localResults = ItemActionOperation.READ(zsc, session, octxt, mbox,
        					Requester.SOAP, local, type, flagValue, tcon).getResult();
        	} else if (opStr.equals(OP_COLOR)) {
        		byte color = (byte) action.getAttributeLong(MailService.A_COLOR);
        		localResults = ItemActionOperation.COLOR(zsc, session, octxt, mbox,
        					Requester.SOAP, local, type, flagValue, tcon, color).getResult();
        	} else if (opStr.equals(OP_HARD_DELETE)) {
        		localResults = ItemActionOperation.HARD_DELETE(zsc, session, octxt, mbox,
        					Requester.SOAP, local, type, flagValue, tcon).getResult();
        	} else if (opStr.equals(OP_MOVE)) {
        		ItemId iidFolder = new ItemId(action.getAttribute(MailService.A_FOLDER), zsc);
        		localResults = ItemActionOperation.MOVE(zsc, session, octxt, mbox,
        					Requester.SOAP, local, type, flagValue, tcon, iidFolder).getResult();
        	} else if (opStr.equals(OP_SPAM)) {
        		int defaultFolder = flagValue ? Mailbox.ID_FOLDER_SPAM : Mailbox.ID_FOLDER_INBOX;
        		int folderId = (int) action.getAttributeLong(MailService.A_FOLDER, defaultFolder);
        		localResults = ItemActionOperation.SPAM(zsc, session, octxt, mbox,
        					Requester.SOAP, local, type, flagValue, tcon, folderId).getResult();
        	} else if (opStr.equals(OP_UPDATE)) {
                String folderId = action.getAttribute(MailService.A_FOLDER, null);
                ItemId iidFolder = new ItemId(folderId == null ? "-1" : folderId, zsc);
        		if (!iidFolder.belongsTo(mbox))
        			throw ServiceException.INVALID_REQUEST("cannot move item between mailboxes", null);
                else if (folderId != null && iidFolder.getId() <= 0)
                    throw MailServiceException.NO_SUCH_FOLDER(iidFolder.getId());
        		String flags = action.getAttribute(MailService.A_FLAGS, null);
        		String tags  = action.getAttribute(MailService.A_TAGS, null);
        		byte color = (byte) action.getAttributeLong(MailService.A_COLOR, -1);
        		localResults = ItemActionOperation.UPDATE(zsc, session, octxt, mbox,
        					Requester.SOAP, local, type, tcon, iidFolder, 
        					flags, tags, color).getResult();
        	} else {
        		throw ServiceException.INVALID_REQUEST("unknown operation: " + opStr, null);
        	}
        	successes.append(successes.length() > 0 ? "," : "").append(localResults);
        }

        return successes.toString();
    }

    static void partitionItems(ZimbraSoapContext zsc, String ids, ArrayList<Integer> local, HashMap<String, StringBuffer> remote) throws ServiceException {
        Account acct = getRequestedAccount(zsc);
        String targets[] = ids.split(",");
        for (int i = 0; i < targets.length; i++) {
            ItemId iid = new ItemId(targets[i], zsc);
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

    protected StringBuffer proxyRemoteItems(Element action, Map remote, Element request, Map<String,Object> context)
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

    protected String extractSuccesses(Element response) {
        try {
            return response.getElement(MailService.E_ACTION).getAttribute(MailService.A_ID);
        } catch (ServiceException e) {
            ZimbraLog.misc.warn("could not extract ItemAction successes from proxied response", e);
            return "";
        }
    }
}
