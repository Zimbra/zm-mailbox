/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.account.ldap.upgrade;

import java.io.PrintStream;
import java.io.PrintWriter;

import com.zimbra.cs.account.Entry;

class LdapUpgradePrinter {

    private PrintStream printer = System.out;
    
    void print(String str) {
        printer.print(str);
    }
    
    void println() {
        printer.println();
    }
    
    void println(String str) {
        printer.println(str);
    }
    
    void format(String format, Object ... objects) {
        printer.format(format, objects);
    }
    
    PrintWriter getPrintWriter() {
        return new PrintWriter(printer, true);
    }
    
    void printStackTrace(Throwable e) {
        e.printStackTrace(getPrintWriter());
    }
    
    void printStackTrace(String str, Throwable e) {
        println(str);
        e.printStackTrace(getPrintWriter());
    }
    
    void printCheckingEntry(Entry entry) {
        printer.println("\nChecking " + entry.getEntryType().getName() + " entry " + entry.getLabel());
    }
}
