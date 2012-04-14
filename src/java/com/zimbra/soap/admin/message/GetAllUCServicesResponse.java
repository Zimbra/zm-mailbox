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

package com.zimbra.soap.admin.message;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.zimbra.common.soap.AdminConstants;

import com.zimbra.soap.admin.type.UCServiceInfo;

@XmlRootElement(name=AdminConstants.E_GET_ALL_UC_SERVICES_RESPONSE)
public class GetAllUCServicesResponse {

    /**
     * @zm-api-field-description Information about uc services
     */
    @XmlElement(name=AdminConstants.E_UC_SERVICE)
    private List <UCServiceInfo> ucServiceList = new ArrayList<UCServiceInfo>();

    public GetAllUCServicesResponse() {
    }

    public void addUCService(UCServiceInfo ucService) {
        this.getUCServiceList().add(ucService);
    }

    public List <UCServiceInfo> getUCServiceList() { return ucServiceList; }
}
