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

import java.net.URI;

import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.service.ServiceException;

import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.parameter.Rsvp;
import net.fortuna.ical4j.model.property.Attendee;

public class ZAttendee {
    
    private String mAddress;
    private String mCn;
    private String mRole;
    private String mPartStat;
    private Boolean mRsvp;
    
    public ZAttendee(String address, String cn, String role, String ptst, Boolean rsvp) {
        setAddress(address);
        mCn = cn;
        mRole = role;
        mPartStat = ptst;
        mRsvp = rsvp;
    }
    
    public ZAttendee(String address) {
        setAddress(address);
    }
    
    public ZAttendee(Attendee at) {
        ParameterList params = at.getParameters();
        
        setAddress(at.getCalAddress().getSchemeSpecificPart());
        
        // CN
        Cn cn = (Cn)params.getParameter(Parameter.CN);
        if (cn != null) {
            mCn = cn.getValue();
        }
        
        // role
        Role role = (Role) params.getParameter(Parameter.ROLE);
        if (role != null) {
            mRole = IcalXmlStrMap.sRoleMap.toXml(role.getValue());
        }
        
        // partstat
        PartStat partStat = (PartStat) params.getParameter(Parameter.PARTSTAT);
        if (partStat != null) {
            mPartStat = IcalXmlStrMap.sPartStatMap.toXml(partStat.getValue());
        }
        
        // rsvp?
        Parameter rsvpParam = params.getParameter(Parameter.RSVP);
        if (rsvpParam != null) {
            mRsvp = ((Rsvp) rsvpParam).getRsvp();
        }
    }
    
    private static final String FN_ADDRESS         = "a";
    private static final String FN_CN              = "cn";
    private static final String FN_PARTSTAT        = "ptst";
    private static final String FN_ROLE            = "r";
    private static final String FN_RSVP_BOOL       = "v";

    public String getAddress() {
        return mAddress != null ? mAddress : "";
    }
    public String getCn() { return mCn != null ? mCn : ""; }
    public String getRole() { return mRole != null ? mRole : ""; }
    public String getPartStat() { return mPartStat != null ? mPartStat : ""; }
    public Boolean getRsvp() { return mRsvp != null ? mRsvp : Boolean.FALSE; }
    
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
    public void setRole(String role) { mRole = role; }
    public void setPartStat(String partStat) { mPartStat = partStat; }
    public void setRsvp(Boolean rsvp) { mRsvp = rsvp; }
    
    public boolean hasCn() { return mCn != null; }
    public boolean hasRole() { return mRole != null; }
    public boolean hasPartStat() { return mPartStat != null; }
    public boolean hasRsvp() { return mRsvp != null; }

    public boolean addressesMatch(ZAttendee other) {
        return getAddress().equalsIgnoreCase(other.getAddress());
    }

    public boolean addressMatches(String addr) {
        return getAddress().equalsIgnoreCase(addr);
    }
    
    public String toString() {
        StringBuffer toRet = new StringBuffer("ZATTENDEE:");
        if (mCn != null) {
            toRet.append("CN=").append(mCn).append(";");
        }
        if (mRole != null) {
            toRet.append("ROLE=").append(mRole).append(";");
        }
        if (mPartStat != null) {
            toRet.append("PARTSTAT=").append(mPartStat).append(";");
        }
        if (mRsvp!= null) {
            toRet.append("RSVP=").append(mRsvp).append(";");
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
        
        return new ZAttendee(addressStr, cnStr, roleStr, partStatStr, rsvpBool);
    }
    
    public Attendee iCal4jAttendee() throws ServiceException
    {
        ParameterList p = new ParameterList();
        
        if (mCn != null && !mCn.equals("")) {
            Cn cn = new Cn(mCn);
            p.add(cn);
        }
        
        if (mRole != null && !mRole.equals("")) {
            Role role = new Role(IcalXmlStrMap.sRoleMap.toIcal(mRole));
            p.add(role);
        }
        
        if (mPartStat != null && !mPartStat.equals("")) {
            PartStat partStat = new PartStat(IcalXmlStrMap.sPartStatMap.toIcal(mPartStat));
            p.add(partStat);
        }
        
        if (mRsvp != null) {
            Rsvp rsvp = new Rsvp(mRsvp);
            p.add(rsvp);
        }
        
        try {
            return new Attendee(p, new URI("MAILTO", mAddress, null));
        } catch (java.net.URISyntaxException e) {
            throw ServiceException.FAILURE("Building Attendee URI for address "+mAddress, e);
        }
    }
}
