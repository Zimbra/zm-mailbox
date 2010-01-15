/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
