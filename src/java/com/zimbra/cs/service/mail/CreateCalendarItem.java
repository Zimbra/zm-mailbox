/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
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
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.Element;
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

    static final String DEFAULT_FOLDER = "" + Mailbox.ID_FOLDER_CALENDAR;

    // very simple: generate a new UID and send a REQUEST
    protected class CreateCalendarItemInviteParser extends ParseMimeMessage.InviteParser { 
        public ParseMimeMessage.InviteParserResult parseInviteElement(ZimbraSoapContext lc, Account account, Element inviteElem) throws ServiceException 
        {
            return CalendarUtils.parseInviteForCreate(account, getItemType(), inviteElem, null, null, false, CalendarUtils.RECUR_ALLOWED);
        }
    };

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(lc);
        Mailbox mbox = getRequestedMailbox(lc);
        
        // <M>
        Element msgElem = request.getElement(MailConstants.E_MSG);
        
        // no existing calendar item referenced -- this is a new create!
        String folderIdStr = msgElem.getAttribute(MailConstants.A_FOLDER, DEFAULT_FOLDER);
        ItemId iidFolder = new ItemId(folderIdStr, lc);
        sLog.info("<CreateCalendarItem folder=" + iidFolder.getId() + "> " + lc.toString());
        
        CreateCalendarItemInviteParser parser = new CreateCalendarItemInviteParser();
        CalSendData dat = handleMsgElement(lc, msgElem, acct, mbox, parser);
        
        Element response = getResponseElement(lc);
        return sendCalendarMessage(lc, iidFolder.getId(), acct, mbox, dat, response, false);
    }
}
