/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.admin.message;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Lists;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.MailboxWithMailboxId;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_PURGE_MESSAGES_RESPONSE)
public class PurgeMessagesResponse {

    /**
     * @zm-api-field-description Information about mailboxes where aged messages have been purged
     */
    @XmlElement(name=AdminConstants.E_MAILBOX, required=false)
    private List <MailboxWithMailboxId> mailboxes = Lists.newArrayList();

    public PurgeMessagesResponse() {
    }

    public PurgeMessagesResponse setMailboxes(
            Collection<MailboxWithMailboxId> mailboxes) {
        this.mailboxes.clear();
        if (mailboxes != null) {
            this.mailboxes.addAll(mailboxes);
        }
        return this;
    }

    public PurgeMessagesResponse addMailbox(MailboxWithMailboxId attr) {
        mailboxes.add(attr);
        return this;
    }

    public List<MailboxWithMailboxId> getMailboxes() {
        return Collections.unmodifiableList(mailboxes);
    }
}
