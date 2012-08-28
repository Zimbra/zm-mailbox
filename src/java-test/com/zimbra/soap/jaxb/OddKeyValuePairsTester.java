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
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.soap.account.type.Attr;
import com.zimbra.soap.json.jackson.annotate.ZimbraKeyValuePairs;

/**
 * Test {@link ZimbraKeyValuePairs} annotation, where the key/value pairs use different key/value names
 * to the default
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="key-value-pairs-tester")
public class OddKeyValuePairsTester {
    @XmlElement(name="oddElemName")
    @ZimbraKeyValuePairs
    private List<Attr> attrList;

    public OddKeyValuePairsTester() { }
    public OddKeyValuePairsTester(List<Attr> attrs) { setAttrList(attrs); }

    public List<Attr> getAttrList() { return attrList; }
    public void setAttrList(List<Attr> attrList) { this.attrList = attrList; }
}
