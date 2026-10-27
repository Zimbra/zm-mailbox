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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

/**
 * Adds the Content Security Policy of the Modern UI, including a per-response script nonce.
 *
 * <p>The policy comes from {@code zimbraModernCSP} and {@code zimbraModernCSPPolicy}, read for
 * the virtual host of the request with the global value as the fallback. For the HTML document the filter replaces
 * every {@code __CSP_NONCE__} placeholder with a fresh random nonce, puts the same nonce in the {@code {{nonce}}}
 * token of the policy, and sends the header. Other responses under {@code /modern} get the policy without the nonce
 * source.
 *
 * <p>If the document has no placeholder (an older Modern UI build) no header is sent, because a policy that needs a
 * nonce would block the zimlet loader. The filter never blocks a request: when it cannot read its settings it logs an
 * error and serves the page without a policy.
 */
public class ModernCSPFilter implements Filter {

    /** Token in the policy that stands for the nonce of the current response. */
    static final String NONCE_TOKEN = "{{nonce}}";

    /** Placeholder the Modern UI build leaves in index.html for the nonce. */
    static final String NONCE_PLACEHOLDER = "__CSP_NONCE__";

    private static final String MODERN_PATH = "/modern";
    private static final int NONCE_BYTES = 16;
    private static final long SETTINGS_TTL_SECONDS = 60;
    private static final long MAX_CACHED_HOSTS = 1000;
    private static final byte[] PLACEHOLDER_BYTES = NONCE_PLACEHOLDER.getBytes(StandardCharsets.US_ASCII);

    /** Request headers hidden from the static file servlet so it returns the whole uncompressed file. */
    private static final Set<String> HIDDEN_REQUEST_HEADERS = new HashSet<String>();

    /** Response headers the filter sets itself for a rewritten document. */
    private static final Set<String> OWNED_RESPONSE_HEADERS = new HashSet<String>();

    static {
        for (String name : new String[] { "accept-encoding", "if-none-match", "if-modified-since", "if-match",
                "if-unmodified-since", "if-range", "range" }) {
            HIDDEN_REQUEST_HEADERS.add(name);
        }
        for (String name : new String[] { "content-length", "etag", "last-modified", "cache-control", "expires",
                "pragma" }) {
            OWNED_RESPONSE_HEADERS.add(name);
        }
    }

    /** The validated Modern UI CSP configuration of one virtual host. */
    static final class Settings {
        /** Settings that mean no header is sent. */
        static final Settings DISABLED = new Settings(null, null);

        private final String headerName;
        private final String policy;

        private Settings(String headerName, String policy) {
            this.headerName = headerName;
            this.policy = policy;
        }

        /**
         * Validates the attribute values.
         *
         * @return the settings, or {@link #DISABLED} when CSP is off, the policy is empty, or a value is invalid
         */
        static Settings of(String mode, String policy) {
            if (mode == null || "off".equals(mode) || policy == null || policy.trim().isEmpty()) {
                return DISABLED;
            }
            String name;
            if ("enforce".equals(mode)) {
                name = "Content-Security-Policy";
            } else if ("report-only".equals(mode)) {
                name = "Content-Security-Policy-Report-Only";
            } else {
                ZimbraLog.misc.error("Unsupported Modern UI CSP mode '%s'; /modern is served without a CSP", mode);
                return DISABLED;
            }
            if (policy.indexOf('\r') >= 0 || policy.indexOf('\n') >= 0) {
                ZimbraLog.misc.error("The Modern UI CSP policy contains line breaks; /modern is served without a CSP");
                return DISABLED;
            }
            return new Settings(name, policy.trim());
        }

        boolean isEnabled() {
            return headerName != null;
        }

        String headerName() {
            return headerName;
        }

        boolean usesNonce() {
            return policy.contains(NONCE_TOKEN);
        }

        String policyWithNonce(String nonce) {
            return policy.replace(NONCE_TOKEN, nonce);
        }

        String policyWithoutNonce() {
            return policy.replaceAll("\\s*'nonce-" + Pattern.quote(NONCE_TOKEN) + "'", "")
                    .replace(NONCE_TOKEN, "").trim();
        }
    }

    /** Supplies the settings that apply to a request host. */
    interface SettingsSource {
        Settings settingsFor(String host) throws ServiceException;
    }

    /** Reads the settings from LDAP, with a short cache so static asset requests do not query it. */
    static final class ProvisioningSettingsSource implements SettingsSource {
        private final Cache<String, Settings> cache = CacheBuilder.newBuilder()
                .maximumSize(MAX_CACHED_HOSTS).expireAfterWrite(SETTINGS_TTL_SECONDS, TimeUnit.SECONDS).build();

        @Override
        public Settings settingsFor(String host) throws ServiceException {
            String key = host == null ? "" : host.toLowerCase(Locale.ROOT);
            Settings settings = cache.getIfPresent(key);
            if (settings == null) {
                settings = load(key);
                cache.put(key, settings);
            }
            return settings;
        }

        private Settings load(String host) throws ServiceException {
            Provisioning prov = Provisioning.getInstance();
            Entry source = prov.getConfig();
            if (!host.isEmpty()) {
                Domain domain = prov.get(Key.DomainBy.virtualHostname, host);
                if (domain != null) {
                    source = domain;
                }
            }
            return Settings.of(source.getAttr(Provisioning.A_zimbraModernCSP, true),
                    source.getAttr(Provisioning.A_zimbraModernCSPPolicy, true));
        }
    }

    private final SettingsSource settingsSource;
    private final SecureRandom random = new SecureRandom();

    /** Creates the filter that reads its settings from LDAP. */
    public ModernCSPFilter() {
        this(new ProvisioningSettingsSource());
    }

    ModernCSPFilter(SettingsSource settingsSource) {
        this.settingsSource = settingsSource;
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = pathOf(req);
        Settings settings = null;
        if ("GET".equals(req.getMethod()) && (path.equals(MODERN_PATH) || path.startsWith(MODERN_PATH + "/"))) {
            settings = settingsFor(req);
        }
        if (settings == null || !settings.isEnabled()) {
            chain.doFilter(request, response);
        } else if (isDocumentPath(path)) {
            serveDocument(req, resp, chain, settings);
        } else {
            resp.setHeader(settings.headerName(), settings.policyWithoutNonce());
            chain.doFilter(request, response);
        }
    }

    private Settings settingsFor(HttpServletRequest req) {
        try {
            return settingsSource.settingsFor(req.getServerName());
        } catch (ServiceException | RuntimeException e) {
            ZimbraLog.misc.error("Unable to read the Modern UI CSP settings; /modern is served without a CSP", e);
            return null;
        }
    }

    private void serveDocument(HttpServletRequest req, HttpServletResponse resp, FilterChain chain,
            Settings settings) throws IOException, ServletException {
        BufferedResponse buffered = new BufferedResponse(resp);
        chain.doFilter(new IdentityRequest(req), buffered);
        if (buffered.isHandled()) {
            return;
        }
        byte[] body = buffered.body();
        byte[] out = body;
        String policy = null;
        boolean unique = false;
        if (buffered.isPlainSuccess()) {
            if (!settings.usesNonce()) {
                policy = settings.policyWithoutNonce();
            } else {
                String nonce = newNonce();
                byte[] replaced = replaceAll(body, PLACEHOLDER_BYTES, nonce.getBytes(StandardCharsets.US_ASCII));
                if (replaced != null) {
                    out = replaced;
                    policy = settings.policyWithNonce(nonce);
                    unique = true;
                }
            }
        }
        if (policy != null) {
            resp.setHeader(settings.headerName(), policy);
        }
        if (unique) {
            // The body now holds a nonce that is only valid for this response, so it must not be cached or revalidated.
            resp.setHeader("Cache-Control", "no-store");
            resp.setHeader("Pragma", "no-cache");
            resp.setDateHeader("Expires", 0);
            resp.setHeader("ETag", null);
            resp.setHeader("Last-Modified", null);
        }
        resp.setContentLength(out.length);
        resp.getOutputStream().write(out);
    }

    private String newNonce() {
        byte[] bytes = new byte[NONCE_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** True for the HTML page and for application routes such as /modern/mail/inbox, which serve the same page. */
    static boolean isDocumentPath(String path) {
        if (path.equals(MODERN_PATH) || path.endsWith("/") || path.endsWith(".html")) {
            return true;
        }
        return path.substring(path.lastIndexOf('/') + 1).indexOf('.') < 0;
    }

    private static String pathOf(HttpServletRequest req) {
        String uri = req.getRequestURI();
        if (uri == null) {
            return "";
        }
        String context = req.getContextPath();
        String path = context != null && !context.isEmpty() && uri.startsWith(context)
                ? uri.substring(context.length()) : uri;
        int params = path.indexOf(';');
        return params >= 0 ? path.substring(0, params) : path;
    }

    /** Replaces every occurrence of {@code target}; returns null when there is none. */
    static byte[] replaceAll(byte[] source, byte[] target, byte[] replacement) {
        ByteArrayOutputStream result = new ByteArrayOutputStream(source.length + 64);
        int found = 0;
        int i = 0;
        while (i < source.length) {
            if (matchesAt(source, target, i)) {
                result.write(replacement, 0, replacement.length);
                i += target.length;
                found++;
            } else {
                result.write(source[i]);
                i++;
            }
        }
        return found == 0 ? null : result.toByteArray();
    }

    private static boolean matchesAt(byte[] source, byte[] target, int offset) {
        if (offset + target.length > source.length) {
            return false;
        }
        for (int j = 0; j < target.length; j++) {
            if (source[offset + j] != target[j]) {
                return false;
            }
        }
        return true;
    }

    /** Hides the headers that make the static file servlet answer with a compressed or partial response. */
    private static final class IdentityRequest extends HttpServletRequestWrapper {
        IdentityRequest(HttpServletRequest request) {
            super(request);
        }

        private static boolean hidden(String name) {
            return name != null && HIDDEN_REQUEST_HEADERS.contains(name.toLowerCase(Locale.ROOT));
        }

        @Override
        public String getHeader(String name) {
            return hidden(name) ? null : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return hidden(name) ? Collections.<String>emptyEnumeration() : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = new ArrayList<String>();
            Enumeration<String> all = super.getHeaderNames();
            while (all != null && all.hasMoreElements()) {
                String name = all.nextElement();
                if (!hidden(name)) {
                    names.add(name);
                }
            }
            return Collections.enumeration(names);
        }

        @Override
        public long getDateHeader(String name) {
            return hidden(name) ? -1L : super.getDateHeader(name);
        }

        @Override
        public int getIntHeader(String name) {
            return hidden(name) ? -1 : super.getIntHeader(name);
        }
    }

    /** Collects the response body so the filter can rewrite it before anything is sent. */
    private static final class BufferedResponse extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private ServletOutputStream stream;
        private PrintWriter writer;
        private int status = HttpServletResponse.SC_OK;
        private String contentType;
        private boolean handled;
        private boolean encoded;

        BufferedResponse(HttpServletResponse response) {
            super(response);
        }

        byte[] body() {
            if (writer != null) {
                writer.flush();
            }
            return buffer.toByteArray();
        }

        /** True when the servlet already answered (error or redirect) and the filter must not write a body. */
        boolean isHandled() {
            return handled;
        }

        /** True for a 200 HTML response whose body is not content-encoded, so the placeholder can be replaced. */
        boolean isPlainSuccess() {
            return status == HttpServletResponse.SC_OK && !encoded && contentType != null
                    && contentType.toLowerCase(Locale.ROOT).startsWith("text/html");
        }

        private static boolean owned(String name) {
            return name != null && OWNED_RESPONSE_HEADERS.contains(name.toLowerCase(Locale.ROOT));
        }

        private void noteEncoding(String name, String value) {
            if ("content-type".equalsIgnoreCase(name)) {
                contentType = value;
            }
            if ("content-encoding".equalsIgnoreCase(name) && value != null && !"identity".equalsIgnoreCase(value)) {
                encoded = true;
            }
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (stream == null) {
                stream = new ServletOutputStream() {
                    @Override
                    public void write(int b) {
                        buffer.write(b);
                    }

                    @Override
                    public void write(byte[] b, int off, int len) {
                        buffer.write(b, off, len);
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setWriteListener(WriteListener listener) {
                        throw new IllegalStateException("Asynchronous output is not supported");
                    }
                };
            }
            return stream;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (writer == null) {
                String encoding = getCharacterEncoding();
                Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
                writer = new PrintWriter(new OutputStreamWriter(buffer, charset));
            }
            return writer;
        }

        @Override
        public void setStatus(int sc) {
            status = sc;
            super.setStatus(sc);
        }

        @Override
        public void setContentType(String type) {
            contentType = type;
            super.setContentType(type);
        }

        @Override
        public void sendError(int sc) throws IOException {
            handled = true;
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            handled = true;
            super.sendError(sc, msg);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            handled = true;
            super.sendRedirect(location);
        }

        @Override
        public void setHeader(String name, String value) {
            noteEncoding(name, value);
            if (!owned(name)) {
                super.setHeader(name, value);
            }
        }

        @Override
        public void addHeader(String name, String value) {
            noteEncoding(name, value);
            if (!owned(name)) {
                super.addHeader(name, value);
            }
        }

        @Override
        public void setIntHeader(String name, int value) {
            if (!owned(name)) {
                super.setIntHeader(name, value);
            }
        }

        @Override
        public void addIntHeader(String name, int value) {
            if (!owned(name)) {
                super.addIntHeader(name, value);
            }
        }

        @Override
        public void setDateHeader(String name, long date) {
            if (!owned(name)) {
                super.setDateHeader(name, date);
            }
        }

        @Override
        public void addDateHeader(String name, long date) {
            if (!owned(name)) {
                super.addDateHeader(name, date);
            }
        }

        @Override
        public void setContentLength(int len) {
        }

        @Override
        public void setContentLengthLong(long len) {
        }

        @Override
        public void setBufferSize(int size) {
        }

        @Override
        public void flushBuffer() throws IOException {
        }

        @Override
        public void resetBuffer() {
            buffer.reset();
        }

        @Override
        public void reset() {
            buffer.reset();
            status = HttpServletResponse.SC_OK;
            contentType = null;
            encoded = false;
            super.reset();
        }
    }
}
