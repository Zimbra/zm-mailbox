/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_ABQ_CONFIG_RESPONSE)
public class ABQConfigResponse {
    /**
     * @zm-api-field-tag configName
     * @zm-api-field-description abq config name
     */
    @XmlElement(name=AdminConstants.A_NAME /* config name */, required=true)
    private String configName;

    /**
     * @zm-api-field-tag configValue
     * @zm-api-field-description abq config value
     */
    @XmlElement(name=AdminConstants.A_VALUE /* config value */, required=true)
    private String configValue;

    /**
     * @return the configName
     */
    public String getConfigName() {
        return configName;
    }

    /**
     * @param configName the configName to set
     */
    public void setConfigName(String configName) {
        this.configName = configName;
    }

    /**
     * @return the configValue
     */
    public String getConfigValue() {
        return configValue;
    }

    /**
     * @param configValue the configValue to set
     */
    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }
}
