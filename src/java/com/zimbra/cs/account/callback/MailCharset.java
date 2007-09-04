/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.callback;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
 
public class MailCharset extends AttributeCallback {

    /**
     * check to make sure zimbraPrefMailSignature is shorter than the limit
     */
    public void preModify(Map context, String attrName, Object value,
            Map attrsToModify, Entry entry, boolean isCreate) throws ServiceException {

        if (!(value instanceof String))
            throw ServiceException.INVALID_REQUEST(Provisioning.A_zimbraPrefMailDefaultCharset + " is a single-valued attribute", null);
        
        if (!((entry instanceof Account)||(entry instanceof Cos))) 
            return;
        
        String charset = (String)value;
        try {
            Charset.forName(charset);
        } catch (IllegalCharsetNameException e) {
            throw ServiceException.INVALID_REQUEST("charset name " + charset + " is illegal", e);
        } catch (UnsupportedCharsetException e) {
            throw ServiceException.INVALID_REQUEST("no support for charset " + charset + " is available in this instance of the Java virtual machine", e);
        }
    }

    /**
     * need to keep track in context on whether or not we have been called yet, only 
     * reset info once
     */

    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {

    }
}
