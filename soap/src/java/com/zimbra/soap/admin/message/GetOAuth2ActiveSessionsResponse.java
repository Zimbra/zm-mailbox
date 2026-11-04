/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc. All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import java.util.Collections;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.OAuth2SessionInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_GET_OAUTH2_ACTIVE_SESSIONS_RESPONSE)
public class GetOAuth2ActiveSessionsResponse {

    /**
     * @zm-api-field-tag total
     * @zm-api-field-description Total number of active sessions matching the scope, ignoring offset/limit.
     * 0 when none exist (not an error).
     */
    @XmlAttribute(name = AdminConstants.A_OAUTH2_TOTAL, required = true)
    private int total;

    /**
     * @zm-api-field-description Active OAuth2 sessions
     */
    @XmlElement(name = AdminConstants.E_MOBILE_DEVICE /* session */, required = false)
    private List<OAuth2SessionInfo> mobileDevice = Lists.newArrayList();

    public GetOAuth2ActiveSessionsResponse() {}

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public List<OAuth2SessionInfo> getMobileDevice() { return Collections.unmodifiableList(mobileDevice); }
    public void setMobileDevice(Iterable<OAuth2SessionInfo> sessions) {
        this.mobileDevice.clear();
        if (sessions != null) {
            Iterables.addAll(this.mobileDevice, sessions);
        }
    }
    public void addMobileDevice(OAuth2SessionInfo mobileDevice) {
        this.mobileDevice.add(mobileDevice);
    }
}
