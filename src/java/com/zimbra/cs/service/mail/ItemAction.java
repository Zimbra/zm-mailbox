/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 29, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author dkarp
 */
public class ItemAction extends MailDocumentHandler {

	protected static final String[] OPERATION_PATH = new String[] { MailConstants.E_ACTION, MailConstants.A_OPERATION };
	protected static final String[] TARGET_ITEM_PATH = new String[] { MailConstants.E_ACTION, MailConstants.A_ID };
	
	
	public static final String OP_TAG         = "tag";
	public static final String OP_FLAG        = "flag";
	public static final String OP_READ        = "read";
	public static final String OP_COLOR       = "color";
	public static final String OP_HARD_DELETE = "delete";
    public static final String OP_MOVE        = "move";
    public static final String OP_COPY        = "copy";
    public static final String OP_SPAM        = "spam";
    public static final String OP_TRASH       = "trash";
    public static final String OP_RENAME      = "rename";
    public static final String OP_UPDATE      = "update";

    public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Element action = request.getElement(MailConstants.E_ACTION);
        String operation = action.getAttribute(MailConstants.A_OPERATION).toLowerCase();

        String successes = handleCommon(context, request, operation, MailItem.TYPE_UNKNOWN);

        Element response = zsc.createElement(MailConstants.ITEM_ACTION_RESPONSE);
        Element act = response.addUniqueElement(MailConstants.E_ACTION);
        act.addAttribute(MailConstants.A_ID, successes);
        act.addAttribute(MailConstants.A_OPERATION, operation);
        return response;
    }

    protected String handleCommon(Map<String,Object> context, Element request, String opAttr, byte type) throws ServiceException, SoapFaultException {
        Element action = request.getElement(MailConstants.E_ACTION);
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        SoapProtocol responseProto = zsc.getResponseProtocol();

        // determine the requested operation
        String opStr;
        boolean flagValue = true;
        if (opAttr.length() > 1 && opAttr.startsWith("!")) {
            flagValue = false;
            opStr = opAttr.substring(1);
        } else {
            opStr = opAttr;
        }

        // figure out which items are local and which ones are remote, and proxy accordingly
        List<Integer> local = new ArrayList<Integer>();
        Map<String, StringBuffer> remote = new HashMap<String, StringBuffer>();
        partitionItems(zsc, action.getAttribute(MailConstants.A_ID), local, remote);

        StringBuffer successes = proxyRemoteItems(action, remote, request, context);

        if (!local.isEmpty()) {
            OperationContext octxt = getOperationContext(zsc, context);
        	String constraint = action.getAttribute(MailConstants.A_TARGET_CONSTRAINT, null);
        	TargetConstraint tcon = TargetConstraint.parseConstraint(mbox, constraint);
        	
        	String localResults;
        	
        	// set additional parameters (depends on op type)
        	if (opStr.equals(OP_TAG)) {
        		int tagId = (int) action.getAttributeLong(MailConstants.A_TAG);
        		localResults = ItemActionHelper.TAG(octxt, mbox, responseProto, local, type, flagValue, tcon, tagId).getResult();
        	} else if (opStr.equals(OP_FLAG)) {
        		localResults = ItemActionHelper.FLAG(octxt, mbox, responseProto, local, type, flagValue, tcon).getResult();
        	} else if (opStr.equals(OP_READ)) {
        		localResults = ItemActionHelper.READ(octxt, mbox, responseProto, local, type, flagValue, tcon).getResult();
        	} else if (opStr.equals(OP_COLOR)) {
        		byte color = (byte) action.getAttributeLong(MailConstants.A_COLOR);
        		localResults = ItemActionHelper.COLOR(octxt, mbox, responseProto, local, type, tcon, color).getResult();
        	} else if (opStr.equals(OP_HARD_DELETE)) {
        		localResults = ItemActionHelper.HARD_DELETE(octxt, mbox, responseProto, local, type, tcon).getResult();
            } else if (opStr.equals(OP_MOVE)) {
                ItemId iidFolder = new ItemId(action.getAttribute(MailConstants.A_FOLDER), zsc);
                localResults = ItemActionHelper.MOVE(octxt, mbox, responseProto, local, type, tcon, iidFolder).getResult();
            } else if (opStr.equals(OP_COPY)) {
                ItemId iidFolder = new ItemId(action.getAttribute(MailConstants.A_FOLDER), zsc);
                localResults = ItemActionHelper.COPY(octxt, mbox, responseProto, local, type, tcon, iidFolder).getResult();
        	} else if (opStr.equals(OP_SPAM)) {
        		String defaultFolder = (flagValue ? Mailbox.ID_FOLDER_SPAM : Mailbox.ID_FOLDER_INBOX) + "";
                ItemId iidFolder = new ItemId(action.getAttribute(MailConstants.A_FOLDER, defaultFolder), zsc);
        		localResults = ItemActionHelper.SPAM(octxt, mbox, responseProto, local, type, flagValue, tcon, iidFolder).getResult();
            } else if (opStr.equals(OP_TRASH)) {
                localResults = ItemActionHelper.TRASH(octxt, mbox, responseProto, local, type, tcon).getResult();
            } else if (opStr.equals(OP_RENAME)) {
                String name = action.getAttribute(MailConstants.A_NAME);
                ItemId iidFolder = new ItemId(action.getAttribute(MailConstants.A_FOLDER, "-1"), zsc);
                localResults = ItemActionHelper.RENAME(octxt, mbox, responseProto, local, type, tcon, name, iidFolder).getResult();
        	} else if (opStr.equals(OP_UPDATE)) {
                String folderId = action.getAttribute(MailConstants.A_FOLDER, null);
                ItemId iidFolder = new ItemId(folderId == null ? "-1" : folderId, zsc);
        		if (!iidFolder.belongsTo(mbox))
        			throw ServiceException.INVALID_REQUEST("cannot move item between mailboxes", null);
                else if (folderId != null && iidFolder.getId() <= 0)
                    throw MailServiceException.NO_SUCH_FOLDER(iidFolder.getId());
                String name  = action.getAttribute(MailConstants.A_NAME, null);
        		String flags = action.getAttribute(MailConstants.A_FLAGS, null);
        		String tags  = action.getAttribute(MailConstants.A_TAGS, null);
        		byte color   = (byte) action.getAttributeLong(MailConstants.A_COLOR, -1);
        		localResults = ItemActionHelper.UPDATE(octxt, mbox, responseProto, local, type, tcon, name, iidFolder, flags, 
        					tags, color).getResult();
        	} else {
        		throw ServiceException.INVALID_REQUEST("unknown operation: " + opStr, null);
        	}
        	successes.append(successes.length() > 0 ? "," : "").append(localResults);
        }

        return successes.toString();
    }

    static void partitionItems(ZimbraSoapContext zsc, String ids, List<Integer> local, Map<String, StringBuffer> remote) throws ServiceException {
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

    protected StringBuffer proxyRemoteItems(Element action, Map<String, StringBuffer> remote, Element request, Map<String, Object> context)
    throws ServiceException, SoapFaultException {
        // make sure that the folder ID is fully qualified for the proxy case
        String folderStr = action.getAttribute(MailConstants.A_FOLDER, null);
        if (folderStr != null)
            action.addAttribute(MailConstants.A_FOLDER, new ItemId(folderStr, getZimbraSoapContext(context)).toString());

        StringBuffer successes = new StringBuffer();
        for (Map.Entry<String, StringBuffer> entry : remote.entrySet()) {
            action.addAttribute(MailConstants.A_ID, entry.getValue().toString());
            Element response = proxyRequest(request, context, entry.getKey());
            String completed = extractSuccesses(response);
            successes.append(completed.length() > 0 && successes.length() > 0 ? "," : "").append(completed);
        }
        return successes;
    }

    protected String extractSuccesses(Element response) {
        try {
            return response.getElement(MailConstants.E_ACTION).getAttribute(MailConstants.A_ID);
        } catch (ServiceException e) {
            ZimbraLog.misc.warn("could not extract ItemAction successes from proxied response", e);
            return "";
        }
    }
}
