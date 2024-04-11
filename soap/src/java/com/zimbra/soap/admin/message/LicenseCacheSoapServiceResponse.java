/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
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

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * response class for LicenseCacheService
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_LICENSE_CACHE_SOAP_SERVICE_RESPONSE)
public class LicenseCacheSoapServiceResponse {

    /**
     * no-argument constructor wanted by JAXB
     */
    private LicenseCacheSoapServiceResponse() {
        new LicenseCacheSoapServiceResponse((Boolean) null);
    }
    public LicenseCacheSoapServiceResponse(Boolean cacheRefreshed) {
        this.cacheRefreshed = ZmBoolean.fromBool(cacheRefreshed);

    }

    /**
     * @zm-api-field-tag cacheRefreshed
     * @zm-api-field-description flag for is Cache Refreshed
     */
    @XmlAttribute(name=AdminConstants.LICENSE_CACHE_REFRESHED /* cacheRefreshed */, required=true)
    public ZmBoolean cacheRefreshed;

    public ZmBoolean getCacheRefreshed() {
        return cacheRefreshed;
    }

    public void setCacheRefreshed(ZmBoolean cacheRefreshed) {
        this.cacheRefreshed = cacheRefreshed;
    }
}
