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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.ReindexMailboxInfo;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description ReIndex
 * <br />
 * <b>Access</b>: domain admin sufficient
 * <br />
 * note: this request is by default proxied to the account's home server
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_REINDEX_REQUEST)
public class ReIndexRequest {

    /**
     * @zm-api-field-tag "start|status|cancel"
     * @zm-api-field-description Action to perform
     * <table>
     * <tr> <td> <b>start</b> </td> <td> start reindexing </td> </tr>
     * <tr> <td> <b>status</b> </td> <td> show reindexing progress </td> </tr>
     * <tr> <td> <b>cancel</b> </td> <td> cancel reindexing </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.E_ACTION, required=false)
    private final String action;

    /**
     * @zm-api-field-description Specify reindexing to perform
     * <br />
     * Note: Only one of <b>{ids-comma-sep}</b> and <b>{types-comma-sep}</b> may be specified.
     */
    @XmlElement(name=AdminConstants.E_MAILBOX, required=true)
    private final ReindexMailboxInfo mbox;

    /**
     * @zm-api-field-tag isDeleteOnly
     * @zm-api-field-description Just delete index without reindexing. It cannot be canceled.</b>
     */
    @XmlAttribute(name=AdminConstants.A_DELETE_ONLY, required=false)
    private ZmBoolean isDeleteOnly;

    /**
     * no-argument constructor wanted by JAXB
     */
     @SuppressWarnings("unused")
    private ReIndexRequest() {
        this((String)null, (ReindexMailboxInfo)null);
    }

    public ReIndexRequest(String action, ReindexMailboxInfo mbox) {
        this.action = action;
        this.mbox = mbox;
    }

    public String getAction() { return action; }
    public ReindexMailboxInfo getMbox() { return mbox; }

    public void setDeleteOnly(Boolean deleteOnly) {
        this.isDeleteOnly = ZmBoolean.fromBool(deleteOnly);
    }
    public boolean getDeleteOnly() {return ZmBoolean.toBool(isDeleteOnly); }
}
