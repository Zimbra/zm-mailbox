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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class FolderActionSelector extends ActionSelector {

    @XmlAttribute(name=MailConstants.A_RECURSIVE /* recursive */, required=false)
    private Boolean recursive;

    @XmlAttribute(name=MailConstants.A_URL /* url */, required=false)
    private String url;

    @XmlAttribute(name=MailConstants.A_EXCLUDE_FREEBUSY /* excludeFreeBusy */, required=false)
    private Boolean excludeFreebusy;

    @XmlAttribute(name=MailConstants.A_ZIMBRA_ID /* zid */, required=false)
    private String zimbraId;

    @XmlAttribute(name=MailConstants.A_GRANT_TYPE /* gt */, required=false)
    private String grantType;

    // Use to change folder's default view (useful for migration)
    @XmlAttribute(name=MailConstants.A_DEFAULT_VIEW /* view */, required=false)
    private String view;

    @XmlElement(name=MailConstants.E_GRANT /* grant */, required=false)
    private ActionGrantSelector grant;

    @XmlElementWrapper(name=MailConstants.E_ACL /* acl */, required=false)
    @XmlElement(name=MailConstants.E_GRANT /* grant */, required=false)
    private List<ActionGrantSelector> grants = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_RETENTION_POLICY /* retentionPolicy */, required=false)
    private RetentionPolicy retentionPolicy;

    public FolderActionSelector() {
        this((String) null, (String) null);
    }

    public FolderActionSelector(String ids, String operation) {
        super(ids, operation);
    }

    public void setRecursive(Boolean recursive) { this.recursive = recursive; }
    public void setUrl(String url) { this.url = url; }
    public void setExcludeFreebusy(Boolean excludeFreebusy) {
        this.excludeFreebusy = excludeFreebusy;
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
    public Boolean getRecursive() { return recursive; }
    public String getUrl() { return url; }
    public Boolean getExcludeFreebusy() { return excludeFreebusy; }
    public String getZimbraId() { return zimbraId; }
    public String getGrantType() { return grantType; }
    public String getView() { return view; }
    public ActionGrantSelector getGrant() { return grant; }
    public List<ActionGrantSelector> getGrants() {
        return Collections.unmodifiableList(grants);
    }
    public RetentionPolicy getRetentionPolicy() { return retentionPolicy; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
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
            .add("retentionPolicy", retentionPolicy);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
