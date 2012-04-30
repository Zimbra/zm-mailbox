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
