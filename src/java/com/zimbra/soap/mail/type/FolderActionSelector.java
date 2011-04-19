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
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class FolderActionSelector extends ActionSelector {

    @XmlAttribute(name=MailConstants.A_RECURSIVE, required=false)
    private Boolean recursive;

    @XmlAttribute(name=MailConstants.A_URL, required=false)
    private String url;

    @XmlAttribute(name=MailConstants.A_EXCLUDE_FREEBUSY, required=false)
    private Boolean excludeFreebusy;

    @XmlAttribute(name=MailConstants.A_ZIMBRA_ID, required=false)
    private String zimbraId;

    @XmlAttribute(name=MailConstants.A_GRANT_TYPE, required=false)
    private String grantType;

    @XmlElement(name=MailConstants.E_GRANT, required=false)
    private ActionGrantSelector grant;

    @XmlElementWrapper(name=MailConstants.E_ACL, required=false)
    @XmlElement(name=MailConstants.E_GRANT, required=false)
    private List<ActionGrantSelector> grants = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    protected FolderActionSelector() {
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
        this.grants.clear();
        if (grants != null) {
            Iterables.addAll(this.grants,grants);
        }
    }

    public FolderActionSelector addGrant(ActionGrantSelector grant) {
        this.grants.add(grant);
        return this;
    }

    public Boolean getRecursive() { return recursive; }
    public String getUrl() { return url; }
    public Boolean getExcludeFreebusy() { return excludeFreebusy; }
    public String getZimbraId() { return zimbraId; }
    public String getGrantType() { return grantType; }
    public ActionGrantSelector getGrant() { return grant; }
    public List<ActionGrantSelector> getGrants() {
        return Collections.unmodifiableList(grants);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("recursive", recursive)
            .add("url", url)
            .add("excludeFreebusy", excludeFreebusy)
            .add("zimbraId", zimbraId)
            .add("grantType", grantType)
            .add("grant", grant)
            .add("grants", grants)
            .toString();
    }
}
