/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/**
 * 
 */
package com.zimbra.cs.mailbox.calendar;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;

public class InviteInfo implements Comparable<InviteInfo> {
    private int mMsgId; // ID of the MESSAGE which this invite was originally encoded in 
    private int mComponentId; // component number in that message
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

    private static final String FN_MSGID        ="i";
    private static final String FN_COMPNUM      ="c";
    private static final String FN_RECURRENCEID ="r";
    private static final String FN_METHOD       ="m";
    
    private static final int METHOD_REQUEST = 3;
    private static final int METHOD_CANCEL = 4;
    
    static public InviteInfo fromMetadata(Metadata md, TimeZoneMap tzmap) throws ServiceException {
        int msgId, compNum;
        RecurId recurrenceId = null;
        int methodInt = 0;
        String method = ICalTok.REQUEST.toString();
        
        msgId = (int)md.getLong(FN_MSGID);
        compNum = (int)md.getLong(FN_COMPNUM);
        
        methodInt = (int)md.getLong(FN_METHOD);
        switch(methodInt) {
        case METHOD_CANCEL:
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