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
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlRootElement;

import org.w3c.dom.Element;

import com.google.common.collect.Lists;

/**
 * Test JAXB class to exercise a field annotated with {@link XmlAnyElement}, {@link XmlMixed} and
 * {@link XmlElementRefs} and {@link XmlElementRef}
 *
 * {@link XmlAnyElement} means that some of the objects in the list can be {@link Element}
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="mixed-tester")
public class MixedAnyTester {
    @XmlElementRefs({
        @XmlElementRef(type=StringAttribIntValue.class) /* note: tests out case where name isn't specified here */
    })
    @XmlAnyElement
    @XmlMixed
    private List<Object> elems = Lists.newArrayList();

    public MixedAnyTester() { }

    public List<Object> getElems() { return elems; }
    public void setElems(List<Object> elems) { this.elems = elems; }
}
