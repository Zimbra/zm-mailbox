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
