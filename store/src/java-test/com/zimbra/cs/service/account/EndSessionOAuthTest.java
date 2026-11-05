

/*

***** BEGIN LICENSE BLOCK *****

Zimbra Collaboration Suite Server

Copyright (C) 2026 Synacor, Inc.



This program is free software: you can redistribute it and/or modify

it under the terms of the GNU General Public License as published by

the Free Software Foundation, version 2 of the License.



This program is distributed in the hope that it will be useful, but

WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY

or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License

for more details.

***** END LICENSE BLOCK *****
*/
package com.zimbra.cs.service.account;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.objenesis.ObjenesisStd;
import com.zimbra.common.soap.OAuthConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.soap.SoapServlet;
public class EndSessionOAuthTest {
    private final EndSession endSession = new EndSession();

}