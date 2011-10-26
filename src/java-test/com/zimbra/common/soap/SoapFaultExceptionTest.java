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

package com.zimbra.common.soap;

import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.service.ServiceException.Argument;

public class SoapFaultExceptionTest {

    @SuppressWarnings("serial")
    private class TestException extends ServiceException {
        TestException(String msg, String code, List<Argument> args) {
            super(msg, code, true, null, args);
        }
    };
    
    /**
     * Confirms that SOAP fault arguments are marshalled to and from elements.
     */
    @Test
    public void soapFaultArgs() throws Exception {
        // Create original exception.
        String msg = "Something bad happened.";
        Argument howBad = new Argument("howBad", "Really bad", Argument.Type.STR);
        Argument howManyTimes = new Argument("howManyTimes", "2", Argument.Type.NUM);
        String accountId = UUID.randomUUID().toString();
        String itemId = accountId + ":123";
        Argument itemIdArg = new Argument("itemId", itemId, Argument.Type.IID);
        Argument accountIdArg = new Argument("accountId", accountId, Argument.Type.ACCTID);
        String code = "mail:SOMETHING_BAD";
        List<Argument> args = Lists.newArrayList(howBad, howManyTimes, itemIdArg, accountIdArg); 
        TestException te = new TestException(msg, code, args);
        
        // Marshal and unmarshal to a SOAP 1.2 fault.
        Element fault = SoapProtocol.Soap12.soapFault(te);
        Element detail = fault.getElement(Soap12Protocol.DETAIL);
        SoapFaultException sfe = new SoapFaultException(msg, detail, false, fault);
        
        // Compare.
        Assert.assertEquals(msg, sfe.getMessage());
        Assert.assertEquals(code, sfe.getCode());
        Assert.assertEquals(args, sfe.getArgs());

        // Marshal and unmarshal to a SOAP 1.1 fault.
        fault = SoapProtocol.Soap11.soapFault(te);
        detail = fault.getElement(Soap11Protocol.DETAIL);
        sfe = new SoapFaultException(msg, detail, false, fault); 
        
        // Compare.
        Assert.assertEquals(msg, sfe.getMessage());
        Assert.assertEquals(code, sfe.getCode());
        Assert.assertEquals(args, sfe.getArgs());

        // Marshal and unmarshal to a SOAP JS fault.
        fault = SoapProtocol.SoapJS.soapFault(te);
        detail = fault.getElement(SoapJSProtocol.DETAIL);
        sfe = new SoapFaultException(msg, detail, false, fault); 
        
        // Compare.
        Assert.assertEquals(msg, sfe.getMessage());
        Assert.assertEquals(code, sfe.getCode());
        Assert.assertEquals(args, sfe.getArgs());
    }
}
