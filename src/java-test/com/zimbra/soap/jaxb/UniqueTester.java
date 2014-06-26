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

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;

/** Test JAXB class with a variety of XmlElements which should be treated as unique or normally */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="unique-tester")
public class UniqueTester {
    @ZimbraUniqueElement
    @XmlElement(name="unique-str-elem", namespace="urn:zimbraTest1", required=false)
    private String uniqueStrElem;

    @XmlElement(name="non-unique-elem", namespace="urn:zimbraTest1", required=false)
    private String nonUniqueStrElem;

    @ZimbraUniqueElement
    @XmlElement(name="unique-complex-elem", required=false)
    private StringAttribIntValue uniqueComplexElem;

    public UniqueTester() { }

    public String getUniqueStrElem() {
        return uniqueStrElem;
    }
    public void setUniqueStrElem(String uniqueStrElem) {
        this.uniqueStrElem = uniqueStrElem;
    }
    public String getNonUniqueStrElem() {
        return nonUniqueStrElem;
    }
    public void setNonUniqueStrElem(String nonUniqueStrElem) {
        this.nonUniqueStrElem = nonUniqueStrElem;
    }
    public StringAttribIntValue getUniqueComplexElem() {
        return uniqueComplexElem;
    }
    public void setUniqueComplexElem(StringAttribIntValue uniqueComplexElem) {
        this.uniqueComplexElem = uniqueComplexElem;
    }
}
