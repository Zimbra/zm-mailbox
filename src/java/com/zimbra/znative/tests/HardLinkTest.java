/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.znative.tests;

import java.io.File;
import java.io.IOException;

import com.zimbra.znative.IO;

public class HardLinkTest {

	private static void testArgCheck(String a1, String a2) {
		boolean passed = false;
		try {
			IO.link(a1, a2);
		} catch (NullPointerException npe) {
			passed = true;
		} catch (IllegalArgumentException iae) {
			passed = true;
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		System.out.println((passed ? "PASS: " : "FAIL") + a1 + ", " + a2);
	}
	
	private static void testArg() {
		testArgCheck(null, "haha");
		testArgCheck("haha", null);
		testArgCheck(null, null);
		
		testArgCheck("", "haha");
		testArgCheck("haha", "");
		testArgCheck("", "");
	}
	
	/* [0] = loop count
	 * [1] = file to link to
	 * [2] = directory in which to create linkss
	 */
	
	private static void testLoop(String[] args) {
		if (args.length != 3) {
			System.out.println("ERROR: args for loop test are { loop count, oldpath, linkdir } ");
			return;
		}
		int n = Integer.valueOf(args[0]).intValue();
		String oldpath = args[1];
		String outdir = args[2];
		
		int fname = 0;
		for (int i = 0; i < n; i++) {
			try {
				String fstr = new Integer(fname).toString();
				IO.link(oldpath, new File(outdir, fstr).getPath());
				fname++;
				if ((fname % 30000) == 0) {
					for (int x = 0; x < fname; x++) {
						String xstr = new Integer(x).toString();
						new File(outdir, xstr).delete();
					}
				}
			} catch (IOException ioe) {
				System.out.println("FAIL: in loop test");
				ioe.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		testArg();
		testLoop(args);
	}
}
