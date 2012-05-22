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

import com.zimbra.soap.account.type.ObjectInfo;

/**
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="object-info-tester")
public class ObjectInfoTester {
    @XmlElement(name="obj-info", required=true)
    private ObjectInfo objectInfo;
    public ObjectInfoTester() { }
    public ObjectInfoTester(ObjectInfo oi) { setObjectInfo(oi); }
    public ObjectInfo getObjectInfo() { return objectInfo; }
    public void setObjectInfo(ObjectInfo objectInfo) { this.objectInfo = objectInfo; }
}
