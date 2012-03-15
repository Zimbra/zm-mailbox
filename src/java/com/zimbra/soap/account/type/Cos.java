/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * <cos name="cos-name" id="cos-id"/>
 */
@XmlAccessorType(XmlAccessType.NONE)
public class Cos {

    /**
     * @zm-api-field-tag cos-name
     * @zm-api-field-description Class of Service (COS) name
     */
    @XmlAttribute private String name;

    /**
     * @zm-api-field-tag cos-id
     * @zm-api-field-description Class of Service (COS) ID
     */
    @XmlAttribute private String id;

    public Cos() {
    }

    public String getName() { return name; }
    public String getId() { return id; }

    public Cos setName(String name) {
        this.name = name;
        return this;
    }

    public Cos setId(String id) {
        this.id = id;
        return this;
    }
}
