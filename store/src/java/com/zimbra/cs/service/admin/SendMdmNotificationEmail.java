/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016, 2022 Synacor, Inc.
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

import java.nio.charset.Charset;
import java.util.Map;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SyncAdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.rmgmt.RemoteManager;
import com.zimbra.cs.rmgmt.RemoteResult;
import com.zimbra.cs.service.admin.AdminDocumentHandler;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.Server;


public class SendMdmNotificationEmail extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Server localServer = prov.getLocalServer();
        checkRight(zsc, context, localServer, Admin.R_SendMdmNotificationEmail);
        String message = request.getElement(SyncAdminConstants.E_MESSAGE).getText();
        String status = request.getElement(SyncAdminConstants.E_STATUS).getText();
        RemoteManager remoteManager = RemoteManager.getRemoteManager(prov.getLocalServer());
        String command = "zmmdmnotificationmail \"" + message + "\" \"" + status + "\"";
        Element response = zsc.createElement(AdminConstants.E_SEND_MDM_NOTIFICATION_EMAIL_RESPONSE);
        RemoteResult remoteResult = remoteManager.execute(command);
        Charset csUtf8 = Charset.forName("UTF-8");
        String stdOut = (remoteResult.getMStdout() != null) ? new String(remoteResult.getMStdout(), csUtf8) : null;
        String stdErr = (remoteResult.getMStderr() != null) ? new String(remoteResult.getMStderr(), csUtf8) : null;
        if (remoteResult.getMExitStatus() == SyncAdminConstants.REMOTE_SERVER_SUCCESS) {
            if (stdErr != null) {
                response.addElement(SyncAdminConstants.E_STATUS).setText(MailConstants.SUCCESS);
                ZimbraLog.sync.debug(
                        "Command \"%s\" completed successfully with stderr output; exit code=%d; stderr=\n%s", command,
                        remoteResult.getMExitStatus(), stdErr);
            }
        } else {
            response.addElement(SyncAdminConstants.E_STATUS).setText("Failure");
            String errorMsg = String.format("Command \"%s\" failed; exit code=%d; stderr=\n%s", command,
                    remoteResult.getMExitStatus(), stdErr);
            ZimbraLog.sync.debug("errorMsg: %s ", errorMsg);
            throw ServiceException.FAILURE(errorMsg, null);
        }
        return response;
    }


    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_SendMdmNotificationEmail);
    }
}
