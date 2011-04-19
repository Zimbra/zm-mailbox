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

package com.zimbra.soap.mail.type;

import com.google.common.collect.Maps;
import com.google.common.base.Objects;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class RuleCondition implements RuleComponent {

    @XmlAttribute(name=MailConstants.A_NAME, required=true)
    private final String name;

    @XmlAttribute(name=MailConstants.A_OPERATION, required=false)
    private String operation;

    @XmlAttribute(name=MailConstants.A_RHS, required=false)
    private String rhs;

    @XmlAttribute(name=MailConstants.A_MODIFIER, required=false)
    private String modifier;

    // Attributes with name matching pattern "k#" where # is a number.
    @XmlAnyAttribute
    private Map<QName,Object> extraAttributes = Maps.newHashMap();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private RuleCondition() {
        this((String) null);
    }

    public RuleCondition(String name) {
        this.name = name;
    }

    public void setOperation(String operation) { this.operation = operation; }
    public void setRhs(String rhs) { this.rhs = rhs; }
    public void setModifier(String modifier) { this.modifier = modifier; }

    public void setExtraAttributes(Map<QName,Object> extraAttributes) {
        this.extraAttributes.clear();
        if (extraAttributes != null) {
            this.extraAttributes.putAll(extraAttributes);
        }
    }

    public void addExtraAttribute(QName qn, Object value) {
        if ((qn != null) && (value != null)) {
            this.extraAttributes.put(qn, value);
        }
    }

    public String getName() { return name; }
    public String getOperation() { return operation; }
    public String getRhs() { return rhs; }
    public String getModifier() { return modifier; }
    public Map<QName,Object> getExtraAttributes() { return extraAttributes; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("name", name)
            .add("operation", operation)
            .add("rhs", rhs)
            .add("modifier", modifier)
            .add("extraAttributes", extraAttributes)
            .toString();
    }
}
