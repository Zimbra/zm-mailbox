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

package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name="ActionGrantSelector", description="Input for grants")
public class ActionGrantSelector {

    /**
     * @zm-api-field-tag rights
     * @zm-api-field-description Rights
     */
    @XmlAttribute(name=MailConstants.A_RIGHTS /* perm */, required=true)
    @GraphQLNonNull
    @GraphQLQuery(name="permissions", description="Rights - Some combination of (r)ead, (w)rite, (i)nsert, (d)elete, (a)dminister, workflow action (x), view (p)rivate, view (f)reebusy, (c)reate subfolder")
    private final String rights;

    // FolderActionRequest/action/grant/gt - FolderAction uses com.zimbra.cs.mailbox.ACL.stringToType
    // CreateFolder uses FolderAction.parseACL which uses com.zimbra.cs.mailbox.ACL.stringToType
    /**
     * @zm-api-field-tag grantee-type
     * @zm-api-field-description Grantee Type - usr | grp | cos | dom | all | pub | guest | key
     */
    @XmlAttribute(name=MailConstants.A_GRANT_TYPE /* gt */, required=true)
    @GraphQLNonNull
    @GraphQLQuery(name="granteeType", description="Grantee Type - usr | grp | cos | dom | all | pub | guest | key")
    private final String grantType;

    /**
     * @zm-api-field-tag zimbra-id
     * @zm-api-field-description Zimbra ID
     */
    @XmlAttribute(name=MailConstants.A_ZIMBRA_ID /* zid */, required=false)
    @GraphQLQuery(name="granteeId", description="Zimbra id")
    private String zimbraId;

    /**
     * @zm-api-field-tag grantee-name
     * @zm-api-field-description Name or email address of the grantee.
     * Not present if <b>{grantee-type}</b> is "all" or "pub"
     */
    @XmlAttribute(name=MailConstants.A_DISPLAY /* d */, required=false)
    @GraphQLQuery(name="granteeName", description="Name or email address of the grantee. Not present if granteeType is all or pub")
    private String displayName;

    // See Bug 30891
    /**
     * @zm-api-field-tag obsolete-use-pw
     * @zm-api-field-description Retained for backwards compatibility.  Old way of specifying password
     */
    @XmlAttribute(name=MailConstants.A_ARGS /* args */, required=false)
    @GraphQLIgnore
    private String args;

    /**
     * @zm-api-field-tag password
     * @zm-api-field-description Optional argument.  Password when <b>{grantee-type}</b> is "gst" (not yet supported)
     */
    @XmlAttribute(name=MailConstants.A_PASSWORD /* pw */, required=false)
    @GraphQLQuery(name="password", description="Password when granteeType is gst")
    private String password;

    /**
     * @zm-api-field-tag access-key
     * @zm-api-field-description Optional argument.  Access key when <b>{grantee-type}</b> is "key"
     */
    @XmlAttribute(name=MailConstants.A_ACCESSKEY /* key */, required=false)
    @GraphQLQuery(name="accessKey", description="Access key when granteeType is key")
    private String accessKey;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ActionGrantSelector() {
        this((String) null, (String) null);
    }

    public ActionGrantSelector(String rights, String grantType) {
        this.rights = rights;
        this.grantType = grantType;
    }

    public void setZimbraId(String zimbraId) { this.zimbraId = zimbraId; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setArgs(String args) { this.args = args; }
    public void setPassword(String password) { this.password = password; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public String getRights() { return rights; }
    public String getGrantType() { return grantType; }
    public String getZimbraId() { return zimbraId; }
    public String getDisplayName() { return displayName; }
    public String getArgs() { return args; }
    public String getPassword() { return password; }
    public String getAccessKey() { return accessKey; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("rights", rights)
            .add("grantType", grantType)
            .add("zimbraId", zimbraId)
            .add("displayName", displayName)
            .add("args", args)
            .add("password", password)
            .add("accessKey", accessKey);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
