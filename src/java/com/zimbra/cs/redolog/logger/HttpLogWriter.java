/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.redolog.logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.http.HttpStatus;

import com.zimbra.cs.redolog.op.RedoableOp;

public class HttpLogWriter implements LogWriter {

    @Override
    public void open() throws IOException {
        //create db connection and populate initial vars
    }

    @Override
    public void close() throws IOException {
        //close db connection
    }

    @Override
    public void log(RedoableOp op, InputStream data, boolean synchronous)
            throws IOException {
        //TODO: if synchronous...

        HttpClient client = new HttpClient();
        //TODO: dynamic host/port...
        PostMethod post = new PostMethod("http://localhost:8080/redolog");
        post.setRequestEntity(new InputStreamRequestEntity(data));
        int code = client.executeMethod(post);
        if (code != HttpStatus.SC_OK) {
            throw new IOException("non-OK response from redolog servlet [" + code + "] message:[" + post.getResponseBodyAsString() + "]");
        }
    }

    @Override
    public void flush() throws IOException {
        //maybe do nothing; maybe add buffering...
    }

    @Override
    public long getSize() {
        //size since rollover
        return 0;
    }

    @Override
    public long getCreateTime() {
        //timestamp of start or last rollover (first op?)
        return 0;
    }

    @Override
    public long getLastLogTime() {
        //last op
        return 0;
    }

    @Override
    public boolean isEmpty() throws IOException {
        //count > 0
        return false;
    }

    @Override
    public boolean exists() {
        //similar to isEmpty?
        return false;
    }

    @Override
    public boolean delete() {
        //delete since last rollover mark?
        return false;
    }

    @Override
    public void rollover(LinkedHashMap activeOps) throws IOException {
        //add a rollover mark, reset counters, perhaps increment sequence (or does that happen externally)
    }

    @Override
    public long getSequence() {
        //current sequence of rollover set?
        return 0;
    }

}
