/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
