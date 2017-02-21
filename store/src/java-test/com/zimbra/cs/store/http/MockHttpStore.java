/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.store.http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;

import com.google.common.collect.Maps;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ByteUtil;

public class MockHttpStore {
    static final int PORT = 7678;
    static final String URL_PREFIX = "http://localhost:" + PORT + "/store/";

    private static ServerSocket ssock;
    private static Map<String, byte[]> blobs = Maps.newHashMap();

    //for mocking error conditions
    private static AtomicBoolean fail = new AtomicBoolean(false);
    private static AtomicBoolean delay = new AtomicBoolean(false);

    public static void startup() throws IOException {
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

    public static void shutdown() throws IOException {
        ssock.close();
        ssock = null;

        purge();
    }

    public static void purge() {
        blobs.clear();
    }

    public static int size() {
        return blobs.size();
    }

    public static void setFail() {
        fail.set(true);
    }

    public static void setDelay() {
        delay.set(true);
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
            if (fail.compareAndSet(true, false)) {
                out.write("HTTP/1.0 400 force fail".getBytes());
            } else if (delay.compareAndSet(true, false)) {
              try {
                Thread.sleep(LC.httpclient_internal_connmgr_so_timeout.longValue() + 10000);
            } catch (InterruptedException e) {
            }
            } else {
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
