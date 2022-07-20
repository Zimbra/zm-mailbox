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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.admin.message.SendEmailRequest;
import com.zimbra.soap.admin.message.SendEmailResponse;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.rmgmt.RemoteManager;
import com.zimbra.cs.rmgmt.RemoteResult;
import com.zimbra.common.soap.SyncAdminConstants;
import com.zimbra.common.soap.MailConstants;

public class SendEmail extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        SendEmailRequest req = zsc.elementToJaxb(request);
        String to = request.getElement(SyncAdminConstants.E_TO).getText();
        String subject = request.getElement(SyncAdminConstants.E_SUBJECT).getText();
        String message = request.getElement(SyncAdminConstants.E_MESSAGE).getText();
        RemoteManager remoteManager = RemoteManager.getRemoteManager(prov.getLocalServer());
        String command = "zmemail \"" + to + "\" \"" + subject + "\" \"" + message + "\"";
        Element response = zsc.createElement(AdminConstants.E_SEND_EMAIL_RESPONSE);
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
}
