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

package com.zimbra.cs.redolog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface RedoLogFile {
    /**
     * Copy the contents of this file reference to a physical file
     * @param target directory
     * @throws IOException
     */
    public void copyToDir(File targetDir) throws IOException;

    /**
     * Get a reference to the data. Use is discouraged, but exists for legacy usage
     * @return redolog file. This is a read only view; do not attempt to write.
     * @throws IOException
     * @deprecated - use of getInputStream is preferred when possible
     */
    @Deprecated
    public File getFile() throws IOException;

    /**
     * Return an input stream to the content in this file.
     * @return
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException;

    /**
     * length of the file/stream
     * @return long, or -1 if unknown
     */
    public long getLength();

    /**
     * log sequence number
     * @return
     */
    public long getSeq();

    /**
     * get the unique filename/identifier for this file
     * @return
     */
    public String getName();
}
