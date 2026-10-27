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
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
import com.google.common.collect.ImmutableSet;
import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

/**
 * Sends the Content Security Policy of the Modern UI, mapped to {@code /modern/*}.
 *
 * <p>Every response gets the policy from {@code zimbraModernCSP} and {@code zimbraModernCSPPolicy} with
 * {@code {{nonce}}} replaced by a fresh random nonce. For the HTML page the same nonce also replaces every
 * {@code __CSP_NONCE__} placeholder in the body. When the settings cannot be read the page is served without a policy.
 */
public class ModernCSPFilter implements Filter {

    /** Token in the policy that stands for the nonce of the current response. */
    static final String NONCE_TOKEN = "{{nonce}}";

    /** Placeholder the Modern UI build leaves in index.html for the nonce. */
    static final String NONCE_PLACEHOLDER = "__CSP_NONCE__";

    private static final int NONCE_BYTES = 16;
    private static final long SETTINGS_TTL_SECONDS = 60;
    private static final long MAX_CACHED_HOSTS = 1000;

    /** Request headers hidden from the static file servlet so it returns the whole uncompressed file. */
    private static final Set<String> HIDDEN_REQUEST_HEADERS =
            ImmutableSet.of("accept-encoding", "if-none-match", "if-modified-since", "range");

    /** The Modern UI CSP configuration of one virtual host. */
    static final class Settings {
        /** Settings that mean no header is sent. */
        static final Settings DISABLED = new Settings(null, null);

        private final String headerName;
        private final String policy;

        private Settings(String headerName, String policy) {
            this.headerName = headerName;
            this.policy = policy;
        }

        /** Builds the settings from the attribute values, which the attribute definitions already validate. */
        static Settings of(String mode, String policy) {
            if (policy == null || policy.trim().isEmpty()) {
                return DISABLED;
            }
            if ("enforce".equals(mode)) {
                return new Settings("Content-Security-Policy", policy.trim());
            }
            if ("report-only".equals(mode)) {
                return new Settings("Content-Security-Policy-Report-Only", policy.trim());
            }
            return DISABLED;
        }

        boolean isEnabled() {
            return headerName != null;
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
            Domain domain = host.isEmpty() ? null : prov.get(Key.DomainBy.virtualHostname, host);
            if (domain != null) {
                source = domain;
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
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        Settings settings = "GET".equals(req.getMethod()) ? settingsFor(req) : null;
        if (settings == null || !settings.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }
        String nonce = newNonce();
        resp.setHeader(settings.headerName, settings.policy.replace(NONCE_TOKEN, nonce));
        if (!isDocument(req)) {
            chain.doFilter(request, response);
            return;
        }
        BufferedResponse buffered = new BufferedResponse(resp);
        chain.doFilter(new IdentityRequest(req), buffered);
        if (resp.isCommitted()) {
            return;
        }
        String page = new String(buffered.bytes(), StandardCharsets.UTF_8);
        byte[] body = page.replace(NONCE_PLACEHOLDER, nonce).getBytes(StandardCharsets.UTF_8);
        resp.setHeader("Cache-Control", "no-store");
        resp.setContentLength(body.length);
        resp.getOutputStream().write(body);
    }

    private Settings settingsFor(HttpServletRequest req) {
        try {
            return settingsSource.settingsFor(req.getServerName());
        } catch (ServiceException | RuntimeException e) {
            ZimbraLog.misc.error("Unable to read the Modern UI CSP settings; /modern is served without a CSP", e);
            return null;
        }
    }

    private String newNonce() {
        byte[] bytes = new byte[NONCE_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** True for the HTML page and for application routes such as /modern/mail/inbox, which serve the same page. */
    static boolean isDocument(HttpServletRequest req) {
        String path = req.getServletPath() + (req.getPathInfo() == null ? "" : req.getPathInfo());
        return path.endsWith(".html") || path.lastIndexOf('.') < path.lastIndexOf('/');
    }

    /** Hides the headers that make the static file servlet answer with a compressed or partial response. */
    private static final class IdentityRequest extends HttpServletRequestWrapper {
        IdentityRequest(HttpServletRequest request) {
            super(request);
        }

        private static boolean hidden(String name) {
            return HIDDEN_REQUEST_HEADERS.contains(name.toLowerCase(Locale.ROOT));
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
        public long getDateHeader(String name) {
            return hidden(name) ? -1L : super.getDateHeader(name);
        }
    }

    /** Collects the body the static file servlet writes so it can be rewritten before anything is sent. */
    private static final class BufferedResponse extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        BufferedResponse(HttpServletResponse response) {
            super(response);
        }

        byte[] bytes() {
            return buffer.toByteArray();
        }

        @Override
        public ServletOutputStream getOutputStream() {
            return new ServletOutputStream() {
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

        @Override
        public void flushBuffer() {
        }
    }
}
