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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

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
