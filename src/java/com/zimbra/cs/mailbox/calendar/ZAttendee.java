/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox.calendar;

import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZParameter;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.StringUtil;

public class ZAttendee extends CalendarUser {

    private static final String FN_CUTYPE          = "cut";
    private static final String FN_ROLE            = "r";
    private static final String FN_PARTSTAT        = "ptst";
    private static final String FN_RSVP_BOOL       = "v";
    private static final String FN_MEMBER          = "member";
    private static final String FN_DELEGATED_TO    = "delto";
    private static final String FN_DELEGATED_FROM  = "delfrom";

    private String mCUType;
    private String mRole;
    private String mPartStat;
    private Boolean mRsvp;
    private String mMember;
    private String mDelegatedTo;
    private String mDelegatedFrom;

    public boolean hasCUType() { return !StringUtil.isNullOrEmpty(mCUType); }
    public String getCUType() { return mCUType != null ? mCUType : ""; }
    public void setCUType(String cutype) {
        if (cutype != null && !IcalXmlStrMap.sCUTypeMap.validXml(cutype)) {
            cutype = IcalXmlStrMap.sCUTypeMap.toXml(cutype);
        }
        mCUType = cutype; 
    }

    public boolean hasRole() { return !StringUtil.isNullOrEmpty(mRole); }
    public String getRole() { return mRole != null ? mRole : ""; }
    public void setRole(String role) {
        if (role != null && !IcalXmlStrMap.sRoleMap.validXml(role)) {
            role = IcalXmlStrMap.sRoleMap.toXml(role);
        }
        mRole = role; 
    }

    public boolean hasPartStat() { return !StringUtil.isNullOrEmpty(mPartStat); }
    public String getPartStat() { return mPartStat != null ? mPartStat : ""; }
    public void setPartStat(String partStat) { 
        if (partStat != null && !IcalXmlStrMap.sPartStatMap.validXml(partStat)) {
            partStat = IcalXmlStrMap.sPartStatMap.toXml(partStat);
        }
        mPartStat = partStat; 
    }

    public boolean hasRsvp() { return mRsvp != null; }
    public Boolean getRsvp() { return mRsvp; }
    public void setRsvp(Boolean rsvp) { mRsvp = rsvp; }

    public boolean hasMember() { return !StringUtil.isNullOrEmpty(mMember); }
    public String getMember() { return mMember; }
    public void setMember(String member) { mMember = getMailToAddress(member); }

    public boolean hasDelegatedTo() { return !StringUtil.isNullOrEmpty(mDelegatedTo); }
    public String getDelegatedTo() { return mDelegatedTo; }
    public void setDelegatedTo(String delTo) { mDelegatedTo = getMailToAddress(delTo); }

    public boolean hasDelegatedFrom() { return !StringUtil.isNullOrEmpty(mDelegatedFrom); }
    public String getDelegatedFrom() { return mDelegatedFrom; }
    public void setDelegatedFrom(String delFrom) { mDelegatedFrom = getMailToAddress(delFrom); }

    public ZAttendee(String address,
                     String cn,
                     String sentBy,
                     String dir,
                     String language,
                     String cutype,
                     String role,
                     String ptst,
                     Boolean rsvp,
                     String member,
                     String delegatedTo,
                     String delegatedFrom
                     ) {
        super(address, cn, sentBy, dir, language);
        setCUType(cutype);
        setRole(role);
        setPartStat(ptst);
        setRsvp(rsvp);
        setMember(member);
        setDelegatedTo(delegatedTo);
        setDelegatedFrom(delegatedFrom);
    }

    public ZAttendee(String address) {
        this(address, null, null, null, null,
             null, null, null, null, null, null, null);
    }

    public ZAttendee(ZProperty prop) {
        super(prop);
        setCUType(prop.paramVal(ICalTok.CUTYPE, null));
        setRole(prop.paramVal(ICalTok.ROLE, null));
        setPartStat(prop.paramVal(ICalTok.PARTSTAT, null));

        String rsvpStr = prop.paramVal(ICalTok.RSVP, "FALSE");
        boolean rsvp = false;
        if (rsvpStr.equalsIgnoreCase("TRUE"))
            rsvp = true;
        setRsvp(rsvp);

        setMember(prop.paramVal(ICalTok.MEMBER, null));
        setDelegatedTo(prop.paramVal(ICalTok.DELEGATED_TO, null));
        setDelegatedFrom(prop.paramVal(ICalTok.DELEGATED_FROM, null));
    }

    public ZAttendee(Metadata meta) throws ServiceException {
        super(meta);
        setCUType(meta.get(FN_CUTYPE, null));
        setRole(meta.get(FN_ROLE, null));
        setPartStat(meta.get(FN_PARTSTAT, null));

        Boolean rsvp = null;
        if (meta.containsKey(FN_RSVP_BOOL)) {
            if (meta.getBool(FN_RSVP_BOOL))
                rsvp = Boolean.TRUE;
            else
                rsvp = Boolean.FALSE;
        }
        setRsvp(rsvp);

        setMember(meta.get(FN_MEMBER, null));
        setDelegatedTo(meta.get(FN_DELEGATED_TO, null));
        setDelegatedFrom(meta.get(FN_DELEGATED_FROM, null));
    }

    public Metadata encodeAsMetadata() {
        Metadata meta = super.encodeMetadata();
        if (hasCUType())
            meta.put(FN_CUTYPE, getCUType());
        if (hasRole())
            meta.put(FN_ROLE, getRole());
        if (hasPartStat())
            meta.put(FN_PARTSTAT, getPartStat());
        if (hasRsvp() && getRsvp().booleanValue())
            meta.put(FN_RSVP_BOOL, "1");
        if (hasMember())
            meta.put(FN_MEMBER, getMember());
        if (hasDelegatedTo())
            meta.put(FN_DELEGATED_TO, getDelegatedTo());
        if (hasDelegatedFrom())
            meta.put(FN_DELEGATED_FROM, getDelegatedFrom());

        return meta;
    }

    protected ICalTok getPropertyName() {
        return ICalTok.ATTENDEE;
    }

    protected void setProperty(ZProperty prop) throws ServiceException {
        super.setProperty(prop);
        if (hasCUType())
            prop.addParameter(new ZParameter(ICalTok.CUTYPE, IcalXmlStrMap.sCUTypeMap.toIcal(getCUType())));
        if (hasRole())
            prop.addParameter(new ZParameter(ICalTok.ROLE, IcalXmlStrMap.sRoleMap.toIcal(getRole())));
        if (hasPartStat())
            prop.addParameter(new ZParameter(ICalTok.PARTSTAT, IcalXmlStrMap.sPartStatMap.toIcal(getPartStat())));
        if (hasRsvp())
            prop.addParameter(new ZParameter(ICalTok.RSVP, getRsvp()));
        if (hasMember())
            prop.addParameter(new ZParameter(ICalTok.MEMBER, "MAILTO:" + getMember()));
        if (hasDelegatedTo())
            prop.addParameter(new ZParameter(ICalTok.DELEGATED_TO, "MAILTO:" + getDelegatedTo()));
        if (hasDelegatedFrom())
            prop.addParameter(new ZParameter(ICalTok.DELEGATED_FROM, "MAILTO:" + getDelegatedFrom()));
    }

    protected StringBuilder addToStringBuilder(StringBuilder sb) {
        if (hasCUType()) {
            if (sb.length() > 0) sb.append(';');
            sb.append("CUTYPE=").append(getCUType());
        }
        if (hasRole()) {
            if (sb.length() > 0) sb.append(';');
            sb.append("ROLE=").append(getRole());
        }
        if (hasPartStat()) {
            if (sb.length() > 0) sb.append(';');
            sb.append("PARTSTAT=").append(getPartStat());
        }
        if (hasRsvp()) {
            if (sb.length() > 0) sb.append(';');
            sb.append("RSVP=");
            if (getRsvp().booleanValue())
                sb.append("TRUE");
            else
                sb.append("FALSE");
        }
        if (hasMember()) {
            if (sb.length() > 0) sb.append(';');
            sb.append("MEMBER=\"MAILTO:").append(getMember()).append('"');
        }
        if (hasDelegatedTo()) {
            if (sb.length() > 0) sb.append(';');
            sb.append("DELEGATED-TO=\"MAILTO:").append(getDelegatedTo()).append('"');
        }
        if (hasDelegatedTo()) {
            if (sb.length() > 0) sb.append(';');
            sb.append("DELEGATED-FROM=\"MAILTO:").append(getDelegatedFrom()).append('"');
        }

        sb = super.addToStringBuilder(sb);
        return sb;
    }

    public boolean addressesMatch(ZAttendee other) {
        return getAddress().equalsIgnoreCase(other.getAddress());
    }

    public boolean addressMatches(String addr) {
        return getAddress().equalsIgnoreCase(addr);
    }
}
