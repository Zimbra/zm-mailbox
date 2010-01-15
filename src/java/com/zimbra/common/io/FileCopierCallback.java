/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2010 Zimbra, Inc.
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
