/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.common.calendar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.zimbra.common.calendar.ZCalendar.ZParameter;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;

/**
 * Common calendar utilities.
 */
public class CalendarUtil {

    public static List<ZParameter> parseXParams(Element element) throws ServiceException {
        List<ZParameter> params = new ArrayList<ZParameter>();
        for (Iterator<Element> paramIter = element.elementIterator(MailConstants.E_CAL_XPARAM);
             paramIter.hasNext(); ) {
            Element paramElem = paramIter.next();
            String paramName = paramElem.getAttribute(MailConstants.A_NAME);
            String paramValue = paramElem.getAttribute(MailConstants.A_VALUE, null);
            ZParameter xparam = new ZParameter(paramName, paramValue);
            params.add(xparam);
        }
        return params;
    }

    /**
     * Use JAXB e.g. {@link com.zimbra.soap.mail.type.XParam} where possible
     * instead of using this
     */
    public static void encodeXParams(Element parent, Iterator<ZParameter> xparamsIterator) {
        while (xparamsIterator.hasNext()) {
            ZParameter xparam = xparamsIterator.next();
            String paramName = xparam.getName();
            if (paramName == null) continue;
            Element paramElem = parent.addElement(MailConstants.E_CAL_XPARAM);
            paramElem.addAttribute(MailConstants.A_NAME, paramName);
            String paramValue = xparam.getValue();
            if (paramValue != null)
                paramElem.addAttribute(MailConstants.A_VALUE, paramValue);
        }
    }

}
