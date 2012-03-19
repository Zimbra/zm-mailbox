/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.lmtpserver;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;

public interface LmtpCallback {

    /**
     * Called after the message is delivered to the given account.
     */
    public void afterDelivery(Account account, Mailbox mbox, String envelopeSender, String recipientEmail, Message newMessage);

    /**
     * Called when mail forwarding is set up for the account but delivery to mailbox is disabled.
     */
    public void forwardWithoutDelivery(Account account, Mailbox mbox, String envelopeSender, String recipientEmail, ParsedMessage pm);
}
