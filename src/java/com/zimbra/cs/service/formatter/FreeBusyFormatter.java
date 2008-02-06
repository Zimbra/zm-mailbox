/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.formatter;

import java.io.IOException;

import javax.servlet.ServletException;

import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.common.service.ServiceException;

public class FreeBusyFormatter extends Formatter {

    private static final String ATTR_FREEBUSY = "zimbra_freebusy";

    public String getType() {
        return "freebusy";
    }

    public boolean requiresAuth() {
        return false;
    }
    
    public String getDefaultSearchTypes() {
        return MailboxIndex.SEARCH_FOR_APPOINTMENTS;
    }

    public void formatCallback(Context context)
    throws IOException, ServiceException, UserServletException, ServletException {
        context.req.setAttribute(ATTR_FREEBUSY, "true");
        HtmlFormatter.dispatchJspRest(context.getServlet(), context);
    }

    public boolean canBeBlocked() {
        return false;
    }
}
