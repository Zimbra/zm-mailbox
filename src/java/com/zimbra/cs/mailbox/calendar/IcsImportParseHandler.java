/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008 Zimbra, Inc.
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

package com.zimbra.cs.mailbox.calendar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.fortuna.ical4j.data.ParserException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite.InviteVisitor;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZICalendarParseHandler;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZParameter;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;

// ical4j parse handler that adds VEVENT/VTODOs as they are parsed
// This is memory efficient compared to the default parse handler which builds a list of parsed objects
// and iterates them to add.  Downside: This handler can't deal with TZID references which occur before
// the referenced VTIMEZONEs.
public class IcsImportParseHandler implements ZICalendarParseHandler {
    ZVCalendar mCurCal = null;
    List<ZComponent> mComponents = new ArrayList<ZComponent>();
    ZProperty mCurProperty = null;
    private int mNumCals;
    private boolean mInZCalendar;

    private Account mAccount;
    private boolean mContinueOnError;
    private String mMethod;
    private TimeZoneMap mTimeZoneMap;
    private Set<String> mTZIDsSeen;

    private InviteVisitor mInviteVisitor;

    public IcsImportParseHandler(OperationContext ctxt, Account account, Folder folder,
                                 boolean continueOnError, boolean removeAlarms) {
        mAccount = account;
        mContinueOnError = continueOnError;
        mTZIDsSeen = new HashSet<String>();
        mInviteVisitor = new ImportInviteVisitor(ctxt, folder, removeAlarms);
    }

    public IcsImportParseHandler(OperationContext ctxt, Account account, InviteVisitor visitor,
                                 boolean continueOnError, boolean removeAlarms) {
        mAccount = account;
        mContinueOnError = continueOnError;
        mTZIDsSeen = new HashSet<String>();
        mInviteVisitor = visitor;
    }

    public void startCalendar() throws ParserException {
        mComponents.clear();
        mInZCalendar = true;
        mCurCal = new ZVCalendar();

        mMethod = null;
        mTimeZoneMap = new TimeZoneMap(ICalTimeZone.getAccountTimeZone(mAccount));
    }

    public void endCalendar() throws ParserException {
        mInZCalendar = false;
        mNumCals++;
        mCurCal = null;
    }

    public boolean inZCalendar() { return mInZCalendar; }
    public int getNumCals() { return mNumCals; }

    public void startComponent(String name) {
        if (mComponents.isEmpty())
            mTZIDsSeen.clear();
        mComponents.add(new ZComponent(name));
    }

    public void endComponent(String name) throws ParserException {
        if (mComponents.isEmpty())
            throw new ParserException("Found END:" + name + " without BEGIN");

        ZComponent comp = mComponents.remove(mComponents.size() - 1);
        if (mComponents.size() == 0) {
            ICalTok tok = comp.getTok();
            if (tok != null) {
                try {
                    switch (tok) {
                    case VEVENT:
                    case VTODO:
                        doComp(comp);
                        break;
                    case VTIMEZONE:
                        ICalTimeZone tz = ICalTimeZone.fromVTimeZone(comp);
                        mTimeZoneMap.add(tz);
                        break;
                    }
                } catch (ServiceException e) {
                    throw new ParserException("Error while parsing " + tok.toString(), e);
                }
            }
        } else {
            mComponents.get(mComponents.size() - 1).mComponents.add(comp);
        }
    }

    public void startProperty(String name) { 
        mCurProperty = new ZProperty(name);
        
        if (mComponents.size() > 0) {
            mComponents.get(mComponents.size()-1).mProperties.add(mCurProperty);
        } else {
            mCurCal.mProperties.add(mCurProperty);
        }
    }

    public void propertyValue(String value) { 
        mCurProperty.mValue = value;
        if (mComponents.size() == 0 && ICalTok.METHOD.equals(mCurProperty.getToken()))
            mMethod = value;
    }

    public void endProperty(String name) { mCurProperty = null; }

    public void parameter(String name, String value) { 
        ZParameter param = new ZParameter(name, value);
        if (mCurProperty != null) {
            mCurProperty.mParameters.add(param);
            if (ICalTok.TZID.equals(param.getToken()))
                mTZIDsSeen.add(value);
        } else {
            ZimbraLog.calendar.debug("ERROR: got parameter " + name + "," + value + " outside of Property");
        }
    }

    private void doComp(ZComponent comp) throws ServiceException {
        // Create a mew TimeZoneMap containing only the timezones used by the current component.
        TimeZoneMap tzmap = null;
        for (String tzid : mTZIDsSeen) {
            ICalTimeZone tz = mTimeZoneMap.getTimeZone(tzid);
            if (tz != null) {
                if (tzmap == null)
                    tzmap = new TimeZoneMap(tz);
                tzmap.add(tz);
            } else {
                throw ServiceException.PARSE_ERROR(
                        "TZID reference encountered before/without its VTIMEZONE: " + tzid, null);
            }
        }
        if (tzmap == null)
            tzmap = new TimeZoneMap(mTimeZoneMap.getLocalTimeZone());

        List<ZComponent> comps = new ArrayList<ZComponent>(1);
        comps.add(comp);
        Invite.createFromCalendar(mAccount, null, mMethod, tzmap, comps.iterator(),
                                 true, mContinueOnError, mInviteVisitor);
    }

    public static class ImportInviteVisitor implements InviteVisitor {
        private OperationContext mCtxt;
        private Folder mFolder;
        private boolean mRemoveAlarms;

        public ImportInviteVisitor(OperationContext ctxt, Folder folder, boolean removeAlarms) {
            mCtxt = ctxt;
            mFolder = folder;
            mRemoveAlarms = removeAlarms;
        }

        public void visit(Invite inv) throws ServiceException {
            // handle missing UIDs on remote calendars by generating them as needed
            if (inv.getUid() == null)
                inv.setUid(LdapUtil.generateUUID());
            // and add the invite to the calendar!
            mFolder.getMailbox().addInvite(mCtxt, inv, mFolder.getId(), false, null, mRemoveAlarms);
            if (ZimbraLog.calendar.isDebugEnabled()) {
                if (inv.isEvent())
                    ZimbraLog.calendar.debug("Appointment imported: UID=" + inv.getUid());
                else if (inv.isTodo())
                    ZimbraLog.calendar.debug("Task imported: UID=" + inv.getUid());
            }
        }
    }
}
