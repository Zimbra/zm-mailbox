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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Under normal situations, the EAGER auto provisioning 
 * task(thread) should be started/stopped automatically by the server when appropriate.  
 * The task should be running when zimbraAutoProvPollingInterval is not 0 and 
 * zimbraAutoProvScheduledDomains is not empty.  The task should be stopped otherwise.
 * This API is to manually force start/stop or query status of the EAGER auto provisioning task. 
 * It is only for diagnosis purpose and should not be used under normal situations.
 */
@XmlRootElement(name=AdminConstants.E_AUTO_PROV_TASK_CONTROL_REQUEST)
public class AutoProvTaskControlRequest {
    
    @XmlEnum
    public static enum Action {
        start,
        status,
        stop;
        
        public static Action fromString(String action) throws ServiceException {
            try {
                return Action.valueOf(action);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown action: " + action, e);
            }
        }
    }

    /**
     * @zm-api-field-description Action to perform - one of <b>start|status|stop</b>
     */
    @XmlAttribute(name=AdminConstants.E_ACTION, required=true)
    private final Action action;
    
    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AutoProvTaskControlRequest() {
        this((Action)null);
    }
    
    public AutoProvTaskControlRequest(Action action) {
        this.action = action;
    }
    
    public Action getAction() {
        return action;
    }
}
