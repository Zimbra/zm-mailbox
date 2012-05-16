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
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

/** Test JAXB class to exercise a field annotated with {@link XmlElementRef} */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="elem-ref-tester")
public class ElementRefTester {
    @XmlElementRef(name="string-attr-int-value", type=StringAttribIntValue.class)
    private StringAttribIntValue byRef;

    public ElementRefTester() { }

    public StringAttribIntValue getByRef() { return byRef; }
    public void setByRef(StringAttribIntValue byRef) { this.byRef = byRef; }
}
