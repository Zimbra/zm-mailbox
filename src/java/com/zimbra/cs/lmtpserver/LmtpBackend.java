package com.zimbra.cs.lmtpserver;

public interface LmtpBackend {
	
	/**
	 * Get account status.
	 */
	public LmtpStatus getAddressStatus(LmtpAddress address);

	/**
	 * Deliver this message to the list of recipients in the message, and set the 
	 * delivery status on each of those recipeint addresses. 
	 */
	public void deliver(LmtpEnvelope env, byte[] data);
}