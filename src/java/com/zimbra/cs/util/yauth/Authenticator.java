package com.zimbra.cs.util.yauth;

import java.io.IOException;

public interface Authenticator {
    RawAuth authenticate() throws IOException;
    void invalidate();
}
