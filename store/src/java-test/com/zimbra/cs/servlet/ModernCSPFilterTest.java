/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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
package com.zimbra.cs.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import com.zimbra.common.service.ServiceException;

/**
 * Tests {@link ModernCSPFilter} with small in-memory servlet fakes, so no server or LDAP is needed.
 */
public class ModernCSPFilterTest {

    private static final String POLICY =
            "default-src 'self'; script-src 'self' 'nonce-{{nonce}}'; object-src 'none'";
    private static final String NEW_UI = "<html><meta name=\"csp-nonce\" content=\"__CSP_NONCE__\">"
            + "<style nonce=\"__CSP_NONCE__\"></style></html>";
    private static final Pattern NONCE_IN_BODY = Pattern.compile("content=\"([^\"]+)\"");
    private static final Pattern NONCE_IN_HEADER = Pattern.compile("'nonce-([^']+)'");

    /** What the filter sends and what the downstream servlet saw. */
    private static final class Result {
        final Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>();
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        final Map<String, String> seenByServlet = new HashMap<String, String>();
        int errorStatus = -1;
        String redirect;
        int contentLength = -1;
        boolean committed;

        String header(String name) {
            List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
            return values == null || values.isEmpty() ? null : values.get(0);
        }

        String bodyText() {
            return new String(body.toByteArray(), StandardCharsets.UTF_8);
        }

        String nonceInHeader() {
            Matcher m = NONCE_IN_HEADER.matcher(header("Content-Security-Policy"));
            assertTrue(m.find());
            return m.group(1);
        }
    }

    /** Plays the static file servlet: writes a body the way Jetty's DefaultServlet would. */
    private interface Servlet {
        void service(HttpServletRequest request, HttpServletResponse response) throws IOException;
    }

    private static Servlet file(final String content) {
        return new Servlet() {
            @Override
            public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
                byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                response.setContentType("text/html");
                response.setContentLength(bytes.length);
                response.getOutputStream().write(bytes);
            }
        };
    }

    private static Result run(ModernCSPFilter filter, String method, String path, final Servlet servlet)
            throws Exception {
        final Result result = new Result();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept-Encoding", "gzip, br");
        headers.put("If-None-Match", "\"abc\"");
        headers.put("Range", "bytes=0-10");
        filter.doFilter(fakeRequest(method, path, headers), fakeResponse(result), new FilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse resp) throws IOException, ServletException {
                HttpServletRequest http = (HttpServletRequest) req;
                for (String name : new String[] { "Accept-Encoding", "If-None-Match", "Range" }) {
                    result.seenByServlet.put(name, http.getHeader(name));
                }
                servlet.service(http, (HttpServletResponse) resp);
            }
        });
        return result;
    }

    private static Result get(ModernCSPFilter filter, String path, Servlet servlet) throws Exception {
        return run(filter, "GET", path, servlet);
    }

    private static ModernCSPFilter filterFor(final String mode, final String policy) {
        return new ModernCSPFilter(new ModernCSPFilter.SettingsSource() {
            @Override
            public ModernCSPFilter.Settings settingsFor(String host) {
                return ModernCSPFilter.Settings.of(mode, policy);
            }
        });
    }

    @Test
    public void documentGetsFreshNonceInBodyAndHeader() throws Exception {
        Result r = get(filterFor("enforce", POLICY), "/modern/", file(NEW_UI));

        Matcher body = NONCE_IN_BODY.matcher(r.bodyText());
        assertTrue(body.find());
        assertEquals(body.group(1), r.nonceInHeader());
        assertFalse(r.bodyText().contains("__CSP_NONCE__"));
        assertTrue(r.bodyText().contains("nonce=\"" + r.nonceInHeader() + "\""));
        assertEquals(r.body.size(), r.contentLength);
        assertTrue(r.header("Content-Security-Policy").endsWith("object-src 'none'"));
    }

    @Test
    public void nonceIsRandomAndAtLeast128Bits() throws Exception {
        ModernCSPFilter filter = filterFor("enforce", POLICY);
        String first = get(filter, "/modern/", file(NEW_UI)).nonceInHeader();
        String second = get(filter, "/modern/", file(NEW_UI)).nonceInHeader();

        assertNotEquals(first, second);
        assertEquals(16, Base64.getUrlDecoder().decode(first).length);
    }

    @Test
    public void documentIsNeverCached() throws Exception {
        Result r = get(filterFor("enforce", POLICY), "/modern/index.html", file(NEW_UI));

        assertEquals("no-store", r.header("Cache-Control"));
    }

    @Test
    public void documentRequestHidesCompressionAndConditionalHeaders() throws Exception {
        Result r = get(filterFor("enforce", POLICY), "/modern/", file(NEW_UI));

        assertNull(r.seenByServlet.get("Accept-Encoding"));
        assertNull(r.seenByServlet.get("If-None-Match"));
        assertNull(r.seenByServlet.get("Range"));
    }

    @Test
    public void applicationRoutesAreHandledLikeTheDocument() throws Exception {
        Result r = get(filterFor("enforce", POLICY), "/modern/mail/inbox", file(NEW_UI));

        Matcher body = NONCE_IN_BODY.matcher(r.bodyText());
        assertTrue(body.find());
        assertEquals(body.group(1), r.nonceInHeader());
        assertNull(r.seenByServlet.get("Accept-Encoding"));
    }

    @Test
    public void everyPlaceholderIsReplaced() throws Exception {
        Result r = get(filterFor("enforce", POLICY), "/modern/", file("__CSP_NONCE__ and __CSP_NONCE__"));

        String nonce = r.nonceInHeader();
        assertEquals(nonce + " and " + nonce, r.bodyText());
    }

    @Test
    public void reportOnlyUsesTheReportOnlyHeader() throws Exception {
        Result r = get(filterFor("report-only", POLICY), "/modern/", file(NEW_UI));

        assertNull(r.header("Content-Security-Policy"));
        assertNotNull(r.header("Content-Security-Policy-Report-Only"));
    }

    @Test
    public void offModeAndEmptyPolicyLeaveTheResponseAlone() throws Exception {
        for (ModernCSPFilter filter : new ModernCSPFilter[] { filterFor("off", POLICY), filterFor("enforce", "  ") }) {
            Result r = get(filter, "/modern/", file(NEW_UI));

            assertNull(r.header("Content-Security-Policy"));
            assertEquals(NEW_UI, r.bodyText());
            assertEquals("gzip, br", r.seenByServlet.get("Accept-Encoding"));
        }
    }

    @Test
    public void assetsGetThePolicyButAreNotRewritten() throws Exception {
        Result r = get(filterFor("enforce", POLICY), "/modern/scripts/app.js", file("console.log(1)"));

        assertNotNull(r.header("Content-Security-Policy"));
        assertEquals("console.log(1)", r.bodyText());
        assertEquals("gzip, br", r.seenByServlet.get("Accept-Encoding"));
        assertNull(r.header("Cache-Control"));
    }

    @Test
    public void errorsAndRedirectsAreLeftToTheServlet() throws Exception {
        Result notFound = get(filterFor("enforce", POLICY), "/modern/", new Servlet() {
            @Override
            public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
                response.sendError(404);
            }
        });
        Result redirect = get(filterFor("enforce", POLICY), "/modern", new Servlet() {
            @Override
            public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
                response.sendRedirect("/modern/");
            }
        });

        assertEquals(404, notFound.errorStatus);
        assertEquals(0, notFound.body.size());
        assertEquals("/modern/", redirect.redirect);
        assertEquals(0, redirect.body.size());
    }

    @Test
    public void unreadableSettingsServeThePageWithoutPolicy() throws Exception {
        ModernCSPFilter filter = new ModernCSPFilter(new ModernCSPFilter.SettingsSource() {
            @Override
            public ModernCSPFilter.Settings settingsFor(String host) throws ServiceException {
                throw ServiceException.FAILURE("ldap down", null);
            }
        });
        Result r = get(filter, "/modern/", file(NEW_UI));

        assertNull(r.header("Content-Security-Policy"));
        assertEquals(NEW_UI, r.bodyText());
    }

    @Test
    public void onlyGetRequestsAreHandled() throws Exception {
        Result r = run(filterFor("enforce", POLICY), "POST", "/modern/", file(NEW_UI));

        assertNull(r.header("Content-Security-Policy"));
        assertEquals(NEW_UI, r.bodyText());
    }

    @Test
    public void settingsAreDisabledForOffNullModeAndBlankPolicy() {
        assertFalse(ModernCSPFilter.Settings.of("off", POLICY).isEnabled());
        assertFalse(ModernCSPFilter.Settings.of(null, POLICY).isEnabled());
        assertFalse(ModernCSPFilter.Settings.of("enforce", null).isEnabled());
        assertFalse(ModernCSPFilter.Settings.of("enforce", "   ").isEnabled());
        assertTrue(ModernCSPFilter.Settings.of("enforce", POLICY).isEnabled());
        assertTrue(ModernCSPFilter.Settings.of("report-only", POLICY).isEnabled());
    }

    @Test
    public void documentsAreHtmlAndRoutesButNotFiles() throws Exception {
        assertTrue(isDocument("/modern/"));
        assertTrue(isDocument("/modern/index.html"));
        assertTrue(isDocument("/modern/mail/inbox"));
        assertFalse(isDocument("/modern/app.js"));
        assertFalse(isDocument("/modern/css/site.css"));
    }

    private static boolean isDocument(String path) {
        return ModernCSPFilter.isDocument(fakeRequest("GET", path, new HashMap<String, String>()));
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return Boolean.FALSE;
        }
        if (type == int.class) {
            return Integer.valueOf(0);
        }
        if (type == long.class) {
            return Long.valueOf(0L);
        }
        return null;
    }

    private static HttpServletRequest fakeRequest(final String method, final String path,
            final Map<String, String> headers) {
        final Map<String, String> lower = new HashMap<String, String>();
        for (Map.Entry<String, String> e : headers.entrySet()) {
            lower.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue());
        }
        return (HttpServletRequest) Proxy.newProxyInstance(ModernCSPFilterTest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class }, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method m, Object[] args) {
                        String name = m.getName();
                        if ("getMethod".equals(name)) {
                            return method;
                        } else if ("getServletPath".equals(name)) {
                            return path;
                        } else if ("getServerName".equals(name)) {
                            return "mail.example.com";
                        } else if ("getHeader".equals(name)) {
                            return lower.get(((String) args[0]).toLowerCase(Locale.ROOT));
                        } else if ("getHeaders".equals(name)) {
                            String v = lower.get(((String) args[0]).toLowerCase(Locale.ROOT));
                            return v == null ? Collections.emptyEnumeration()
                                    : Collections.enumeration(Collections.singletonList(v));
                        } else if ("getDateHeader".equals(name)) {
                            return lower.containsKey(((String) args[0]).toLowerCase(Locale.ROOT)) ? 1L : -1L;
                        }
                        return defaultValue(m.getReturnType());
                    }
                });
    }

    private static HttpServletResponse fakeResponse(final Result result) {
        final ServletOutputStream out = new ServletOutputStream() {
            @Override
            public void write(int b) {
                result.body.write(b);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener listener) {
            }
        };
        return (HttpServletResponse) Proxy.newProxyInstance(ModernCSPFilterTest.class.getClassLoader(),
                new Class<?>[] { HttpServletResponse.class }, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method m, Object[] args) {
                        String name = m.getName();
                        if ("setHeader".equals(name) || "addHeader".equals(name)) {
                            String key = ((String) args[0]).toLowerCase(Locale.ROOT);
                            List<String> values = result.headers.get(key);
                            if (values == null || "setHeader".equals(name)) {
                                values = new ArrayList<String>();
                                result.headers.put(key, values);
                            }
                            values.add((String) args[1]);
                        } else if ("setContentLength".equals(name)) {
                            result.contentLength = (Integer) args[0];
                        } else if ("sendError".equals(name)) {
                            result.errorStatus = (Integer) args[0];
                            result.committed = true;
                        } else if ("sendRedirect".equals(name)) {
                            result.redirect = (String) args[0];
                            result.committed = true;
                        } else if ("isCommitted".equals(name)) {
                            return result.committed;
                        } else if ("getOutputStream".equals(name)) {
                            return out;
                        }
                        return defaultValue(m.getReturnType());
                    }
                });
    }
}
