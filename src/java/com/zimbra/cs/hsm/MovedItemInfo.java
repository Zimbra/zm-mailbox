package com.zimbra.cs.hsm;

public class MovedItemInfo {
    
    private int mId; 
    private short mVolumeId;
    private int mRevision;
    private String mBlobDigest;
    
    MovedItemInfo(int id, short volumeId, int revision, String blobDigest) {
        mId = id;
        mVolumeId = volumeId;
        mRevision = revision;
        mBlobDigest = blobDigest;
    }
    
    public int getId() {
        return mId;
    }
    
    public short getVolumeId() {
        return mVolumeId;
    }
    
    public int getRevision() {
        return mRevision;
    }
    
    public String getBlobDigest() {
        return mBlobDigest;
    }
}
