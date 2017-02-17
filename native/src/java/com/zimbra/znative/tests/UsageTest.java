/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.Arrays;

import com.zimbra.znative.ProcessorUsage;
import com.zimbra.znative.ResourceUsage;

public class UsageTest {
    public static void main(String[] args) {
        new Thread() {
            public void run() {
                while (true) {
                    byte[] ba = new byte[1024];
                    Arrays.fill(ba, (byte)'A');
                    try {
                        sleep(10);
                        System.err.write(ba);
                    } catch (Exception e) {
                        e.printStackTrace(System.out);
                        System.out.flush();
                    }
                }
            }
        }.start();

        try {
            ProcessorUsage then = ProcessorUsage.getProcessorUsage();
            while (true) {
                Thread.sleep(5000);
                System.out.println(ResourceUsage.getResourceUsage(ResourceUsage.TYPE_SELF).toString());
                ProcessorUsage now = ProcessorUsage.getProcessorUsage();
                System.out.println(ProcessorUsage.usageInTicks(now, then));
                System.out.println(ProcessorUsage.usageInMillis(now, then));
                System.out.println();
                then = now;
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.out.flush();
        }
    }
}
