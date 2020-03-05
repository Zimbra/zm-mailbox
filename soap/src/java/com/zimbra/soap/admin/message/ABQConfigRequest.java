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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Get ABQ Configs
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_ABQ_CONFIG_REQUEST)
public class ABQConfigRequest {
    @XmlEnum
    public enum AbqConfigOperation {
        // case must match
        get, add, modify, delete;

        public static AbqConfigOperation fromString(String s) throws ServiceException {
            try {
                return AbqConfigOperation.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    /**
     * @zm-api-field-tag op
     * @zm-api-field-description op can be either get, add, modify, delete
     */
    @XmlAttribute(name=AdminConstants.A_OP /* op */, required=true)
    private AbqConfigOperation op;

    /**
     * @zm-api-field-tag config-name
     * @zm-api-field-description ABQ config name
     */
    @XmlAttribute(name=AdminConstants.A_NAME /* s */, required=true)
    private String configName;

    /**
     * @zm-api-field-tag config-value
     * @zm-api-field-description ABQ config value
     */
    @XmlAttribute(name=AdminConstants.A_VALUE /* s */, required=false)
    private String configValue;

    /**
     * @zm-api-field-tag config-desc
     * @zm-api-field-description ABQ config description
     */
    @XmlAttribute(name=AdminConstants.A_DESCRIPTION /* s */, required=false)
    private String configDesc;

    /**
     * @zm-api-field-tag config-append
     * @zm-api-field-description ABQ config append to replace or append the value
     */
    @XmlAttribute(name=AdminConstants.E_ABQ_CONFIG_APPEND /* s */, required=false)
    private boolean configAppend;

    /**
     * @return the op
     */
    public AbqConfigOperation getOp() {
        return op;
    }

    /**
     * @param op the op to set
     */
    public void setOp(AbqConfigOperation op) {
        this.op = op;
    }

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

    /**
     * @return the configDesc
     */
    public String getConfigDesc() {
        return configDesc;
    }

    /**
     * @param configDesc the configDesc to set
     */
    public void setConfigDesc(String configDesc) {
        this.configDesc = configDesc;
    }

    /**
     * @return the configAppend
     */
    public boolean isConfigAppend() {
        return configAppend;
    }

    /**
     * @param configAppend the configAppend to set
     */
    public void setConfigAppend(boolean configAppend) {
        this.configAppend = configAppend;
    }
}