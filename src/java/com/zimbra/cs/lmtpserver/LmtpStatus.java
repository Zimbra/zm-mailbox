package com.zimbra.cs.lmtpserver;


public final class LmtpStatus {
	private String mStatus;
	
	private LmtpStatus(String status) {
		mStatus = status;
	} 

	public static final LmtpStatus ACCEPT = new LmtpStatus("ACCEPT");
	public static final LmtpStatus REJECT = new LmtpStatus("REJECT");
	public static final LmtpStatus TRYAGAIN = new LmtpStatus("TRYAGAIN");
	public static final LmtpStatus OVERQUOTA = new LmtpStatus("OVERQUOTA");
    
	public String toString() {
		return mStatus;
	}
}
