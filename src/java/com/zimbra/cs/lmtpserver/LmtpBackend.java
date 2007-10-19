/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.lmtpserver;

import java.io.InputStream;

public interface LmtpBackend {
	
	/**
	 * Gets account status.
	 */
	public LmtpStatus getAddressStatus(LmtpAddress address);

	/**
	 * Delivers this message to the list of recipients in the message, and sets the 
	 * delivery status on each recipient address. 
	 */
	public void deliver(LmtpEnvelope env, InputStream in, int sizeHint);
}