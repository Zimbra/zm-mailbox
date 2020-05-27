package com.zimbra.cs.redolog.op;

import com.zimbra.cs.redolog.RedoLogBlobStore.PendingRedoBlobOperation;

public interface BlobRecorder {

    public String getBlobDigest();

    public void setRedoBlobOperation(PendingRedoBlobOperation op);

}
