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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import com.zimbra.common.soap.AccountConstants;

/*
     <identity name={identity-name} id="...">
       <a name="{name}">{value}</a>
       ...
       <a name="{name}">{value}</a>
     </identity>*

 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Identity extends AttrsImpl {

    @XmlAttribute(name=AccountConstants.A_NAME, required=false)
    private final String name;

    @XmlAttribute(name=AccountConstants.A_ID, required=false)
    private final String id;

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

    public Identity(Identity i) {
        name = i.getName();
        id = i.getId();
        super.setAttrs(Lists.transform(i.getAttrs(), Attr.COPY));
    }

    public String getName() { return name; }
    public String getId() { return id; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("name", name)
            .add("id", id)
            .add("attrs", super.getAttrs())
            .toString();
    }

}
