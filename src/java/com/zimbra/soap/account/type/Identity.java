/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

package com.zimbra.soap.account.type;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AccountConstants;

/*
     <identity name={identity-name} id="...">
       <a name="{name}">{value}</a>
       ...
       <a name="{name}">{value}</a>
     </identity>*

 */
@XmlAccessorType(XmlAccessType.NONE)
public class Identity extends AttrsImpl {

    // TODO:Want constructor for old style Identity

    /**
     * @zm-api-field-tag identity-name
     * @zm-api-field-description Identity name
     */
    @XmlAttribute(name=AccountConstants.A_NAME, required=false)
    private final String name;

    /**
     * @zm-api-field-tag identity-id
     * @zm-api-field-description Identity ID
     */
    @XmlAttribute(name=AccountConstants.A_ID, required=false)
    private String id;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private Identity() {
        this((String) null, (String) null);
    }

    public Identity(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public static Identity fromName(String name) {
        return new Identity(name, null);
    }

    public static Identity fromNameAndId(String name, String id) {
        return new Identity(name, id);
    }
    public Identity(Identity i) {
        name = i.getName();
        id = i.getId();
        super.setAttrs(Lists.transform(i.getAttrs(), Attr.COPY));
    }

    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public String getId() { return id; }

    @Override
    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("name", name)
            .add("id", id);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }

}
