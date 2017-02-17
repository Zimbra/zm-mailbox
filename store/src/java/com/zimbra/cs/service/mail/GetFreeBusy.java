/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.fb.FreeBusyQuery;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

public class GetFreeBusy extends MailDocumentHandler {


//    <GetFreeBusyRequest s="date" e="date" [uid="id,..."]/>
//    <GetFreeBusyResponse>
//      <usr id="id">
//        <f s="date" e="date"/>*
//        <b s="date" e="date"/>*
//        <t s="date" e="date"/>*
//        <o s="date" e="date"/>*
//      </usr>
//    <GetFreeBusyResponse>
//
//    (f)ree (b)usy (t)entative and (o)ut-of-office

    private static final long MSEC_PER_DAY = 1000*60*60*24;

    private static final long MAX_PERIOD_SIZE_IN_DAYS = 200;

    protected static void validateRange(long rangeStart, long rangeEnd) throws ServiceException {
        if (rangeEnd <= rangeStart)
            throw ServiceException.INVALID_REQUEST("End time must be after Start time", null);
        long days = (rangeEnd - rangeStart) / MSEC_PER_DAY;
        if (days > MAX_PERIOD_SIZE_IN_DAYS)
            throw ServiceException.INVALID_REQUEST("Requested range is too large (Maximum "+MAX_PERIOD_SIZE_IN_DAYS+" days)", null);
    }

    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }

    /**
     * FreeBusy requests need to always return valid looking output in able to provide consistent behavior between
     * requests against non-existent accounts and those which don't allow access to the requested data.
     * Returning true here flags GetFreeBusy as accepting responsibility for measures to prevent account harvesting
     */
    @Override
    public boolean handlesAccountHarvesting() {
        return true;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zc = getZimbraSoapContext(context);

        long rangeStart = request.getAttributeLong(MailConstants.A_CAL_START_TIME);
        long rangeEnd = request.getAttributeLong(MailConstants.A_CAL_END_TIME);
        validateRange(rangeStart, rangeEnd);

        Element response = getResponseElement(zc);

        // MailConstants.A_UID should be deprecated at some point, bug 21776, comment #14
        String uidParam = request.getAttribute(MailConstants.A_UID, null);    // comma-separated list of account emails or zimbraId GUIDs that *must* match UUID format
        String idParam = request.getAttribute(MailConstants.A_ID, null);    // comma-separated list of account zimbraId GUIDs
        String nameParam = request.getAttribute(MailConstants.A_NAME, null); // comma-separated list of account emails
        String exApptUid = request.getAttribute(MailConstants.A_APPT_FREEBUSY_EXCLUDE_UID, null);

        Account requestor = Provisioning.getInstance().get(Key.AccountBy.id, zc.getAuthtokenAccountId());
    	FreeBusyQuery fbQuery = new FreeBusyQuery((HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST), zc, requestor, rangeStart, rangeEnd, exApptUid);

        String[] idStrs = null;

        // uidParam should be deprecated at some point, bug 21776 comment #14
    	if (uidParam != null) {
    		idStrs = uidParam.split(",");
    		for (String idStr : idStrs)
        		fbQuery.addId(idStr, FreeBusyQuery.CALENDAR_FOLDER_ALL);
    	}
    	if (idParam != null) {
    		idStrs = idParam.split(",");
    		for (String idStr : idStrs)
        		fbQuery.addAccountId(idStr, FreeBusyQuery.CALENDAR_FOLDER_ALL);
    	}
    	if (nameParam != null) {
    		idStrs = nameParam.split(",");
    		for (String idStr : idStrs)
        		fbQuery.addEmailAddress(idStr, FreeBusyQuery.CALENDAR_FOLDER_ALL);
    	}

    	for (Iterator<Element> usrIter = request.elementIterator(MailConstants.E_FREEBUSY_USER); usrIter.hasNext(); ) {
    	    Element usrElem = usrIter.next();
    	    int folderId = (int) usrElem.getAttributeLong(MailConstants.A_FOLDER, FreeBusyQuery.CALENDAR_FOLDER_ALL);
    	    if (folderId == Mailbox.ID_FOLDER_USER_ROOT || folderId == 0)
    	        folderId = FreeBusyQuery.CALENDAR_FOLDER_ALL;
    	    String id = usrElem.getAttribute(MailConstants.A_ID, null);
    	    if (id != null)
    	        fbQuery.addAccountId(id, folderId);
    	    String name = usrElem.getAttribute(MailConstants.A_NAME, null);
    	    if (name != null)
    	        fbQuery.addEmailAddress(name, folderId);
    	}

    	fbQuery.getResults(response);
        return response;
    }
}
