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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CosSelector;

/**
 * @zm-api-command-description Copy Class of service (COS)
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_COPY_COS_REQUEST)
@XmlType(propOrder = {})
public class CopyCosRequest {

    /**
     * @zm-api-field-tag dest-cos-name
     * @zm-api-field-description Destination name for COS
     */
    @XmlElement(name=AdminConstants.E_NAME)
    private String newName;

    /**
     * @zm-api-field-description Source COS
     */
    @XmlElement(name=AdminConstants.E_COS)
    private CosSelector cos;

    public CopyCosRequest() {
    }

    public CopyCosRequest(CosSelector cos, String newName) {
        this.newName = newName;
        this.cos = cos;
    }

    public void setNewName(String name) {
        this.newName = name;
    }

    public void setCos(CosSelector cos) {
        this.cos = cos;
    }

    public String getNewName() { return newName; }
    public CosSelector getCos() { return cos; }
}
