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

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.admin.type.MailboxVolumesInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=BackupConstants.E_GET_MAILBOX_VOLUMES_RESPONSE)
@XmlType(propOrder = {})
public class GetMailboxVolumesResponse {

    /**
     * @zm-api-field-description Mailbox Volume Information
     */
    @XmlElement(name=BackupConstants.E_ACCOUNT /* account */, required=true)
    private MailboxVolumesInfo account;

    private GetMailboxVolumesResponse() {
    }

    private GetMailboxVolumesResponse(MailboxVolumesInfo account) {
        setAccount(account);
    }

    public static GetMailboxVolumesResponse create(MailboxVolumesInfo account) {
        return new GetMailboxVolumesResponse(account);
    }

    public void setAccount(MailboxVolumesInfo account) { this.account = account; }
    public MailboxVolumesInfo getAccount() { return account; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("account", account);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
