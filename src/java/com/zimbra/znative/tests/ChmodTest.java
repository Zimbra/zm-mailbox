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
 * Portions created by Zimbra are Copyright (C) 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
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
