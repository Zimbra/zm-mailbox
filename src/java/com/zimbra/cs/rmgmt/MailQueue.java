/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.rmgmt;

import java.io.InputStream;
import java.util.Map;

public class MailQueue {

	public static final String MQ_ID = "id";         // id=A4578828AC1
	public static final String MQ_TIME = "time"; 	 // time=1142475151
	public static final String MQ_SIZE = "size"; 	 // size=550
	public static final String MQ_SENDER = "sender"; //	sender=anandp@phillip.liquidsys.com
	public static final String MQ_CADDR = "caddr"; 	 // caddr=10.10.130.27
	public static final String MQ_CNAME = "cname"; 	 // cname=phillip.liquidsys.com
	public static final String MQ_REASON = "reason"; // reason=connect to 127.0.0.1[127.0.0.1]: Connection refused
	public static final String MQ_RECIP = "recip"; 	 // recip=admin@bolton.liquidsys.com,anandp@bolton.liquidsys.com

	private String mServerName;
	
	private String mQueueName;
	
	private boolean mParseCompleted = false;
	
	private StringBuilder mParseErrors = new StringBuilder();

	private class MailQueueVisitor implements SimpleMapsParser.Visitor {
		public void handle(int lineNo, Map<String, String> map) {
			String id = map.get(MQ_ID);
			if (id == null) {
				synchronized (mParseErrors) {
					mParseErrors.append("Found no " + MQ_ID + " attribute in input after line " + lineNo + "\n");
				}
				return;
			}
		}
	}
	
	public MailQueue(String serverName, String queueName, InputStream in) {
		mServerName = serverName;
		mQueueName = queueName;
		new Thread() {
			public void run() {
				
			}
		}.start();
	}
}
