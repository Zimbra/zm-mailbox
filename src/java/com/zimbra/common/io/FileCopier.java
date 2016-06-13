/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.common.io;
import java.io.File;
import java.io.IOException;

public interface FileCopier {

    public boolean isAsync();

    public void start() throws IOException;

    public void shutdown() throws IOException;

    public void setIgnoreMissingSource(boolean ignore);

    /**
     * Copy src to dest.
     * @param src
     * @param dest
     * @param cb
     * @param cbarg
     * @throws IOException
     */
    public void copy(File src, File dest,
                     FileCopierCallback cb, Object cbarg)
    throws IOException;

    /**
     * Copy src to dest and make dest read-only.
     * @param src
     * @param dest
     * @param cb
     * @param cbarg
     * @throws IOException
     */
    public void copyReadOnly(File src, File dest,
                             FileCopierCallback cb, Object cbarg)
    throws IOException;

    /**
     * Create a link to a file.  Only works within the same filesystem.
     * @param file existing file
     * @param link new link to be created
     * @param cb
     * @param cbarg
     * @throws IOException
     */
    public void link(File file, File link,
                     FileCopierCallback cb, Object cbarg)
    throws IOException;

    /**
     * Rename oldPath to newPath.  Same caveats as java.io.File.renameTo().
     * @param oldPath
     * @param newPath
     * @param cb
     * @param cbarg
     * @throws IOException
     */
    public void move(File oldPath, File newPath,
                     FileCopierCallback cb, Object cbarg)
    throws IOException;

    /**
     * Delete a file.  Same caveats as java.io.File.delete().
     * @param file
     * @param cb
     * @param cbarg
     * @throws IOException
     */
    public void delete(File file,
                       FileCopierCallback cb, Object cbarg)
    throws IOException;
}
