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
import java.util.Iterator;

import javax.mail.Part;
import javax.mail.internet.ContentDisposition;
import javax.mail.internet.ParseException;

import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServlet.Context;

public class CsvFormatter extends Formatter {

    public String getType() {
        return "csv";
    }

    public String getDefaultSearchTypes() {
        return MailboxIndex.SEARCH_FOR_CONTACTS;
    }

    public void format(Context context, MailItem mailItem) throws IOException, ServiceException {
        Iterator iterator = getMailItems(context, mailItem, -1, -1);
        
        StringBuffer sb = new StringBuffer();
        ContactCSV.toCSV(iterator, sb);

        ContentDisposition cd = null;
        try { cd = new ContentDisposition(Part.ATTACHMENT); } catch (ParseException e) {}
        // todo: get from folder name
        String fname = context.itemPath;
        if (fname == null || fname.length() == 0) fname ="contacts";
        cd.setParameter("filename", fname+".csv");
        context.resp.addHeader("Content-Disposition", cd.toString());
        context.resp.setCharacterEncoding("UTF-8");
        context.resp.setContentType("text/plain");
        context.resp.getOutputStream().print(sb.toString());
    }

    public boolean canBeBlocked() {
        return false;
    }

}
