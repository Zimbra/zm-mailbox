/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.client;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.VoiceConstants;

import java.util.ArrayList;
import java.util.List;

public class ZSelectiveCallForwarding extends ZCallFeature {

    private List<String> mForwardFrom;

    public ZSelectiveCallForwarding(String name) {
        super(name);
        mForwardFrom = new ArrayList<String>();        
    }

    public List<String> getForwardFrom() {
        return new ArrayList<String>(mForwardFrom);
    }

    public synchronized void setForwardFrom(List<String> list) {
        mForwardFrom.clear();
        mForwardFrom.addAll(list);
    }

    public synchronized void assignFrom(ZCallFeature that) {
        super.assignFrom(that);
        if (that instanceof ZSelectiveCallForwarding) {
            this.mForwardFrom = new ArrayList<String>(((ZSelectiveCallForwarding)that).mForwardFrom);
        }
    }

    synchronized void fromElement(Element element) throws ServiceException {
        super.fromElement(element);
        mForwardFrom = new ArrayList<String>();
        for (Element fromEl : element.listElements(VoiceConstants.E_PHONE)) {
             mForwardFrom.add(fromEl.getAttribute(VoiceConstants.A_PHONE_NUMBER));
        }
    }

    void toElement(Element element) throws ServiceException {
        super.toElement(element);
        for (String name : mForwardFrom) {
            Element fromEl = element.addElement(VoiceConstants.E_PHONE);
            fromEl.addAttribute(VoiceConstants.A_PHONE_NUMBER, name);
            fromEl.addAttribute(VoiceConstants.A_ACTIVE, "true");
        }
    }
}
