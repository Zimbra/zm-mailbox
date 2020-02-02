/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
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
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Manage index for Delayed Index feature. When delete-index is specified, all index data
 * of an account is removed. When enable-indexing is specified, zimbraDelayedIndexStatus is set to indexing and index
 * data is created on an account. delete-index and enable-indexing are exclusive and one of them needs to be specified.
 * <br />
 * <b>Access</b>: domain admin sufficient
 * <br />
 * note: this request is by default proxied to the account's home server
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_MANAGE_INDEX_REQUEST)
public class ManageIndexRequest {

    /**
     * @zm-api-field-tag "start|status|cancel"
     * @zm-api-field-description Action to perform
     * <table>
     * <tr> <td> <b>start</b> </td> <td> start management </td> </tr>
     * <tr> <td> <b>status</b> </td> <td> show management progress </td> </tr>
     * <tr> <td> <b>cancel</b> </td> <td> cancel management </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.E_ACTION, required=false)
    private final String action;

    /**
     * @zm-api-field-description Specify mailbox to manage
     */
    @XmlElement(name=AdminConstants.E_MAILBOX, required=true)
    private final MailboxByAccountIdSelector mbox;

    /**
     * @zm-api-field-tag delete-index
     * @zm-api-field-description Just delete index without reindexing. It cannot be canceled.
     */
    @XmlAttribute(name=AdminConstants.A_DELETE_INDEX, required=false)
    private ZmBoolean isDeleteIndex;

    /**
     * @zm-api-field-tag enable-indexing
     * @zm-api-field-description Set zimbraDelayedIndexStatus to indexing when zimbraFeatureDelayedIndexEnabled is TRUE.
     */
    @XmlAttribute(name=AdminConstants.A_ENABLE_INDEXING, required=false)
    private ZmBoolean enableIndexing;

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
        this.isDeleteIndex = ZmBoolean.fromBool(false);
        this.enableIndexing = ZmBoolean.fromBool(false);
    }

    public String getAction() { return action; }
    public MailboxByAccountIdSelector getMbox() { return mbox; }

    public void setDeleteIndex(Boolean isDeleteIndex) {
        this.isDeleteIndex = ZmBoolean.fromBool(isDeleteIndex);
    }
    public boolean getDeleteIndex() {return ZmBoolean.toBool(isDeleteIndex); }

    public void setEnableIndexing(Boolean enableIndexing) {
        this.enableIndexing = ZmBoolean.fromBool(enableIndexing);
    }
    public boolean getEnableIndexing() {return ZmBoolean.toBool(enableIndexing); }
}
