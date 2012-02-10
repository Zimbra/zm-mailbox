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
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class CalendarResourceSelector {

    @XmlEnum
    public enum CalendarResourceBy {
        // case must match protocol
        id, foreignPrincipal, name;

        public static CalendarResourceBy fromString(String s) throws ServiceException {
            try {
                return CalendarResourceBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    /**
     * @zm-api-field-tag cal-resource-selector-by
     * @zm-api-field-description Select the meaning of <b>{cal-resource-selector-key}</b>
     */
    @XmlAttribute(name=AdminConstants.A_BY)
    private final
    CalendarResourceBy calResourceBy;

    /**
     * @zm-api-field-tag cal-resource-selector-key
     * @zm-api-field-description The key used to identify the account. Meaning determined by
     * <b>{cal-resource-selector-by}</b>
     */
    @XmlValue
    private final String key;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CalendarResourceSelector() {
        this.calResourceBy = null;
        this.key = null;
    }

    public CalendarResourceSelector(CalendarResourceBy by, String key) {
        this.calResourceBy = by;
        this.key = key;
    }

    public String getKey() { return key; }

    public CalendarResourceBy getBy() { return calResourceBy; }
}
