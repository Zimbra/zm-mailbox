package com.zimbra.cs.mailbox.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Arrays;

import com.google.common.base.MoreObjects;
import com.google.common.io.Files;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.UUIDUtil;
import com.zimbra.common.util.ZimbraLog;

public class MailboxClusterUtil {

    private static PodInfo podInfo;

    static {
        try {
            String mailboxWorkerName = Files.asCharSource(new File("/var/run/pod-info/pod-name"), Charset.defaultCharset()).read();
            podInfo = new PodInfo(mailboxWorkerName);
        } catch (IOException e) {
            ZimbraLog.misc.error("error reading pod info!", e);
            podInfo = PodInfo.generateUnknownPodInfo();
        }
    }

    public static String getMailboxWorkerName() {
        return podInfo.getName();
    }

    public static int getWorkerIndex() {
        return podInfo.getIndex();
    }

    public static WorkerType getWorkerType() {
        return podInfo.getType();
    }

    public static PodInfo getPodInfo() {
        return podInfo;
    }

    public static int getNumPodsByType(WorkerType type) throws ServiceException {
        try {
            InetAddress[] addrs = InetAddress.getAllByName(type.getServiceName());
            return addrs.length;
        } catch (UnknownHostException e) {
            throw ServiceException.NOT_FOUND(String.format("Host exception for ", WorkerType.MAILBOX.getServiceName()), e);
        }
    }

    public static String[] getAllMailboxPodIPs() throws ServiceException {
        return getAllMailboxPodIPs(WorkerType.MAILBOX.getServiceName());
    }

    public static String[] getAllMailboxPodIPs(String mboxServiceName) throws ServiceException {
        try {
            InetAddress[] addrs = InetAddress.getAllByName(mboxServiceName);
            return Arrays.stream(addrs).map(addr -> addr.getHostAddress().trim()).toArray(String[]::new);
        } catch (UnknownHostException e) {
            throw ServiceException.NOT_FOUND(String.format("Error resolving IPs for %s", mboxServiceName), e);
        }
    }

    public static boolean isMailboxPod() {
        return podInfo.getType() == WorkerType.MAILBOX;
    }

    public static boolean isBackupRestorePod() {
        return podInfo.getType() == WorkerType.BACKUP_RESTORE;
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

    public static enum WorkerType {
        MAILBOX("zmc-mailbox"),
        BACKUP_RESTORE("zmc-backup-restore"),
        UNKNOWN("unknown");

        private String serviceName;

        private WorkerType(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getServiceName() {
            return serviceName;
        }
    }

    public static class PodInfo {

        public static final int UNKNOWN_POD_INDEX = -1;
        private String podName;
        private WorkerType podType;
        private int podIndex;

        public PodInfo(String podName) {
            this.podName  = podName;
            this.podIndex = parseWorkerIndex(podName);
            this.podType = parseWorkerType(podName);
        }

        public PodInfo(WorkerType type, int index) {
            this.podType = type;
            this.podIndex = index;
            this.podName = String.format("%s-%s", type.getServiceName(), index);
        }

        private PodInfo(String podName, WorkerType type, int index) {
            this.podName = podName;
            this.podType = type;
            this.podIndex = index;
        }

        private int parseWorkerIndex(String podName) {
            String[] parts = podName.split("-");
            try {
                return Integer.parseInt(parts[parts.length - 1]);
            } catch (NumberFormatException e) {
                ZimbraLog.mailbox.warn("unable to determine worker index, setting to %s", UNKNOWN_POD_INDEX);
                return UNKNOWN_POD_INDEX;
            }
        }

        private WorkerType parseWorkerType(String podName) {
            for (WorkerType type: WorkerType.values()) {
                if (podName.startsWith(type.getServiceName())) {
                    return type;
                }
            }
            ZimbraLog.mailbox.warn("unable to determine worker type, setting to %s", WorkerType.UNKNOWN);
            return WorkerType.UNKNOWN;
        }

        public String getName() {
            return podName;
        }

        public WorkerType getType() {
            return podType;
        }

        public int getIndex() {
            return podIndex;
        }

        public static PodInfo generateUnknownPodInfo() {
            return new PodInfo(String.format("pod-%s", UUIDUtil.generateUUID()), WorkerType.UNKNOWN, UNKNOWN_POD_INDEX);
        }

        public String getFQN() {
            return String.format("%s.%s.default.svc.cluster.local", getName(), getType().getServiceName());
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof PodInfo) {
                PodInfo otherPod = (PodInfo) o;
                return otherPod.podIndex == podIndex && otherPod.getType() == podType;
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("name", podName)
                    .add("type", podType)
                    .add("idx", podIndex).toString();
        }
    }
}
