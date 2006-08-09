package com.zimbra.cs.ozserver;

import java.nio.ByteBuffer;

public class OzAllMatcher implements OzMatcher {

    public boolean match(ByteBuffer buf) {
       int n = buf.remaining();
       for (int i = 0; i < n; i++) {
            buf.get();
       }
       return true;
    }

    public boolean matched() {
        return true;
    }

    public void reset() {
    }

    public int trailingTrimLength() {
        return 0;
    }

}
