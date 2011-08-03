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

import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.CalTZInfoInterface;
import com.zimbra.soap.type.TzOnsetInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class CalTZInfo implements CalTZInfoInterface {

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    @XmlAttribute(name=MailConstants.A_CAL_TZ_STDOFFSET /* stdoff */,
                    required=true)
    private final Integer tzStdOffset;

    @XmlAttribute(name=MailConstants.A_CAL_TZ_DAYOFFSET /* dayoff */,
                    required=true)
    private final Integer tzDayOffset;

    @XmlElement(name=MailConstants.E_CAL_TZ_STANDARD /* standard */,
                    required=false)
    private TzOnsetInfo standardTzOnset;

    @XmlElement(name=MailConstants.E_CAL_TZ_DAYLIGHT /* daylight */,
                    required=false)
    private TzOnsetInfo daylightTzOnset;

    @XmlAttribute(name=MailConstants.A_CAL_TZ_STDNAME /* stdname */,
                    required=false)
    private String standardTZName;

    @XmlAttribute(name=MailConstants.A_CAL_TZ_DAYNAME /* dayname */,
                    required=false)
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

    @Override
    public CalTZInfoInterface createFromIdStdOffsetDayOffset(String id,
            Integer tzStdOffset, Integer tzDayOffset) {
        return new CalTZInfo(id, tzStdOffset, tzDayOffset);
    }

    @Override
    public void setStandardTzOnset(TzOnsetInfo standardTzOnset) {
        this.standardTzOnset = standardTzOnset;
    }

    @Override
    public void setDaylightTzOnset(TzOnsetInfo daylightTzOnset) {
        this.daylightTzOnset = daylightTzOnset;
    }

    @Override
    public void setStandardTZName(String standardTZName) {
        this.standardTZName = standardTZName;
    }

    @Override
    public void setDaylightTZName(String daylightTZName) {
        this.daylightTZName = daylightTZName;
    }

    @Override
    public String getId() { return id; }
    @Override
    public Integer getTzStdOffset() { return tzStdOffset; }
    @Override
    public Integer getTzDayOffset() { return tzDayOffset; }
    @Override
    public TzOnsetInfo getStandardTzOnset() { return standardTzOnset; }
    @Override
    public TzOnsetInfo getDaylightTzOnset() { return daylightTzOnset; }
    @Override
    public String getStandardTZName() { return standardTZName; }
    @Override
    public String getDaylightTZName() { return daylightTZName; }

    public static Iterable <CalTZInfo> fromInterfaces(Iterable <CalTZInfoInterface> params) {
        if (params == null)
            return null;
        List <CalTZInfo> newList = Lists.newArrayList();
        for (CalTZInfoInterface param : params) {
            newList.add((CalTZInfo) param);
        }
        return newList;
    }

    public static List <CalTZInfoInterface> toInterfaces(Iterable <CalTZInfo> params) {
        if (params == null)
            return null;
        List <CalTZInfoInterface> newList = Lists.newArrayList();
        Iterables.addAll(newList, params);
        return newList;
    }
    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("tzStdOffset", tzStdOffset)
            .add("tzDayOffset", tzDayOffset)
            .add("standardTzOnset", standardTzOnset)
            .add("daylightTzOnset", daylightTzOnset)
            .add("standardTZName", standardTZName)
            .add("daylightTZName", daylightTZName);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
