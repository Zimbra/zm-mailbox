/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.WellKnownTimeZones;
import com.zimbra.cs.mailbox.calendar.cache.CalSummaryCache;
import com.zimbra.cs.mailbox.calendar.cache.CalendarCacheManager;
import com.zimbra.cs.mailbox.calendar.cache.CalendarData;
import com.zimbra.cs.mailbox.calendar.cache.CalendarItemData;
import com.zimbra.cs.mailbox.calendar.cache.InstanceData;
import com.zimbra.cs.mailbox.calendar.cache.CalSummaryCache.CalendarDataResult;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMailbox.ZGetMiniCalResult;
import com.zimbra.cs.zclient.ZMailbox.ZMiniCalError;
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
        Element response = getResponseElement(zsc);

        long rangeStart = request.getAttributeLong(MailConstants.A_CAL_START_TIME);
        long rangeEnd = request.getAttributeLong(MailConstants.A_CAL_END_TIME);

        List<ItemId> folderIids = new ArrayList<ItemId>();
        for (Iterator<Element> foldersIter = request.elementIterator(MailConstants.E_FOLDER); foldersIter.hasNext(); ) {
            Element fElem = foldersIter.next();
            ItemId iidFolder = new ItemId(fElem.getAttribute(MailConstants.A_ID), zsc);
            folderIids.add(iidFolder);
        }

        ICalTimeZone tz = parseTimeZone(request);
        if (tz == null)
            tz = ICalTimeZone.getAccountTimeZone(authAcct);  // requestor's time zone, not mailbox owner's
        TreeSet<String> busyDates = new TreeSet<String>();

        Provisioning prov = Provisioning.getInstance();
        MailboxManager mboxMgr = MailboxManager.getInstance();
        Server localServer = prov.getLocalServer();

        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);
        Map<ItemId, Resolved> resolved = resolveMountpoints(octxt, mbox, folderIids);
        Map<ItemId /* resolved iid */, ItemId /* requested iid */> reverseMap = new HashMap<ItemId, ItemId>();
        for (Map.Entry<ItemId, Resolved> entry : resolved.entrySet()) {
            ItemId requestedIid = entry.getKey();
            Resolved res = entry.getValue();
            if (res.error == null) {
                reverseMap.put(res.iid, requestedIid);
            } else {
                addError(response, ifmt.formatItemId(requestedIid), res.error.getCode(), res.error.getMessage());
            }
        }
        Map<Server, Map<String /* account id */, List<Integer> /* folder ids */>> groupedByServer =
            Search.groupByServer(groupFoldersByAccount(resolved));

        // Look up in calendar cache first.
        if (LC.calendar_cache_enabled.booleanValue()) {
            CalSummaryCache calCache = CalendarCacheManager.getInstance().getSummaryCache();
            Calendar cal = new GregorianCalendar(tz);
            for (Iterator<Map.Entry<Server, Map<String, List<Integer>>>> serverIter = groupedByServer.entrySet().iterator();
                 serverIter.hasNext(); ) {
                Map.Entry<Server, Map<String, List<Integer>>> serverMapEntry = serverIter.next();
                Map<String, List<Integer>> accountFolders = serverMapEntry.getValue();
                // for each account
                for (Iterator<Map.Entry<String, List<Integer>>> acctIter = accountFolders.entrySet().iterator();
                     acctIter.hasNext(); ) {
                    Map.Entry<String, List<Integer>> acctEntry = acctIter.next();
                    String acctId = acctEntry.getKey();
                    List<Integer> folderIds = acctEntry.getValue();
                    // for each folder
                    for (Iterator<Integer> iterFolderId = folderIds.iterator(); iterFolderId.hasNext(); ) {
                        int folderId = iterFolderId.next();
                        try {
                            CalendarDataResult result = calCache.getCalendarSummary(octxt, acctId, folderId, MailItem.TYPE_APPOINTMENT, rangeStart, rangeEnd, true);
                            if (result != null) {
                                // Found data in cache.
                                iterFolderId.remove();
                                addBusyDates(cal, result.data, rangeStart, rangeEnd, busyDates);
                            }
                        } catch (ServiceException e) {
                            iterFolderId.remove();
                            ItemId iid = new ItemId(acctId, folderId);
                            ItemId reqIid = reverseMap.get(iid);  // Error must mention folder id requested by client.
                            if (reqIid != null) {
                                ZimbraLog.calendar.warn("Error accessing calendar folder " + ifmt.formatItemId(reqIid), e);
                                addError(response, ifmt.formatItemId(reqIid), e.getCode(), e.getMessage());
                            } else {
                                ZimbraLog.calendar.warn("Error accessing calendar folder; resolved id=" +
                                        ifmt.formatItemId(iid) + " (missing reverse mapping)", e);
                                addError(response, ifmt.formatItemId(iid), e.getCode(), e.getMessage());
                            }
                        }
                    }
                    if (folderIds.isEmpty())
                        acctIter.remove();
                }
                if (accountFolders.isEmpty())
                    serverIter.remove();
            }
        }

        // For any remaining calendars, we have to get the data the hard way.
        for (Map.Entry<Server, Map<String, List<Integer>>> serverMapEntry : groupedByServer.entrySet()) {
            Server server = serverMapEntry.getKey();
            Map<String, List<Integer>> accountFolders = serverMapEntry.getValue();
            if (server.equals(localServer)) {  // local server
                for (Map.Entry<String, List<Integer>> entry : accountFolders.entrySet()) {
                    String acctId = entry.getKey();
                    List<Integer> folderIds = entry.getValue();
                    Account targetAcct = prov.get(AccountBy.id, acctId);
                    if (targetAcct == null) {
                        ZimbraLog.calendar.warn("Skipping unknown account " + acctId + " during minical search");
                        continue;
                    }
                    Mailbox targetMbox = mboxMgr.getMailboxByAccount(targetAcct);
                    for (int folderId : folderIds) {
                        try {
                            doLocalFolder(octxt, tz, targetMbox, folderId, rangeStart, rangeEnd, busyDates);
                        } catch (ServiceException e) {
                            ItemId iid = new ItemId(acctId, folderId);
                            ItemId reqIid = reverseMap.get(iid);  // Error must mention folder id requested by client.
                            if (reqIid != null) {
                                ZimbraLog.calendar.warn("Error accessing calendar folder " + ifmt.formatItemId(reqIid), e);
                                addError(response, ifmt.formatItemId(reqIid), e.getCode(), e.getMessage());
                            } else {
                                ZimbraLog.calendar.warn("Error accessing calendar folder; resolved id=" +
                                        ifmt.formatItemId(iid) + " (missing reverse mapping)", e);
                                addError(response, ifmt.formatItemId(iid), e.getCode(), e.getMessage());
                            }
                        }
                    }
                }
            } else {  // remote server
                String nominalTargetAcctId = null;  // mail service soap requests want to see a target account
                List<String> folderList = new ArrayList<String>();
                for (Map.Entry<String, List<Integer>> entry : accountFolders.entrySet()) {
                    String acctId = entry.getKey();
                    if (nominalTargetAcctId == null)
                        nominalTargetAcctId = acctId;
                    ItemIdFormatter ifmtRemote = new ItemIdFormatter(authAcct.getId(), acctId, false);
                    List<Integer> folderIds = entry.getValue();
                    for (int folderId : folderIds) {
                        folderList.add(ifmtRemote.formatItemId(folderId));
                    }
                }
                doRemoteFolders(zsc, nominalTargetAcctId, folderList, rangeStart, rangeEnd, busyDates, response, reverseMap, ifmt);
            }
        }

        for (String datestamp : busyDates) {
        	Element dateElem = response.addElement(MailConstants.E_CAL_MINICAL_DATE);
        	dateElem.setText(datestamp);
        }

        return response;
	}

	private static void addBusyDates(Calendar cal, CalendarData calData, long rangeStart, long rangeEnd, Set<String> busyDates)
	throws ServiceException {
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

	private static void doLocalFolder(OperationContext octxt, ICalTimeZone tz, Mailbox mbox, int folderId,
									  long rangeStart, long rangeEnd, Set<String> busyDates)
	throws ServiceException {
		Calendar cal = new GregorianCalendar(tz);
        CalendarDataResult result = mbox.getCalendarSummaryForRange(
                octxt, folderId, MailItem.TYPE_APPOINTMENT, rangeStart, rangeEnd);
        if (result != null)
            addBusyDates(cal, result.data, rangeStart, rangeEnd, busyDates);
	}

    private static void doRemoteFolders(
            ZimbraSoapContext zsc, String remoteAccountId, List<String> remoteFolders, long rangeStart, long rangeEnd,
            Set<String> busyDates, Element response, Map<ItemId, ItemId> reverseIidMap, ItemIdFormatter ifmt) {
        try {
            Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, remoteAccountId);
            if (target == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(remoteAccountId);
            ZMailbox.Options zoptions = new ZMailbox.Options(zsc.getAuthToken().toZAuthToken(), AccountUtil.getSoapUri(target));
            zoptions.setTargetAccount(remoteAccountId);
            zoptions.setTargetAccountBy(AccountBy.id);
            zoptions.setNoSession(true);
            ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
            String remoteIds[] = new String[remoteFolders.size()];
            for (int i=0; i < remoteIds.length; i++) remoteIds[i] = remoteFolders.get(i).toString();
            ZGetMiniCalResult result = zmbx.getMiniCal(rangeStart, rangeEnd, remoteIds);
            Set<String> dates = result.getDates();
            if (dates != null) {
                for (String datestamp : dates) {
                    busyDates.add(datestamp);
                }
            }
            List<ZMiniCalError> errors = result.getErrors();
            if (errors != null) {
                for (ZMiniCalError error : errors) {
                    try {
                        ItemId iid = new ItemId(error.getFolderId(), zsc);
                        ItemId reqIid = reverseIidMap.get(iid);  // Error must mention folder id requested by client.
                        String fid = ifmt.formatItemId(reqIid != null ? reqIid : iid);
                        addError(response, fid, error.getErrCode(), error.getErrMsg());
                    } catch (ServiceException e) {}
                }
            }
        } catch (ServiceException e) {
            ZimbraLog.calendar.warn("Error making remote GetMiniCalRequest", e);
            // Mark all remote folders with the same error.
            for (String remoteFid : remoteFolders) {
                try {
                    ItemId iid = new ItemId(remoteFid, zsc);
                    ItemId reqIid = reverseIidMap.get(iid);  // Error must mention folder id requested by client.
                    String fid = ifmt.formatItemId(reqIid != null ? reqIid : iid);
                    addError(response, fid, e.getCode(), e.getMessage());
                } catch (ServiceException e2) {}
            }
        }
    }

	private static String getDatestamp(Calendar cal, long millis) {
		cal.setTimeInMillis(millis);
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		return Integer.toString(year * 10000 + month * 100 + day);
	}

	private static ICalTimeZone parseTimeZone(Element request) throws ServiceException {
        Element tzElem = request.getOptionalElement(MailConstants.E_CAL_TZ);
        if (tzElem != null) {
            String tzid = tzElem.getAttribute(MailConstants.A_ID, null);
            if (tzid != null) {
                ICalTimeZone knownTZ = WellKnownTimeZones.getTimeZoneById(tzid);
                if (knownTZ != null)
                    return knownTZ;
            }

            // custom timezone            
            String stdOffset = tzElem.getAttribute(MailConstants.A_CAL_TZ_STDOFFSET, null);
            if (stdOffset == null)
                throw ServiceException.INVALID_REQUEST(
                        "Unknown TZ: \"" + tzid + "\" and no " + MailConstants.A_CAL_TZ_STDOFFSET + " specified", null);
            
            return CalendarUtils.parseTzElement(tzElem);
        } else {
            return null;
        }
	}

	private static class Resolved {
	    public ItemId iid;
	    public ServiceException error;
	    Resolved(ItemId iid, ServiceException e) {
	        this.iid = iid;
	        this.error = e;
	    }
	}

	// Resolve mountpoints for each requested folder.  Resolution can result in error per folder.  Typical errors
	// include PERM_DENIED (if sharer revoked permission), NO_SUCH_FOLDER (if sharer deleted shared folder), and
	// NO_SUCH_ACCOUNT (if sharer account has been deleted).
    private static Map<ItemId, Resolved> resolveMountpoints(OperationContext octxt, Mailbox mbox, List<ItemId> folderIids) {
        Map<ItemId, Resolved> result = new HashMap<ItemId, Resolved>();
        for (ItemId iidFolder : folderIids) {
            String targetAccountId = iidFolder.getAccountId();
            int folderId = iidFolder.getId();
            try {
                ServiceException error = null;
                if (mbox.getAccountId().equals(targetAccountId)) {
                    boolean isMountpoint = true;
                    int hopCount = 0;
                    // resolve local mountpoint to a real folder; deal with possible mountpoint chain
                    while (isMountpoint && hopCount < ZimbraSoapContext.MAX_HOP_COUNT) {
                        Folder folder = mbox.getFolderById(octxt, folderId);
                        isMountpoint = folder instanceof Mountpoint;
                        if (isMountpoint) {
                            Mountpoint mp = (Mountpoint) folder;
                            folderId = mp.getRemoteId();
                            if (!mp.isLocal()) {
                                // done resolving if pointing to a different account
                                targetAccountId = mp.getOwnerId();
                                Account targetAcct = Provisioning.getInstance().get(Provisioning.AccountBy.id, targetAccountId);
                                if (targetAcct == null)
                                    error = AccountServiceException.NO_SUCH_ACCOUNT(targetAccountId);
                                break;
                            }
                            hopCount++;
                        }
                    }
                    if (hopCount >= ZimbraSoapContext.MAX_HOP_COUNT)
                        error = MailServiceException.TOO_MANY_HOPS(iidFolder);
                }
                result.put(iidFolder, new Resolved(new ItemId(targetAccountId, folderId), error));
            } catch (ServiceException e) {
                ItemIdFormatter ifmt = new ItemIdFormatter();
                ZimbraLog.calendar.warn("Error resolving calendar folder " + ifmt.formatItemId(iidFolder), e);
                result.put(iidFolder, new Resolved(new ItemId(targetAccountId, folderId), e));
            }
        }
        return result;
    }

    private static Map<String /* account id */, List<Integer> /* folder ids */> groupFoldersByAccount(Map<ItemId, Resolved> map) {
        Map<String, List<Integer>> foldersMap = new HashMap<String, List<Integer>>();
        for (Map.Entry<ItemId, Resolved> entry : map.entrySet()) {
            Resolved res = entry.getValue();
            if (res.error == null) {
                List<Integer> folderList = foldersMap.get(res.iid.getAccountId());
                if (folderList == null) {
                    folderList = new ArrayList<Integer>();
                    foldersMap.put(res.iid.getAccountId(), folderList);
                }
                folderList.add(res.iid.getId());
            }
        }
        return foldersMap;
    }

    private static void addError(Element parent, String folderId, String errcode, String errmsg) {
        Element errorElem = parent.addElement(MailConstants.E_ERROR);
        errorElem.addAttribute(MailConstants.A_ID, folderId);
        errorElem.addAttribute(MailConstants.A_CAL_CODE, errcode);
        errorElem.setText(errmsg);
    }
}
