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
