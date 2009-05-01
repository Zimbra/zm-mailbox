/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.calendar.cache;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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

    public void add(int calFolderId) {
        mCalendars.add(calFolderId);
        incrementSeq();
    }

    public void remove(int calFolderId) {
        mCalendars.remove(calFolderId);
        incrementSeq();
    }

    public void incrementSeq() {
        ++mVerSeq;
        setVersion();
    }

    public boolean contains(int calFolderId) {
        return mCalendars.contains(calFolderId);
    }

    public String getVersion() { return mVerString; }

    private void setVersion() {
        mVerString = mVerPrefix + Long.toString(mVerSeq);
    }

    public Collection<Integer> getCalendars() { return mCalendars; }
}
