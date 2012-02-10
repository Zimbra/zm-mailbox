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

import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ZimletInfo extends AdminObjectInfo {

    /**
     * @zm-api-field-tag keyword
     * @zm-api-field-description Keyword
     */
    @XmlAttribute(name=AdminConstants.A_HAS_KEYWORD /* hasKeyword */, required=false)
    private final String hasKeyword;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ZimletInfo() {
        this((String) null, (String) null, (Collection <Attr>) null,
                (String) null);
    }

    public ZimletInfo(String id, String name) {
        this(id, name, null, (String) null);
    }

    public ZimletInfo(String id, String name, Collection <Attr> attrs) {
        this(id, name, attrs, (String) null);
    }

    public ZimletInfo(String id, String name, Collection <Attr> attrs,
                    String hasKeyword) {
        super(id, name, attrs);
        this.hasKeyword = hasKeyword;
    }

    public String getHasKeyword() { return hasKeyword; }
}
