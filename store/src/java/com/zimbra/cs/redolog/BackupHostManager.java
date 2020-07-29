package com.zimbra.cs.redolog;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbBackupHosts;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxManager.FetchMode;
import com.zimbra.cs.mailbox.util.MailboxClusterUtil;
import com.zimbra.cs.mailbox.util.MailboxClusterUtil.PodInfo;
import com.zimbra.cs.mailbox.util.MailboxClusterUtil.WorkerType;
import com.zimbra.cs.pubsub.PubSubService;
import com.zimbra.cs.pubsub.message.BackupHostMsg;

public class BackupHostManager {

    private HostAssignmentAlgorithm hostAssigner;
    private BackupHostMappingStore store;
    private PodInfo currentPod = MailboxClusterUtil.getPodInfo();
    private RedoStreamSelector streamSelector;
    private LoadingCache<Integer, BackupHost> hostCache;

    private static BackupHostManager instance;

    private BackupHostManager() {
        this.store = initMappingStore();
        this.hostAssigner = initAssignmentAlgorithm();
        this.hostCache = initHostCache();
    }

    public static BackupHostManager getInstance() {
        if (instance == null) {
            synchronized(BackupHostManager.class) {
                if (instance == null) {
                    instance = new BackupHostManager();
                }
            }
        }
        return instance;
    }

    public synchronized void initStreamSelector() {
        if (streamSelector == null) {
            ZimbraLog.redolog.info("initializing RedoStreamSelector");
            streamSelector = new RedoStreamSelector(this);
        }
    }

    protected BackupHostMappingStore initMappingStore() {
        return new DbMappingStore();
    }

    protected HostAssignmentAlgorithm initAssignmentAlgorithm() {
        return new HashedBackupHostAssignment(store);
    }

    public BackupHostAssignment getBackupHostAssignment(String accountId) throws ServiceException {
        boolean preAssigned;
        BackupHost host = store.getPendingBackupHostForAccount(accountId);
        if (host == null) {
            host = hostAssigner.assignHost(accountId);
            if (host == null) {
                return null;
            }
            preAssigned = false;
        } else {
            preAssigned = true;
        }
        return new BackupHostAssignment(host, preAssigned);
    }

    public BackupHost getBackupHostForAccount(Account account) throws ServiceException {
        return getBackupHostForAccount(account.getId(), account.getName(), false);
    }

    public BackupHost getBackupHostForAccount(String accountId, String email) throws ServiceException {
        return getBackupHostForAccount(accountId, email, false);
    }

    public BackupHost getBackupHostForAccount(String accountId, String email, boolean checkDeleted) throws ServiceException {
        return getBackupHostForAccount(accountId, email, checkDeleted, true);
    }

    public BackupHost getBackupHostForAccount(String accountId, boolean checkDeleted, boolean stagePendingMapping) throws ServiceException {
        return getBackupHostForAccount(accountId, null, checkDeleted, stagePendingMapping);
    }

    public BackupHost getBackupHostForAccount(String accountId, String accountName, boolean checkDeleted, boolean stagePendingMapping) throws ServiceException {
        BackupHost host = null;
        host = store.getBackupHostForAccount(accountId, checkDeleted);
        if (host == null) {
            BackupHostAssignment hostAssignment = getBackupHostAssignment(accountId);
            if (hostAssignment == null) {
                return null;
            }
            host = hostAssignment.getHost();
            if (hostAssignment.isPreAssigned()) {
                ZimbraLog.backup.debug("found pending backup host assignment for account %s: %s", accountId, host.getHost());
            } else if (stagePendingMapping){
                store.storePendingHostMapping(accountId, accountName, host);
                ZimbraLog.backup.debug("no host mapping found for account %s, mapped to %s and stored as pending assignment", accountId, host.getHost());
            }
        }
        return host;
    }

    public void updateMapping(String accountId, BackupHost host) throws ServiceException {
        store.storeHostMapping(accountId, host);
    }

    public boolean isLocal(Account account, boolean stagePendingMapping) throws ServiceException {
        if (currentPod.getType() == WorkerType.BACKUP_RESTORE) {
            BackupHost host = getBackupHostForAccount(account.getId(), account.getName(), false, stagePendingMapping);
            return host.getPod().isLocal();
        } else {
            return false;
        }
    }

    public void registerAsHost() {
        PodInfo thisPod = MailboxClusterUtil.getPodInfo();
        if (thisPod.getType() == WorkerType.BACKUP_RESTORE) {
            try {
                if(store.registerBackupHost(thisPod.getName())) {
                    ZimbraLog.backup.info("registered %s as a backup host", thisPod.getName());
                    streamSelector.reload();
                    PubSubService.getInstance().broadcast(new BackupHostMsg(thisPod.getName()));
                } else {
                    ZimbraLog.backup.info("%s is already registered as a backup host", thisPod.getName());
                }
            } catch (ServiceException e) {
                ZimbraLog.backup.error("error registering %s as a backup host", thisPod.getName(), e);
            }
        }
    }

    public List<String> getAccountIdsForBackupHost(BackupHost host) throws ServiceException {
        return getAccountIdsForBackupHost(host, false);
    }

    public List<String> getAccountIdsForBackupHost(BackupHost host, boolean includeDeleted) throws ServiceException {
        return store.getAllAccountIds(host, includeDeleted);
    }

    public BackupHost getLocalBackupHost() throws ServiceException {
        for (BackupHost host: getBackupHosts()) {
            if (host.getPod().equals(currentPod)) {
                return host;
            }
        }
        return null;
    }

    public BackupHost getBackupHostByName(String backupHostName) throws ServiceException {
        for (BackupHost host: getBackupHosts()) {
            if (host.getHost().equals(backupHostName)) {
                return host;
            }
        }
        throw ServiceException.FAILURE(String.format("no backup host found for '%s'", backupHostName), null);
    }

    private LoadingCache<Integer, BackupHost> initHostCache() {
        LoadingCache<Integer, BackupHost> cache = CacheBuilder.newBuilder()
                .build(new CacheLoader<Integer, BackupHost>() {
                    @Override
                    public BackupHost load(Integer backupHostId) throws Exception {
                        BackupHost host = store.getBackupHost(backupHostId);
                        ZimbraLog.backup.debug("caching %s", host);
                        return host;
                    }
                });
        return cache;
    }

    public BackupHost getBackupHost(int backupHostId) throws ServiceException {
        try {
            return hostCache.get(backupHostId);
        } catch (ExecutionException e) {
            throw ServiceException.FAILURE(String.format("error getting BackupHost with id=%s", backupHostId), e);
        }

    }

    public List<BackupHost> getBackupHosts() throws ServiceException {
        return store.getBackupHosts();
    }

    public RedoStreamSelector getStreamSelector() {
        return streamSelector;
    }

    public void changeBackupHostStatus(BackupHost host, BackupHostStatus status) throws ServiceException {
        store.changeBackupHostStatus(host, status);
        hostCache.invalidate(host.getHostId());
    }

    public static class BackupHost {

        private PodInfo hostPod;
        private int hostId;
        private long createdAt;
        private BackupHostStatus status;

        public BackupHost(int hostId, String hostname, long createdAt, BackupHostStatus status) {
            this.hostId = hostId;
            this.hostPod = new PodInfo(hostname);
            this.createdAt = createdAt;
            this.status = status;
        }

        public String getHost() {
            return hostPod.getName();
        }

        public PodInfo getPod() {
            return hostPod;
        }

        public int getHostId() {
            return hostId;
        }

        public boolean isAssignable() {
            return status == BackupHostStatus.ASSIGNABLE;
        }

        public BackupHostStatus getStatus() {
            return status;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        @Override
        public String toString() {
            return String.format("BackupHost[%s]", getHost());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof BackupHost) {
                return ((BackupHost) obj).getHost().equals(getHost());
            } else {
                return false;
            }
        }
    }

    public static interface HostAssignmentAlgorithm {

        public BackupHost assignHost(String accountId) throws ServiceException;
    }


    public static class HashedBackupHostAssignment implements HostAssignmentAlgorithm {

        private HashFunction hasher;
        protected BackupHostMappingStore mappingStore;

        public HashedBackupHostAssignment(BackupHostMappingStore mappingStore) {
            hasher = Hashing.murmur3_128();
            this.mappingStore = mappingStore;

        }

        protected List<BackupHost> getCandidateHosts() throws ServiceException {
            List<BackupHost> allHosts = mappingStore.getBackupHosts();
            if (allHosts.isEmpty()) {
                ZimbraLog.backup.warn("No backup hosts available!");
            }
            List<BackupHost> assignableHosts = allHosts.stream().filter(host -> host.isAssignable()).collect(Collectors.toList());
            if (assignableHosts.isEmpty()) {
                ZimbraLog.backup.warn("No assignable backup hosts available! Using all hosts as candidates instead");
                return allHosts;
            } else {
                return assignableHosts;
            }
        }

        @Override
        public BackupHost assignHost(String accountId) throws ServiceException {
            List<BackupHost> candidates = getCandidateHosts();
            if (candidates.isEmpty()) {
                return null;
            }
            int hostNum = Math.abs(hasher.hashString(accountId, Charsets.UTF_8).asInt()) % candidates.size();
            return candidates.get(hostNum);
        }
    }

    public static abstract class BackupHostMappingStore {

        public BackupHost getBackupHostForAccount(String accountId) throws ServiceException {
            return getBackupHostForAccount(accountId, false);
        }

        public abstract BackupHost getBackupHostForAccount(String accountId, boolean checkDeleted) throws ServiceException;

        public abstract BackupHost getPendingBackupHostForAccount(String accountId) throws ServiceException;

        public abstract void storeHostMapping(String accountId, BackupHost host) throws ServiceException;

        public abstract void storePendingHostMapping(String accountId, String accountName, BackupHost host) throws ServiceException;

        public abstract List<String> getAllAccountIds(BackupHost host, boolean includeDeleted) throws ServiceException;

        public abstract List<BackupHost> getBackupHosts() throws ServiceException;

        public abstract boolean registerBackupHost(String hostname) throws ServiceException;

        public abstract BackupHost getBackupHost(int backupHostId) throws ServiceException;

        public abstract void changeBackupHostStatus(BackupHost host, BackupHostStatus status) throws ServiceException;
    }

    public static class DbMappingStore extends BackupHostMappingStore {

        private Pair<DbConnection, Boolean> newConnection() throws ServiceException {
            return new Pair<>(DbPool.getConnection(), true);
        }

        private Pair<DbConnection, Boolean> getConnection(String accountId) throws ServiceException {
            // check if there's an active transaction that we can use
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(accountId, FetchMode.ONLY_IF_CACHED, true);
            if (mbox == null) {
                return newConnection();
            }
            if (mbox.isInTransaction()) {
                try {
                    return new Pair<>(mbox.getOperationConnection(), false);
                } catch (ServiceException e) {
                    return newConnection();
                }
            } else {
                return newConnection();
            }
        }

        @Override
        public BackupHost getBackupHostForAccount(String accountId, boolean checkDeleted) throws ServiceException {
            Pair<DbConnection, Boolean> connInfo = getConnection(accountId);
            DbConnection conn = connInfo.getFirst();
            boolean isNew = connInfo.getSecond();
            try {
                return DbMailbox.getBackupHostForAccount(conn, accountId, checkDeleted);
            } finally {
                if (isNew) {
                    DbPool.quietClose(conn);
                }
            }
        }

        @Override
        public void storeHostMapping(String accountId, BackupHost host) throws ServiceException {
            Pair<DbConnection, Boolean> connInfo = getConnection(accountId);
            DbConnection conn = connInfo.getFirst();
            boolean isNew = connInfo.getSecond();
            try {
                DbMailbox.setBackupHostForAccount(conn, accountId, host);
            } finally {
                if (isNew) {
                    DbPool.quietClose(conn);
                }
            }
        }

        @Override
        public List<String> getAllAccountIds(BackupHost host, boolean includeDeleted) throws ServiceException{
            return DbMailbox.getAccountsForBackupHost(host, includeDeleted);
        }

        @Override
        public List<BackupHost> getBackupHosts() throws ServiceException {
            return DbBackupHosts.getBackupHosts();
        }

        @Override
        public boolean registerBackupHost(String hostname) throws ServiceException {
            return DbBackupHosts.registerBackupHost(hostname);
        }

        @Override
        public BackupHost getBackupHost(int backupHostId) throws ServiceException {
            return DbBackupHosts.getBackupHost(backupHostId);
        }

        @Override
        public void storePendingHostMapping(String accountId, String accountName, BackupHost host) throws ServiceException {
            DbMailbox.setPendingBackupHostForAccount(accountId, accountName, host);
        }

        @Override
        public BackupHost getPendingBackupHostForAccount(String accountId) throws ServiceException {
            return DbMailbox.getPendingBackupHostAssignment(accountId);
        }

        @Override
        public void changeBackupHostStatus(BackupHost host, BackupHostStatus status) throws ServiceException {
            DbBackupHosts.setStatus(host, status);
        }
    }

    public static class BackupHostAssignment extends Pair<BackupHost, Boolean> {

        public BackupHostAssignment(BackupHost host, boolean preAssigned) {
            super(host, preAssigned);
        }

        public BackupHost getHost() {
            return getFirst();
        }

        public boolean isPreAssigned() {
            return getSecond();
        }
    }

    public static enum BackupHostStatus {

        UNASSIGNABLE(0),
        ASSIGNABLE(1);

        private int val;

        private BackupHostStatus(int val) {
            this.val = val;
        }

        public int getValue() {
            return val;
        }

        public static BackupHostStatus fromValue(int value) {
            for (BackupHostStatus status: BackupHostStatus.values()) {
                if (status.val == value) {
                    return status;
                }
            }
            throw new IllegalArgumentException(String.format("Unrecognised BackupHostStatus value: %s", value));
        }
    }
}
