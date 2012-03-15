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

package com.zimbra.soap.account.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AccountConstants;

/*
 * Used in GetInfoResponse if ZimbraLicenseExtension is installed
 */
@XmlAccessorType(XmlAccessType.NONE)
public class LicenseAttr {

    /**
     * @zm-api-field-tag license-attr-name
     * @zm-api-field-description Name - always "SMIME" at present
     */
    @XmlAttribute(name=AccountConstants.A_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-tag license-attr-value
     * @zm-api-field-description Value - value is "TRUE" or "FALSE"
     */
    @XmlValue
    private String content;

    public LicenseAttr() {
    }

    public void setName(String name) { this.name = name; }
    public void setContent(String content) { this.content = content; }
    public String getName() { return name; }
    public String getContent() { return content; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("content", content);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
