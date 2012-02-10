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

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.base.LocaleInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class LocaleInfo
implements LocaleInterface {

    /**
     * @zm-api-field-tag locale-id
     * @zm-api-field-description Locale ID.  e.g. "en_US"
     */
    @XmlAttribute(name=AccountConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-tag locale-name
     * @zm-api-field-description Locale name - the name in the locale itself.  e.g. "English (United States)"
     */
    @XmlAttribute(name=AccountConstants.A_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag locale-local-name
     * @zm-api-field-description Locale name in the user's locale.  e.g. "English (United States)"
     */
    @XmlAttribute(name=AccountConstants.A_LOCAL_NAME /* localName */, required=false)
    private final String localName;

    /**
     * no-argument constructor wanted by JAXB
     */
    private LocaleInfo() {
        this((String) null, (String) null, (String) null);
    }

    private LocaleInfo(String id, String name, String localName) {
        this.id = id;
        this.name = name;
        this.localName = localName;
    }

    public static LocaleInfo createForIdNameAndLocalName(String id, String name, String localName) {
        return new LocaleInfo(id, name, localName);
    }

    @Override
    public String getId() { return id; }
    @Override
    public String getName() { return name; }
    @Override
    public String getLocalName() { return localName; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("name", name)
            .add("localName", localName);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
