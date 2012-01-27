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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.admin.type.ExportMailboxSelector;

/**
 * @zm-api-command-description Export Mailbox (OLD mailbox move mechanism)
 * <br />
 * This request blocks until mailbox move is complete and can take a long time.  Client side should set timeout
 * accordingly.
 * <br />
 * Note: This is the old mailbox move request.  The new mailbox move request is <b>MoveMailboxRequest</b>.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=BackupConstants.E_EXPORTMAILBOX_REQUEST)
public class ExportMailboxRequest {

    /**
     * @zm-api-field-description Export Mailbox details
     */
    @XmlElement(name=BackupConstants.E_ACCOUNT /* account */, required=true)
    private final ExportMailboxSelector account;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ExportMailboxRequest() {
        this((ExportMailboxSelector) null);
    }

    public ExportMailboxRequest(ExportMailboxSelector account) {
        this.account = account;
    }

    public ExportMailboxSelector getAccount() { return account; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("account", account);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
