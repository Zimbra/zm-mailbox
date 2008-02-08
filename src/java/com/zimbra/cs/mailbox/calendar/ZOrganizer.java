/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox.calendar;

import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZParameter;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.service.mail.CalendarUtils;
import com.zimbra.cs.service.mail.ToXML;

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

    public Element toXml(Element parent) {
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

        ToXML.encodeXParams(orgElt, xparamsIterator());

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

        List<ZParameter> xparams = CalendarUtils.parseXParams(element);

        ZOrganizer org = new ZOrganizer(address, cn, sentBy, dir, lang, xparams);
        return org;
    }
}
