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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.ConstraintInfo;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;

@XmlAccessorType(XmlAccessType.NONE)
public class EffectiveAttrInfo {

    /**
     * @zm-api-field-tag attribute-name
     * @zm-api-field-description Attribute name
     */
    @XmlAttribute(name=AdminConstants.A_N /* n */, required=true)
    private final String name;

    /**
     * @zm-api-field-description Constraint information
     */
    @XmlElement(name=AdminConstants.E_CONSTRAINT /* constraint */, required=false)
    private ConstraintInfo constraint;

    /**
     * @zm-api-field-description Inherited default value(or values if the attribute is multi-valued)
     */
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=AdminConstants.E_DEFAULT /* default */, required=false)
    @XmlElement(name=AdminConstants.E_VALUE /* v */, required=false)
    private List <String> values = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private EffectiveAttrInfo() {
        this((String) null, (ConstraintInfo) null);
    }

    public EffectiveAttrInfo(String name) {
        this(name, (ConstraintInfo) null);
    }

    public EffectiveAttrInfo(String name, ConstraintInfo constraint) {
        this.name = name;
        this.setConstraint(constraint);
    }

    public EffectiveAttrInfo setValues(Collection <String> values) {
        this.values.clear();
        if (values != null) {
            this.values.addAll(values);
        }
        return this;
    }

    public EffectiveAttrInfo addValue(String value) {
        values.add(value);
        return this;
    }

    public List <String> getValues() {
        return Collections.unmodifiableList(values);
    }

    public String getName() {
        return name;
    }

    public void setConstraint(ConstraintInfo constraint) {
        this.constraint = constraint;
    }

    public ConstraintInfo getConstraint() {
        return constraint;
    }
}
