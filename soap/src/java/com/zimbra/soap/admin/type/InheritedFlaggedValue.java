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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class InheritedFlaggedValue {

    /**
     * @zm-api-field-tag inherited-flag
     * @zm-api-field-description Inherited flag
     * <table>
     * <tr> <td> <b>1 (true)</b> </td> <td> inherited from a group </td> </tr>
     * <tr> <td> <b>0 (false)</b> </td> <td> set directly on the entry </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.A_INHERITED /* inherited */, required=true)
    private final ZmBoolean inherited;

    /**
     * @zm-api-field-tag value
     * @zm-api-field-description Value
     */
    @XmlValue
    private final String value;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private InheritedFlaggedValue() {
        this(false, (String) null);
    }

    public InheritedFlaggedValue(boolean inherited, String value) {
        this.inherited = ZmBoolean.fromBool(inherited);
        this.value = value;
    }

    public boolean getInherited() { return ZmBoolean.toBool(inherited); }
    public String getValue() { return value; }
}
