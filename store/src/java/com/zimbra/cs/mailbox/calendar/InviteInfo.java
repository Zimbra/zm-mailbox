/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.calendar.TimeZoneMap;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.mailbox.Metadata;

/**
 * Within a calendar entry:<br />
 *  <b>mRecurrenceId</b> should be unique (null for the main series)<br />
 *  The combination of <b>mMsgId</b> and <b>mComponentId</b> should be unique<br />
 *
 *  ZCS-3055 - The latter restriction could be violated for pseudo exceptions created as a
 *  result of replies to individual instances.
 */
public class InviteInfo implements Comparable<InviteInfo>, Cloneable {

    private static final String FN_MSGID        ="i";
    private static final String FN_COMPNUM      ="c";
    private static final String FN_RECURRENCEID ="r";
    private static final String FN_METHOD       ="m";

    private static final int METHOD_REQUEST = 3;
    private static final int METHOD_CANCEL = 4;

    /** ID of the MESSAGE which this invite was originally encoded in or a unique pseudo item ID associated
     *  with a component. (ZWC expects each exception to have a different mMsgId) */
    private int mMsgId;
    /** component number in that message, or perhaps 0 if each component has a different mMsgId */
    private int mComponentId;
    private RecurId mRecurrenceId; // RECURID, in the iCal (rfc2445) sense
    private String mMethod; // REQUEST, REPLY, PUBLISH, COUNTER, etc...

    public InviteInfo(Invite inv) {
        mMsgId = inv.getMailItemId();
        mComponentId = inv.getComponentNum();
        mRecurrenceId = inv.getRecurId();
        mMethod = inv.getMethod();
    }

    private InviteInfo(int msgId, int componentId, RecurId recurrenceId, String method) {
        mMsgId = msgId;
        mComponentId = componentId;
        mRecurrenceId = recurrenceId;
        mMethod = method;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof InviteInfo)) return false;
        InviteInfo other = (InviteInfo) o;
        return
            mMsgId == other.mMsgId &&
            mComponentId == other.mComponentId &&
            StringUtil.equal(mMethod, other.mMethod) &&
            ((mRecurrenceId == null && other.mRecurrenceId == null) ||
             (mRecurrenceId != null && mRecurrenceId.equals(other.mRecurrenceId)));
    }

    @Override
    public int compareTo(InviteInfo other) {
        if (other == null) return 1;  // null sorts first
        int toRet = mMsgId - other.mMsgId;
        if (toRet == 0) {
            toRet = mComponentId - other.mComponentId;
            if (toRet == 0) {
                String rid = mRecurrenceId != null ? mRecurrenceId.toString() : null;
                String ridOther = other.mRecurrenceId != null ? other.mRecurrenceId.toString() : null;
                toRet = StringUtil.compareTo(rid, ridOther);
                if (toRet == 0)
                    toRet = StringUtil.compareTo(mMethod, other.mMethod);
            }
        }
        return toRet;
    }

    @Override
    public Object clone() {
        RecurId rid = mRecurrenceId != null ? (RecurId) mRecurrenceId.clone() : null;
        return new InviteInfo(mMsgId, mComponentId, rid, mMethod);
    }

    static public InviteInfo fromMetadata(Metadata md, TimeZoneMap tzmap) throws ServiceException {
        int msgId;
        int compNum;
        RecurId recurrenceId = null;
        int methodInt = 0;
        String method = ICalTok.REQUEST.toString();

        msgId = (int)md.getLong(FN_MSGID);
        compNum = (int)md.getLong(FN_COMPNUM);

        methodInt = (int)md.getLong(FN_METHOD);
        if (METHOD_CANCEL == methodInt) {
            method = ICalTok.CANCEL.toString();
        }

        if (md.containsKey(FN_RECURRENCEID)) {
                recurrenceId = RecurId.decodeMetadata(md.getMap(FN_RECURRENCEID), tzmap);
        }

        return new InviteInfo(msgId, compNum, recurrenceId, method);
    }

    public Metadata encodeMetadata() {
        Metadata md = new Metadata();
        md.put(FN_MSGID, mMsgId);
        md.put(FN_COMPNUM, mComponentId);
        if (mRecurrenceId != null) {
            md.put(FN_RECURRENCEID, mRecurrenceId.encodeMetadata());
        }

        if (mMethod.equals(ICalTok.CANCEL.toString())) {
            md.put(FN_METHOD, METHOD_CANCEL);
        } else {
            md.put(FN_METHOD, METHOD_REQUEST);
        }

        return md;
    }

    @Override
    public String toString() {
        return mMsgId + "-" + mComponentId + "-"+mMethod+"-"+mRecurrenceId;
    }

    public int getMsgId() {
        return mMsgId;
    }

    public int getComponentId() {
        return mComponentId;
    }

    public boolean hasRecurrenceId() { return mRecurrenceId != null; }

    public RecurId getRecurrenceId() {
        return mRecurrenceId;
    }

    public String getMethod() {
        return mMethod;
    }
}