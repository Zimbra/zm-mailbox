package com.zimbra.cs.service.formatter;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServlet.Context;

public abstract class Formatter {
    
    public abstract String getType();
    public abstract boolean format(UserServlet.Context context, MailItem item) throws IOException, ServiceException;
    
    public boolean notImplemented(Context context, String message) throws IOException {
        context.resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, message);
        return false;
    }
    
    public boolean notImplemented(Context context) throws IOException {
        context.resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "support for requested item/format not implemented yet");
        return false;
    }

}
