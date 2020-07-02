package com.zimbra.cs.util;

import java.io.Closeable;

public class IOUtil {
    /**
     * Quietly close given closeable
     * @param closeable closeable to close
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
            }
        }
    }
}
