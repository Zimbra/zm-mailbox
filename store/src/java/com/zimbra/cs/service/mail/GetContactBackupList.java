/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.GetContactBackupListResponse;

public final class GetContactBackupList extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        List<String> backup = getContactBackupList(account);

        GetContactBackupListResponse res = new GetContactBackupListResponse(backup);
        return zsc.jaxbToElement(res);
    }

    // TODO : Update this method along with contact backup functionality
    private List<String> getContactBackupList(Account account) {
        List<String> list = new ArrayList<String>();
        list.add("file1.tgz");
        list.add("file2.tgz");
        list.add("file3.tgz");
        list.add("file4.tgz");
        return list;
    }
}
