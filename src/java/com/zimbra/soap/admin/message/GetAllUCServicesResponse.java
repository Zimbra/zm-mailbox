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
