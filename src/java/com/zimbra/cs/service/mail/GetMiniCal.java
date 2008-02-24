/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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

package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.cache.CalendarData;
import com.zimbra.cs.mailbox.calendar.cache.CalendarItemData;
import com.zimbra.cs.mailbox.calendar.cache.InstanceData;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMailbox.ZGetMiniCalResult;
import com.zimbra.soap.ZimbraSoapContext;

/*
<GetMiniCalRequest s="range start time in millis" e="range end time in millis">
  <folder id="..."/>+
</GetMiniCalRequest>

<GetMiniCalResponse>
  <date>yyyymmdd</date>*
</GetMiniCalResponse>
 */

public class GetMiniCal extends CalendarRequest {

	@Override
	public Element handle(Element request, Map<String, Object> context)
			throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        Account authAcct = getAuthenticatedAccount(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        long rangeStart = request.getAttributeLong(MailConstants.A_CAL_START_TIME);
        long rangeEnd = request.getAttributeLong(MailConstants.A_CAL_END_TIME);

        List<ItemId> folderIids = new ArrayList<ItemId>();
        for (Iterator<Element> foldersIter = request.elementIterator(MailConstants.E_FOLDER); foldersIter.hasNext(); ) {
            Element fElem = foldersIter.next();
            ItemId iidFolder = new ItemId(fElem.getAttribute(MailConstants.A_ID), zsc);
            folderIids.add(iidFolder);
        }
        Map<String, List<Integer>> groupedByAcct = ItemId.groupFoldersByAccount(octxt, mbox, folderIids);

        ICalTimeZone tz = ICalTimeZone.getAccountTimeZone(authAcct);  // requestor's time zone, not mailbox owner's
        TreeSet<String> busyDates = new TreeSet<String>();

        List<Integer> localFolders = groupedByAcct.get(null);
        if (localFolders != null) {
            for (int folderId : localFolders) {
            	doLocalFolder(octxt, tz, mbox, folderId, rangeStart, rangeEnd, busyDates);
            }
        }
        for (Map.Entry<String, List<Integer>> entry : groupedByAcct.entrySet()) {
        	String acctId = entry.getKey();
        	if (acctId != null) {
            	List<Integer> remoteFolders = entry.getValue();
            	doRemoteAccount(authAcct, acctId, tz, remoteFolders, rangeStart, rangeEnd, busyDates);
        	}
        }

        Element response = getResponseElement(zsc);
        for (String datestamp : busyDates) {
        	Element dateElem = response.addElement(MailConstants.E_CAL_MINICAL_DATE);
        	dateElem.setText(datestamp);
        }

        return response;
	}

	private static void doLocalFolder(OperationContext octxt, ICalTimeZone tz, Mailbox mbox, int folderId,
									  long rangeStart, long rangeEnd, Set<String> busyDates)
	throws ServiceException {
		Calendar cal = new GregorianCalendar(tz);
        CalendarData calData = mbox.getCalendarSummaryForRange(
                octxt, folderId, MailItem.TYPE_APPOINTMENT, rangeStart, rangeEnd);
        if (calData != null) {
        	for (Iterator<CalendarItemData> itemIter = calData.calendarItemIterator(); itemIter.hasNext(); ) {
        		CalendarItemData item = itemIter.next();
        		for (Iterator<InstanceData> instIter = item.instanceIterator(); instIter.hasNext(); ) {
        			InstanceData inst = instIter.next();
        			Long start = inst.getDtStart();
        			if (start != null) {
        				String datestampStart = getDatestamp(cal, start);
        				busyDates.add(datestampStart);
        				Long duration = inst.getDuration();
        				if (duration != null) {
        					long end = start + duration;
        					String datestampEnd = getDatestamp(cal, end);
        					busyDates.add(datestampEnd);
        				}
        			}
        		}
        	}
        }
	}

    private static void doRemoteAccount(Account authAcct, String remoteAccountId, ICalTimeZone tz, List<Integer> remoteFolders,
    								    long rangeStart, long rangeEnd, Set<String> busyDates)
    throws ServiceException {
        String authtoken;
        try {
            authtoken = AuthToken.getAuthToken(authAcct).getEncoded();
        } catch (AuthTokenException e) {
            throw ServiceException.FAILURE("could not get auth token", e);
        }

        Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, remoteAccountId);
        ZMailbox.Options zoptions = new ZMailbox.Options(authtoken, AccountUtil.getSoapUri(target));
        zoptions.setTargetAccount(remoteAccountId);
        zoptions.setTargetAccountBy(AccountBy.id);
        zoptions.setNoSession(true);
        zoptions.setRequestProtocol(SoapProtocol.SoapJS);
        zoptions.setResponseProtocol(SoapProtocol.SoapJS);
        ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
        ZGetMiniCalResult result = zmbx.getMiniCal(remoteFolders, rangeStart, rangeEnd);

        for (String datestamp : result.getBusyDates()) {
        	busyDates.add(datestamp);
        }
    }

	private static String getDatestamp(Calendar cal, long millis) {
		cal.setTimeInMillis(millis);
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		return String.format("%04d%02d%02d", year, month, day);
	}
}
