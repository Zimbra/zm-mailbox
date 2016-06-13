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
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.AutoProvisionThread;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.AutoProvTaskControlRequest;
import com.zimbra.soap.admin.message.AutoProvTaskControlResponse;

public class AutoProvTaskControl extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        checkRight(zsc, context, null, AdminRight.PR_SYSTEM_ADMIN_ONLY);
        
        AutoProvTaskControlRequest.Action action = AutoProvTaskControlRequest.Action.fromString(request.getAttribute(MailConstants.E_ACTION));
        
        AutoProvTaskControlResponse.Status result;
        switch (action) {
            case start:
                result = handleStart();
                break;
            case status:
                result = handleStatus();
                break;
            case stop: 
                result = handleStop();
                break;
            default:
                throw ServiceException.INVALID_REQUEST("unsupported action: " + action, null);
        }
        
        Element response = zsc.createElement(AdminConstants.AUTO_PROV_TASK_CONTROL_RESPONSE);
        response.addAttribute(AdminConstants.A_STATUS, result.name());
        return response;
    }
    
    private AutoProvTaskControlResponse.Status handleStart() {
        if (AutoProvisionThread.isRunning()) {
            return AutoProvTaskControlResponse.Status.running;
        } else {
            AutoProvisionThread.startup();
            return AutoProvTaskControlResponse.Status.started;
        }
    }
    
    private AutoProvTaskControlResponse.Status handleStatus() {
        if (AutoProvisionThread.isRunning()) {
            return AutoProvTaskControlResponse.Status.running;
        } else {
            return AutoProvTaskControlResponse.Status.idle;
        }
    }
    
    private AutoProvTaskControlResponse.Status handleStop() {
        if (AutoProvisionThread.isRunning()) {
            AutoProvisionThread.shutdown();
            return AutoProvTaskControlResponse.Status.stopped;
        } else {
            return AutoProvTaskControlResponse.Status.idle;
        }
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.SYSTEM_ADMINS_ONLY);
    }
}
