/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.mailbox.calendar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.fortuna.ical4j.data.ParserException;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.calendar.ICalTimeZone.TZID_NAME_ASSIGNMENT_BEHAVIOR;
import com.zimbra.common.calendar.TimeZoneMap;
import com.zimbra.common.calendar.WellKnownTimeZones;
import com.zimbra.common.calendar.ZCalendar;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZICalendarParseHandler;
import com.zimbra.common.calendar.ZCalendar.ZParameter;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite.InviteVisitor;

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

    private final Account mAccount;
    private final boolean mContinueOnError;
    private String mMethod;
    private TimeZoneMap mTimeZoneMap;
    private final Set<String> mTZIDsSeen = Sets.newHashSet();
    private final Map<String,String> tzidRenames = Maps.newHashMap();

    private final InviteVisitor mInviteVisitor;

    public IcsImportParseHandler(OperationContext ctxt, Account account, Folder folder,
                                 boolean continueOnError, boolean preserveExistingAlarms) {
        mAccount = account;
        mContinueOnError = continueOnError;
        mInviteVisitor = new ImportInviteVisitor(ctxt, folder, preserveExistingAlarms);
    }

    public IcsImportParseHandler(OperationContext ctxt, Account account, InviteVisitor visitor,
                                 boolean continueOnError, boolean removeAlarms) {
        mAccount = account;
        mContinueOnError = continueOnError;
        mInviteVisitor = visitor;
    }

    @Override
    public void startCalendar() throws ParserException {
        mComponents.clear();
        mInZCalendar = true;
        mCurCal = new ZVCalendar();

        mMethod = ICalTok.PUBLISH.toString();
        mTimeZoneMap = new TimeZoneMap(Util.getAccountTimeZone(mAccount));
    }

    @Override
    public void endCalendar() throws ParserException {
        mInZCalendar = false;
        mNumCals++;
        mCurCal = null;
    }

    @Override
    public boolean inZCalendar() { return mInZCalendar; }

    @Override
    public int getNumCals() { return mNumCals; }

    @Override
    public void startComponent(String name) {
        if (mComponents.isEmpty()) {
            mTZIDsSeen.clear();
        }
        mComponents.add(new ZComponent(name));
    }

    @Override
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
                        String origTZID = comp.getPropVal(ICalTok.TZID, null);
                        ICalTimeZone tz = ICalTimeZone.fromVTimeZone(comp, false /* skipLookup */,
                                TZID_NAME_ASSIGNMENT_BEHAVIOR.KEEP_IF_DOESNT_CLASH);
                        if ((null != origTZID) && (origTZID != tz.getID())) {
                            tzidRenames.put(origTZID, tz.getID());
                        }
                        mTimeZoneMap.add(tz);
                        break;
                    default:
                        break;
                    }
                } catch (ServiceException e) {
                    throw new ParserException("Error while parsing " + tok.toString(), e);
                }
            }
        } else {
            mComponents.get(mComponents.size() - 1).addComponent(comp);
        }
    }

    @Override
    public void startProperty(String name) {
        mCurProperty = new ZProperty(name);

        if (mComponents.size() > 0) {
            mComponents.get(mComponents.size()-1).addProperty(mCurProperty);
        } else {
            mCurCal.addProperty(mCurProperty);
        }
    }

    @Override
    public void propertyValue(String value) throws ParserException {
        ICalTok token = mCurProperty.getToken();
        if (ICalTok.CATEGORIES.equals(token) || ICalTok.RESOURCES.equals(token) || ICalTok.FREEBUSY.equals(token))
            mCurProperty.setValueList(ZCalendar.parseCommaSepText(value));
        else
            mCurProperty.setValue(ZCalendar.unescape(value));
        if (mComponents.size() == 0) {
            if (ICalTok.METHOD.equals(mCurProperty.getToken()))
                mMethod = value;
            if (ICalTok.VERSION.equals(mCurProperty.getToken())) {
                if (ZCalendar.sObsoleteVcalVersion.equals(value))
                    throw new ParserException("vCalendar 1.0 format not supported; use iCalendar instead");
                if (!ZCalendar.sIcalVersion.equals(value))
                    throw new ParserException("Unknow iCalendar version " + value);
            }
        }
    }

    @Override
    public void endProperty(String name) { mCurProperty = null; }

    @Override
    public void parameter(String name, String value) {
        if (ICalTok.TZID.equals(name) && (null != value)) {
            String newTzid = tzidRenames.get(value);
            if (newTzid != null) {
                value = newTzid;
            }
        }
        ZParameter param = new ZParameter(name, value);
        if (mCurProperty != null) {
            mCurProperty.addParameter(param);
            // Keep track of TZIDs we've encountered.  Do it only for well-known properties.
            if (ICalTok.TZID.equals(param.getToken()) && mCurProperty.getToken() != null)
                mTZIDsSeen.add(value);
        } else {
            ZimbraLog.calendar.debug("ERROR: got parameter %s=\"%s\"  outside of Property", name, value);
        }
    }

    private void doComp(ZComponent comp) throws ServiceException {
        // Create a mew TimeZoneMap containing only the timezones used by the current component.
        TimeZoneMap tzmap = null;
        for (String tzid : mTZIDsSeen) {
            ICalTimeZone tz = mTimeZoneMap.getTimeZone(tzid);
            if (tz == null) {
                // Undefined TZID means bad incoming data, but if it happens to be a well-known timezone,
                // let's be lenient and use the predefined definition.
                String sanitizedTzid = TimeZoneMap.sanitizeTZID(tzid);
                tz = WellKnownTimeZones.getTimeZoneById(sanitizedTzid);
            }
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
        private final OperationContext mCtxt;
        private final Folder mFolder;
        private final boolean mPreserveExistingAlarms;
        private final Set<String> mUidsSeen = new HashSet<String>();

        public ImportInviteVisitor(OperationContext ctxt, Folder folder, boolean preserveExistingAlarms) {
            mCtxt = ctxt;
            mFolder = folder;
            mPreserveExistingAlarms = preserveExistingAlarms;
        }

        @Override
        public void visit(Invite inv) throws ServiceException {
            // handle missing UIDs on remote calendars by generating them as needed
            String uid = inv.getUid();
            if (uid == null) {
                uid = LdapUtil.generateUUID();
                inv.setUid(uid);
            }
            boolean addRevision;
            if (!mUidsSeen.contains(uid)) {
                addRevision = true;
                mUidsSeen.add(uid);
            } else {
                addRevision = false;
            }
            // and add the invite to the calendar!
            try {
                mFolder.getMailbox().addInvite(mCtxt, inv, mFolder.getId(), mPreserveExistingAlarms, addRevision);
                if (ZimbraLog.calendar.isDebugEnabled()) {
                    if (inv.isEvent())
                        ZimbraLog.calendar.debug("Appointment imported: UID=" + inv.getUid());
                    else if (inv.isTodo())
                        ZimbraLog.calendar.debug("Task imported: UID=" + inv.getUid());
                }
            } catch (ServiceException se) {
                if (se.getCode().equals(ServiceException.FORBIDDEN)) {
                    Invite.logIcsParseImportError(inv, se);
                } else {
                    throw se;
                }
            }
        }
    }
}
