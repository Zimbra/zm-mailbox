/*
* ***** BEGIN LICENSE BLOCK *****
* Zimbra Collaboration Suite Server
* Copyright (C) 2014, 2016 Synacor, Inc.
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

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.CalendarItem.Instance;

/**
 * Captures differences between Before and After Invites for an Organizer calendar
 * Builds on {@link InviteChanges} but allows for Before or After invites being null and offers
 * other utility methods to aid CalDAV auto-scheduling
 */
public class OrganizerInviteChanges {
    private final Invite oldInvite;
    public final Invite newInvite;
    private List<Instance> newExdates = null;
    private List<Instance> oldExdates = null;
    private List<Instance> exdatesOnlyInNew = null;
    private List<Instance> exdatesOnlyInOld = null;
    private List<ZAttendee> attendeesOnlyInNew = null;
    private List<ZAttendee> attendeesOnlyInOld = null;
    private InviteChanges changes = null;

    /**
     * @param oldInvite - may be null
     * @param newInvite - may be null
     */
    public OrganizerInviteChanges (Invite oldInvite, Invite newInvite) {
        this.oldInvite = oldInvite;
        this.newInvite = newInvite;
        if ((oldInvite != null) && (newInvite != null)) {
            changes = new InviteChanges(oldInvite, newInvite);
        }
    }

    public String getSubject() {
        if (newInvite != null) {
            return newInvite.getName();
        } else if (oldInvite != null) {
            return oldInvite.getName();
        }
        return "";
    }

    public boolean inviteCanceled() {
        return (newInvite == null) && (oldInvite != null);
    }

    public boolean isReplyInvalidatingChange() {
        if (changes == null) {
            return true;
        } else {
            return changes.isReplyInvalidatingChange();
        }
    }

    /**
     * Clients don't always increment SEQUENCE for important changes, so look for other changes
     */
    public boolean isChanged() {
        if ((newInvite == null) && (oldInvite == null)) {
            return false;  // would be odd...
        }
        if ((oldInvite == null) || (newInvite == null)) {
            return true;
        }
        if (!changes.noChange()) {
            ZimbraLog.calendar.trace("changes.noChange()=false for invite %s", newInvite.toString());
            return true;
        }
        if (!getExdatesOnlyInNew().isEmpty()) {
            ZimbraLog.calendar.trace("isChanged=true (new EXDATEs) for invite %s", newInvite.toString());
            return true;
        }
        if (!getExdatesOnlyInOld().isEmpty()) {
            ZimbraLog.calendar.trace("isChanged=true (old EXDATEs) for invite %s", newInvite.toString());
            return true;
        }
        if (!getAttendeesOnlyInNew().isEmpty()) {
            ZimbraLog.calendar.trace("isChanged=true (new ATTENDEEs) for invite %s", newInvite.toString());
            return true;
        }
        if (!getAttendeesOnlyInOld().isEmpty()) {
            ZimbraLog.calendar.trace("isChanged=true (old ATTENDEEs) for invite %s", newInvite.toString());
            return true;
        }
        ZimbraLog.calendar.trace("isChanged=false for invite %s", newInvite.toString());
        return false;
    }

    public List<Instance> getNewExdates() {
        if (newExdates == null) {
            newExdates = Invite.getExdates(newInvite);
        }
        return newExdates;
    }

    public List<Instance> getOldExdates() {
        if (oldExdates == null) {
            oldExdates = Invite.getExdates(oldInvite);
        }
        return oldExdates;
    }

    public List<Instance> getExdatesOnlyInFirst(List<Instance> first, List<Instance> second) {
        List<Instance> newList = Lists.newArrayList(first);
            Iterator<Instance> iter = newList.iterator();
            while (iter.hasNext()) {
                Instance curr = iter.next();
                for (Instance fromSecond: second) {
                    if (fromSecond.sameTime(curr)) {
                        iter.remove();
                        break;
                    }
                }
            }
        return newList;
    }

    public List<Instance> getExdatesOnlyInNew() {
        if (exdatesOnlyInNew == null) {
            exdatesOnlyInNew = getExdatesOnlyInFirst(getNewExdates(), getOldExdates());
        }
        return exdatesOnlyInNew;
    }
    public List<Instance> getExdatesOnlyInOld() {
        if (exdatesOnlyInOld == null) {
            exdatesOnlyInOld = getExdatesOnlyInFirst(getOldExdates(), getNewExdates());
        }
        return exdatesOnlyInOld;
    }

    /**
     * Just interested in the attendee names.  Things like differing PARTSTAT are not taken into account
     */
    public List<ZAttendee> getAttendeesOnlyInFirst(List<ZAttendee> first, List<ZAttendee> second) {
        if (first == null) {
            return Lists.newArrayListWithCapacity(0);
        }
        List<ZAttendee> newList = Lists.newArrayList(first);
        if (second == null) {
            return newList;
        }
        Iterator<ZAttendee> iter = newList.iterator();
        while (iter.hasNext()) {
            ZAttendee curr = iter.next();
            for (ZAttendee fromSecond: second) {
                if (fromSecond.addressesMatch(curr)) {
                    iter.remove();
                    break;
                }
            }
        }
        return newList;
    }

    public List<ZAttendee> getAttendeesOnlyInNew() {
        if (attendeesOnlyInNew == null) {
            attendeesOnlyInNew = getAttendeesOnlyInFirst(
                    (newInvite == null) ? null : newInvite.getAttendees(),
                            (oldInvite == null) ? null : oldInvite.getAttendees());
        }
        return attendeesOnlyInNew;
    }

    public List<ZAttendee> getAttendeesOnlyInOld() {
        if (attendeesOnlyInOld == null) {
            attendeesOnlyInOld = getAttendeesOnlyInFirst(
                    (oldInvite == null) ? null : oldInvite.getAttendees(),
                            (newInvite == null) ? null : newInvite.getAttendees());
        }
        return attendeesOnlyInOld;
    }
}
