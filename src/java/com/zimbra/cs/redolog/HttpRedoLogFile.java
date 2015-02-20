/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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
package com.zimbra.cs.redolog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;

public class HttpRedoLogFile implements RedoLogFile {

    private String name;
    private long length = -1;
    private long seq = -1;

    private HttpRedoLogFile() {
        super();
    }

    public HttpRedoLogFile(RedoLogFile logFile) {
        super();
        this.name = logFile.getName();
        this.length = logFile.getLength();
        this.seq = logFile.getSeq();

    }

    //callers be sure to release the returned connection
    private GetMethod downloadFile() throws IOException {
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        GetMethod get = new GetMethod(HttpRedoLogManager.URL);
        get.setQueryString("fmt=file&name="+name+"&seq="+seq);
        int code = client.executeMethod(get);
        if (code != HttpServletResponse.SC_OK) {
            get.releaseConnection();
            throw new IOException("non-200 response getting redolog file ["+code+"] response:"+get.getResponseBodyAsString());
        }
        return get;
    }

    @Override
    public void copyToDir(File targetDir) throws IOException {
        GetMethod get = downloadFile();
        try {
            FileOutputStream fos = new FileOutputStream(targetDir + "/" + name);
            ByteUtil.copy(get.getResponseBodyAsStream(), true, fos, true);
        } finally {
            get.releaseConnection();
        }
    }

    @Override
    public File getFile() throws IOException {
        GetMethod get = downloadFile();
        try {
            //TODO: need a better tmp file caching mechanism here; cleanup on exit is not sufficient for real world
            File logFile = File.createTempFile("httpredolog", ".tmp");
            FileOutputStream fos = new FileOutputStream(logFile);
            ByteUtil.copy(get.getResponseBodyAsStream(), true, fos, true);
            return logFile;
        } finally {
            get.releaseConnection();
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        GetMethod get = downloadFile();
        return get.getResponseBodyAsStream();
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public long getSeq() {
        return seq;
    }

    @Override
    public String getName() {
        return name;
    }

    public String encodeToString() {
        return name + "," + length + "," + seq;
    }

    public static HttpRedoLogFile decodeFromString(String encoded) {
        HttpRedoLogFile redoLog = new HttpRedoLogFile();
        String[] parts = encoded.split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException(encoded +" is not a valid httpredolog identifier");
        }
        redoLog.name = parts[0];
        redoLog.length = Long.valueOf(parts[1]);
        redoLog.seq = Long.valueOf(parts[2]);
        return redoLog;
    }

}
