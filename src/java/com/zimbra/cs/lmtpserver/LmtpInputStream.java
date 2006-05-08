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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
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

	public byte[] readMessage(int sizeHint) throws IOException {
		// We start our state as though \r\n was already matched - so if
		// the first line is ".\r\n" we recognize that as end of message.
		int matched = 1;
		boolean initialPhantomMatch = true;
		
		if (sizeHint == 0) {
			sizeHint = 8192;
		}

		ByteArrayOutputStream bos  = new ByteArrayOutputStream(sizeHint);
		while (true) {
			int ch = read();

			if (ch == -1) {
				throw new IOException("EOF when looking for <CR><LF>.<CR><LF>");
			}
			
			if (ch == EOM[matched + 1]) {
				matched++;
				if (matched == (EOMLEN-1)) {
                    // see bug 6326 and RFC 2821 section 4.1.1.4 for why we need
                    // to preserve the CRLF that was part of the <CRLF>.<CRLF>
                    bos.write(CR);
                    bos.write(LF);
					return bos.toByteArray();
				} else {
					continue; // match more characters
				}
			}
			
			// Flush sequence that started looking like EOM, but wasn't.
			if (matched > -1) {
				
				int flushFrom = 0;
				if (initialPhantomMatch) {
					initialPhantomMatch = false;
					flushFrom = 2;
				}

				for (int i = flushFrom; i <= matched; i++) {
					if (i == 2) {
						// We encountered "\r\n." but it did not lead to EOM.
						// Swallow "." so we end up removing SMTP transparency.
						continue;
					}
					bos.write((char)EOM[i]);
				}
				
				// Reset match counter.
				matched = -1;
				
				// We might be at the beginning of EOM.
				if (ch == EOM[0]) {
					matched++;
					continue;
				}
			}

			bos.write(ch);
		}
	}
}
