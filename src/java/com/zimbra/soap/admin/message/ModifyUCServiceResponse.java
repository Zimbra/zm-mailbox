/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 1012 Zimbra, Inc.
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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.UCServiceInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_MODIFY_UC_SERVICE_RESPONSE)
@XmlType(propOrder = {})
public class ModifyUCServiceResponse {

    /**
     * @zm-api-field-description Information about ucservice
     */
    @XmlElement(name=AdminConstants.E_UC_SERVICE)
    private UCServiceInfo ucService;

    public ModifyUCServiceResponse() {
    }

    public void setUCService(UCServiceInfo ucService) {
        this.ucService = ucService;
    }

    public UCServiceInfo getUCService() {
        return ucService;
    }
}
