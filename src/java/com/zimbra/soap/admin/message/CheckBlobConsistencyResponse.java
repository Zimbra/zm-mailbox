/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.MailboxBlobConsistency;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CHECK_BLOB_CONSISTENCY_RESPONSE)
public class CheckBlobConsistencyResponse {

    /**
     * @zm-api-field-description Information for mailboxes
     */
    @XmlElement(name=AdminConstants.E_MAILBOX /* mbox */, required=false)
    private List<MailboxBlobConsistency> mailboxes = Lists.newArrayList();

    public CheckBlobConsistencyResponse() {
    }

    public void setMailboxes(Iterable <MailboxBlobConsistency> mailboxes) {
        this.mailboxes.clear();
        if (mailboxes != null) {
            Iterables.addAll(this.mailboxes, mailboxes);
        }
    }

    public void addMailboxe(MailboxBlobConsistency mailboxe) {
        this.mailboxes.add(mailboxe);
    }

    public List<MailboxBlobConsistency> getMailboxes() {
        return Collections.unmodifiableList(mailboxes);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("mailboxes", mailboxes);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
