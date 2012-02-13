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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;

/**
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
