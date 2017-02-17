/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
