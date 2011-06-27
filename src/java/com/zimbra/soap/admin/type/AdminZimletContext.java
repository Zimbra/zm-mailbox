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

@XmlAccessorType(XmlAccessType.FIELD)
public class AdminZimletContext {

    @XmlAttribute(name=AccountConstants.A_ZIMLET_BASE_URL /* baseUrl */,
                            required=true)
    private final String zimletBaseUrl;

    @XmlAttribute(name=AccountConstants.A_ZIMLET_PRIORITY /* priority */,
                            required=false)
    private final Integer zimletPriority;

    @XmlAttribute(name=AccountConstants.A_ZIMLET_PRESENCE /* presence */,
                            required=true)
    private final String zimletPresence;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AdminZimletContext() {
        this((String) null, (Integer) null, (String) null);
    }

    public AdminZimletContext(String zimletBaseUrl, Integer zimletPriority,
                            String zimletPresence) {
        this.zimletBaseUrl = zimletBaseUrl;
        this.zimletPriority = zimletPriority;
        this.zimletPresence = zimletPresence;
    }

    public String getZimletBaseUrl() { return zimletBaseUrl; }
    public Integer getZimletPriority() { return zimletPriority; }
    public String getZimletPresence() { return zimletPresence; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("zimletBaseUrl", zimletBaseUrl)
            .add("zimletPriority", zimletPriority)
            .add("zimletPresence", zimletPresence);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
