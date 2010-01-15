/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2010 Zimbra, Inc.
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
package com.zimbra.cs.im;

import java.util.Formatter;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.IMConstants;

public class IMLeftChatNotification extends IMChatNotification {

    IMLeftChatNotification(IMAddr from, String threadId) {
        super(from, threadId);
    }

    public String toString() {
        return new Formatter().format("IMLeftChatNotification: %s", super.toString()).toString();
    }

    /* (non-Javadoc)
    * @see com.zimbra.cs.im.IMNotification#toXml(com.zimbra.common.soap.Element)
    */
    public Element toXml(Element parent) {
        Element toRet = create(parent, IMConstants.E_LEFTCHAT);
        super.toXml(toRet);
        return toRet;
    }
}
