/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.UCServiceSelector;
import com.zimbra.soap.type.AttributeSelectorImpl;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Get UC Service
 */
@XmlRootElement(name=AdminConstants.E_GET_UC_SERVICE_REQUEST)
public class GetUCServiceRequest extends AttributeSelectorImpl {

    /**
     * @zm-api-field-description UC Service
     */
    @XmlElement(name=AdminConstants.E_UC_SERVICE)
    private UCServiceSelector ucService;

    public GetUCServiceRequest() {
        this(null);
    }

    public GetUCServiceRequest(UCServiceSelector ucService) {
        setUCService(ucService);
    }

    public void setUCService(UCServiceSelector ucService) {
        this.ucService = ucService;
    }

    public UCServiceSelector getUCService() { return ucService; }
}

