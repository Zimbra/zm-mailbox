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
