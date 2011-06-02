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

package com.zimbra.soap.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_METADATA)
public class CustomMetadata extends KeyValuePairsImpl {

    // Normally present but sometimes an empty element is created
    // to show that CustomMetadata info is present but there are
    // no sections to report on
    @XmlAttribute(name=MailConstants.A_SECTION, required=false)
    private final String section;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CustomMetadata() {
        this((String) null);
    }

    public CustomMetadata(String section) {
        this.section = section;
    }

    public String getSection() { return section; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("section", section)
            .toString();
    }
}
