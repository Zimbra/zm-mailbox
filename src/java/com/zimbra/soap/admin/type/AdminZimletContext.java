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
import com.zimbra.soap.base.ZimletContextInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class AdminZimletContext
implements ZimletContextInterface {

    /**
     * @zm-api-field-tag zimlet-base-url
     * @zm-api-field-description Zimlet Base URL
     */
    @XmlAttribute(name=AccountConstants.A_ZIMLET_BASE_URL /* baseUrl */, required=true)
    private String zimletBaseUrl;

    /**
     * @zm-api-field-tag zimlet-priority
     * @zm-api-field-description Zimlet Priority
     */
    @XmlAttribute(name=AccountConstants.A_ZIMLET_PRIORITY /* priority */, required=false)
    private Integer zimletPriority;

    /**
     * @zm-api-field-tag zimlet-presence
     * @zm-api-field-description Zimlet presence
     * <br />
     * Valid values: <b>mandatory</b> | <b>enabled</b> | <b>disabled</b>
     */
    @XmlAttribute(name=AccountConstants.A_ZIMLET_PRESENCE /* presence */, required=true)
    private String zimletPresence;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AdminZimletContext() {
        this((String) null, (Integer) null, (String) null);
    }

    public AdminZimletContext(String zimletBaseUrl, Integer zimletPriority,
                            String zimletPresence) {
        this.setZimletBaseUrl(zimletBaseUrl);
        this.setZimletPriority(zimletPriority);
        this.setZimletPresence(zimletPresence);
    }

    public static AdminZimletContext createForBaseUrlPriorityAndPresence(
            String zimletBaseUrl, Integer zimletPriority, String zimletPresence) {
        return new AdminZimletContext(zimletBaseUrl, zimletPriority, zimletPresence);
    }

    @Override
    public void setZimletBaseUrl(String zimletBaseUrl) { this.zimletBaseUrl = zimletBaseUrl; }
    @Override
    public void setZimletPriority(Integer zimletPriority) { this.zimletPriority = zimletPriority; }
    @Override
    public void setZimletPresence(String zimletPresence) { this.zimletPresence = zimletPresence; }

    @Override
    public String getZimletBaseUrl() { return zimletBaseUrl; }
    @Override
    public Integer getZimletPriority() { return zimletPriority; }
    @Override
    public String getZimletPresence() { return zimletPresence; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("zimletBaseUrl", getZimletBaseUrl())
            .add("zimletPriority", getZimletPriority())
            .add("zimletPresence", getZimletPresence());
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
