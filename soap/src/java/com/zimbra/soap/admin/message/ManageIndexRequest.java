/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2023 Synacor, Inc.
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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.MailboxByAccountIdSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Manage index for Delayed Index feature. When disableIndexing is specified,
 * zimbraFeatureDelayedIndexEnabled is set to TRUE, zimbraDelayedIndexStatus is set to suppressed, and index data
 * except Contacts is removed. When enableIndexing is specified, zimbraDelayedIndexStatus is set to indexing and
 * index data is created.
 * <br />
 * <b>Access</b>: domain admin sufficient
 * <br />
 * note: this request is by default proxied to the account's home server
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_MANAGE_INDEX_REQUEST)
public class ManageIndexRequest {

    /**
     * @zm-api-field-description Specify mailbox to manage
     */
    @XmlElement(name=AdminConstants.E_MAILBOX, required=true)
    private final MailboxByAccountIdSelector mbox;

    /**
     * @zm-api-field-tag "disableIndexing|enableIndexing"
     * @zm-api-field-description Action to perform
     * <table>
     * <tr> <td> <b>disableIndexing</b> </td> <td> disable indexing and delete index </td> </tr>
     * <tr> <td> <b>enableIndexing</b> </td> <td> enable indexing and create index </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.E_ACTION, required=true)
    private final String action;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ManageIndexRequest() {
        this((String)null, (MailboxByAccountIdSelector)null);
    }

    public ManageIndexRequest(String action, MailboxByAccountIdSelector mbox) {
        this.action = action;
        this.mbox = mbox;
    }

    public String getAction() { return action; }
    public MailboxByAccountIdSelector getMbox() { return mbox; }
}
