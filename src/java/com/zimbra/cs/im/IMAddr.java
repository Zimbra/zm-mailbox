package com.zimbra.cs.im;

public class IMAddr {
    private String mAddr;
    
    public IMAddr(String addr) {
        mAddr = addr;
    }
    
    public String getAddr() { return mAddr; }
    
    public String toString() { return mAddr; }
    
    public boolean equals(Object other) {
        return (((IMAddr)other).mAddr).equals(mAddr);
    }
    
    public int hashCode() {
        return mAddr.hashCode();
    }
}
