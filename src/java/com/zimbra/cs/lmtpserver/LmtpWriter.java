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

package com.zimbra.cs.lmtpserver;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

public class LmtpWriter extends PrintWriter {

    private static final String lineSeparator = "\r\n";

    public LmtpWriter(Writer out) {
        super(out, false);
    }

    public LmtpWriter(OutputStream out) {
        super(out, false);
    }

    public void println () {
        synchronized (lock) {
            write(lineSeparator);
        }
    }

    public void println(boolean x) {
        synchronized (lock) {
            print(x);
            println();
        }
    }

    public void println(char x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    public void println (int x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    public void println (long x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    public void println (float x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    public void println (double x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    public void println (char[] x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    public void println (String x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }

    public void println (Object x) {
        synchronized (lock) {
            print (x);
            println ();
        }
    }
}
