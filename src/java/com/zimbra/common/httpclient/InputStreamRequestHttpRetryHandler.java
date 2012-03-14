/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.common.httpclient;

import java.io.IOException;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;


public class InputStreamRequestHttpRetryHandler extends DefaultHttpMethodRetryHandler {

    /**
     * Same as default, but returns false if method is an unbuffered input stream request 
     * This avoids HttpMethodDirector masking real IO exception with bogus 'Unbuffered content cannot be retried' exception
     */
    @Override
    public boolean retryMethod(HttpMethod method, IOException exception,
            int executionCount) {
        boolean canRetry = super.retryMethod(method, exception, executionCount);
        if (canRetry && method instanceof EntityEnclosingMethod) {
            RequestEntity reqEntity = ((EntityEnclosingMethod) method).getRequestEntity();
            if (reqEntity instanceof InputStreamRequestEntity) {
                return ((InputStreamRequestEntity) reqEntity).isRepeatable();
            }
        }
        return canRetry;
    }
}
