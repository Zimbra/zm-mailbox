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
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.type.RetentionPolicy;

/**
 * @since May 26, 2004
 * @author schemers
 */
public class TagAction extends ItemAction  {

    public static final String OP_UNFLAG = '!' + OP_FLAG;
    public static final String OP_UNTAG  = '!' + OP_TAG;
    public static final String OP_RETENTION_POLICY = "retentionpolicy";

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Element action = request.getElement(MailConstants.E_ACTION);
        String operation = action.getAttribute(MailConstants.A_OPERATION).toLowerCase();

        if (operation.equals(OP_TAG) || operation.equals(OP_FLAG) || operation.equals(OP_UNTAG) || operation.equals(OP_UNFLAG)) {
            throw ServiceException.INVALID_REQUEST("cannot tag/flag a tag", null);
        }
        if (operation.endsWith(OP_MOVE) || operation.endsWith(OP_COPY) || operation.endsWith(OP_SPAM) || operation.endsWith(OP_TRASH)) {
            throw ServiceException.INVALID_REQUEST("invalid operation on tag: " + operation, null);
        }
        
        String successes;
        if (operation.equals(OP_RETENTION_POLICY)) {
            Mailbox mbox = getRequestedMailbox(zsc);
            OperationContext octxt = getOperationContext(zsc, context);
            ItemIdFormatter ifmt = new ItemIdFormatter(zsc);
            ItemId iid = new ItemId(action.getAttribute(MailConstants.A_ID), zsc);
            mbox.setRetentionPolicy(octxt, iid.getId(), MailItem.Type.TAG,
                new RetentionPolicy(action.getElement(MailConstants.E_RETENTION_POLICY)));
            successes = ifmt.formatItemId(iid);
        } else {
            successes = handleCommon(context, request, operation, MailItem.Type.TAG);
        }

        Element response = zsc.createElement(MailConstants.TAG_ACTION_RESPONSE);
        Element result = response.addUniqueElement(MailConstants.E_ACTION);
        result.addAttribute(MailConstants.A_ID, successes);
        result.addAttribute(MailConstants.A_OPERATION, operation);
        return response;
    }
}
