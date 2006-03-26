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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox.calendar;

import java.io.UnsupportedEncodingException;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZParameter;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.StringUtil;

public class ZAttendee {
    
    private String mAddress;
    private String mCn;
    private String mCUType;
    private String mRole;
    private String mPartStat;
    private Boolean mRsvp;
    
    public ZAttendee(String address, String cn, String cutype, String role, String ptst, Boolean rsvp) {
        setAddress(address);
        mCn = cn;
        setCUType(cutype);
        setRole(role);
        setPartStat(ptst);
        mRsvp = rsvp;
    }
    
    public ZAttendee(String address) {
        setAddress(address);
    }
    
    private static final String FN_ADDRESS         = "a";
    private static final String FN_CN              = "cn";
    private static final String FN_PARTSTAT        = "ptst";
    private static final String FN_CUTYPE          = "cut";
    private static final String FN_ROLE            = "r";
    private static final String FN_RSVP_BOOL       = "v";

    public String getAddress() {
        return mAddress != null ? mAddress : "";
    }
    public String getCn() { return mCn != null ? mCn : ""; }
    public String getCUType() { return mCUType != null ? mCUType : ""; }
    public String getRole() { return mRole != null ? mRole : ""; }
    public String getPartStat() { return mPartStat != null ? mPartStat : ""; }
    public Boolean getRsvp() { return mRsvp != null ? mRsvp : Boolean.FALSE; }

    public Address getFriendlyAddress() throws MailServiceException {
        InternetAddress addr;
        try {
            if (hasCn())
                addr = new InternetAddress(getAddress(),
                                           getCn(),
                                           Mime.P_CHARSET_UTF8);
            else
                addr = new InternetAddress(getAddress());
            return addr;
        } catch (UnsupportedEncodingException e) {
            throw MailServiceException.ADDRESS_PARSE_ERROR(e);
        } catch (AddressException e) {
            throw MailServiceException.ADDRESS_PARSE_ERROR(e);
        }
    }
    
    public void setAddress(String address) {
        if (address != null) {
            if (address.toLowerCase().startsWith("mailto:")) {
                // MAILTO:  --> 
                address = address.substring(7); 
            }
        }
        mAddress = address; 
    }
    public void setCn(String cn) { mCn = cn; }
    public void setCUType(String cutype) {
        if (cutype != null && !IcalXmlStrMap.sCUTypeMap.validXml(cutype)) {
            cutype = IcalXmlStrMap.sCUTypeMap.toXml(cutype);
        }
        mCUType = cutype; 
    }
    public void setRole(String role) {
        if (role != null && !IcalXmlStrMap.sRoleMap.validXml(role)) {
            role = IcalXmlStrMap.sRoleMap.toXml(role);
        }
        mRole = role; 
    }
    public void setPartStat(String partStat) { 
        if (partStat != null && !IcalXmlStrMap.sPartStatMap.validXml(partStat)) {
            partStat = IcalXmlStrMap.sPartStatMap.toXml(partStat);
        }
        mPartStat = partStat; 
    }
    
    public void setRsvp(Boolean rsvp) { mRsvp = rsvp; }
    
    public boolean hasCn() { return !StringUtil.isNullOrEmpty(mCn); }
    public boolean hasCUType() { return !StringUtil.isNullOrEmpty(mCUType); }
    public boolean hasRole() { return !StringUtil.isNullOrEmpty(mRole); }
    public boolean hasPartStat() { return !StringUtil.isNullOrEmpty(mPartStat); }
    public boolean hasRsvp() { return mRsvp != null; }

    public boolean addressesMatch(ZAttendee other) {
        return getAddress().equalsIgnoreCase(other.getAddress());
    }

    public boolean addressMatches(String addr) {
        return getAddress().equalsIgnoreCase(addr);
    }
    
    public String toString() {
        StringBuffer toRet = new StringBuffer("ZATTENDEE:");
        if (hasCn()) {
            toRet.append("CN=").append(mCn).append(";");
        }
        if (hasRole()) {
            toRet.append("ROLE=").append(mRole).append(";");
        }
        if (hasPartStat()) {
            toRet.append("PARTSTAT=").append(mPartStat).append(";");
        }
        if (hasRsvp()) {
            toRet.append("RSVP=").append(mRsvp).append(";");
        }
        if (hasCUType()) {
            toRet.append("CUTYPE=").append(mCUType).append(";");
        }
        
        toRet.append(mAddress);
        return toRet.toString();
    }
    
    public Metadata encodeAsMetadata() {
        Metadata meta = new Metadata();
        
        meta.put(FN_ADDRESS, mAddress);
        
        if (mCn != null) {
            meta.put(FN_CN, mCn);
        }

        if (mCUType != null) {
        	meta.put(FN_CUTYPE, mCUType);
        }

        if (mRole != null) {
            meta.put(FN_ROLE, mRole);
        }
        
        if (mPartStat!= null) {
            meta.put(FN_PARTSTAT, mPartStat);
        }
        
        if (mRsvp != null) {
            meta.put(FN_RSVP_BOOL, "1");
        }
        return meta;
    }
    
    public static ZAttendee parseAtFromMetadata(Metadata meta) throws ServiceException {
        if (meta == null) {
            return null;
        }
        String addressStr = meta.get(FN_ADDRESS, null);
        String cnStr = meta.get(FN_CN, null);
        String cutypeStr = meta.get(FN_CUTYPE, null);
        String roleStr = meta.get(FN_ROLE, null);
        String partStatStr = meta.get(FN_PARTSTAT, null);
        Boolean rsvpBool = null;
        if (meta.containsKey(FN_RSVP_BOOL)) {
            if (meta.getBool(FN_RSVP_BOOL)) {
                rsvpBool = Boolean.TRUE;
            } else {
                rsvpBool = Boolean.FALSE;
            }
        }
        
        return new ZAttendee(addressStr, cnStr, cutypeStr, roleStr, partStatStr, rsvpBool);
    }
    
    public static ZAttendee fromProperty(ZProperty prop) {
        String cn = prop.paramVal(ICalTok.CN, null);
        String role = prop.paramVal(ICalTok.ROLE, null);
        String cutype = prop.paramVal(ICalTok.CUTYPE, null);
        String partstat = prop.paramVal(ICalTok.PARTSTAT, null);
        String rsvpStr = prop.paramVal(ICalTok.RSVP, "FALSE");
        boolean rsvp = false;
        if (rsvpStr.equalsIgnoreCase("TRUE")) {
            rsvp = true;
        }
        
        ZAttendee toRet = new ZAttendee(prop.mValue, cn, cutype, role, partstat, rsvp);
        return toRet;
    }
    
    public ZProperty toProperty() throws ServiceException {
        ZProperty toRet = new ZProperty(ICalTok.ATTENDEE, "MAILTO:"+getAddress());
        if (hasCn()) 
            toRet.addParameter(new ZParameter(ICalTok.CN, getCn()));
        if (hasPartStat())
            toRet.addParameter(new ZParameter(ICalTok.PARTSTAT, IcalXmlStrMap.sPartStatMap.toIcal(getPartStat())));
        if (hasRsvp())
            toRet.addParameter(new ZParameter(ICalTok.RSVP, getRsvp()));
        if (hasRole())
            toRet.addParameter(new ZParameter(ICalTok.ROLE, IcalXmlStrMap.sRoleMap.toIcal(getRole())));
        if (hasCUType())
            toRet.addParameter(new ZParameter(ICalTok.CUTYPE, IcalXmlStrMap.sCUTypeMap.toIcal(getCUType())));
        
        return toRet;
    }
}
