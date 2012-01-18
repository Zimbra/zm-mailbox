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
package com.zimbra.cs.store.http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;

import com.google.common.collect.Maps;
import com.zimbra.common.util.ByteUtil;

public class MockHttpStore {
    static final int PORT = 7678;
    static final String URL_PREFIX = "http://localhost:" + PORT + "/store/";

    private static ServerSocket ssock;
    private static Map<String, byte[]> blobs = Maps.newHashMap();

    static void startup() throws IOException {
        final ServerSocket s = ssock = new ServerSocket(PORT);
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        handle(s.accept());
                    } catch (IOException e) {
                        break;
                    }
                }
            }
        }.start();
    }

    static void shutdown() throws IOException {
        ssock.close();
        ssock = null;

        purge();
    }

    static void purge() {
        blobs.clear();
    }

    static int size() {
        return blobs.size();
    }

    static void handle(Socket socket) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new BufferedInputStream(socket.getInputStream());
            out = socket.getOutputStream();

            StringBuilder reqline = new StringBuilder();
            int b;
            while ((b = in.read()) != -1 && b != '\n') {
                reqline.append((char) b);
            }
            String[] reqparts = reqline.toString().trim().split(" ");

            if (reqparts.length != 3 || !reqparts[2].startsWith("HTTP/")) {
                out.write("HTTP/1.0 400 malformed request-line\r\n\r\n".getBytes());
            } else {
                String method = reqparts[0].toUpperCase(), filename = getFilename(reqparts[1]), version = reqparts[2];
                try {
                    InternetHeaders headers = new InternetHeaders(in);

                    if (method.equals("GET")) {
                        doGet(version, filename, out);
                    } else if (method.equals("POST") || method.equals("PUT")) {
                        doPost(version, headers, in, out);
                    } else if (method.equals("DELETE")) {
                        doDelete(version, filename, out);
                    } else {
                        out.write((version + " 400 unknown method: " + reqparts[0] + "\r\n\r\n").getBytes());
                    }
                } catch (MessagingException e) {
                    out.write((version + " 500 Internal Server Error\r\n\r\n").getBytes());
                    return;
                }
            }
            out.flush();
        } finally {
            ByteUtil.closeStream(in);
            ByteUtil.closeStream(out);
            socket.close();
        }
    }

    private static void doGet(String httpversion, String filename, OutputStream out) throws IOException {
        // send the content
        byte[] content = blobs.get(filename);
        if (content == null) {
            out.write((httpversion + " 404 Not Found\r\n\r\n").getBytes());
        } else {
            out.write((httpversion + " 200 OK content follows\r\n").getBytes());
            out.write(("Content-Length: " + content.length + "\r\n\r\n").getBytes());
            out.write(content);
        }
    }

    private static void doPost(String httpversion, InternetHeaders headers, InputStream in, OutputStream out)
    throws IOException {
        String clen = headers.getHeader("Content-Length", null);
        int length = clen == null ? -1 : Integer.parseInt(clen);

        String filename = UUID.randomUUID().toString();

        blobs.put(filename, ByteUtil.readInput(in, length, length));
        out.write((httpversion + " 201 Created\r\n").getBytes());
        out.write(("Location: " + URL_PREFIX + filename + "\r\n\r\n").getBytes());
    }

    private static void doDelete(String httpversion, String filename, OutputStream out) throws IOException {
        blobs.remove(filename);
        out.write((httpversion + " 204 No Content\r\n\r\n").getBytes());
    }

    private static String getFilename(String uri) {
        String[] segments = uri.split("/");
        return segments[segments.length - 1];
    }
}
