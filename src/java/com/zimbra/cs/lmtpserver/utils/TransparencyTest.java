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

package com.zimbra.cs.lmtpserver.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import com.zimbra.cs.lmtpserver.LmtpInputStream;

class TransparencyTest {
	
	private static void test(String input, String expectedOutput) {
		ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
		LmtpInputStream lin = new LmtpInputStream(in);
		byte[] readBytes = null;
		
		System.out.println("--- INPUT --------------------------------------------------------------");
		System.out.println(input.replace("\r", "\\r").replace("\n", "\\n"));
		System.out.println("--- EXPECTED");
		System.out.println(expectedOutput == null ? "<exception>" : expectedOutput.replace("\r", "\\r").replace("\n", "\\n"));
		
		try {
			readBytes = lin.readMessage(input.length());
		} catch (IOException ioe) {
			if (expectedOutput == null) {
				System.out.println("--- RESULT: PASS EXPECTED: " + ioe.getMessage());
				//ioe.printStackTrace(System.out);
			} else {
				System.out.println("--- RESULT: FAIL UNEXPECTED: " + ioe.getMessage());
				//ioe.printStackTrace(System.out);
			}
			return;
		}
		
		String readString = new String(readBytes);
		if (readString.equals(expectedOutput)) {
			System.out.println("--- RESULT: PASS");
		} else {
			System.out.println("--- RESULT: FAIL");
			System.out.println("--- GOT");
			System.out.println(readString.replace("\r", "\\r").replace("\n", "\\n"));
		}
	}

	private static final String EOM = "\r\n.\r\n";

	private static void testSimple(String input) {
		test(input + EOM, input);
	}
	
	
	public static void main(String[] args) {
		// Exception cases
		test("abcd\r\n", null);
		test("abcd\r\n.\r", null);
		
		// Simple input
		testSimple("ab\r\ncd");
		
		// Transparency
		test(".\r\n", "");
		test(".\rabcd" + EOM, "\rabcd");
		test(".a" + EOM, "a");
		test(".a" + EOM, "a");
		test("a\r\n.a" + EOM, "a\r\na");
		test(".\r\n", "");
	}
}
