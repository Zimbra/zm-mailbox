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
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Lists;

/** Test JAXB class to exercise a field annotated with {@link XmlElementRefs} and {@link XmlElementRef} */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="elem-ref-tester")
public class ElementRefsTester {
    @XmlElementRefs({
        @XmlElementRef(name="saivByRef", type=StringAttribIntValue.class),
        @XmlElementRef(name="eaByRef", type=EnumAttribs.class)
    })
    private List<Object> elems = Lists.newArrayList();

    public ElementRefsTester() { }

    public List<Object> getElems() { return elems; }
    public void setElems(List<Object> elems) { this.elems = elems; }
}
