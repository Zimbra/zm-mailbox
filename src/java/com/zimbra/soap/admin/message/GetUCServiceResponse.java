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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.UCServiceInfo;

@XmlRootElement(name=AdminConstants.E_GET_UC_SERVICE_RESPONSE)
public class GetUCServiceResponse {

    /**
     * @zm-api-field-description Information about ucservice
     */
    @XmlElement(name=AdminConstants.E_UC_SERVICE)
    private final UCServiceInfo ucService;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetUCServiceResponse() {
        this(null);
    }

    public GetUCServiceResponse(UCServiceInfo ucService) {
        this.ucService = ucService;
    }

    public UCServiceInfo getUCService() { return ucService; }
}
