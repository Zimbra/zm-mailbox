/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.common.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;

public final class BackupConstants {
    public static final String NAMESPACE_STR = AdminConstants.NAMESPACE_STR;
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

    public static final String E_EXPORTMAILBOX_REQUEST = "ExportMailboxRequest";
    public static final String E_EXPORTMAILBOX_RESPONSE = "ExportMailboxResponse";
    public static final String E_BACKUP_REQUEST = "BackupRequest";
    public static final String E_BACKUP_RESPONSE = "BackupResponse";
    public static final String E_BACKUP_QUERY_REQUEST = "BackupQueryRequest";
    public static final String E_BACKUP_QUERY_RESPONSE = "BackupQueryResponse";
    public static final String E_BACKUP_ACCOUNT_QUERY_REQUEST = "BackupAccountQueryRequest";
    public static final String E_BACKUP_ACCOUNT_QUERY_RESPONSE = "BackupAccountQueryResponse";
    public static final String E_RESTORE_REQUEST = "RestoreRequest";
    public static final String E_RESTORE_RESPONSE = "RestoreResponse";
    public static final String E_ROLLOVER_REDOLOG_REQUEST = "RolloverRedoLogRequest";
    public static final String E_ROLLOVER_REDOLOG_RESPONSE = "RolloverRedoLogResponse";
    public static final String E_PURGE_MOVED_MAILBOX_REQUEST = "PurgeMovedMailboxRequest";
    public static final String E_PURGE_MOVED_MAILBOX_RESPONSE = "PurgeMovedMailboxResponse";
    public static final String E_SCHEDULE_BACKUPS_REQUEST = "ScheduleBackupsRequest";
    public static final String E_SCHEDULE_BACKUPS_RESPONSE = "ScheduleBackupsResponse";
    public static final String E_MOVE_MAILBOX_REQUEST = "MoveMailboxRequest";
    public static final String E_MOVE_MAILBOX_RESPONSE = "MoveMailboxResponse";
    public static final String E_GET_MAILBOX_VERSION_REQUEST = "GetMailboxVersionRequest";
    public static final String E_GET_MAILBOX_VERSION_RESPONSE = "GetMailboxVersionResponse";
    public static final String E_GET_MAILBOX_VOLUMES_REQUEST = "GetMailboxVolumesRequest";
    public static final String E_GET_MAILBOX_VOLUMES_RESPONSE = "GetMailboxVolumesResponse";
    public static final String E_UNLOAD_MAILBOX_REQUEST = "UnloadMailboxRequest";
    public static final String E_UNLOAD_MAILBOX_RESPONSE = "UnloadMailboxResponse";
    public static final String E_RELOAD_ACCOUNT_REQUEST = "ReloadAccountRequest";
    public static final String E_RELOAD_ACCOUNT_RESPONSE = "ReloadAccountResponse";
    public static final String E_REGISTER_MAILBOX_MOVE_OUT_REQUEST = "RegisterMailboxMoveOutRequest";
    public static final String E_REGISTER_MAILBOX_MOVE_OUT_RESPONSE = "RegisterMailboxMoveOutResponse";
    public static final String E_UNREGISTER_MAILBOX_MOVE_OUT_REQUEST = "UnregisterMailboxMoveOutRequest";
    public static final String E_UNREGISTER_MAILBOX_MOVE_OUT_RESPONSE = "UnregisterMailboxMoveOutResponse";
    public static final String E_QUERY_MAILBOX_MOVE_REQUEST = "QueryMailboxMoveRequest";
    public static final String E_QUERY_MAILBOX_MOVE_RESPONSE = "QueryMailboxMoveResponse";

    public static final QName EXPORTMAILBOX_REQUEST = QName.get(E_EXPORTMAILBOX_REQUEST, NAMESPACE);
    public static final QName EXPORTMAILBOX_RESPONSE = QName.get(E_EXPORTMAILBOX_RESPONSE, NAMESPACE);

    public static final QName BACKUP_REQUEST = QName.get(E_BACKUP_REQUEST, NAMESPACE);
    public static final QName BACKUP_RESPONSE = QName.get(E_BACKUP_RESPONSE, NAMESPACE);
    public static final QName BACKUP_QUERY_REQUEST = QName.get(E_BACKUP_QUERY_REQUEST, NAMESPACE);
    public static final QName BACKUP_QUERY_RESPONSE = QName.get(E_BACKUP_QUERY_RESPONSE, NAMESPACE);
    public static final QName BACKUP_ACCOUNT_QUERY_REQUEST = QName.get(E_BACKUP_ACCOUNT_QUERY_REQUEST, NAMESPACE);
    public static final QName BACKUP_ACCOUNT_QUERY_RESPONSE = QName.get(E_BACKUP_ACCOUNT_QUERY_RESPONSE, NAMESPACE);
    public static final QName RESTORE_REQUEST = QName.get(E_RESTORE_REQUEST, NAMESPACE);
    public static final QName RESTORE_RESPONSE = QName.get(E_RESTORE_RESPONSE, NAMESPACE);
    public static final QName ROLLOVER_REDOLOG_REQUEST = QName.get(E_ROLLOVER_REDOLOG_REQUEST, NAMESPACE);
    public static final QName ROLLOVER_REDOLOG_RESPONSE = QName.get(E_ROLLOVER_REDOLOG_RESPONSE, NAMESPACE);

    public static final QName PURGE_MOVED_MAILBOX_REQUEST = QName.get(E_PURGE_MOVED_MAILBOX_REQUEST, NAMESPACE);
    public static final QName PURGE_MOVED_MAILBOX_RESPONSE = QName.get(E_PURGE_MOVED_MAILBOX_RESPONSE, NAMESPACE);

    public static final QName SCHEDULE_BACKUPS_REQUEST = QName.get(E_SCHEDULE_BACKUPS_REQUEST, NAMESPACE);
    public static final QName SCHEDULE_BACKUPS_RESPONSE = QName.get(E_SCHEDULE_BACKUPS_RESPONSE, NAMESPACE);

    public static final QName MOVE_MAILBOX_REQUEST = QName.get(E_MOVE_MAILBOX_REQUEST, NAMESPACE);
    public static final QName MOVE_MAILBOX_RESPONSE = QName.get(E_MOVE_MAILBOX_RESPONSE, NAMESPACE);
    public static final QName GET_MAILBOX_VERSION_REQUEST = QName.get(E_GET_MAILBOX_VERSION_REQUEST, NAMESPACE);
    public static final QName GET_MAILBOX_VERSION_RESPONSE = QName.get(E_GET_MAILBOX_VERSION_RESPONSE, NAMESPACE);
    public static final QName GET_MAILBOX_VOLUMES_REQUEST = QName.get(E_GET_MAILBOX_VOLUMES_REQUEST, NAMESPACE);
    public static final QName GET_MAILBOX_VOLUMES_RESPONSE = QName.get(E_GET_MAILBOX_VOLUMES_RESPONSE, NAMESPACE);
    public static final QName UNLOAD_MAILBOX_REQUEST = QName.get(E_UNLOAD_MAILBOX_REQUEST, NAMESPACE);
    public static final QName UNLOAD_MAILBOX_RESPONSE = QName.get(E_UNLOAD_MAILBOX_RESPONSE, NAMESPACE);
    public static final QName RELOAD_ACCOUNT_REQUEST = QName.get(E_RELOAD_ACCOUNT_REQUEST, NAMESPACE);
    public static final QName RELOAD_ACCOUNT_RESPONSE = QName.get(E_RELOAD_ACCOUNT_RESPONSE, NAMESPACE);
    public static final QName REGISTER_MAILBOX_MOVE_OUT_REQUEST = QName.get(E_REGISTER_MAILBOX_MOVE_OUT_REQUEST, NAMESPACE);
    public static final QName REGISTER_MAILBOX_MOVE_OUT_RESPONSE = QName.get(E_REGISTER_MAILBOX_MOVE_OUT_RESPONSE, NAMESPACE);
    public static final QName UNREGISTER_MAILBOX_MOVE_OUT_REQUEST = QName.get(E_UNREGISTER_MAILBOX_MOVE_OUT_REQUEST, NAMESPACE);
    public static final QName UNREGISTER_MAILBOX_MOVE_OUT_RESPONSE = QName.get(E_UNREGISTER_MAILBOX_MOVE_OUT_RESPONSE, NAMESPACE);
    public static final QName QUERY_MAILBOX_MOVE_REQUEST = QName.get(E_QUERY_MAILBOX_MOVE_REQUEST, NAMESPACE);
    public static final QName QUERY_MAILBOX_MOVE_RESPONSE = QName.get(E_QUERY_MAILBOX_MOVE_RESPONSE, NAMESPACE);

    public static final String ZM_SCHEDULE_BACKUP = "zmschedulebackup";

    public static final String E_ACCOUNT = "account";
    public static final String E_ACCOUNTS = "accounts";
    public static final String E_CURRENT_ACCOUNTS = "currentAccounts";
    public static final String E_MAILBOX = "mbox";
    public static final String E_QUERY = "query";
    public static final String E_BACKUP = "backup";
    public static final String E_RESTORE = "restore";
    public static final String E_ERROR = "error";
    public static final String E_STATS = "stats";
    public static final String E_COUNTER = "counter";

    public static final String A_MAILBOXID = "mbxid";
    public static final String A_NAME = "name";
    public static final String A_METHOD = "method";
    public static final String A_ACCOUNT_ID = "accountId";
    public static final String A_PREFIX = "prefix";
    public static final String A_INCLUDEINCREMENTALS = "includeIncrementals";
    public static final String A_SYSDATA = "sysData";
    public static final String A_BACKUP_TARGET = "target";
    public static final String A_LABEL = "label";
    public static final String A_TYPE = "type";  // "full" or "incremental"
    public static final String A_FROM = "from";
    public static final String A_TO = "to";
    public static final String A_ABORTED = "aborted";
    public static final String A_BACKUP_LIST_OFFSET = "backupListOffset";
    public static final String A_BACKUP_LIST_COUNT = "backupListCount";
    public static final String A_ACCOUNT_LIST_STATUS = "accountListStatus";
    public static final String A_ACCOUNT_LIST_OFFSET = "accountListOffset";
    public static final String A_ACCOUNT_LIST_COUNT = "accountListCount";
    public static final String A_VERBOSE = "verbose";
    public static final String A_STATS = "stats";
    public static final String A_COMPLETION_COUNT = "completionCount";
    public static final String A_ERROR_COUNT = "errorCount";
    public static final String A_TOTAL_COUNT = "total";
    public static final String A_MORE = "more";
    public static final String A_LIVE = "live";
    public static final String A_ERROR_MESSAGE = "errorMessage";
    public static final String A_START = "start";
    public static final String A_END = "end";
    public static final String A_MIN_REDO_SEQ = "minRedoSeq";
    public static final String A_MAX_REDO_SEQ = "maxRedoSeq";
    public static final String A_SYNC = "sync";
    public static final String A_ZIP = "zip";
    public static final String A_ZIP_STORE = "zipStore";
    public static final String A_SERVER = "server";
    public static final String A_STATUS = "status";
    public static final String A_REPLAY_CURRENT_REDOLOGS = "replayRedo";
    public static final String A_REBUILTSCHEMA = "rebuiltSchema";
    public static final String A_CONTINUE = "continue";
    public static final String A_BEFORE = "before";
    public static final String A_INCR_LABEL = "incr-label";
    public static final String A_SKIP_DELETED_ACCT = "skipDeletedAccounts";
    public static final String A_RESTORE_TO_TIME = "restoreToTime";
    public static final String A_RESTORE_TO_REDO_SEQ = "restoreToRedoSeq";
    public static final String A_RESTORE_TO_INCR_LABEL = "restoreToIncrLabel";
    public static final String A_RESTORE_IN_DR_MODE = "restoreInDRMode";
    public static final String A_IGNORE_REDO_ERRORS = "ignoreRedoErrors";
    public static final String A_SKIP_DELETE_OPS = "skipDeleteOps";
    public static final String A_COUNTER_UNIT = "unit";
    public static final String A_COUNTER_SUM = "sum";
    public static final String A_COUNTER_NUM_SAMPLES = "numSamples";
    public static final String A_TOTAL_SPACE = "totalSpace";
    public static final String A_FREE_SPACE = "freeSpace";

    // mailbox move
    public static final String A_SOURCE = "src";
    public static final String A_TARGET = "dest";
    public static final String A_PORT = "destPort";
    public static final String A_OVERWRITE = "overwrite";
    public static final String A_MAX_SYNCS = "maxSyncs";
    public static final String A_SYNC_FINISH_THRESHOLD = "syncFinishThreshold";
    public static final String A_TEMP_DIR = "tempDir";
    public static final String A_CHECK_PEER = "checkPeer";
    public static final String A_NO_PEER = "noPeer";
    public static final String A_MAJOR_VERSION = "majorVer";
    public static final String A_MINOR_VERSION = "minorVer";
    public static final String A_DB_VERSION = "dbVer";
    public static final String A_INDEX_VERSION = "indexVer";
    public static final String A_SKIP_REMOTE_LOCKOUT = "skipRemoteLockout";
    public static final String A_SKIP_MEMCACHE_PURGE = "skipMemcachePurge";

    // FileCopier options
    public static final String E_FILE_COPIER = "fileCopier";
    public static final String A_FC_METHOD = "fcMethod";
    public static final String A_FC_IOTYPE = "fcIOType";
    public static final String A_FC_OIO_COPY_BUFSIZE = "fcOIOCopyBufferSize";
    public static final String A_FC_ASYNC_QUEUE_CAPACITY = "fcAsyncQueueCapacity";
    public static final String A_FC_PARALLEL_WORKERS = "fcParallelWorkers";
    public static final String A_FC_PIPES = "fcPipes";
    public static final String A_FC_PIPE_BUFFER_SIZE = "fcPipeBufferSize";
    public static final String A_FC_PIPE_READERS = "fcPipeReadersPerPipe";
    public static final String A_FC_PIPE_WRITERS = "fcPipeWritersPerPipe";

    // include/exclude full backup components
    public static final String A_SEARCH_INDEX = "searchIndex";
    public static final String A_BLOBS = "blobs";
    public static final String A_SECONDARY_BLOBS = "secondaryBlobs";
    public static final String V_INCLUDE = "include";
    public static final String V_EXCLUDE = "exclude";
    public static final String V_CONFIG = "config";
    public static final String A_FORCE_DELETE_BLOBS = "forceDeleteBlobs";
}
