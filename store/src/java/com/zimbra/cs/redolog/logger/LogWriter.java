/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on 2004. 7. 22.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.redolog.logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;

import com.zimbra.cs.redolog.RedoLogManager.RedoOpContext;
import com.zimbra.cs.redolog.op.RedoableOp;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface LogWriter {

	/**
	 * Opens the log.
	 * @throws IOException
	 */
	public void open() throws IOException;

	/**
	 * Closes the log.
	 * @throws IOException
	 */
	public void close() throws IOException;

    public void log(RedoOpContext context, InputStream data, boolean synchronous) throws IOException;

	/**
	 * Logs an entry.
     * @param op entry being logged
     * @param data the data stream; must not be null;
     *             while it is possible to compute data from op, only what
     *             is passed in as data gets logged
	 * @param synchronous if true, method doesn't return until log entry
	 *                    has been written to disk safely, or has been
	 *                    securely stored in an equivalent manner depending
	 *                    on the logger implementation
	 * @throws IOException
	 */
	public void log(RedoableOp op, InputStream data, boolean synchronous) throws IOException;

    /**
     * Make sure all writes are committed to disk, or whatever the log
     * destination medium is.  This is mainly useful only when we need to
     * make sure the commit record is on disk, because fsync of commit record
     * is deferred until the logging of the next redo record for performance
     * reasons.
     * @throws IOException
     */
    public void flush() throws IOException;

	/**
	 * Returns the current size of the log.  Used for rollover tracking.
	 * @return
	 */
	public long getSize() throws IOException;

	/**
	 * Returns the time of the log creation.
	 * @return
	 */
	public long getCreateTime();

	/**
     * Returns the time of the last entry logged.
     * @return
     */
    public long getLastLogTime();

	/**
	 * Whether the current log is empty, i.e. has no entries logged.
	 * @return
	 * @throws IOException
	 */
	public boolean isEmpty() throws IOException;

	/**
	 * Whether the underlying logfile exists.
	 * @return
	 */
	public boolean exists();

	/**
	 * Returns the absolute pathname for the underlying logfile.
	 * @return
	 */
	public String getAbsolutePath();

	/**
	 * Renames the underlying logfile.
	 * @param dest
	 * @return true if and only if the renaming succeeded; false otherwise
	 */
	public boolean renameTo(File dest);

	/**
	 * Deletes the underlying logfile.  The logger should be closed first
	 * if open.
	 * @return true if and only if the deletion succeeded; false otherwise
	 */
	public boolean delete() throws IOException;

    /**
     * Performs log rollover.
     * @param activeOps map of pending transactions; these should be logged
     *                  at the beginning of new log file
     * @return java.io.File object for rolled over logfile
     * @throws IOException
     */
    public File rollover(LinkedHashMap /*<TxnId, RedoableOp>*/ activeOps)
    throws IOException;

    /**
     * Returns the sequence number of redolog.  Only file-based log writers
     * will return a meaningful number.  Others return 0.
     * @return
     */
    public long getSequence();
}
