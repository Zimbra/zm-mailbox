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

package com.zimbra.soap.account.message;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Synchronize with the Global Address List
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_SYNC_GAL_REQUEST)
public class SyncGalRequest {

    /**
     * @zm-api-field-tag previous-token
     * @zm-api-field-description The previous synchronization token if applicable
     */
    @XmlAttribute(name=MailConstants.A_TOKEN /* token */, required=false)
    private String token;

    /**
     * @zm-api-field-tag gal-sync-account-id
     * @zm-api-field-description GAL sync account ID
     */
    @XmlAttribute(name=AccountConstants.A_GAL_ACCOUNT_ID /* galAcctId */, required=false)
    private String galAccountId;

    /**
     * @zm-api-field-description Flag whether only the ID attributes for matching contacts should be returned.
     * <br />
     * If the request has <b>idOnly</b> set to 1 (true), then the response will contain the id attribute without all
     * the contact fields populated.  The sync client then should make a batch request to the server to fetch the
     * contact fields with <b>&lt;GetContactsRequest/></b>.
     * <br />
     * Note: <b>idOnly</b> only works when GAL sync account is configured/enabled.  If <b>idOnly</b> is specified and
     * GAL sync account is not enabled, <b>idOnly</b> will be ignored.
     */
    @XmlAttribute(name=AccountConstants.A_ID_ONLY /* idOnly */, required=false)
    private ZmBoolean idOnly;

    /**
     * @zm-api-field-tag getCount.
     * @zm-api-field-description Flag whether count of remaining records should be returned in response or not.
     * Note: <b>getCount</b> works only when idOnly is set to true and GAL sync account is configured/enabled.
     */
    @XmlAttribute(name=AccountConstants.A_GET_COUNT /* getCount */, required=false)
    private ZmBoolean getCount;

    /**
     * @zm-api-field-tag limit
     * @zm-api-field-description Page size control for SyncGalRequest. The maximum entries that can be returned for
 every SyncGal Request can be controlled by specifying this limit.
     */
    @XmlAttribute(name=MailConstants.A_LIMIT /* limit */, required=false)
    private Integer limit;

    public SyncGalRequest() {
    }

    public void setToken(String token) { this.token = token; }
    public void setGalAccountId(String galAccountId) { this.galAccountId = galAccountId; }
    public void setIdOnly(Boolean idOnly) { this.idOnly = ZmBoolean.fromBool(idOnly); }
    public void setLimit(int limit) { this.limit = limit; }
    public String getToken() { return token; }
    public String getGalAccountId() { return galAccountId; }
    public Boolean getIdOnly() { return ZmBoolean.toBool(idOnly); }
    public Integer getLimit() { return (limit != null ? limit : 0); }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("token", token)
            .add("galAccountId", galAccountId)
            .add("idOnly", idOnly)
            .add("getCount", getCount)
            .add("limit", limit);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
