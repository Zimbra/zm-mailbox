/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class TagAction extends ItemAction  {

    protected String[] getProxiedIdPath(Element request) {
        String operation = getXPath(request, OPERATION_PATH);
        if (operation != null && TAG_OPS.contains(operation.toLowerCase()))
            return TARGET_ITEM_PATH;
        return super.getProxiedIdPath(request);
    }

    public static final String OP_UNFLAG = '!' + OP_FLAG;
    public static final String OP_UNTAG  = '!' + OP_TAG;
	public static final String OP_RENAME = "rename";

    private static final Set<String> TAG_OPS = new HashSet<String>(Arrays.asList(new String[] {
        OP_RENAME, OP_UPDATE
    }));

	public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
		ZimbraSoapContext lc = getZimbraSoapContext(context);

        Element action = request.getElement(MailService.E_ACTION);
        String operation = action.getAttribute(MailService.A_OPERATION).toLowerCase();

        if (operation.equals(OP_TAG) || operation.equals(OP_FLAG) || operation.equals(OP_UNTAG) || operation.equals(OP_UNFLAG))
            throw ServiceException.INVALID_REQUEST("cannot tag/flag a tag", null);
        if (operation.endsWith(OP_MOVE) || operation.endsWith(OP_SPAM))
            throw ServiceException.INVALID_REQUEST("invalid operation on tag: " + operation, null);
        String successes;
        if (TAG_OPS.contains(operation))
            successes = handleTag(context, request, operation);
        else
            successes = handleCommon(context, request, operation, MailItem.TYPE_TAG);

        Element response = lc.createElement(MailService.TAG_ACTION_RESPONSE);
    	Element result = response.addUniqueElement(MailService.E_ACTION);
    	result.addAttribute(MailService.A_ID, successes);
    	result.addAttribute(MailService.A_OPERATION, operation);
        return response;
	}

    private String handleTag(Map<String, Object> context, Element request, String operation) throws ServiceException {
        Element action = request.getElement(MailService.E_ACTION);

        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();
        ItemId iid = new ItemId(action.getAttribute(MailService.A_ID), lc);

        if (operation.equals(OP_RENAME)) {
            String name = action.getAttribute(MailService.A_NAME);
            mbox.renameTag(octxt, iid.getId(), name);
        } else if (operation.equals(OP_UPDATE)) {
            String name = action.getAttribute(MailService.A_NAME, null);
            byte color = (byte) action.getAttributeLong(MailService.A_COLOR, -1);
            if (name != null)
                mbox.renameTag(octxt, iid.getId(), name);
            if (color >= 0)
                mbox.setColor(octxt, iid.getId(), MailItem.TYPE_TAG, color);
        } else {
            throw ServiceException.INVALID_REQUEST("unknown operation: " + operation, null);
        }

        return lc.formatItemId(iid);
    }
}
