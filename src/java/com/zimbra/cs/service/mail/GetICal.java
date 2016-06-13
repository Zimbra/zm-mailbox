/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.HttpUtil.Browser;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;


public class GetICal extends MailDocumentHandler {

    private static final String[] TARGET_OBJ_PATH = new String[] { MailConstants.A_ID };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_OBJ_PATH; }

    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
     */
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account authAccount = getAuthenticatedAccount(zsc);
        Mailbox mbx = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        String iidStr = request.getAttribute(MailConstants.A_ID, null);
        long rangeStart = request.getAttributeLong(MailConstants.A_CAL_START_TIME, -1);
        long rangeEnd = request.getAttributeLong(MailConstants.A_CAL_END_TIME, -1);
        
//        int compNum = (int)request.getAttributeLong(MailService.A_CAL_COMPONENT_NUM);
        int compNum = 0;

        Browser browser = HttpUtil.guessBrowser(zsc.getUserAgent());
        boolean useOutlookCompatMode = Browser.IE.equals(browser);
        try {
            try {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                OutputStreamWriter wout = new OutputStreamWriter(buf);

                ZVCalendar cal = null;
                ItemId iid = iidStr != null ? new ItemId(iidStr, zsc) : null;
                if (iid != null) {
                    CalendarItem calItem = mbx.getCalendarItemById(octxt, iid.getId());
                    if (calItem == null) {
                        throw MailServiceException.NO_SUCH_CALITEM(iid.toString(), "Could not find calendar item");
                    }
                    Invite inv = calItem.getInvite(iid.getSubpartId(), compNum);
                    if (inv == null) {
                        throw MailServiceException.INVITE_OUT_OF_DATE(iid.toString());
                    }
                    boolean allowPrivateAccess = calItem.allowPrivateAccess(authAccount, zsc.isUsingAdminPrivileges());
                    cal = inv.newToICalendar(useOutlookCompatMode, allowPrivateAccess);
                    cal.toICalendar(wout);
                } else {
                    mbx.writeICalendarForRange(wout, octxt, rangeStart, rangeEnd, Mailbox.ID_FOLDER_CALENDAR,
                                               useOutlookCompatMode, false, false);
                }
                wout.flush();

                Element response = getResponseElement(zsc);
                Element icalElt = response.addElement(MailConstants.E_CAL_ICAL);
                if (iid != null)
                    icalElt.addAttribute(MailConstants.A_ID, new ItemIdFormatter(zsc).formatItemId(iid));
                icalElt.addText(buf.toString());
                return response;
            } catch(MailServiceException.NoSuchItemException e) {
                throw ServiceException.FAILURE("Error could get default invite for Invite: "+ iidStr + "-" + compNum, e);
            } catch (IOException e) {
                throw ServiceException.FAILURE("IO Exception while outputing Calendar for Invite: "+ iidStr + "-" + compNum, e);
            }
        } catch(MailServiceException.NoSuchItemException e) {
            throw ServiceException.FAILURE("No Such Invite Message: "+ iidStr, e);
        }
    }
}
