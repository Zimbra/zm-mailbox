/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.znative.Process;
import com.zimbra.znative.OperationFailedException;

public class ProcessTest {

    public static void main(String[] args) throws OperationFailedException {
        if (args.length < 1) {
            System.out.println("Error: no arguments specified");
            return;
        }
        
        String userName = args[0];
        int uid = Integer.parseInt(args[1]);
        int gid = Integer.parseInt(args[2]);
        String beforeFile = args[3];
        String afterFile = args[4];
        
        
        System.out.println("Before: ");
        System.out.println("  uid="  + Process.getuid());
        System.out.println("  euid=" + Process.geteuid());
        System.out.println("  gid="  + Process.getgid());
        System.out.println("  egid=" + Process.getegid());
        System.out.println("  creating " + beforeFile);
        try {
            new File(beforeFile).createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        Process.setPrivileges(userName, uid, gid);

        System.out.println("After: ");
        System.out.println("  uid="  + Process.getuid());
        System.out.println("  euid=" + Process.geteuid());
        System.out.println("  gid="  + Process.getgid());
        System.out.println("  egid=" + Process.getegid());
        System.out.println("  creating " + afterFile);
        try {
            new File(afterFile).createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
