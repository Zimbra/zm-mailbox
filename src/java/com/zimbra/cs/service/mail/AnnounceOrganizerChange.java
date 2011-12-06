/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;

public class AnnounceOrganizerChange extends CalendarRequest {

    private static final String[] TARGET_PATH = new String[] { MailConstants.A_ID };

    @Override
    protected String[] getProxiedIdPath(Element request) {
        return TARGET_PATH;
    }

    @Override
    protected boolean checkMountpointProxy(Element request) {
        return false;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(zsc);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemId iid = new ItemId(request.getAttribute(MailConstants.A_ID), zsc);

        MailSendQueue sendQueue = new MailSendQueue();
        Element response = getResponseElement(zsc);
        mbox.lock.lock();
        try {
            CalendarItem calItem = mbox.getCalendarItemById(octxt, iid.getId());
            sendOrganizerChangeMessage(zsc, octxt, calItem, acct, mbox, sendQueue);
        } finally {
            mbox.lock.release();
            sendQueue.send();
        }
        return response;
    }
}
