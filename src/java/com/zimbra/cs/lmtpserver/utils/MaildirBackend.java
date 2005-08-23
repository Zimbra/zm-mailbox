/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

package com.zimbra.cs.lmtpserver.utils;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.lmtpserver.LmtpStatus;
import com.zimbra.cs.lmtpserver.LmtpAddress;
import com.zimbra.cs.lmtpserver.LmtpBackend;
import com.zimbra.cs.lmtpserver.LmtpEnvelope;

public class MaildirBackend implements LmtpBackend {

	private static Log mLog = LogFactory.getLog(MaildirBackend.class);
	
	private Map mUsers;
	
	public MaildirBackend(Map users) {
		mUsers = users;
	}
	
	public LmtpStatus getAddressStatus(LmtpAddress address) {
		if (mUsers.get(address.getEmailAddress().toLowerCase()) == null) {
			return LmtpStatus.REJECT;
		} else {
			return LmtpStatus.ACCEPT;
		}
	}

	private File chkdir(String base, String subdir) {
		File dir = new File(base + File.separator + subdir);
		if (dir.exists()) {
			if (!dir.isDirectory()) {
				mLog.warn("is not a directory: " + dir);
				return null;
			}
		} else {
			dir.mkdirs();
			if (!dir.exists() || !dir.isDirectory()) {
				mLog.warn("could not create directory: " + dir);
				return null;
			}
		}
		return dir;
	}

	private static Random mUniqueFileRandom = new Random();
	
	private static int mUniqueFileCounter = 0;
	
	private static File createUniqueFile(File dir) throws IOException {
		File newFile = null;
		do { 
			StringBuffer sb = new StringBuffer(64);
			sb.append(System.currentTimeMillis());
			int rand = mUniqueFileRandom.nextInt();
			if (rand >= 0) {
				sb.append("-");
			}
			sb.append(rand);
			sb.append("-");
			sb.append(++mUniqueFileCounter);
			newFile = new File(dir + File.separator + sb.toString());
		} while (!newFile.createNewFile());
		return newFile;
	}

	public void deliver(LmtpEnvelope env, byte[] data) {
		List recipients = env.getRecipients();
		for (Iterator iter = recipients.iterator(); iter.hasNext();) {
			LmtpAddress recipient = (LmtpAddress)iter.next();
			String key = recipient.getEmailAddress().toLowerCase();
			String base = (String)mUsers.get(key);

			if (base == null) {
				mLog.warn("did not find directory for user: " + key);
				recipient.setDeliveryStatus(LmtpStatus.REJECT);
				continue;
			}
			
			File dnew = chkdir(base, "new");
			File dtmp = chkdir(base, "tmp");
			if (dnew == null || dtmp == null) {
				recipient.setDeliveryStatus(LmtpStatus.TRYAGAIN);
				continue;
			}
			
			try {
				File ftmp = createUniqueFile(dtmp);
				FileOutputStream os = new FileOutputStream(ftmp);
				os.write(data);
				os.close();
				File fnew = new File(dnew + File.separator + ftmp.getName());
				ftmp.renameTo(fnew);
			} catch (IOException ioe) {
				recipient.setDeliveryStatus(LmtpStatus.TRYAGAIN);
				continue;
			}

			recipient.setDeliveryStatus(LmtpStatus.ACCEPT);
		}
	}
}
