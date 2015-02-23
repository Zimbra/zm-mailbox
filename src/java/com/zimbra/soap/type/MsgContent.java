/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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

package com.zimbra.soap.type;

import javax.xml.bind.annotation.XmlEnum;


/**
 * Message Content the client expects in response
 *
 */
@XmlEnum
public enum MsgContent {

    full, // The complete message
    original, // Only the Message and not quoted text
    both; // The complete message and also this message without quoted text 

    public static MsgContent fromString(String msgContent) {
        try {
            if (msgContent != null)
                return MsgContent.valueOf(msgContent);
            else
                return null;
        } catch (Exception e) {
            return null;
        }
    }
}

