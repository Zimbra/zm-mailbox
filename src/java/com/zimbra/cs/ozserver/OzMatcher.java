package com.zimbra.cs.ozserver;

import java.nio.ByteBuffer;

public interface OzMatcher {
    int match(ByteBuffer buffer);
    void trim(ByteBuffer buffer);
    void clear();
}
