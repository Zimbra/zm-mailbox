/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox.calendar;

import java.util.List;

import com.zimbra.common.calendar.CalendarUtil;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZParameter;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.mail.type.CalOrganizer;

public class ZOrganizer extends CalendarUser {
    public ZOrganizer(String address, String cn) {
        super(address, cn, null, null, null);
    }

    public ZOrganizer(String address,
                      String cn,
                      String sentBy,
                      String dir,
                      String language,
                      List<ZParameter> xparams) {
        super(address, cn, sentBy, dir, language, xparams);
    }

    public ZOrganizer(ZOrganizer other) {
        super(other);
    }

    public ZOrganizer(ZProperty prop) {
        super(prop);
    }

    public ZOrganizer(Metadata meta) throws ServiceException {
        super(meta);
    }

    protected ICalTok getPropertyName() {
        return ICalTok.ORGANIZER;
    }

    // Note: CalOrganizer represents an organizer in the "urn:zimbraMail"
    //       namespace.
    public CalOrganizer toJaxb() {
        CalOrganizer org = new CalOrganizer();
        String str = getAddress();
        org.setAddress(IDNUtil.toUnicode(str));
        org.setUrl(str); // for backward compatibility
        if (hasCn())
            org.setDisplayName(getCn());
        if (hasSentBy())
            org.setSentBy(getSentBy());
        if (hasDir())
            org.setDir(getDir());
        if (hasLanguage())
            org.setLanguage(getLanguage());
        org.setXParams(ToXML.jaxbXParams(xparamsIterator()));
        return org;
    }

    public Element toXml(Element parent) {
        // TODO:  See Bug 61429.  This can be called for SOAP headers in
        // namespace "urn:Zimbra" as well as for namespace "urn:ZimbraMail",
        // so, we cannot just use one JAXB object to represent an organizer.
        // return JaxbUtil.addChildElementFromJaxb(parent,
        //         MailConstants.E_CAL_ORGANIZER, MailConstants.NAMESPACE_STR,
        //          toJaxb());
        // Perhaps could use parent.getQName() to determine the namespace?
        Element orgElt = parent.addUniqueElement(MailConstants.E_CAL_ORGANIZER);
        String str = getAddress();
        orgElt.addAttribute(MailConstants.A_ADDRESS, IDNUtil.toUnicode(str));
        orgElt.addAttribute(MailConstants.A_URL, str);  // for backward compatibility
        if (hasCn())
            orgElt.addAttribute(MailConstants.A_DISPLAY, getCn());
        if (hasSentBy())
            orgElt.addAttribute(MailConstants.A_CAL_SENTBY, getSentBy());
        if (hasDir())
            orgElt.addAttribute(MailConstants.A_CAL_DIR, getDir());
        if (hasLanguage())
            orgElt.addAttribute(MailConstants.A_CAL_LANGUAGE, getLanguage());

        CalendarUtil.encodeXParams(orgElt, xparamsIterator());

        return orgElt;
    }

    public static ZOrganizer parse(Element element) throws ServiceException {
        String address = IDNUtil.toAscii(element.getAttribute(MailConstants.A_ADDRESS, null));
        if (address == null) {
            address = element.getAttribute(MailConstants.A_URL, null); //4.5 back compat
            if (address == null) {
                throw ServiceException.INVALID_REQUEST(
                        "missing organizer address", null);
            }
        } 
        String cn = element.getAttribute(MailConstants.A_DISPLAY, null);
        String sentBy = element.getAttribute(MailConstants.A_CAL_SENTBY, null);
        String dir = element.getAttribute(MailConstants.A_CAL_DIR, null);
        String lang = element.getAttribute(MailConstants.A_CAL_LANGUAGE, null);

        List<ZParameter> xparams = CalendarUtil.parseXParams(element);

        ZOrganizer org = new ZOrganizer(address, cn, sentBy, dir, lang, xparams);
        return org;
    }
}
