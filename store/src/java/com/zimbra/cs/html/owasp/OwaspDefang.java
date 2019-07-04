/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
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
package com.zimbra.cs.html.owasp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.html.AbstractDefang;
import com.zimbra.cs.servlet.ZThreadLocal;

public class OwaspDefang extends AbstractDefang {

    private static final int finishBefore = DebugConfig.owasp_html_sanitizer_timeout;
    ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public void defang(InputStream is, boolean neuterImages, Writer out) throws IOException {
        String html = CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
        String sanitizedHtml = runSanitizer(html, neuterImages);
        out.write(sanitizedHtml);
        out.close();
    }

    @Override
    public void defang(Reader reader, boolean neuterImages, Writer out) throws IOException {
        String html = CharStreams.toString(reader);
        String sanitizedHtml = runSanitizer(html, neuterImages);
        if (sanitizedHtml != null) {
            out.write(sanitizedHtml);
        }
        out.close();
    }

    private String runSanitizer(String html, boolean neuterImages) {
        String vHost = null;
        if (ZThreadLocal.getRequestContext() != null) {
            vHost = ZThreadLocal.getRequestContext().getVirtualHost();
        }
        Callable<String> task = new OwaspHtmlSanitizer(html, neuterImages, vHost);
        Future<String> future = executor.submit(task);
        String sanitizedHtml = null;
        try {
            sanitizedHtml = future.get(finishBefore, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            if (ZimbraLog.soap.isDebugEnabled()) {
                ZimbraLog.soap.debug("Exception during HTML sanitization", e);
            }
            ZimbraLog.soap.warn("Exception during HTML sanitization: %s", e.getMessage());
            return null;
        }
        return sanitizedHtml;
    }

}