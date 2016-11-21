/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.cs.mime;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.Account;

public abstract class CertificateProcessor {

    private static CertificateProcessor processorInstance = null;

    public static void registerProcessor(CertificateProcessor processor) {
        processorInstance = processor;
    }

    public static CertificateProcessor getProcessor() {
        return processorInstance;
    }

    public abstract void process(Account account, Element elem, String certData, 
        SoapProtocol mResponseProtocol);

}
