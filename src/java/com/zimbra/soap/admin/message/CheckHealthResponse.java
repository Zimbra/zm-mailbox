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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CHECK_HEALTH_RESPONSE)
@XmlType(propOrder = {})
public class CheckHealthResponse {

    /**
     * @zm-api-field-description Flags whether healthy or not
     */
    @XmlAttribute(name=AdminConstants.A_HEALTHY, required=true)
    private ZmBoolean healthy;

    public CheckHealthResponse() { }

    public CheckHealthResponse(boolean healthy) { this.healthy = ZmBoolean.fromBool(healthy); }

    public void setHealthy(boolean healthy) { this.healthy = ZmBoolean.fromBool(healthy); }

    public boolean isHealthy() { return ZmBoolean.toBool(healthy); }
}
