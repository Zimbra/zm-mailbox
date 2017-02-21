/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
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
