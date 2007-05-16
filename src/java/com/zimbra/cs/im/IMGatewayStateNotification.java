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
package com.zimbra.cs.im;

import java.util.Formatter;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.IMConstants;
import com.zimbra.common.util.ZimbraLog;

/**
 * 
 */
public class IMGatewayStateNotification extends IMNotification {
    
    String mServiceName;
    String mState;
    String mDelay;
    
    IMGatewayStateNotification(String serviceName, String state, String delay) {
        mServiceName = serviceName;
        mState = state;
        mDelay= delay;
    }

    public String toString() {
        return new Formatter().format("IMGatewayStateNotification(%s, State=%s)",
            mServiceName, mState).toString();
    }
    

    /* @see com.zimbra.cs.im.IMNotification#toXml(com.zimbra.common.soap.Element) */
    @Override
    public Element toXml(Element parent) throws ServiceException {
        ZimbraLog.im.debug(this.toString());
        Element toRet = create(parent, IMConstants.E_GATEWAY_STATUS);
        toRet.addAttribute(IMConstants.A_SERVICE, mServiceName); 
        toRet.addAttribute(IMConstants.A_STATE, mState);
        if (mDelay != null) {
            toRet.addAttribute(IMConstants.A_TIME_UNTIL_NEXT_CONNECT, mDelay);
        }
        return toRet;
    }
}
