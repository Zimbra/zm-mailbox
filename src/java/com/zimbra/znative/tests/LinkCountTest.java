/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.znative.tests;

import java.io.IOException;

import com.zimbra.znative.IO;

public class LinkCountTest {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Error: no arguments specified");
            return;
        }
        for (int i = 0; i < args.length; i++) {
            try {
                System.out.println(args[i] + ": " + IO.linkCount(args[i]));
            } catch (IOException ioe) {
                System.out.println(args[i] + ": " + ioe);
            }
        }
    }
}
