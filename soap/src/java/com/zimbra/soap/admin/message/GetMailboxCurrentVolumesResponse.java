/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2022 Zimbra, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.admin.type.MailboxVolumesInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=BackupConstants.E_GET_MAILBOX_CURRENT_VOLUMES_RESPONSE)
@XmlType(propOrder = {})
public class GetMailboxCurrentVolumesResponse {
    /**
     * @zm-api-field-description Mailbox Volume Information
     */
    @XmlElement(name=BackupConstants.E_ACCOUNT /* account */, required=true)
    private MailboxVolumesInfo account;

    private GetMailboxCurrentVolumesResponse() {
    }

    private GetMailboxCurrentVolumesResponse(MailboxVolumesInfo account) {
        setAccount(account);
    }

    public static GetMailboxCurrentVolumesResponse create(MailboxVolumesInfo account) {
        return new GetMailboxCurrentVolumesResponse(account);
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
