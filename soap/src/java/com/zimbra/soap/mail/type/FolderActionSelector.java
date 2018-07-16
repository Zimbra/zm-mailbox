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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;
import com.zimbra.soap.type.ZmBoolean;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name="FolderActionSelector", description="Input for folder actions")
public class FolderActionSelector extends ActionSelector {

    /**
     * @zm-api-field-tag delete-subfolders
     * @zm-api-field-description  For op="empty" - hard-delete all items in the folder (and all the folder's
     * subfolders if "recursive" is set)
     */
    @XmlAttribute(name=MailConstants.A_RECURSIVE /* recursive */, required=false)
    @GraphQLQuery(name="recursive", description="For op=empty - hard-delete all items in the folder (and all the folder's")
    private ZmBoolean recursive;

    /**
     * @zm-api-field-tag target-url
     * @zm-api-field-description Target URL
     */
    @XmlAttribute(name=MailConstants.A_URL /* url */, required=false)
    @GraphQLQuery(name="url", description="Target URL")
    private String url;

    /**
     * @zm-api-field-tag exclude-free-busy-boolean
     * @zm-api-field-description For <b>op="fb"</b> - set the excludeFreeBusy boolean for this folder (must specify
     * <b>{exclude-free-busy-boolean}</b> for op="fb")
     */
    @XmlAttribute(name=MailConstants.A_EXCLUDE_FREEBUSY /* excludeFreeBusy */, required=false)
    @GraphQLQuery(name="excludeFreeBusy", description="For fb operation - set the excludeFreeBusy boolean for this folder (must specify for fb operation)")
    private ZmBoolean excludeFreebusy;

    /**
     * @zm-api-field-tag grantee-zimbra-id
     * @zm-api-field-description Grantee Zimbra ID
     */
    @XmlAttribute(name=MailConstants.A_ZIMBRA_ID /* zid */, required=false)
    @GraphQLQuery(name="zimbraId", description="Grantee Zimbra ID")
    private String zimbraId;

    /**
     * @zm-api-field-tag grantee-type
     * @zm-api-field-description Grantee Type
     */
    @XmlAttribute(name=MailConstants.A_GRANT_TYPE /* gt */, required=false)
    @GraphQLQuery(name="granteeType", description="Grantee type")
    private String grantType;

    /**
     * @zm-api-field-tag default-view
     * @zm-api-field-description Use with <b>op="update"</b> to change folder's default view (useful for migration)
     */
    @XmlAttribute(name=MailConstants.A_DEFAULT_VIEW /* view */, required=false)
    @GraphQLQuery(name="view", description="User with op=update to change folder's default view (usefor for migration)")
    private String view;

    /**
     * @zm-api-field-tag
     * @zm-api-field-description
     */
    @ZimbraUniqueElement
    @XmlElement(name=MailConstants.E_GRANT /* grant */, required=false)
    @GraphQLQuery(name="grant", description="Grant")
    private ActionGrantSelector grant;

    /**
     * @zm-api-field-description Grants (used with <b>op="grant"</b> and <b>op="!grant"</b>
     */
    @XmlElementWrapper(name=MailConstants.E_ACL /* acl */, required=false)
    @XmlElement(name=MailConstants.E_GRANT /* grant */, required=false)
    @GraphQLQuery(name="grants", description="List of grants used with grant and !grant operations")
    private List<ActionGrantSelector> grants = Lists.newArrayList();

    /**
     * @zm-api-field-description Retention policy
     */
    @XmlElement(name=MailConstants.E_RETENTION_POLICY /* retentionPolicy */, required=false)
    @GraphQLQuery(name="retentionPolicy", description="Retention policy")
    private RetentionPolicy retentionPolicy;

    /**
     * @zm-api-field-tag web-offline-sync-days
     * @zm-api-field-description Number of days for which web client would sync folder data for offline use
     */
    @XmlAttribute(name=MailConstants.A_NUM_DAYS /* numDays */, required=false)
    @GraphQLQuery(name="numDays", description="Number of days for which web client would sync folder data for offline use")
    private Integer numDays;

    public FolderActionSelector() {
        this((String) null, (String) null);
    }

    public FolderActionSelector(String ids, String operation) {
        super(ids, operation);
    }

    public void setRecursive(Boolean recursive) { this.recursive = ZmBoolean.fromBool(recursive); }
    public void setUrl(String url) { this.url = url; }
    public void setExcludeFreebusy(Boolean excludeFreebusy) {
        this.excludeFreebusy = ZmBoolean.fromBool(excludeFreebusy);
    }
    public void setZimbraId(String zimbraId) { this.zimbraId = zimbraId; }
    public void setGrantType(String grantType) { this.grantType = grantType; }
    public void setGrant(ActionGrantSelector grant) { this.grant = grant; }
    public void setGrants(Iterable <ActionGrantSelector> grants) {
        this.grants = null;
        if (grants != null) {
            this.grants = Lists.newArrayList();
            Iterables.addAll(this.grants,grants);
        }
    }

    public void addGrant(ActionGrantSelector grant) {
        if (grants == null)
            this.grants = Lists.newArrayList();
        this.grants.add(grant);
    }

    public void setRetentionPolicy(RetentionPolicy retentionPolicy) { this.retentionPolicy = retentionPolicy; }
    public void setNumDays(Integer numDays) { this.numDays = numDays; }
    @GraphQLQuery(name = "recursive", description = "For op=empty - hard-delete all items in the folder (and all the folder's")
    public Boolean getRecursive() { return ZmBoolean.toBool(recursive); }
    @GraphQLQuery(name = "url", description = "Target URL")
    public String getUrl() { return url; }
    @GraphQLQuery(name = "excludeFreeBusy", description = "For op=fb - set the excludeFreeBusy boolean for this folder (must specify for op=fb)")
    public Boolean getExcludeFreebusy() { return ZmBoolean.toBool(excludeFreebusy); }
    @GraphQLQuery(name = "zimbraId", description = "Grantee Zimbra ID")
    public String getZimbraId() { return zimbraId; }
    @GraphQLQuery(name = "granteeType", description = "Grantee type")
    public String getGrantType() { return grantType; }
    @GraphQLQuery(name = "view", description = "User with op=update to change folder's default view (usefor for migration)")
    public String getView() { return view; }
    @GraphQLQuery(name = "grant", description = "Grant")
    public ActionGrantSelector getGrant() { return grant; }
    @GraphQLQuery(name = "grants", description = "List of grants used with op=grant and op=!grant")
    public List<ActionGrantSelector> getGrants() {
        return Collections.unmodifiableList(grants);
    }
    @GraphQLQuery(name = "retentionPolicy", description = "Retention policy")
    public RetentionPolicy getRetentionPolicy() { return retentionPolicy; }
    @GraphQLQuery(name = "numDays", description = "Number of days for which web client would sync folder data for offline use")
    public Integer getNumDays() { return numDays; }

    @Override
    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("recursive", recursive)
            .add("url", url)
            .add("excludeFreebusy", excludeFreebusy)
            .add("zimbraId", zimbraId)
            .add("grantType", grantType)
            .add("view", view)
            .add("grant", grant)
            .add("grants", grants)
            .add("retentionPolicy", retentionPolicy)
            .add("numDays", numDays);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
