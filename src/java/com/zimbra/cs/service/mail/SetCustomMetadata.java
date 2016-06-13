/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;

public class SetCustomMetadata extends MailDocumentHandler {
    private static final String[] TARGET_ITEM_PATH = new String[] { MailConstants.A_ID };
    @Override protected String[] getProxiedIdPath(Element request)     { return TARGET_ITEM_PATH; }
    @Override protected boolean checkMountpointProxy(Element request)  { return false; }

    @Override public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        Element meta = request.getElement(MailConstants.E_METADATA);
        String section = meta.getAttribute(MailConstants.A_SECTION);
        section = section.trim();
        if (section.length() == 0 || section.length() > 36)
            throw ServiceException.INVALID_REQUEST("invalid length for custom metadata section name", null);

        CustomMetadata custom = new CustomMetadata(section);
        for (Element.KeyValuePair kvp : meta.listKeyValuePairs())
            custom.put(kvp.getKey(), kvp.getValue());

        ItemId iid = new ItemId(request.getAttribute(MailConstants.A_ID), zsc);
        mbox.setCustomData(octxt, iid.getId(), MailItem.Type.UNKNOWN, custom);

        Element response = zsc.createElement(MailConstants.SET_METADATA_RESPONSE);
        response.addAttribute(MailConstants.A_ID, ifmt.formatItemId(iid));
        return response;
    }
}
