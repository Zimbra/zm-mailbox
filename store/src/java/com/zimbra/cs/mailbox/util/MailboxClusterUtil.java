package com.zimbra.cs.mailbox.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import com.google.common.io.Files;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.UUIDUtil;
import com.zimbra.common.util.ZimbraLog;

public class MailboxClusterUtil {

    private static String mailboxWorkerName;
    static {
        try {
            mailboxWorkerName = Files.asCharSource(new File("/var/run/pod-info/pod-name"), Charset.defaultCharset()).read();
            ZimbraLog.mailbox.info("mailbox worker name is %s", mailboxWorkerName);
        } catch (IOException e) {
            mailboxWorkerName = String.format("zmc-mailbox-%s", UUIDUtil.generateUUID());
            ZimbraLog.mailbox.warn("Unable to determine mailbox worker name! Generated name '%s' instead", mailboxWorkerName);
        }
    }

    public static String getMailboxWorkerName() {
        return mailboxWorkerName;
    }

    /**
     * This class lets the mailbox temporarily bypass its liveness check, in case it
     * gets into a scenario where it is blocked waiting on some external dependency to
     * be restored. If this causes PingRequest to fail, the mailbox pod may be unnecessarily
     * recycled.
     * Ideally this should never happen, but it is useful to have as a fallback mechanism.
     */
    public static class LivenessProbeOverride implements Closeable {

        private static final String filename = LC.mailbox_liveness_probe_override_file.value();
        private static File file;

        public LivenessProbeOverride() {
            file = new File(filename);
            beginOverride();
        }

        public void beginOverride() {
            try {
                if (file.createNewFile()) {
                    ZimbraLog.misc.info("created liveness probe override file %s", filename);
                } else {
                    ZimbraLog.misc.info("liveness probe override file %s already exists", filename);
                }
            } catch (IOException | SecurityException e) {
                ZimbraLog.misc.error("unable to create liveness probe override file", e);
            }
        }

        public void endOverride() {
            try {
                if (file.delete()) {
                    ZimbraLog.misc.info("deleted liveness probe override file");
                } else {
                    ZimbraLog.misc.info("liveness probe override file was not deleted!");
                }
            } catch (SecurityException e) {
                ZimbraLog.misc.error("unable to delete liveness probe override file", e);
            }
        }

        @Override
        public void close() {
            endOverride();

        }

    }

}
