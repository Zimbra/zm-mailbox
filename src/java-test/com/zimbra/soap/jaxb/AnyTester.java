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
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.w3c.dom.Element;

import com.google.common.collect.Lists;

/**
 * Test JAXB class to exercise a field annotated with {@link XmlAnyElement}
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="any-elem-tester")
public class AnyTester {
    @XmlElement
    private String given;
    
    @XmlAnyElement
    private List<Element> elems = Lists.newArrayList();

    public AnyTester() { }

    public List<Element> getElems() { return elems; }
    public void setElems(List<Element> elems) { this.elems = elems; }

    public String getGiven() { return given; }
    public void setGiven(String given) { this.given = given; }
}
