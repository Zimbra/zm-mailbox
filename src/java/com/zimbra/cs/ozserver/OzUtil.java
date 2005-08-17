package com.liquidsys.coco.ozserver;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import com.liquidsys.coco.util.LiquidLog;

class OzUtil {

    public static String byteBufferToString(String bufferName, ByteBuffer buff, boolean flip) {
        ByteBuffer buf = buff.duplicate();
        if (flip) {
            buf.flip();
        }

        StringBuffer sb = new StringBuffer(buf.remaining() * 4);
        
        sb.append(bufferName);
        sb.append(" position=").append(buf.position());
        sb.append(" limit=").append(buf.limit());
        sb.append(" capacity=").append(buf.capacity());
        sb.append(" hasRemaining=").append(buf.hasRemaining()).append('\n');

        int n = 1;
        int remaining = buf.remaining();
        sb.append("[").append(intToDecString(0, 10, ' ')).append("] ");
        
        while (buf.hasRemaining()) {
            byte b = buf.get();
            char ch = (char)b;
            if (ch >= 41 && ch <= 126) {
                sb.append(" " + ch);
            } else if (ch == '\r') {
                sb.append("\\r");
            } else if (ch == '\n') {
                sb.append("\\n");
            } else if (ch == ' ') {
                sb.append("\\b");
            } else {
                sb.append(intToHexString(ch, 2, '0'));
            }
            if (n != remaining) {
                if ((n % 22) == 0) {
                    sb.append('\n');
                    sb.append("[").append(intToDecString(n, 10, ' ')).append("] ");
                } else {
                    sb.append(' ');
                }
            }
            n++;
        }
        return sb.toString();
    }

    public static String intToHexString(int value, int width, char pad) {
        StringBuffer sb = new StringBuffer(width);
        String sv = Integer.toHexString(value).toUpperCase();
        int numPad = width - sv.length();
        for (int i = 0; i < numPad; i++) {
            sb.append(pad);
        }
        sb.append(sv);
        return sb.toString();
    }

    public static String intToDecString(int value, int width, char pad) {
        StringBuffer sb = new StringBuffer(width);
        String sv = Integer.toString(value);
        int numPad = width - sv.length();
        for (int i = 0; i < numPad; i++) {
            sb.append(pad);
        }
        sb.append(sv);
        return sb.toString();
    }
    
	public static String asciiByteArrayToString(ByteBuffer buffer) {
        StringBuffer sb = new StringBuffer(buffer.limit() - buffer.position());
        for (int i = buffer.position(); i < buffer.limit(); i++) {
        	sb.append((char)buffer.get());
        }
		return sb.toString();
	}

	public static void logSelectionKey(SelectionKey selectionKey, int id, String where) {
        synchronized (selectionKey) {
        	if (selectionKey.isValid()) {
        		LiquidLog.ozserver.debug(where +
        				" cid=" + id + 
						" iops=" + selectionKey.interestOps() + 
						" rops=" + selectionKey.readyOps() + 
						" key=" + Integer.toHexString(selectionKey.hashCode()));
            } else {
            	LiquidLog.ozserver.debug(where + 
                        " invalid cid=" + id + 
                        " key=" + Integer.toHexString(selectionKey.hashCode()));
            }
        } 
	}
}
