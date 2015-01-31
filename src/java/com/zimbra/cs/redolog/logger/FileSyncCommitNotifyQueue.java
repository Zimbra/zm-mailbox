/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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
package com.zimbra.cs.redolog.logger;

import java.io.IOException;

public class FileSyncCommitNotifyQueue extends CommitNotifyQueue {

    private FileLogWriter logWriter;

    public FileSyncCommitNotifyQueue(int size, FileLogWriter logWriter) {
        super(size);
        this.logWriter = logWriter;
    }

    @Override
    public synchronized void flush() throws IOException {
        flushWithOptionalSync(true);
    }

    public synchronized void flushWithOptionalSync(boolean fsync) throws IOException {
        if (fsync) {
            logWriter.fsync();
        }
        super.flush();
    }
}
