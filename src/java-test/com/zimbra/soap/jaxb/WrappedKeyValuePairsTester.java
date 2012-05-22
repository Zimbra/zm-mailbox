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

import com.zimbra.common.soap.Element;
import com.zimbra.soap.json.jackson.annotate.ZimbraKeyValuePairs;
import com.zimbra.soap.type.KeyValuePair;

/**
 * Test {@link ZimbraKeyValuePairs} annotation
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="wrapped-key-value-pairs-tester")
public class WrappedKeyValuePairsTester {
    // Note: Can use a different name BUT won't be able to round trip back to JAXB from JSON if you do
    @XmlElementWrapper(name="wrapper")
    @XmlElement(name=Element.XMLElement.E_ATTRIBUTE /* a */)
    @ZimbraKeyValuePairs
    private List<KeyValuePair> attrList;

    public WrappedKeyValuePairsTester() { }
    public WrappedKeyValuePairsTester(List<KeyValuePair> attrs) { setAttrList(attrs); }

    public List<KeyValuePair> getAttrList() { return attrList; }
    public void setAttrList(List<KeyValuePair> attrList) { this.attrList = attrList; }
}
