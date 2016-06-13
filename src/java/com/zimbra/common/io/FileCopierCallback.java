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

public interface FileCopierCallback {

    /**
     * Callback that is called when a file operation begins.
     * 
     * If this method returns true, the file operation is allowed to run and
     * the fileCopierCallbackEnd() callback is guaranteed to be called later.
     * 
     * If this method returns false, the file operation is rejected and the
     * fileCopierCallbackEnd() callback is guaranteed not to be called.
     * 
     * (These guarantees are actually made by FileCopier implementations.)
     * 
     * @param cbarg
     * @return true if operation may proceed; false if operation should not
     *         proceed, usually due to an error in earlier operation
     */
    public boolean fileCopierCallbackBegin(Object cbarg);

    /**
     * Callback that is called when a file operation completes
     * @param cbarg
     * @param err null if successful; non-null if there was an error
     */
    public void fileCopierCallbackEnd(Object cbarg, Throwable err);
}
