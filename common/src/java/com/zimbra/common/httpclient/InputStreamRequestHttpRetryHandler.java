/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016, 2018 Synacor, Inc.
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

package com.zimbra.common.httpclient;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;

public class InputStreamRequestHttpRetryHandler extends DefaultHttpRequestRetryHandler {

    @Override
    /**
     * Same as default, but returns false if method is an unbuffered input stream request 
     * This avoids HttpMethodDirector masking real IO exception with bogus 'Unbuffered content cannot be retried' exception
     */
    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {

        boolean canRetry = super.retryRequest(exception, executionCount, context);

        HttpClientContext clientContext = HttpClientContext.adapt(context);
        HttpRequest request = clientContext.getRequest();
        boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
        if (canRetry && idempotent) {
            HttpEntity reqEntity = ((HttpEntityEnclosingRequest) request).getEntity();
            if (reqEntity.isRepeatable()) {
                canRetry = true;
            }
        }
        return canRetry;

    }
}
