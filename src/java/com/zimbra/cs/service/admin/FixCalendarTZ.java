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

package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.calendar.tzfixup.TimeZoneFixupRules;
import com.zimbra.cs.mailbox.calendar.tzfixup.XmlFixupRules;
import com.zimbra.soap.ZimbraSoapContext;

public class FixCalendarTZ extends AdminDocumentHandler {

    public static final String ALL = "all";

    // Default cutoff time is December 31, 2007, 11:00:00 UTC.
    // This is January 1, 2008, 00:00:00 in the eastern-most (GMT+13) TZ.
    private static final long DEFAULT_FIXUP_AFTER = 1199098800000L;

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        boolean sync = request.getAttributeBool(AdminConstants.A_TZFIXUP_SYNC, false);
        long after = request.getAttributeLong(AdminConstants.A_TZFIXUP_AFTER, DEFAULT_FIXUP_AFTER);
        List<Element> acctElems = request.listElements(AdminConstants.E_ACCOUNT);
        Element tzfixupElem = request.getElement(XmlFixupRules.E_TZFIXUP);
        TimeZoneFixupRules tzfixupRules = new TimeZoneFixupRules(tzfixupElem);
        List<String> acctNames = parseAccountNames(acctElems);
        if (acctNames.isEmpty())
            throw ServiceException.INVALID_REQUEST("Accounts must be specified", null);
        if (sync) {
            fixAccounts(acctNames, after, tzfixupRules);
        } else {
            CalendarTimeZoneFixupThread thread =
                new CalendarTimeZoneFixupThread(acctNames, after, tzfixupRules);
            thread.start();
        }

        Element response = lc.createElement(AdminConstants.FIX_CALENDAR_TZ_RESPONSE);
        return response;
    }

    protected List<String> parseAccountNames(List<Element> acctElems) throws ServiceException {
        List<String> a = new ArrayList<String>(acctElems.size());
        for (Element elem : acctElems) {
            String name = elem.getAttribute(AdminConstants.A_NAME);
            if (ALL.equals(name)) {
                List<String> all = new ArrayList<String>(1);
                all.add(ALL);
                return all;
            } else {
                String[] parts = name.split("@");
                if (parts.length != 2)
                    throw ServiceException.INVALID_REQUEST("invalid account email address: " + name, null);
            }
            a.add(name);
        }
        return a;
    }

    private static List<NamedEntry> getAccountsOnServer() throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        String serverName = prov.getLocalServer().getAttr(Provisioning.A_zimbraServiceHostname);
        List<NamedEntry> accts = prov.searchAccounts(
                "(zimbraMailHost=" + serverName + ")",
                new String[] { Provisioning.A_zimbraId }, null, false,
                Provisioning.SA_ACCOUNT_FLAG | Provisioning.SA_CALENDAR_RESOURCE_FLAG);
        ZimbraLog.calendar.info("Found " + accts.size() + " accounts on server " + serverName);
        return accts;
    }

    private static void fixAccounts(List<String> acctNames, long after, TimeZoneFixupRules tzfixupRules)
    throws ServiceException {
        int numAccts = acctNames.size();
        boolean all = (numAccts == 1 && ALL.equals(acctNames.get(0)));
        int numFixedAccts = 0;
        int numFixedAppts = 0;
        List<NamedEntry> accts;
        if (all) {
            accts = getAccountsOnServer();
        } else {
            accts = new ArrayList<NamedEntry>(acctNames.size());
            for (String name : acctNames) {
                try {
                    accts.add(Provisioning.getInstance().get(AccountBy.name, name));
                } catch (ServiceException e) {
                    ZimbraLog.calendar.error(
                            "Error looking up account " + name + ": " + e.getMessage(), e);
                }
            }
        }
        numAccts = accts.size();
        int every = 10;
        for (NamedEntry entry : accts) {
            if (!(entry instanceof Account))
                continue;
            Account acct = (Account) entry;
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
            try {
                numFixedAppts += mbox.fixAllCalendarItemTZ(null, after, tzfixupRules);
            } catch (ServiceException e) {
                ZimbraLog.calendar.error(
                        "Error fixing timezones in mailbox " + mbox.getId() +
                        ": " + e.getMessage(), e);
            }
            numFixedAccts++;
            if (numFixedAccts % every == 0) {
                ZimbraLog.calendar.info(
                        "Progress: fixed calendar timezones in " + numFixedAccts + "/" +
                        numAccts + " accounts");
            }
        }
        ZimbraLog.calendar.info(
                "Fixed timezones in total " + numFixedAppts + " calendar items in " + numFixedAccts + " accounts");
    }

    private static class CalendarTimeZoneFixupThread extends Thread {
        private List<String> mAcctNames;
        private long mAfter;
        private TimeZoneFixupRules mFixupRules;

        public CalendarTimeZoneFixupThread(List<String> acctNames, long after, TimeZoneFixupRules tzfixupRules) {
            setName("CalendarTimeZoneFixupThread");
            mAcctNames = acctNames;
            mAfter = after;
            mFixupRules = tzfixupRules;
        }

        public void run() {
            try {
                fixAccounts(mAcctNames, mAfter, mFixupRules);
            } catch (ServiceException e) {
                ZimbraLog.calendar.error(
                        "Error while fixing up calendar timezones: " + e.getMessage(), e);
            }
        }
    }
}
