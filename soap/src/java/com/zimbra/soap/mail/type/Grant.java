/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.GrantGranteeType;

import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

/*
 * Delete this class in bug 66989
 */

/*
<grant perm="{rights}" gt="{grantee-type}" zid="{zimbra-id}" [expiry="{millis-since-epoch}"] [d="{grantee-name}"] [pw="{password-for-guest}"] [key=="{access-key}"]/>*
 */
@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name="Grant", description="A grant")
public class Grant {

    /**
     * @zm-api-field-tag rights
     * @zm-api-field-description Rights - Some combination of (r)ead, (w)rite, (i)nsert, (d)elete, (a)dminister,
     * workflow action (x), view (p)rivate, view (f)reebusy, (c)reate subfolder
     */
    @XmlAttribute(name=MailConstants.A_RIGHTS /* perm */, required=true)
    @GraphQLNonNull
    @GraphQLQuery(name="permissions", description="Rights - Some combination of (r)ead, (w)rite, (i)nsert, (d)elete, (a)dminister, workflow action (x), view (p)rivate, view (f)reebusy, (c)reate subfolder")
    private String perm;

    /**
     * @zm-api-field-tag grantee-type
     * @zm-api-field-description The type of Grantee:
     * <pre>
     *     "usr",
     *     "grp",
     *     "dom" (domain),
     *     "cos",
     *     "all" (all authenticated users), "pub" (public authenticated and unauthenticated access),
     *     "guest" (non-Zimbra email address and password),
     *     "key" (non-Zimbra email address and access key)
     * </pre>
     */
    @XmlAttribute(name=MailConstants.A_GRANT_TYPE /* gt */, required=true)
    @GraphQLNonNull
    @GraphQLQuery(name="granteeType", description="The type of grantee")
    private GrantGranteeType granteeType;

    /**
     * @zm-api-field-tag grantee-id
     * @zm-api-field-description Grantee ID
     */
    @XmlAttribute(name=MailConstants.A_ZIMBRA_ID /* zid */, required=true)
    @GraphQLNonNull
    @GraphQLQuery(name="granteeId", description="The grantee id")
    private String granteeId;

    /**
     * @zm-api-field-tag millis-since-epoch
     * @zm-api-field-description Time when this grant expires.
     * For internal/guest grant:  If this attribute is not specified, the expiry of the grant is derived
     * from internalGrantExpiry/guestGrantExpiry of the ACL it is part of.  If this attribute is
     * specified (overridden), the expiry value can not be greater than the corresponding expiry value in
     * the ACL.
     * For public grant:  If this attribute is not specified, defaults to the maximum allowed expiry for
     * a public grant.  If not specified in the response, defaults to 0.  Value of 0 indicates that this
     * grant never expires.
     */
    @XmlAttribute(name=MailConstants.A_EXPIRY /* expiry */, required=false)
    @GraphQLQuery(name="expiry", description="Time when this grant expires")
    private Long expiry;

    /**
     * @zm-api-field-tag grantee-name
     * @zm-api-field-description Name or email address of the principal being granted rights.  optional if
     * {grantee-type} is "all"/"guest"/"pub".  When specified in a request, this can be just the username portion of
     * the address in the default domain.
     */
    @XmlAttribute(name=MailConstants.A_DISPLAY /* d */, required=false)
    @GraphQLQuery(name="granteeName", description="Name or email address of the principal being granted rights.")
    private String granteeName;

    /**
     * @zm-api-field-tag guest-password
     * @zm-api-field-description Optional argument.  password when {grantee-type} is "guest"
     */
    @XmlAttribute(name=MailConstants.A_PASSWORD /* pw */, required=false)
    @GraphQLQuery(name="password", description="Password for when granteeType is guest")
    private String guestPassword;

    /**
     * @zm-api-field-tag access-key
     * @zm-api-field-description Optional argument.  Access key when {grantee-type} is "key"
     */
    @XmlAttribute(name=MailConstants.A_ACCESSKEY /* key */, required=false)
    @GraphQLQuery(name="accessKey", description="Access key when granteeType is key")
    private String accessKey;

    public Grant() {
    }

    public void setPerm(String perm) { this.perm = perm; }
    public void setGranteeType(GrantGranteeType granteeType) { this.granteeType = granteeType; }
    public void setGranteeId(String granteeId) { this.granteeId = granteeId; }
    public void setExpiry(Long expiry) { this.expiry = expiry; }
    public void setGranteeName(String granteeName) { this.granteeName = granteeName; }
    public void setGuestPassword(String guestPassword) { this.guestPassword = guestPassword; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    @GraphQLNonNull
    @GraphQLQuery(name="permissions", description="Rights - Some combination of (r)ead, (w)rite, (i)nsert, (d)elete, (a)dminister, workflow action (x), view (p)rivate, view (f)reebusy, (c)reate subfolder")
    public String getPerm() { return perm; }
    @GraphQLNonNull
    @GraphQLQuery(name="granteeType", description="The type of grantee")
    public GrantGranteeType getGranteeType() { return granteeType; }
    @GraphQLNonNull
    @GraphQLQuery(name="granteeId", description="The grantee id")
    public String getGranteeId() { return granteeId; }
    @GraphQLQuery(name="expiry", description="Time when this grant expires")
    public Long getExpiry() { return expiry; }
    @GraphQLQuery(name="granteeName", description="Name or email address of the principal being granted rights.")
    public String getGranteeName() { return granteeName; }
    @GraphQLQuery(name="password", description="Password for when granteeType is guest")
    public String getGuestPassword() { return guestPassword; }
    @GraphQLQuery(name="accessKey", description="Access key when granteeType is key")
    public String getAccessKey() { return accessKey; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("perm", perm)
            .add("granteeType", granteeType)
            .add("granteeId", granteeId)
            .add("expiry", expiry)
            .add("granteeName", granteeName)
            .add("guestPassword", guestPassword)
            .add("accessKey", accessKey);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
