package com.zimbra.cs.redolog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamInfo;
import org.redisson.api.StreamMessageId;
import org.redisson.client.codec.ByteArrayCodec;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.mailbox.util.MailboxClusterUtil.PodInfo;
import com.zimbra.cs.mailbox.util.MailboxClusterUtil.WorkerType;
import com.zimbra.cs.redolog.BackupHostManager.BackupHost;
import com.zimbra.cs.redolog.op.BlobRecorder;
import com.zimbra.cs.redolog.op.CommitTxn;
import com.zimbra.cs.redolog.op.RedoableOp;

public class RedoStreamSelector {

   private BackupHostManager backupHostManager;

    private Map<PodInfo, StreamsOnPod> streamsByPod = new HashMap<>();
    private List<RedoStreamSpec> allStreams = new ArrayList<>(); // used for operations that can go to any pod
    private HashFunction hasher;

    public RedoStreamSelector(BackupHostManager backupHostManager) {
        this.backupHostManager = backupHostManager;
        hasher = Hashing.murmur3_128();
        initStreamMapping();
    }

    private void initStreamMapping() {
        List<BackupHost> hosts;
        try {
            hosts = backupHostManager.getBackupHosts();
        } catch (ServiceException e) {
            ZimbraLog.redolog.error("cannot initialize RedoStreamSelector!", e);
            return;
        }
        if (hosts.isEmpty()) {
            ZimbraLog.redolog.warn("no backup hosts configured yet!");
        } else {
            for (BackupHost host: hosts) {
                PodInfo pod = host.getPod();
                StreamsOnPod streamsOnPod = new StreamsOnPod(pod);
                streamsByPod.put(pod, streamsOnPod);
                allStreams.addAll(streamsOnPod.getStreams());
                ZimbraLog.redolog.info("initialized %s streams for %s", streamsOnPod.getStreams().size(), host);
            }
        }
    }

    public List<RedoStreamSpec> getStreamsForPod(PodInfo pod) {
        StreamsOnPod streamsOnPod = streamsByPod.get(pod);
        if (streamsOnPod == null) {
            return Collections.emptyList();
        } else {
            return streamsOnPod.getStreams();
        }
    }

    public List<RedoStreamSpec> getAllStreams() {
        return allStreams;
    }

    private RedoStreamSpec defaultStream() {
        return allStreams.get(0);
    }

    private boolean hasExternalBlobStore() {
        return RedoLogProvider.getInstance().getRedoLogManager().hasExternalBlobStore();
    }

    public RedoStreamSpec getStream(RedoableOp op) throws ServiceException {
        BackupHost host = op.getBackupHost();
        if (host  != null) {
            StreamsOnPod streamsOnPod = streamsByPod.get(host.getPod());
            if (streamsOnPod != null) {
                return selectStream(op.getAccountId(), streamsOnPod.getStreams());
            } else {
                return null;
            }
        } else if (hasExternalBlobStore() && op instanceof CommitTxn) {
            // StoreIncomingBlob  commit ops for a given digest should for a go to the same stream, so that they are
            // processed serially on the consumer side. This is to avoid a race condition in updating blob references,
            // in case the BlobReferenceManager implementation is not atomic.
            CommitTxn commit = (CommitTxn) op;
            BlobRecorder blobData = commit.getBlobRecorder();
            if (blobData != null && blobData.getBlobDigest() != null) {
                return selectStream(blobData.getBlobDigest(), allStreams);
            } else {
                RedoStreamSpec stream = defaultStream();
                ZimbraLog.redolog.warn("CommitTxn for %s doesn't have blob data! sending to stream %s", op.getTxnOpCode(), stream.getStreamName());
                return stream;
            }
        } else {
            return null;
        }
    }

    private RedoStreamSpec selectStream(String stringToHash, List<RedoStreamSpec> candidates) {
        int streamIndex = Math.abs(hasher.hashString(stringToHash, Charsets.UTF_8).asInt()) % candidates.size();
        return candidates.get(streamIndex);
    }

    public void reload() {
        synchronized(this) {
            ZimbraLog.redolog.info("reloading %s", getClass().getSimpleName());
            allStreams.clear();
            streamsByPod.clear();
            initStreamMapping();
        }
    }

    public static class RedoStreamSpec {

        private PodInfo destionationPod;
        private int streamNum;
        private String streamName;
        private RStream<byte[], byte[]> stream = null;

        public RedoStreamSpec(int podNum, int streamNum) {
            this.destionationPod = new PodInfo(WorkerType.BACKUP_RESTORE, podNum);
            this.streamNum = streamNum;
            this.streamName = String.format("%s_%s.%s", LC.redis_streams_redo_log_stream_prefix.value(), destionationPod.getIndex(), streamNum);
        }

        public String getStreamName() {
            return streamName;
        }

        public RStream<byte[], byte[]> getStream() {
            if (stream == null) {
                initStream();
            }
            return stream;
        }

        private void initStream() {
            if (stream != null) {
                return;
            }
            RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();
            stream = client.getStream(getStreamName(), ByteArrayCodec.INSTANCE);
            String consumerGroup = LC.redis_streams_redo_log_group.value();
            if (!stream.isExists()) {
                stream.createGroup(consumerGroup, StreamMessageId.ALL); // stream will be auto-created
                ZimbraLog.redolog.info("created consumer group %s and stream %s", consumerGroup, stream.getName());
            } else {
                // create group if it doesn't exist on the stream
                StreamInfo<byte[], byte[]> info = stream.getInfo();
                if (info.getGroups() == 0) {
                    stream.createGroup(consumerGroup, StreamMessageId.ALL);
                    ZimbraLog.redolog.info("created consumer group %s for existing stream %s", consumerGroup, stream.getName());
                }
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RedoStreamSpec) {
                RedoStreamSpec other = (RedoStreamSpec) obj;
                return other.destionationPod.equals(destionationPod) && other.streamNum == streamNum;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(destionationPod, streamNum);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("dest", destionationPod.getName())
                    .add("stream", streamNum).toString();
        }

        public PodInfo getDestinationPod() {
            return destionationPod;
        }
    }

    private static class StreamsOnPod {

        private PodInfo pod;
        private List<RedoStreamSpec> streams;

        public StreamsOnPod(PodInfo pod) {
            this.pod = pod;
            streams = new ArrayList<>();
            initStreams();
        }

        private int getNumStreamsOnPod() {
            // TODO: make this configurable per-pod?
            return LC.redis_num_redolog_streams_per_backup_worker.intValue();
        }

        private void initStreams() {
            int numStreams = getNumStreamsOnPod();
            for (int i = 0; i < numStreams; i++) {
                streams.add(new RedoStreamSpec(pod.getIndex(), i));
            }
        }

        public List<RedoStreamSpec> getStreams() {
            return streams;
        }
    }
}