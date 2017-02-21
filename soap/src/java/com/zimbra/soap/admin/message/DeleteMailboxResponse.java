/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.MailboxWithMailboxId;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_DELETE_MAILBOX_RESPONSE)
@XmlType(propOrder = {})
public class DeleteMailboxResponse {

    /**
     * @zm-api-field-description Details of the deleted mailbox.
     * <br />
     * Tthe <b>&lt;mbox></b> element is left out of the response if no mailbox existed for that account
     */
    @XmlElement(name=AdminConstants.E_MAILBOX, required=false)
    private MailboxWithMailboxId mbox;

    public DeleteMailboxResponse() {
    }

    public void setMbox(MailboxWithMailboxId mbox) {
        this.mbox = mbox;
    }

    public MailboxWithMailboxId getMbox() { return mbox; }
}
