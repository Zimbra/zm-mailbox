/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
