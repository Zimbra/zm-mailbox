/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.mailbox.calendar.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;

// mainly for CalDAV but could be useful for other protocols
// list of calendar folders (or mountpoints) in an account
public class CalList {
    private Set<Integer> mCalendars;
    private String mVerPrefix;
    private long mVerSeq;
    private String mVerString;  // externally visible "version" of this calendar list
                                // This is updated on every calendar change in the account.

    CalList(Set<Integer> calendars) {
        mCalendars = new HashSet<Integer>(calendars);
        mVerPrefix = Long.toString(System.currentTimeMillis()) + ":";
        mVerSeq = 1;
        setVersion();
    }

    CalList(CalList other) {
        mCalendars = new HashSet<Integer>(other.mCalendars);
        mVerPrefix = other.mVerPrefix;
        mVerSeq = other.mVerSeq;
        mVerString = other.mVerString;
    }

    private static final String FN_CALS = "c";
    private static final String FN_VERSION_PREFIX = "vp";
    private static final String FN_VERSION_SEQ = "vs";

    Metadata encodeMetadata() {
        Metadata meta = new Metadata();
        List<Integer> list = new ArrayList<Integer>(mCalendars);
        MetadataList calsMetaList = new MetadataList(list);
        meta.put(FN_CALS, calsMetaList);
        meta.put(FN_VERSION_PREFIX, mVerPrefix);
        meta.put(FN_VERSION_SEQ, mVerSeq);
        return meta;
    }

    @SuppressWarnings("unchecked")
    CalList(Metadata meta) throws ServiceException {
        MetadataList calsMetaList = meta.getList(FN_CALS, true);
        if (calsMetaList != null) {
            mCalendars = new HashSet<Integer>(calsMetaList.size());
            List vals = calsMetaList.asList();
            for (Object val : vals) {
                if (val instanceof Long) {
                    // force integer
                    mCalendars.add((int) ((Long) val).longValue());
                } else if (val instanceof Integer) {
                    mCalendars.add((Integer) val);
                } else {
                    throw ServiceException.FAILURE("Invalid calendar id value: " + val.toString(), null);
                }
            }
        } else {
            mCalendars = new HashSet<Integer>(0);
        }
        mVerPrefix = meta.get(FN_VERSION_PREFIX, "");
        mVerSeq = meta.getLong(FN_VERSION_SEQ, 1);
        setVersion();
    }

    void add(int calFolderId) {
        mCalendars.add(calFolderId);
    }

    void remove(int calFolderId) {
        mCalendars.remove(calFolderId);
    }

    void incrementSeq() {
        ++mVerSeq;
        setVersion();
    }

    boolean contains(int calFolderId) {
        return mCalendars.contains(calFolderId);
    }

    public String getVersion() { return mVerString; }

    private void setVersion() {
        mVerString = mVerPrefix + Long.toString(mVerSeq);
    }

    public Collection<Integer> getCalendars() { return mCalendars; }

}
