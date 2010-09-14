/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
        int rev = (int)revisionElem.getAttributeLong(MailConstants.A_REVISION);
        boolean includeOlderRevisions = revisionElem.getAttributeBool(MailConstants.A_INCLUDE_OLDER_REVISIONS, false);
        Mailbox mbox = getRequestedMailbox(zsc);
        mbox.purgeRevision(getOperationContext(zsc, context), iid.getId(), rev, includeOlderRevisions);
        
        return zsc.createElement(MailConstants.PURGE_REVISION_RESPONSE);
    }
}
