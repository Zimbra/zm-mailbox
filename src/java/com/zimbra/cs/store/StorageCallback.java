/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.store;

import java.io.IOException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

public abstract class StorageCallback {

    private static Integer sDiskStreamingThreshold;

    public static int getDiskStreamingThreshold() throws ServiceException {
        if (sDiskStreamingThreshold == null)
            loadSettings();
        return sDiskStreamingThreshold;
    }

    public static void loadSettings() throws ServiceException {
        Server server = Provisioning.getInstance().getLocalServer(); 
        sDiskStreamingThreshold = server.getMailDiskStreamingThreshold();
    }


    public void wrote(Blob blob, byte[] data, int numBytes) throws IOException {
        wrote(blob, data, 0, numBytes);
    }

    public abstract void wrote(Blob blob, byte[] data, int offset, int numBytes) throws IOException;

}
