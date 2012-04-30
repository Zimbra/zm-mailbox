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

/** Test JAXB class for exercising changes in namespace associated with elements */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="ns-delta", namespace="urn:ZimbraTest4")
public class NamespaceDeltaElem {
    //@XmlElementRef(name="strAttrStrElem", namespace="urn:ZimbraTest5", type=StringAttrStringElem.class)
    @XmlElement(name="strAttrStrElem", namespace="urn:ZimbraTest5")
    private StringAttrStringElem sase;
    public NamespaceDeltaElem() { }
    public StringAttrStringElem getSase() { return sase; }
    public void setSase(StringAttrStringElem sase) { this.sase = sase; }
}
