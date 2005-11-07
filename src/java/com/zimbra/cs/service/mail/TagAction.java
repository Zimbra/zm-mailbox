/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraContext;

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

    private static final Set TAG_OPS = new HashSet(Arrays.asList(new String[] {
        OP_RENAME
    }));

	public Element handle(Element request, Map context) throws ServiceException, SoapFaultException {
		ZimbraContext lc = getZimbraContext(context);

        Element action = request.getElement(MailService.E_ACTION);
        String operation = action.getAttribute(MailService.A_OPERATION).toLowerCase();

        if (operation.equals(OP_TAG) || operation.equals(OP_FLAG) || operation.equals(OP_UNTAG) || operation.equals(OP_UNFLAG))
            throw MailServiceException.CANNOT_TAG();
        if (operation.endsWith(OP_MOVE) || operation.endsWith(OP_UPDATE) || operation.endsWith(OP_SPAM))
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

    private String handleTag(Map context, Element request, String operation) throws ServiceException {
        Element action = request.getElement(MailService.E_ACTION);

        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();
        ItemId iid = new ItemId(action.getAttribute(MailService.A_ID), lc);

        if (operation.equals(OP_RENAME)) {
            String name = action.getAttribute(MailService.A_NAME);
            mbox.renameTag(octxt, iid.getId(), name);
        } else
            throw ServiceException.INVALID_REQUEST("unknown operation: " + operation, null);

        return iid.toString(lc);
    }
}
