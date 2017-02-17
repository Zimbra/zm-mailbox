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

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;

@XmlAccessorType(XmlAccessType.NONE)
public class ConstraintInfo {

    /**
     * @zm-api-field-description Minimum value
     */
    @XmlElement(name=AdminConstants.E_MIN, required=false)
    private final String min;

    /**
     * @zm-api-field-description Maximum value
     */
    @XmlElement(name=AdminConstants.E_MAX, required=false)
    private final String max;

    /**
     * @zm-api-field-description Acceptable Values
     */
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=AdminConstants.E_VALUES, required=false)
    @XmlElement(name=AdminConstants.E_VALUE, required=false)
    private List <String> values = Lists.newArrayList();

    public ConstraintInfo() {
        this((String) null, (String) null);
    }

    public ConstraintInfo(String min, String max) {
        this.min = min;
        this.max = max;
    }

    public ConstraintInfo setValues(Collection <String> values) {
        this.values.clear();
        if (values != null) {
            this.values.addAll(values);
        }
        return this;
    }

    public ConstraintInfo addValue(String value) {
        values.add(value);
        return this;
    }

    public List <String> getValues() {
        return Collections.unmodifiableList(values);
    }
    public String getMin() { return min; }
    public String getMax() { return max; }
}
