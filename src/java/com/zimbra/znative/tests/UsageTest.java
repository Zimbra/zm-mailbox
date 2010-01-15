/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
