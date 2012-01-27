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

package com.zimbra.soap.admin.message;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.AdminAttrsImpl;
import com.zimbra.soap.admin.type.Attr;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-description Create a distribution list
 * <br />
 * Notes:
 * <ul>
 * <li> dynamic - create a dynamic distribution list
 * <li> Extra attrs: <b>description</b>, <b>zimbraNotes</b>
 * </ul>
 * <b>Access</b>: domain admin sufficient
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CREATE_DISTRIBUTION_LIST_REQUEST)
public class CreateDistributionListRequest extends AdminAttrsImpl {

    /**
     * @zm-api-field-tag new-dl-name
     * @zm-api-field-description Name for distribution list
     */
    @XmlAttribute(name=AdminConstants.E_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-description If <b>1 (true)</b> then create a dynamic distribution list
     */
    @XmlAttribute(name=AdminConstants.A_DYNAMIC /* dynamic */, required=false)
    private ZmBoolean dynamic;

    public CreateDistributionListRequest() {
        this((String)null);
    }

    public CreateDistributionListRequest(String name) {
        this(name, (Collection<Attr>) null, false);
    }

    public CreateDistributionListRequest(String name, Collection<Attr> attrs, boolean dynamic) {
        super(attrs);
        this.name = name;
        this.setDynamic(dynamic);
    }

    public void setName(String name) { this.name = name; }
    public String getName() { return name; }

    public void setDynamic(Boolean dynamic) { this.dynamic = ZmBoolean.fromBool(dynamic); }
    public Boolean getDynamic() { return ZmBoolean.toBool(dynamic); }
}
