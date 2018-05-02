/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.List;

import javax.mail.Address;
import javax.mail.internet.AddressException;

import com.zimbra.common.calendar.CalendarUtil;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZParameter;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.soap.mail.type.CalendarAttendee;

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

    public ZAttendee(ZAttendee other) {
        super(other);
        mCUType = other.mCUType;
        mRole = other.mRole;
        mPartStat = other.mPartStat;
        mRsvp = other.mRsvp;
        mMember = other.mMember;
        mDelegatedTo = other.mDelegatedTo;
        mDelegatedFrom = other.mDelegatedFrom;
    }

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
                     String delegatedFrom,
                     List<ZParameter> xparams) {
        super(address, cn, sentBy, dir, language, xparams);
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
             null, null, null, null, null, null, null, null);
    }

    public ZAttendee(ZProperty prop) {
        super(prop);
        setCUType(prop.paramVal(ICalTok.CUTYPE, null));
        setRole(prop.paramVal(ICalTok.ROLE, null));
        setPartStat(prop.paramVal(ICalTok.PARTSTAT, null));

        String rsvpStr = prop.paramVal(ICalTok.RSVP, null);
        boolean rsvp = false;
        if (null != rsvpStr) {
            if (rsvpStr.equalsIgnoreCase("TRUE")) {
                rsvp = true;
            }
            setRsvp(rsvp);
        }

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

    @Override
    protected ICalTok getPropertyName() {
        return ICalTok.ATTENDEE;
    }

    @Override
    protected void setProperty(ZProperty prop) throws ServiceException {
        super.setProperty(prop);
        if (hasCUType())
            prop.addParameter(new ZParameter(ICalTok.CUTYPE, IcalXmlStrMap.sCUTypeMap.toIcal(getCUType())));
        if (hasRole())
            prop.addParameter(new ZParameter(ICalTok.ROLE, IcalXmlStrMap.sRoleMap.toIcal(getRole())));
        if (hasPartStat())
            prop.addParameter(new ZParameter(ICalTok.PARTSTAT, IcalXmlStrMap.sPartStatMap.toIcal(getPartStat())));
        if (hasRsvp()) {
            // Apple Mac Calendar thinks a reply is still required if RSVP is set.  Suppress this if the PARTSTAT
            // isn't NEEDS-ACTION
            if (IcalXmlStrMap.PARTSTAT_NEEDS_ACTION.equals(getPartStat()) || IcalXmlStrMap.CUTYPE_RESOURCE.equals(getCUType())) {
                prop.addParameter(new ZParameter(ICalTok.RSVP, getRsvp()));
            }
        }
        if (hasMember())
            prop.addParameter(new ZParameter(ICalTok.MEMBER, "MAILTO:" + getMember()));
        if (hasDelegatedTo())
            prop.addParameter(new ZParameter(ICalTok.DELEGATED_TO, "MAILTO:" + getDelegatedTo()));
        if (hasDelegatedFrom())
            prop.addParameter(new ZParameter(ICalTok.DELEGATED_FROM, "MAILTO:" + getDelegatedFrom()));
    }

    @Override
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

    public boolean addressMatches(Address address) {
        if (address == null) {
            ZimbraLog.calendar.debug("addressMatches called with 'null' - can never match Zattendee '%s'", this);
            return false;
        }
        JavaMailInternetAddress jmia;
        try {
            jmia = new JavaMailInternetAddress(address.toString(), true);
            return getAddress().equalsIgnoreCase(jmia.getAddress());
        } catch (AddressException e) {
            ZimbraLog.calendar.debug("problem matching address '%s' against Zattendee '%s'", address, this, e);
            return false;
        }
    }

    public CalendarAttendee toJaxb() {
        CalendarAttendee attendee = new CalendarAttendee();
        // address
        attendee.setAddress(IDNUtil.toUnicode(getAddress()));
        attendee.setUrl(getAddress());  // for backward compatibility
        // CN
        if (hasCn())
            attendee.setDisplayName(getCn());
        // SENT-BY
        if (hasSentBy())
            attendee.setSentBy(getSentBy());
        // DIR
        if (hasDir())
            attendee.setDir(getDir());
        // LANGUAGE
        if (hasLanguage())
            attendee.setLanguage(getLanguage());
        // CUTYPE
        if (hasCUType())
            attendee.setCuType(getCUType());
        // ROLE
        if (hasRole())
            attendee.setRole(getRole());
        // PARTSTAT
        if (hasPartStat())
            attendee.setPartStat(getPartStat());
        // RSVP
        if (hasRsvp())
            attendee.setRsvp(getRsvp());
        // MEMBER
        if (hasMember())
            attendee.setMember(getMember());
        // DELEGATED-TO
        if (hasDelegatedTo())
            attendee.setDelegatedTo(getDelegatedTo());
        // DELEGATED-FROM
        if (hasDelegatedFrom())
            attendee.setDelegatedFrom(getDelegatedFrom());

        attendee.setXParams(ToXML.jaxbXParams(xparamsIterator()));
        return attendee;
    }

    public Element toXml(Element parent) {
        Element atElt = parent.addElement(MailConstants.E_CAL_ATTENDEE);
        // address
        atElt.addAttribute(MailConstants.A_ADDRESS, IDNUtil.toUnicode(getAddress()));
        atElt.addAttribute(MailConstants.A_URL, getAddress());  // for backward compatibility
        // CN
        if (hasCn())
            atElt.addAttribute(MailConstants.A_DISPLAY, getCn());
        // SENT-BY
        if (hasSentBy())
            atElt.addAttribute(MailConstants.A_CAL_SENTBY, getSentBy());
        // DIR
        if (hasDir())
            atElt.addAttribute(MailConstants.A_CAL_DIR, getDir());
        // LANGUAGE
        if (hasLanguage())
            atElt.addAttribute(MailConstants.A_CAL_LANGUAGE, getLanguage());
        // CUTYPE
        if (hasCUType())
            atElt.addAttribute(MailConstants.A_CAL_CUTYPE, getCUType());
        // ROLE
        if (hasRole())
            atElt.addAttribute(MailConstants.A_CAL_ROLE, getRole());
        // PARTSTAT
        if (hasPartStat())
            atElt.addAttribute(MailConstants.A_CAL_PARTSTAT, getPartStat());
        // RSVP
        if (hasRsvp())
            atElt.addAttribute(MailConstants.A_CAL_RSVP, getRsvp().booleanValue());
        // MEMBER
        if (hasMember())
            atElt.addAttribute(MailConstants.A_CAL_MEMBER, getMember());
        // DELEGATED-TO
        if (hasDelegatedTo())
            atElt.addAttribute(MailConstants.A_CAL_DELEGATED_TO, getDelegatedTo());
        // DELEGATED-FROM
        if (hasDelegatedFrom())
            atElt.addAttribute(MailConstants.A_CAL_DELEGATED_FROM, getDelegatedFrom());

        CalendarUtil.encodeXParams(atElt, xparamsIterator());

        return atElt;
    }

    public static ZAttendee parse(Element element) throws ServiceException {
        String address = IDNUtil.toAscii(element.getAttribute(MailConstants.A_ADDRESS, null));
        if (address == null) {
        	address = element.getAttribute(MailConstants.A_URL, null); //4.5 back compat
        	if (address == null) {
                throw ServiceException.INVALID_REQUEST("missing attendee address", null);
        	}
        }
        String cn = element.getAttribute(MailConstants.A_DISPLAY, null);
        String sentBy = element.getAttribute(MailConstants.A_CAL_SENTBY, null);
        String dir = element.getAttribute(MailConstants.A_CAL_DIR, null);
        String lang = element.getAttribute(MailConstants.A_CAL_LANGUAGE, null);

        String cutype = element.getAttribute(MailConstants.A_CAL_CUTYPE, null);
        if (cutype != null)
            validateAttr(IcalXmlStrMap.sCUTypeMap, MailConstants.A_CAL_CUTYPE, cutype);

        String role = element.getAttribute(MailConstants.A_CAL_ROLE, null);
        if (role != null)
            validateAttr(IcalXmlStrMap.sRoleMap, MailConstants.A_CAL_ROLE, role);

        String partStat = element.getAttribute(MailConstants.A_CAL_PARTSTAT, null);
        if (partStat != null)
            validateAttr(IcalXmlStrMap.sPartStatMap,
                         MailConstants.A_CAL_PARTSTAT, partStat);

        String member = element.getAttribute(MailConstants.A_CAL_MEMBER, null);
        String delTo = element.getAttribute(MailConstants.A_CAL_DELEGATED_TO, null);
        String delFrom = element.getAttribute(MailConstants.A_CAL_DELEGATED_FROM, null);

        boolean rsvp = element.getAttributeBool(MailConstants.A_CAL_RSVP, false);
//        if (partStat.equals(IcalXmlStrMap.PARTSTAT_NEEDS_ACTION))
//            rsvp = true;

        List<ZParameter> xparams = CalendarUtil.parseXParams(element);

        ZAttendee at =
            new ZAttendee(address, cn, sentBy, dir, lang,
                          cutype, role, partStat,
                          rsvp ? Boolean.TRUE : Boolean.FALSE,
                          member, delTo, delFrom, xparams);

        return at;
    }

    private static void validateAttr(IcalXmlStrMap map, String attrName,
            String value) throws ServiceException {
        if (!map.validXml(value)) {
            throw ServiceException.INVALID_REQUEST("Invalid value '"
                    + value + "' specified for attribute:" + attrName, null);
        }

    }
}
