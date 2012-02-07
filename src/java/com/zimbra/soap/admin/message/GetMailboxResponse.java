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
import com.zimbra.soap.admin.type.MailboxWithMailboxId;

@XmlRootElement(name=AdminConstants.E_GET_MAILBOX_RESPONSE)
public class GetMailboxResponse {

    /**
     * @zm-api-field-description Information about mailbox
     */
    @XmlElement(name=AdminConstants.E_MAILBOX, required=true)
    private final MailboxWithMailboxId mbox;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetMailboxResponse() {
        this(null);
    }

    public GetMailboxResponse(MailboxWithMailboxId mbox) {
        this.mbox = mbox;
    }

    public MailboxWithMailboxId getMbox() { return mbox; }
}
