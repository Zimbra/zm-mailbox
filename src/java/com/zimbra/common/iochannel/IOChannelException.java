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
package com.zimbra.common.iochannel;

import java.io.IOException;

public class IOChannelException extends IOException {

    private static final long serialVersionUID = -506129145508295776L;

    public enum Code {
        NoSuchPeer, PacketTooBig, ChannelClosed, Error
    };

    private final Code errorCode;

    public IOChannelException(Code c, String msg) {
        super(msg);
        errorCode = c;
    }

    public Code getCode() {
        return errorCode;
    }

    public static IOChannelException NoSuchPeer(String peerId) {
        return new IOChannelException(Code.NoSuchPeer, "no such peer " + peerId);
    }

    public static IOChannelException PacketTooBig(String header) {
        return new IOChannelException(Code.PacketTooBig, "large packet from " + header);
    }

    public static IOChannelException ChannelClosed(String channel) {
        return new IOChannelException(Code.ChannelClosed, "channel closed " + channel);
    }
}
