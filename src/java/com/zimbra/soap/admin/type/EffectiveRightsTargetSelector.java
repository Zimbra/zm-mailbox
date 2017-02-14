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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.TargetBy;
import com.zimbra.soap.type.TargetType;

@XmlAccessorType(XmlAccessType.NONE)
public class EffectiveRightsTargetSelector {

    /**
     * @zm-api-field-tag target-type
     * @zm-api-field-description Target type
     */
    @XmlAttribute(name=AdminConstants.A_TYPE, required=true)
    private final TargetType type;

    /**
     * @zm-api-field-tag target-selector-by
     * @zm-api-field-description Select the meaning of <b>{target-selector-key}</b>
     */
    @XmlAttribute(name=AdminConstants.A_BY, required=false)
    private final TargetBy by;

    /**
     * @zm-api-field-tag target-selector-key
     * @zm-api-field-description The key used to identify the target. Meaning determined by <b>{target-selector-by}</b>
     */
    @XmlValue
    private final String value;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private EffectiveRightsTargetSelector() {
        this((TargetType) null, (TargetBy) null,
                (String) null);
    }

    public EffectiveRightsTargetSelector(TargetType type, TargetBy by, String value) {
        this.type = type;
        this.by = by;
        this.value = value;
    }

    public TargetType getType() { return type; }
    public TargetBy getBy() { return by; }
    public String getValue() { return value; }
}
