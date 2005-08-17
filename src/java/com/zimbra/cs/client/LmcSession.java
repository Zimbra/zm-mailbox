package com.zimbra.cs.client;

/**
 * Encapsulate the notion of a session, including auth token, session ID, and
 * whatever else is desired...
 */
public class LmcSession {

	private String mAuthToken;

	private String mSessionID;

	public String getAuthToken() {
		return mAuthToken;
	}

	public String getSessionID() {
		return mSessionID;
	}

	public void setAuthToken(String a) {
		mAuthToken = a;
	}

	public void setSessionID(String s) {
		mSessionID = s;
	}

	public LmcSession(String authToken, String sessionID) {
		mAuthToken = authToken;
		mSessionID = sessionID;
	}
}