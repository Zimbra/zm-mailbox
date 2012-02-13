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
