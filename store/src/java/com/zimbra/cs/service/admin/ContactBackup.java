/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.callback.CallbackUtil;
import com.zimbra.cs.mailbox.ContactBackupThread;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.ContactBackupRequest;
import com.zimbra.soap.admin.message.ContactBackupResponse;

public class ContactBackup extends AdminDocumentHandler {
    protected ZimbraSoapContext zsc = null;
    protected List<Integer> doneIds = null;
    protected List<Integer> skippedIds = null;

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        zsc = getZimbraSoapContext(context);
        ContactBackupRequest req = JaxbUtil.elementToJaxb(request);
        String task = req.getTask();
        ContactBackupResponse resp = new ContactBackupResponse();
        boolean setIds = false;

        switch (task) {
            case "status":
                getContactBackupStatus();
                setIds = true;
                break;
            case "stop":
                stopContactBackup();
                setIds = true;
                break;
            case "start":
            default:
                try {
                    startContactBackup(req.getId());
                } catch (ServiceException se) {
                    if (se.getCode().equals(ServiceException.ALREADY_IN_PROGRESS)) {
                        setIds = true;
                    }
                    throw se;
                }
                break;
        }
        if (setIds) {
            resp.setDoneIds(doneIds);
            resp.setSkippedIds(skippedIds);
        }
        return zsc.jaxbToElement(resp);
    }

    protected void getContactBackupStatus() throws ServiceException {
        if (!ContactBackupThread.isRunning()) {
            throw ServiceException.NOT_IN_PROGRESS(null, "ContactBackup is not running.");
        }
        doneIds = ContactBackupThread.getDoneMailboxIds();
        skippedIds = ContactBackupThread.getSkippedMailboxIds();
    }

    protected void stopContactBackup() throws ServiceException {
        if (!ContactBackupThread.isRunning()) {
            throw ServiceException.NOT_IN_PROGRESS(null, "ContactBackup is not running.");
        }
        doneIds = ContactBackupThread.getDoneMailboxIds();
        skippedIds = ContactBackupThread.getSkippedMailboxIds();
        ContactBackupThread.shutdown();
    }

    protected void startContactBackup(List<Integer> ids) throws ServiceException {
        if (ContactBackupThread.isRunning()) {
            doneIds = ContactBackupThread.getDoneMailboxIds();
            skippedIds = ContactBackupThread.getSkippedMailboxIds();
            throw ServiceException.ALREADY_IN_PROGRESS("ContactBackup is already runnig.");
        }
        if (ids != null && !ids.isEmpty()) {
            ContactBackupThread.startup(ids);
        } else {
            ContactBackupThread.startup(CallbackUtil.getSortedMailboxIdList());
        }
    }
}
