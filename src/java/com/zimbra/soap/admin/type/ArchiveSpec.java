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

package com.zimbra.soap.admin.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.ArchiveConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class ArchiveSpec extends AdminAttrsImpl {

    /**
     * @zm-api-field-tag create-archive-account-flag
     * @zm-api-field-description for <b>&lt;EnableArchiveRequest></b>, Archive account is created by default based
     * on name templates.  You can suppress this by setting this flag to <b>0 (false)</b>.  This is useful if you
     * are going to use a third party system to do the archiving and ZCS is just a mail forker.
     */
    @XmlAttribute(name=ArchiveConstants.A_CREATE /* create */, required=false)
    private ZmBoolean create;

    /**
     * @zm-api-field-tag archive-account-name
     * @zm-api-field-description Archive account name.  If not specified, archive account name is computed based on
     * name templates.
     */
    @XmlElement(name=AdminConstants.E_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-description Selector for Class Of Service (COS)
     */
    @XmlElement(name=AdminConstants.E_COS /* cos */, required=false)
    private CosSelector cos;

    /**
     * @zm-api-field-tag archive-account-password
     * @zm-api-field-description Archive account password - <b>Recommended that password not be specified so only
     * admins can login</b>
     */
    @XmlElement(name=AdminConstants.E_PASSWORD /* password */, required=false)
    private String password;

    public ArchiveSpec() {
    }

    public void setCreate(Boolean create) { this.create = ZmBoolean.fromBool(create); }
    public void setName(String name) { this.name = name; }
    public void setCos(CosSelector cos) { this.cos = cos; }
    public void setPassword(String password) { this.password = password; }
    public Boolean getCreate() { return ZmBoolean.toBool(create); }
    public String getName() { return name; }
    public CosSelector getCos() { return cos; }
    public String getPassword() { return password; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("create", create)
            .add("name", name)
            .add("cos", cos)
            .add("password", password);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
