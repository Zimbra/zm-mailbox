/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.soap.jaxb;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.soap.type.ZmBoolean;

/** Test JAXB class with an XmlElement list of enums */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="wrapped-required")
public class WrappedRequired {
    @XmlElementWrapper(name="wrapper", required=true)
    @XmlElement(name="enum-entry", required=true)
    private List<StringAttribIntValue> entries = Lists.newArrayList();

    @XmlElement(name="required-int", required=true)
    private int requiredInt;

    @XmlElement(name="required-bool", required=true)
    private ZmBoolean requiredBool;

    @XmlElement(name="required-complex", required=true)
    private StringAttribIntValue requiredComplex;


    public WrappedRequired() { }

    public void setEntries(Iterable <StringAttribIntValue> entries) {
        this.entries.clear();
        if (entries != null) {
            Iterables.addAll(this.entries,entries);
        }
    }

    public void addEntry(StringAttribIntValue entry) { this.entries.add(entry); }
    public List<StringAttribIntValue> getEntries() { return entries; }

    public int getRequiredInt() { return requiredInt; }
    public void setRequiredInt(int requiredInt) { this.requiredInt = requiredInt; }

    public Boolean getRequiredBool() { return ZmBoolean.toBool(requiredBool); }
    public void setRequiredBool(Boolean requiredBool) { this.requiredBool = ZmBoolean.fromBool(requiredBool); }

    public StringAttribIntValue getRequiredComplex() { return requiredComplex; }
    public void setRequiredComplex(StringAttribIntValue requiredComplex) { this.requiredComplex = requiredComplex; }
}
