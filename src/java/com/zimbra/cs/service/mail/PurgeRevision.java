/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;

public class PurgeRevision extends MailDocumentHandler {

    private static final String[] TARGET_PATH = new String[] { MailConstants.E_REVISION, MailConstants.A_ID };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_PATH; }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Element revisionElem = request.getElement(MailConstants.E_REVISION);
        ItemId iid = new ItemId(revisionElem.getAttribute(MailConstants.A_ID), zsc);
        int rev = (int)revisionElem.getAttributeLong(MailConstants.A_VERSION);
        boolean includeOlderRevisions = revisionElem.getAttributeBool(MailConstants.A_INCLUDE_OLDER_REVISIONS, false);
        Mailbox mbox = getRequestedMailbox(zsc);
        mbox.purgeRevision(getOperationContext(zsc, context), iid.getId(), rev, includeOlderRevisions);
        
        return zsc.createElement(MailConstants.PURGE_REVISION_RESPONSE);
    }
}
