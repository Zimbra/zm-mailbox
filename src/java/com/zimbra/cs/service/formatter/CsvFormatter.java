/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.Part;
import javax.mail.internet.ContentDisposition;
import javax.mail.internet.ParseException;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.mailbox.ContactCSV;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServlet;

public class CsvFormatter {
    public static void format(UserServlet.Context context, Folder f) throws IOException, ServiceException {
        if (f.getDefaultView() != Folder.TYPE_CONTACT) {
            context.resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "CSV support for requested folder type not implemented yet");
            return;
        }
        List contacts = context.targetMailbox.getContactList(context.opContext, f.getId());
        StringBuffer sb = new StringBuffer();
        if (contacts == null)
            contacts = new ArrayList();
        ContactCSV.toCSV(contacts, sb);

        ContentDisposition cd = null;
        try { cd = new ContentDisposition(Part.ATTACHMENT); } catch (ParseException e) {}
        cd.setParameter("filename", context.itemPath+".csv");
        context.resp.addHeader("Content-Disposition", cd.toString());
        context.resp.setContentType("text/plain");
        context.resp.getOutputStream().print(sb.toString());
    }
}
