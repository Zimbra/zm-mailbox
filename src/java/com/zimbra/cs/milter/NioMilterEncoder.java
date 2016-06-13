/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.milter;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

final class NioMilterEncoder extends ProtocolEncoderAdapter {

    @Override
    public void encode(IoSession session, Object msg, ProtocolEncoderOutput out) {
        MilterPacket packet = (MilterPacket) msg;

        IoBuffer buffer = IoBuffer.allocate(4 + packet.getLength(), false);
        buffer.setAutoExpand(true);
        buffer.putInt(packet.getLength());
        buffer.put(packet.getCommand());
        byte[] data = packet.getData();
        if (data != null && data.length > 0) {
            buffer.put(data);
        }
        buffer.flip();
        out.write(buffer);
    }
}
