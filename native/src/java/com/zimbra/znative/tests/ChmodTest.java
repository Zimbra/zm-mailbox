/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.IOException;
import com.zimbra.znative.IO;

public class ChmodTest {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Error: insufficient args");
            return;
        }
	long mode = 0;
	for (int i = 1; i < args.length; i++) {
	    String arg = args[i];
	    if ("rusr".equals(arg)) {
		mode |= IO.S_IRUSR;
	    } else if ("wusr".equals(arg)) {
		mode |= IO.S_IWUSR;
	    } else if ("xusr".equals(arg)) {
		mode |= IO.S_IXUSR;
	    } else if ("rgrp".equals(arg)) {
		mode |= IO.S_IRGRP;
	    } else if ("wgrp".equals(arg)) {
		mode |= IO.S_IWGRP;
	    } else if ("xgrp".equals(arg)) {
		mode |= IO.S_IXGRP;
	    } else if ("roth".equals(arg)) {
		mode |= IO.S_IROTH;
	    } else if ("woth".equals(arg)) {
		mode |= IO.S_IWOTH;
	    } else if ("xoth".equals(arg)) {
		mode |= IO.S_IXOTH;
	    } else if ("suid".equals(arg)) {
		mode |= IO.S_ISUID;
	    } else if ("sgid".equals(arg)) {
		mode |= IO.S_ISGID;
	    } else if ("svtx".equals(arg)) {
		mode |= IO.S_ISVTX;
	    } else {
		System.err.println("Error: Invalid argument: " + arg);
		return;
	    }
	}
	IO.chmod(args[0], mode);
    }
}
