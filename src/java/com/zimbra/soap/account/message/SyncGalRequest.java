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

package com.zimbra.soap.account.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_SYNC_GAL_REQUEST)
public class SyncGalRequest {

    @XmlAttribute(name=MailConstants.A_TOKEN /* token */, required=false)
    private String token;

    @XmlAttribute(name=AccountConstants.A_GAL_ACCOUNT_ID /* galAcctId */, required=false)
    private String galAccountId;

    @XmlAttribute(name=AccountConstants.A_ID_ONLY /* idOnly */, required=false)
    private ZmBoolean idOnly;

    public SyncGalRequest() {
    }

    public void setToken(String token) { this.token = token; }
    public void setGalAccountId(String galAccountId) { this.galAccountId = galAccountId; }
    public void setIdOnly(Boolean idOnly) { this.idOnly = ZmBoolean.fromBool(idOnly); }
    public String getToken() { return token; }
    public String getGalAccountId() { return galAccountId; }
    public Boolean getIdOnly() { return ZmBoolean.toBool(idOnly); }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("token", token)
            .add("galAccountId", galAccountId)
            .add("idOnly", idOnly);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
