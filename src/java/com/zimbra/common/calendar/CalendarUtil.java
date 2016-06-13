/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
