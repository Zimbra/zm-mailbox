/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.VoiceConstants;

import java.util.ArrayList;
import java.util.List;

public class ZSelectiveCallRejection extends ZCallFeature {

    private List<String> mRejectFrom;

    public ZSelectiveCallRejection(String name) {
        super(name);
        mRejectFrom = new ArrayList<String>();        
    }

    public List<String> getRejectFrom() {
        return new ArrayList<String>(mRejectFrom);
    }

    public synchronized void setRejectFrom(List<String> list) {
        mRejectFrom.clear();
        mRejectFrom.addAll(list);
    }

    public synchronized void assignFrom(ZCallFeature that) {
        super.assignFrom(that);
        if (that instanceof ZSelectiveCallRejection) {
            this.mRejectFrom = new ArrayList<String>(((ZSelectiveCallRejection)that).mRejectFrom);
        }
    }

    synchronized void fromElement(Element element) throws ServiceException {
        super.fromElement(element);
        mRejectFrom = new ArrayList<String>();
        for (Element fromEl : element.listElements(VoiceConstants.E_PHONE)) {
            mRejectFrom.add(fromEl.getAttribute(VoiceConstants.A_PHONE_NUMBER));
        }
    }

    void toElement(Element element) throws ServiceException {
        super.toElement(element);
	for (String name : mRejectFrom) {
            Element fromEl = element.addElement(VoiceConstants.E_PHONE);
            fromEl.addAttribute(VoiceConstants.A_PHONE_NUMBER, name);
            fromEl.addAttribute(VoiceConstants.A_ACTIVE, "true");
        }
    }
}
