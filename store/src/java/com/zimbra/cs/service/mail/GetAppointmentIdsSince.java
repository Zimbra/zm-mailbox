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

package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.mailbox.FolderConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.GetAppointmentIdsSinceResponse;

public class GetAppointmentIdsSince extends MailDocumentHandler {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.A_FOLDER };

    @Override
    protected String[] getProxiedIdPath(Element request) {
        return TARGET_FOLDER_PATH;
    }

    @Override
    protected boolean checkMountpointProxy(Element request) {
        return true;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        OperationContext octxt = getOperationContext(zsc, context);
        Mailbox mbox = getRequestedMailbox(zsc);

        int lastSync = request.getAttributeInt(MailConstants.A_CAL_LAST_SYNC);
        ItemId iidFolder = new ItemId(request.getAttribute(MailConstants.A_FOLDER), zsc);

        Map<Integer, Integer> map = mbox.listCalendarItemIdsAndDateForRange(octxt, MailItem.Type.APPOINTMENT, lastSync, iidFolder.getId());
        GetAppointmentIdsSinceResponse resp = new GetAppointmentIdsSinceResponse();
        map.forEach((id, folderId) -> {
            if (folderId == FolderConstants.ID_FOLDER_TRASH) {
                resp.addDids(id);
            } else {
                resp.addMids(id);
            }
        });
        return JaxbUtil.jaxbToElement(resp);
    }
}
