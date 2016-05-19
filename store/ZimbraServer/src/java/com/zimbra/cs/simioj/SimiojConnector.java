/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.simioj;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.compress.utils.IOUtils;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.op.RedoableOp;

/** Using enum as a simple way to create a Singleton */

public enum SimiojConnector {

    INSTANCE;

    public static final File simiojOpsDir = new File("/opt/zimbra/simioj-ops");

    /**
     * Demo version.  Imagine the real final version would return something like the RAFT log entry index.
     * @param op
     * @return Currently, name of the file which contains a serialization of the Redolog entry.
     */
    public String submit(RedoableOp op) throws ServiceException {
        if (!simiojOpsDir.isDirectory()) {
            simiojOpsDir.mkdir();
        }
        try {
            File submitFile = File.createTempFile(String.format("mb%d-", op.getMailboxId()), ".log", simiojOpsDir);
            op.start(System.currentTimeMillis());
            try (FileOutputStream fos = new FileOutputStream(submitFile)) {
                IOUtils.copy(op.getInputStream(), fos);
                fos.flush();
            }
            return submitFile.getPath();
        } catch (IOException e) {
            ZimbraLog.mailbox.info("Problem submitting operation to the RAFT op=%s", op, e);
            throw ServiceException.FAILURE("Problem committing operation to cluster", e);
        }
    }

    /**
     * Demo version.  Nothing like the real version.
     * Expecting that in the real version, one argument would be the RAFT log entry index.
     * This method would only complete when Simioj has both committed that to the RAFT and persisted it to
     * the DB on the leader.  The return object would be whatever is appropriate for providing info on
     * the log entry.  For instance, for a CreateFolder operation, it might either contain a Folder object or
     * a cut down version of the Folder object containing necessary information like newly created UUIDs.
     * @param raftLogEntryIndex - currently name of the file containing a redo log entry
     */
    public RedoableOp waitForCommit(String raftLogEntryIndex) throws ServiceException {
        File opFile = new File(raftLogEntryIndex);
        RedoableOp op = null;
        try (FileInputStream fis = new FileInputStream(opFile)) {
            RedoLogInput rli = new RedoLogInput(fis);
            op = RedoableOp.deserializeOp(rli);
            op.opType = RedoableOp.OP_TYPE.SIMIOJ_LEADER;
            return op;
        } catch (IOException e) {
            ZimbraLog.mailbox.info("Problem retrieving from the RAFT op=%s", op, e);
            throw ServiceException.FAILURE("Problem committing operation  to cluster- stage 2", e);
        }
    }
}
