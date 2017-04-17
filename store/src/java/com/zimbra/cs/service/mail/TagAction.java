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

import java.util.Map;
import java.util.Set;
import com.google.common.base.Joiner;

import com.google.common.collect.ImmutableSet;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.util.TagUtil;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.type.RetentionPolicy;

/**
 * @since May 26, 2004
 * @author schemers
 */
public class TagAction extends ItemAction  {

    public static final String OP_RETENTION_POLICY = "retentionpolicy";

    private static final Set<String> TAG_ACTIONS = ImmutableSet.of(OP_READ, OP_COLOR, OP_HARD_DELETE, OP_RENAME, OP_UPDATE, OP_RETENTION_POLICY);

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        Element action = request.getElement(MailConstants.E_ACTION);
        String opAttr = action.getAttribute(MailConstants.A_OPERATION).toLowerCase();
        String operation = getOperation(opAttr);

        if (operation.equals(OP_TAG) || operation.equals(OP_FLAG)) {
            throw ServiceException.INVALID_REQUEST("cannot tag/flag a tag", null);
        }
        if (!TAG_ACTIONS.contains(operation)) {
            throw ServiceException.INVALID_REQUEST("invalid operation on tag: " + opAttr, null);
        }

        String tn = action.getAttribute(MailConstants.A_TAG_NAMES, null);
        if (tn != null) {
            // switch tag names to tag IDs, because that's what ItemAction expects
            StringBuilder tagids = new StringBuilder();
            for (String name : TagUtil.decodeTags(tn)) {
                tagids.append(tagids.length() == 0 ? "" : ",").append(mbox.getTagByName(octxt, name).getId());
            }
            action.addAttribute(MailConstants.A_ID, tagids.toString());
        }

        String successes;
        if (operation.equals(OP_RETENTION_POLICY)) {
            ItemId iid = new ItemId(action.getAttribute(MailConstants.A_ID), zsc);
            RetentionPolicy rp = new RetentionPolicy(action.getElement(MailConstants.E_RETENTION_POLICY));
            mbox.setRetentionPolicy(octxt, iid.getId(), MailItem.Type.TAG, rp);
            successes = new ItemIdFormatter(zsc).formatItemId(iid);
        } else {
            successes = Joiner.on(",").join(handleCommon(context, request, MailItem.Type.TAG).getSuccessIds());
        }

        Element response = zsc.createElement(MailConstants.TAG_ACTION_RESPONSE);
        Element result = response.addUniqueElement(MailConstants.E_ACTION);
        result.addAttribute(MailConstants.A_ID, successes);
        result.addAttribute(MailConstants.A_TAG_NAMES, tn);
        result.addAttribute(MailConstants.A_OPERATION, opAttr);
        return response;
    }
}
