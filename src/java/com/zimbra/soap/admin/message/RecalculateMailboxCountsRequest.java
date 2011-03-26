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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.MailboxByAccountIdSelector;

@XmlRootElement(name=AdminConstants.E_RECALCULATE_MAILBOX_COUNTS_REQUEST)
public class RecalculateMailboxCountsRequest {

    public enum Action {
        ALL, FOLDER_TAG, MAIL_ADDRESS
    }

    @XmlElement(name=AdminConstants.E_MAILBOX, required=true)
    private final MailboxByAccountIdSelector mbox;

    @XmlElement(name=AdminConstants.A_ACTION)
    private final Action action;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private RecalculateMailboxCountsRequest() {
        mbox = null;
        action = null;
    }

    public RecalculateMailboxCountsRequest(MailboxByAccountIdSelector mbox, Action action) {
        this.mbox = mbox;
        this.action = action;
    }

    public MailboxByAccountIdSelector getMbox() {
        return mbox;
    }

    public Action getAction() {
        return action;
    }
}
