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
 * Created on Feb 22, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author tim
 */
public class CreateCalendarItem extends CalendarRequest {
    
    private static Log sLog = LogFactory.getLog(CreateCalendarItem.class);

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.E_MSG, MailConstants.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return true; }
    protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

    // very simple: generate a new UID and send a REQUEST
    protected class CreateCalendarItemInviteParser extends ParseMimeMessage.InviteParser { 
        public ParseMimeMessage.InviteParserResult parseInviteElement(ZimbraSoapContext lc, OperationContext octxt, Account account, Element inviteElem) throws ServiceException 
        {
            return CalendarUtils.parseInviteForCreate(account, getItemType(), inviteElem, null, null, false, CalendarUtils.RECUR_ALLOWED);
        }
    };

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(zsc);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        // <M>
        Element msgElem = request.getElement(MailConstants.E_MSG);

        CreateCalendarItemInviteParser parser = new CreateCalendarItemInviteParser();
        CalSendData dat = handleMsgElement(zsc, octxt, msgElem, acct, mbox, parser);

        int defaultFolder = dat.mInvite.isTodo() ? Mailbox.ID_FOLDER_TASKS : Mailbox.ID_FOLDER_CALENDAR;
        String defaultFolderStr = Integer.toString(defaultFolder);
        String folderIdStr = msgElem.getAttribute(MailConstants.A_FOLDER, defaultFolderStr);
        ItemId iidFolder = new ItemId(folderIdStr, zsc);
        sLog.info("<CreateCalendarItem folder=" + iidFolder.getId() + "> " + zsc.toString());
        
        Element response = getResponseElement(zsc);
        return sendCalendarMessage(zsc, octxt, iidFolder.getId(), acct, mbox, dat, response, false);
    }
}
