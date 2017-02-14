/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
        public com.zimbra.common.account.Key.CalendarResourceBy toKeyCalendarResourceBy()
        throws ServiceException {
            return com.zimbra.common.account.Key.CalendarResourceBy.fromString(this.name());
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

    public static CalendarResourceSelector fromId(String id) {
        return new CalendarResourceSelector(CalendarResourceBy.id, id);
    }

    public static CalendarResourceSelector fromName(String name) {
        return new CalendarResourceSelector(CalendarResourceBy.name, name);
    }

    public String getKey() { return key; }

    public CalendarResourceBy getBy() { return calResourceBy; }
}
