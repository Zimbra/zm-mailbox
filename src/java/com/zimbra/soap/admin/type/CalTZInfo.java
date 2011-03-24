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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class CalTZInfo {

    @XmlAttribute(name=MailConstants.A_ID, required=true)
    private final String id;

    @XmlAttribute(name=MailConstants.A_CAL_TZ_STDOFFSET, required=true)
    private final Integer tzStdOffset;

    @XmlAttribute(name=MailConstants.A_CAL_TZ_DAYOFFSET, required=true)
    private final Integer tzDayOffset;

    @XmlElement(name=MailConstants.E_CAL_TZ_STANDARD, required=false)
    private TzOnsetInfo standardTzOnset;

    @XmlElement(name=MailConstants.E_CAL_TZ_DAYLIGHT, required=false)
    private TzOnsetInfo daylightTzOnset;

    @XmlAttribute(name=MailConstants.A_CAL_TZ_STDNAME, required=false)
    private String standardTZName;

    @XmlAttribute(name=MailConstants.A_CAL_TZ_DAYNAME, required=false)
    private String daylightTZName;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CalTZInfo() {
        this((String) null, (Integer) null, (Integer) null);
    }

    public CalTZInfo(String id, Integer tzStdOffset, Integer tzDayOffset) {
        this.id = id;
        this.tzStdOffset = tzStdOffset;
        this.tzDayOffset = tzDayOffset;
    }

    public void setStandardTzOnset(TzOnsetInfo standardTzOnset) {
        this.standardTzOnset = standardTzOnset;
    }

    public void setDaylightTzOnset(TzOnsetInfo daylightTzOnset) {
        this.daylightTzOnset = daylightTzOnset;
    }

    public void setStandardTZName(String standardTZName) {
        this.standardTZName = standardTZName;
    }

    public void setDaylightTZName(String daylightTZName) {
        this.daylightTZName = daylightTZName;
    }

    public String getId() { return id; }
    public Integer getTzStdOffset() { return tzStdOffset; }
    public Integer getTzDayOffset() { return tzDayOffset; }
    public TzOnsetInfo getStandardTzOnset() { return standardTzOnset; }
    public TzOnsetInfo getDaylightTzOnset() { return daylightTzOnset; }
    public String getStandardTZName() { return standardTZName; }
    public String getDaylightTZName() { return daylightTZName; }
}
