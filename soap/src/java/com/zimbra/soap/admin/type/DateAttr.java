/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.DateAttrInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class DateAttr implements DateAttrInterface {

    /**
     * @zm-api-field-tag YYYYMMDDThhmmssZ
     * @zm-api-field-description Date in format : <b>YYYYMMDDThhmmssZ</b>
     */
    @XmlAttribute(name=MailConstants.A_DATE, required=true)
    private final String date;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DateAttr() {
        this((String) null);
    }

    public DateAttr(String date) {
        this.date = date;
    }

    @Override
    public DateAttrInterface create(String date) {
        return new DateAttr(date);
    }

    @Override
    public String getDate() { return date; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("date", date)
            .toString();
    }
}
