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
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.ZimletConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=ZimletConstants.ZIMLET_TAG_SERVER_EXTENSION)
public class ZimletServerExtension {

    @XmlAttribute(name=ZimletConstants.ZIMLET_ATTR_HAS_KEYWORD
                        /* hasKeyword */, required=false)
    private String hasKeyword;

    @XmlAttribute(name=ZimletConstants.ZIMLET_ATTR_EXTENSION_CLASS
                        /* extensionClass */, required=false)
    private String extensionClass;

    @XmlAttribute(name=ZimletConstants.ZIMLET_ATTR_REGEX
                        /* regex */, required=false)
    private String regex;

    public ZimletServerExtension() {
    }

    public void setHasKeyword(String hasKeyword) {
        this.hasKeyword = hasKeyword;
    }
    public void setExtensionClass(String extensionClass) {
        this.extensionClass = extensionClass;
    }
    public void setRegex(String regex) { this.regex = regex; }
    public String getHasKeyword() { return hasKeyword; }
    public String getExtensionClass() { return extensionClass; }
    public String getRegex() { return regex; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("hasKeyword", hasKeyword)
            .add("extensionClass", extensionClass)
            .add("regex", regex);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
