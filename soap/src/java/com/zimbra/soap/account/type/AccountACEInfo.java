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

package com.zimbra.soap.account.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.GranteeType;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class AccountACEInfo {

    /**
     * @zm-api-field-tag zimbra-id
     * @zm-api-field-description Zimbra ID of the grantee
     */
    @XmlAttribute(name=AccountConstants.A_ZIMBRA_ID /* zid */, required=false)
    private String zimbraId;

    /**
     * @zm-api-field-tag grantee-type
     * @zm-api-field-description The type of grantee:
     * <ul>
     * <li> <b>usr</b> - Zimbra user
     * <li> <b>grp</b> - Zimbra group(distribution list)
     * <li> <b>all</b> - all authenticated users
     * <li> <b>gst</b> - non-Zimbra email address and password (not yet supported)
     * <li> <b>key</b> - external user with an accesskey
     * <li> <b>pub</b> - public authenticated and unauthenticated access
     * </ul>
     *
     * If the value is:
     * <ul>
     * <li> <b>usr</b> - either <b>{zimbra-id}</b> or <b>{grantee-name}</b> is required
     * <li> <b>grp</b> - either <b>{zimbra-id}</b> or <b>{grantee-name}</b> is required
     * <li> <b>all</b> - <b>{zimbra-id}</b>, <b>{grantee-name}</b> and <b>{password}</b> are ignored
     * <li> <b>gst</b> - <b>{zimbra-id}</b> is ignored, <b>{grantee-name}</b> is required, <b>{password}</b> is optional
     * <li> <b>key</b> - <b>{zimbra-id}</b> is ignored, <b>{grantee-name}</b> is required
     * <li> <b>pub</b> - <b>{zimbra-id}</b>, <b>{grantee-name}</b> and <b>{password}</b> are ignored
     * </ul>
     * For <b>usr</b> and <b>grp</b>:
     * <ul>
     * <li> if <b>{zimbra-id}</b> is provided, server will lookup the entry by <b>{zimbra-id}</b> and
     * <li> if <b>{zimbra-id}</b> is not provided, server will lookup the grantee by <b>{grantee-type}</b> and
     *      <b>{grantee-name}</b>
     * <li>if the lookup fails, NO_SUCH_ACCOUNT/NO_SUCH_DISTRIBUTION_LIST will be thrown.
     * </ul>
     * If <b>{grantee-type}</b> == <b>key</b>:
     * <ul>
     * <li> if key is given, server will use that as the access key for this grant
     * <li> if key is not given, server will generate an access key
     * </ul>
     * If <b>chkgt<b> is "1 (true)", INVALID_REQUEST will be thrown if wrong grantee type is specified.
     * 
     */
    @XmlAttribute(name=AccountConstants.A_GRANT_TYPE /* gt */, required=true)
    private final GranteeType granteeType;

    /**
     * @zm-api-field-description Right.
     * <br />
     * Valid values: <b>viewFreeBusy</b> | <b>invite</b>
     */
    @XmlAttribute(name=AccountConstants.A_RIGHT /* right */, required=true)
    private final String right;

    /**
     * @zm-api-field-tag grantee-name
     * @zm-api-field-description Name or email address of the grantee.
     * <br />
     * Not present if <b>{grantee-type}</b> is "all" or "pub"
     */
    @XmlAttribute(name=AccountConstants.A_DISPLAY /* d */, required=false)
    private String displayName;

    /**
     * @zm-api-field-description Optional access key when <b>{grantee-type}</b> is "key"
     */
    @XmlAttribute(name=AccountConstants.A_ACCESSKEY /* key */, required=false)
    private String accessKey;

    /**
     * @zm-api-field-tag password
     * @zm-api-field-description Password when <b>{grantee-type}</b> is <b>"gst"</b> <b><i>(not yet supported)</i></b>
     */
    @XmlAttribute(name=AccountConstants.A_PASSWORD /* pw */, required=false)
    private String password;

    /**
     * @zm-api-field-description "1" if a right is specifically denied or "0" (default)
     */
    @XmlAttribute(name=AccountConstants.A_DENY /* deny */, required=false)
    private ZmBoolean deny;

    /**
     * @zm-api-field-description "1 (true)" if check grantee type or "0 (false)" (default)
     */
    @XmlAttribute(name=AccountConstants.A_CHECK_GRANTEE_TYPE /* chkgt */, required=false)
    private ZmBoolean checkGranteeType;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AccountACEInfo() {
        this((GranteeType) null, (String) null);
    }

    public AccountACEInfo(GranteeType granteeType, String right) {
        this.granteeType = granteeType;
        this.right = right;
    }

    public void setZimbraId(String zimbraId) { this.zimbraId = zimbraId; }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public void setPassword(String password) { this.password = password; }
    public void setDeny(Boolean deny) { this.deny = ZmBoolean.fromBool(deny); }
    public void setCheckGranteeType(Boolean chkgt) { this.checkGranteeType = ZmBoolean.fromBool(chkgt); }
    public String getZimbraId() { return zimbraId; }
    public GranteeType getGranteeType() { return granteeType; }
    public String getRight() { return right; }
    public String getDisplayName() { return displayName; }
    public String getAccessKey() { return accessKey; }
    public String getPassword() { return password; }
    public Boolean getDeny() { return ZmBoolean.toBool(deny); }
    public Boolean getCheckGranteeType() { return ZmBoolean.toBool(checkGranteeType); }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("zimbraId", zimbraId)
            .add("granteeType", granteeType)
            .add("right", right)
            .add("displayName", displayName)
            .add("accessKey", accessKey)
            .add("password", password)
            .add("deny", deny)
            .add("checkGranteeType", checkGranteeType);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
