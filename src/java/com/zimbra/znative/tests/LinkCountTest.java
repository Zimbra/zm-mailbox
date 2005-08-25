/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
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
