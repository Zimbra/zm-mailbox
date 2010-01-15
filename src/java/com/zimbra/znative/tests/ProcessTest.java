/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2010 Zimbra, Inc.
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
