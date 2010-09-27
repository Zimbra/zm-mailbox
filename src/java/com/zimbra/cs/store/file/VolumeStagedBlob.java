/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.store.file;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.StagedBlob;

public class VolumeStagedBlob extends StagedBlob {
    private VolumeBlob mLocalBlob;
    private boolean mWasStagedDirectly;

    VolumeStagedBlob(Mailbox mbox, VolumeBlob blob) throws IOException {
        super(mbox, blob.getDigest(), blob.getRawSize());
        mLocalBlob = blob;
    }

    public VolumeBlob getLocalBlob() {
        return mLocalBlob;
    }

    @Override public String getLocator() {
        return Short.toString(mLocalBlob.getVolumeId());
    }

    VolumeStagedBlob markStagedDirectly() {
        mWasStagedDirectly = true;
        return this;
    }

    boolean wasStagedDirectly() {
        return mWasStagedDirectly;
    }
}
