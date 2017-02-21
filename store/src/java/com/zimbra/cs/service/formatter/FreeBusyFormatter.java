/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import javax.servlet.ServletException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.UserServletContext;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;

public class FreeBusyFormatter extends Formatter {

    private static final String ATTR_FREEBUSY = "zimbra_freebusy";

    @Override
    public FormatType getType() {
        return FormatType.FREE_BUSY;
    }

    @Override
    public boolean requiresAuth() {
        return false;
    }

    @Override
    public Set<MailItem.Type> getDefaultSearchTypes() {
        return EnumSet.of(MailItem.Type.APPOINTMENT);
    }

    @Override
    public void formatCallback(UserServletContext context)
            throws IOException, ServiceException, UserServletException, ServletException {
        context.req.setAttribute(ATTR_FREEBUSY, "true");
        HtmlFormatter.dispatchJspRest(context.getServlet(), context);
    }
}
