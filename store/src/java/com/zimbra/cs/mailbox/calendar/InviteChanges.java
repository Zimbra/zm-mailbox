/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.ParsedDuration;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.mailbox.calendar.Recurrence.IRecurrence;

// Captures differences between two Invite objects
public class InviteChanges {

    public static final int LOCATION   = 0x00000001;
    public static final int TIME       = 0x00000002;  // changes to DTSTART, DTEND/DUE, or DURATION
    public static final int RECURRENCE = 0x00000004;
    public static final int SUBJECT    = 0x00000008;
    // maybe others in the future...

    public static final String LABEL_LOCATION   = "location";
    public static final String LABEL_TIME       = "time";
    public static final String LABEL_RECURRENCE = "recurrence";
    public static final String LABEL_SUBJECT    = "subject";

    private static List<Integer> sMaskList;
    private static Map<Integer, String> sMaskToLabel;
    private static Map<String, Integer> sLabelToMask;
    private static void map(int mask, String label) {
        sMaskList.add(mask);
        sMaskToLabel.put(mask, label);
        sLabelToMask.put(label, mask);
        String labelLower = label.toLowerCase();
        if (!labelLower.equals(label))
            sLabelToMask.put(labelLower, mask);
    }
    static {
        sMaskList = new ArrayList<Integer>();
        sMaskToLabel = new HashMap<Integer, String>();
        sLabelToMask = new HashMap<String, Integer>();
        map(LOCATION, LABEL_LOCATION);
        map(TIME, LABEL_TIME);
        map(RECURRENCE, LABEL_RECURRENCE);
        map(SUBJECT, LABEL_SUBJECT);
    }

    private int mChanges;  // bit mask

    public InviteChanges(Invite inv1, Invite inv2) {
        mChanges = 0;
        diffInvites(inv1, inv2);
    }

    public InviteChanges(String changesCSV) {
        mChanges = parse(changesCSV);
    }

    public boolean noChange() { return mChanges == 0; }
    private boolean changed(int bitmask) { return (mChanges & bitmask) != 0; }
    public boolean changedLocation()    { return changed(LOCATION); }
    public boolean changedTime()        { return changed(TIME); }
    public boolean changedRecurrence()  { return changed(RECURRENCE); }
    public boolean changedSubject()     { return changed(SUBJECT); }

    /**
     * Returns true if the change is significant enough to invalidate earlier replies.  Changes to meeting location,
     * time, and/or recurrence warrant a new reply.  See Section 2.1.4 Component Revisions in RFC5546 (iTIP) for
     * more info.
     * Overriding RFC5546 to include SUMMARY so that it will warrant a new reply.
     * 
     * @return true/false Return true/false based on the property change whether to notify the attendees.
     */
    public boolean isReplyInvalidatingChange() {
        return changed(LOCATION | TIME | RECURRENCE | SUBJECT);
    }

    /**
     * Returns true if the change is for a recurrence series and it causes exception instances to be removed.
     * Includes changes to time or recurrence.
     * @return
     */
    public boolean isExceptionRemovingChange() {
        return changedTime() || changedRecurrence();
    }

    private void diffInvites(Invite inv1, Invite inv2) {
        // Subject
        if (!StringUtil.equal(inv1.getName(), inv2.getName()))
            mChanges |= SUBJECT;

        // LOCATION
        if (!StringUtil.equal(inv1.getLocation(), inv2.getLocation()))
            mChanges |= LOCATION;

        // DTSTART
        boolean dtStartChanged;
        ParsedDateTime dtStart1 = inv1.getStartTime();
        ParsedDateTime dtStart2 = inv2.getStartTime();
        if (dtStart1 == null || dtStart2 == null)
            dtStartChanged = dtStart1 != dtStart2;
        else
            dtStartChanged = !StringUtil.equal(dtStart1.getDateTimePartString(false), dtStart2.getDateTimePartString(false));
        if (dtStartChanged) {
            mChanges |= TIME;
        } else {
            // DTEND
            boolean dtEndChanged;
            ParsedDateTime dtEnd1 = inv1.getEndTime();
            ParsedDateTime dtEnd2 = inv2.getEndTime();
            if (dtEnd1 == null || dtEnd2 == null)
                dtEndChanged = dtEnd1 != dtEnd2;
            else
                dtEndChanged = !StringUtil.equal(dtEnd1.getDateTimePartString(false), dtEnd2.getDateTimePartString(false));
            if (dtEndChanged) {
                mChanges |= TIME;
            } else {
                boolean durationChanged;
                ParsedDuration dur1 = inv1.getDuration();
                ParsedDuration dur2 = inv2.getDuration();
                if (dur1 == null || dur2 == null)
                    durationChanged = dur1 != dur2;
                else
                    durationChanged = !dur1.equals(dur2);
                if (durationChanged)
                    mChanges |= TIME;
            }
        }

        // Recurrence
        boolean recurChanged;
        IRecurrence recur1 = inv1.getRecurrence();
        IRecurrence recur2 = inv2.getRecurrence();
        if (recur1 == null || recur2 == null) {
            recurChanged = recur1 != recur2;
        } else {
            recurChanged = !Recurrence.sameSeriesRules(recur1, recur2);
        }
        if (recurChanged)
            mChanges |= RECURRENCE;
    }

    private static int parse(String changesCSV) {
        int changeMask = 0;
        String[] values = changesCSV.split(",");
        for (String value : values) {
            value = value.trim().toLowerCase();
            Integer i = sLabelToMask.get(value);
            if (i != null)
                changeMask |= i.intValue();
        }
        return changeMask;
    }

    public String toString() {
        if (mChanges == 0)
            return "";
        StringBuilder sb = new StringBuilder();
        for (Integer i : sMaskList) {
            if (changed(i)) {
                if (sb.length() != 0)
                    sb.append(',');
                sb.append(sMaskToLabel.get(i));
            }
        }
        return sb.toString();
    }
}
