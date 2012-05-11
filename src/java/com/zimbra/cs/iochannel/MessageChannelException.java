/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.cs.iochannel;

import com.zimbra.common.iochannel.IOChannelException;

public class MessageChannelException extends IOChannelException {

    private static final long serialVersionUID = -3595831838657110474L;

    public MessageChannelException(String msg) {
        super(msg);
    }

    public static MessageChannelException NoSuchMessage(String message) {
        return new MessageChannelException("message doesn't exist " + message);
    }
}
