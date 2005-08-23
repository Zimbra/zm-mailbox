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

package com.zimbra.cs.lmtpserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.zimbra.cs.tcpserver.TcpServerInputStream;

public class LmtpInputStream extends TcpServerInputStream {
	public LmtpInputStream(InputStream is) {
		super(is);
	}
	
	private static final int[] EOM = new int[] { CR, LF, (int)'.', CR, LF };
	private static final int EOMLEN = EOM.length;
	
	//private static Log mLog = LogFactory.getLog(LmtpInputStream.class);
	
	public byte[] readMessage(int sizeHint) throws IOException {
		int matched = -1;
		
		if (sizeHint == 0) {
			sizeHint = 8192;
		}

		ByteArrayOutputStream bos  = new ByteArrayOutputStream(sizeHint);
		while (true) {
			int ch = read();

			if (ch == -1) {
				throw new IOException("premature end of input reading message data");
			}
			
			if (ch == EOM[matched + 1]) {
				matched++;
				if (matched == (EOMLEN-1)) {
					return bos.toByteArray();
				} else {
					continue; // match more characters
				}
			}
			
			if (matched > -1) {
				// Flush sequence that looked like EOM but wasn't. 
				for (int i = 0; i <= matched; i++) {
					bos.write((char)EOM[i]);
				}

				// Reset match counter.
				matched = -1;
				
				// We might be at the beginning of EOM.
				if (ch == EOM[matched + 1]) {
					matched++;
					continue;
				}
			}

			bos.write(ch);
		}
	}
}
