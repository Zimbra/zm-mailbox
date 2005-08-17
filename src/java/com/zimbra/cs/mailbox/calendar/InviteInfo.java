/**
 * 
 */
package com.liquidsys.coco.mailbox.calendar;

import net.fortuna.ical4j.model.property.Method;

import com.liquidsys.coco.mailbox.Invite;
import com.liquidsys.coco.mailbox.Metadata;
import com.liquidsys.coco.service.ServiceException;

public class InviteInfo implements Comparable {
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
        InviteInfo other = (InviteInfo) o;
        if (mMsgId == other.mMsgId
                && mComponentId == other.mComponentId) {
            return true;
        } else {
            return false;
        }
    }
    
    public int compareTo(Object o) {
        InviteInfo other = (InviteInfo) o;
        int toRet = mMsgId - other.mMsgId;
        if (toRet == 0) {
            toRet = mComponentId - other.mComponentId;
            if (toRet == 0) {
                assert (mRecurrenceId == other.mRecurrenceId);
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
        String method = Method.REQUEST.getValue();
        
        msgId = (int)md.getLong(FN_MSGID);
        compNum = (int)md.getLong(FN_COMPNUM);
        
        methodInt = (int)md.getLong(FN_METHOD);
        switch(methodInt) {
        case METHOD_CANCEL:
            method = Method.CANCEL.getValue();
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
        
        if (mMethod.equals(Method.CANCEL.getValue())) {
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