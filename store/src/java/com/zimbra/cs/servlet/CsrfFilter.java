/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Joiner;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.net.HttpHeaders;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.CsrfTokenKey;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.servlet.util.CsrfUtil;
import com.zimbra.soap.RequestContext;

/**
 * @author zimbra
 *
 */
public class CsrfFilter implements Filter {

    /**
     *
     */
    public static final String CSRF_SALT = "CSRF_SALT";

    /**
     * Global CSRF allowed referer hosts built once at init() as an immutable Set
     * for O(1) contains() lookups on every request.
     * volatile ensures safe publication if a future config-reload path
     * reassigns the reference from another thread.
     */
    private volatile Set<String> allowedRefHostsSet = Collections.emptySet();

    /** Kept for debug logging only; no functional use on the hot path. */
    private String[] allowedRefHostsRaw = null;

    /**
     * Per-domain CSRF allowed referer hosts cache.
     * Stores {@code Set<String>} (not {@code String[]}) so that domain-level lookups are O(1) contains()
     * with no per-request merge or array allocation.
     */
    private LoadingCache<String, Set<String>> domainAllowedRefHosts = null;

    public static final String AUTH_TOKEN = "AuthToken";

    public static final String CSRF_TOKEN_CHECK = "CsrfTokenCheck";

    protected int maxCsrfTokenValidityInMs;

    private Random nonceGen = null;

    /** Sentinel: domain has no configured hosts; cached to avoid repeated LDAP misses. */
    private static final Set<String> EMPTY_SET = Collections.emptySet();

    private static final int DEFAULT_DOMAIN_CACHE_EXPIRY_MINS = 60;

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialize the parameters related to CSRF check
        Provisioning prov = Provisioning.getInstance();
        try {
            // Build Set<String> once at startup — O(n) here, O(1) on every subsequent request.
            allowedRefHostsRaw = prov.getConfig().getCsrfAllowedRefererHosts();
            allowedRefHostsSet = buildImmutableSet(allowedRefHostsRaw);
            this.domainAllowedRefHosts = buildDomainAllowedReferrerHostsCache();
            nonceGen = new Random();
            CsrfTokenKey.getCurrentKey();
            if (ZimbraLog.misc.isInfoEnabled()) {
                ZimbraLog.misc.info("CSRF filter was initialized: "
                        + "CSRFAllowedRefHost: [" + Joiner.on(", ").join(allowedRefHostsSet) + "]");
            }
        } catch (ServiceException e) {
            throw new ServletException("Error initializing CSRF filter: "
                    + e.getMessage(), e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
        ZimbraLog.filter.info("Destroying CSRF filter.");
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
     * javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        ZimbraLog.clearContext();

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        req.setAttribute(CSRF_SALT, nonceGen.nextInt() + 1);

        if (ZimbraLog.misc.isDebugEnabled()) {
            ZimbraLog.misc.debug("CSRF Request URI: " + req.getRequestURI());
        }

        boolean csrfCheckEnabled = Boolean.FALSE;
        boolean csrfRefererCheckEnabled = Boolean.FALSE;
        Provisioning prov = Provisioning.getInstance();
        try {
            csrfCheckEnabled = prov.getConfig().isCsrfTokenCheckEnabled();
            csrfRefererCheckEnabled = prov.getConfig().isCsrfRefererCheckEnabled();
        } catch (ServiceException e) {
            ZimbraLog.misc.info("Error in CSRF filter." + e.getMessage(), e);
        }

        if (ZimbraLog.misc.isDebugEnabled()) {
            ZimbraLog.misc.debug(
                    "CSRF filter was initialized : " + "CSRFcheck enabled: " +
                            csrfCheckEnabled + "CSRF referer check enabled: " +
                            csrfRefererCheckEnabled + ", CSRFAllowedRefHost: [" +
                            Joiner.on(", ").join(allowedRefHostsSet) + "]"
                            + ", CSRFTokenValidity " + this.maxCsrfTokenValidityInMs +
                            "ms." + " (domain-level overrides, if any, are logged per-request)");
        }

        if (ZimbraLog.misc.isTraceEnabled()) {
            Enumeration<String> hdrNames = req.getHeaderNames();
            ZimbraLog.misc.trace("Soap request headers.");
            while (hdrNames.hasMoreElements()) {
                String name = hdrNames.nextElement();
                // we do not want to print cookie headers for security reasons.
                if (name.contains(HttpHeaders.COOKIE))
                    continue;
                ZimbraLog.misc.trace(name + "=" + req.getHeader(name));
            }
        }

        // host resolved once here; passed to referer check and DefangFilter.
        String host = CsrfUtil.getRequestHost(req);
        if (csrfRefererCheckEnabled) {
            if (!allowReqBasedOnRefererHeaderCheck(req, host)) {
                ZimbraLog.misc.info("CSRF referer check failed");
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        // we need virtual host information in DefangFilter
        // set them in ThreadLocal here
        RequestContext reqCtxt = new RequestContext();
        reqCtxt.setVirtualHost(host);
        ZThreadLocal.setContext(reqCtxt);
        if (!csrfCheckEnabled) {
            req.setAttribute(CSRF_TOKEN_CHECK, Boolean.FALSE);
            chain.doFilter(req, resp);
        } else {
            req.setAttribute(Provisioning.A_zimbraCsrfTokenCheckEnabled, Boolean.TRUE);
            AuthToken authToken = CsrfUtil.getAuthTokenFromReq(req);
            if (CsrfUtil.doCsrfCheck(req, authToken)) {
                // post request and Auth token is CSRF enabled
                req.setAttribute(CSRF_TOKEN_CHECK, Boolean.TRUE);
            } else {
                req.setAttribute(CSRF_TOKEN_CHECK, Boolean.FALSE);
                ZimbraLog.misc.debug("CSRF check will not be done for URI : %s", req.getRequestURI());
            }
            chain.doFilter(req, resp);
        }
        ZThreadLocal.unset();
    }

    /**
     * @param initParameter
     * @return
     */
    protected static List<String> convertToList(String urlList) {
        List<String> urls = null;
        if (!StringUtil.isNullOrEmpty(urlList)) {
            String[] temp = urlList.split(",");
            for (int i = 0; i < temp.length; ++i) {
                temp[i] = temp[i].toLowerCase();
            }
            urls = Arrays.asList(temp);
        }
        return urls;
    }

    /**
     * Returns true if the request should be allowed based on its Referer header;
     * false if the request should be denied (CSRF).
     * Performs two independent O(1) Set.contains() checks — first against the
     * global allowed hosts set, then against the domain-level set from cache.
     * No array merge or Stream allocation occurs on the hot path.
     * Fail-closed guarantee: if the domain cache lookup throws, the method falls
     * back to the global set only — it never silently allows all traffic.
     *
     * @param req         the current HTTP request
     * @param virtualHost the pre-resolved virtual host from CsrfUtil.getRequestHost()
     * @return true to allow; false to deny with 403
     */
    private boolean allowReqBasedOnRefererHeaderCheck(HttpServletRequest req, String virtualHost) {
        try {
            Set<String> domainHosts = EMPTY_SET;
            if (!StringUtil.isNullOrEmpty(virtualHost)) {
                try {
                    domainHosts = domainAllowedRefHosts.get(virtualHost);
                } catch (ExecutionException e) {
                    ZimbraLog.misc.warn(
                            "CSRF: domain cache lookup failed for virtualHost=%s, "
                                    + "falling back to global-only check.", virtualHost, e);
                }
            }
            if (CsrfUtil.isCsrfRequestBasedOnReferrer(req, allowedRefHostsSet, domainHosts)) {
                return false;
            }
        } catch (MalformedURLException e) {
            ZimbraLog.misc.info("Error while doing referer based check." + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Builds the cache for domain level zimbraCsrfAllowedRefererHosts.
     * Max cache size is configurable by LC.csrf_filter_domain_allowed_ref_hosts_max_size.
     * Cache entries expire after LC.csrf_filter_domain_allowed_ref_hosts_cache_expiry_mins minutes.
     *
     * The cache now stores {@code Set<String>} instead of {@code String[]} so that domain-level
     * lookups are O(1) contains() with no per-request merge or array allocation.
     * On LDAP miss or domain-not-found, EMPTY_SET is cached to prevent repeated lookups.
     *
     * @return LoadingCache for each accessed domain's zimbraCsrfAllowedRefererHosts as a Set
     **/
    protected LoadingCache<String, Set<String>> buildDomainAllowedReferrerHostsCache() {
        int maxCacheSize;
        try {
            maxCacheSize = LC.csrf_filter_domain_allowed_ref_hosts_max_size.intValue();
        } catch (NumberFormatException e) {
            ZimbraLog.misc.warn("Failed to determine CsrfFilter.domainAllowedRefHosts cache max size from" +
                    "LC.csrf_filter_domain_allowed_ref_hosts_max_size value, " +
                    "falling back to LC.ldap_cache_domain_maxsize", e);
            maxCacheSize = LC.ldap_cache_domain_maxsize.intValue();
        }
        int expiryMins;
        try {
            expiryMins = LC.csrf_filter_domain_allowed_ref_hosts_cache_expiry_mins.intValue();
            if (expiryMins <= 0) {
                ZimbraLog.misc.warn("CsrfFilter.domainAllowedRefHosts cache expiry must be > 0," +
                        " falling back to " + DEFAULT_DOMAIN_CACHE_EXPIRY_MINS + " minutes");
                expiryMins = DEFAULT_DOMAIN_CACHE_EXPIRY_MINS;
            }
        } catch (NumberFormatException e) {
            ZimbraLog.misc.warn("Failed to determine CsrfFilter.domainAllowedRefHosts cache expiry from "
                    + "LC.csrf_filter_domain_allowed_ref_hosts_cache_expiry_mins, falling back to  "
                    + DEFAULT_DOMAIN_CACHE_EXPIRY_MINS + " minutes", e);
            expiryMins = DEFAULT_DOMAIN_CACHE_EXPIRY_MINS;
        }

        return CacheBuilder.newBuilder()
                .maximumSize(maxCacheSize)
                .expireAfterWrite(expiryMins, TimeUnit.MINUTES)
                .build(new CacheLoader<String, Set<String>>() {
                    // lazy load each domain's zimbraCsrfAllowedRefererHosts as an immutable Set
                    @Override
                    public Set<String> load(String virtualHost) throws Exception {
                        try {
                            Provisioning prov = Provisioning.getInstance();
                            Domain domain = prov.getDomainByVirtualHostname(virtualHost);
                            if (domain == null) {
                                domain = prov.getDomainByName(virtualHost);
                            }
                            if (domain != null) { // null if no vhost set and name isn't a domain
                                String[] domainHosts = domain.getCsrfAllowedRefererHosts();
                                if (domainHosts != null && domainHosts.length > 0) {
                                    Set<String> result = buildImmutableSet(domainHosts);
                                    ZimbraLog.misc.debug(
                                            "CSRF: additionally using domain-level allowedRefererHosts for "
                                                    + "virtualHost=%s, hosts=[%s]", virtualHost,
                                            Joiner.on(", ").join(result));
                                    return result;
                                }
                            }
                        } catch (ServiceException e) {
                            ZimbraLog.misc.warn(
                                    "CSRF: failed to resolve domain-level allowedRefererHosts for "
                                            + "virtualHost=%s, falling back to globalConfig.",
                                    virtualHost, e);
                        }
                        // always cache the absence to prevent subsequent LDAP misses for this virtual host
                        return EMPTY_SET;
                    }
                });
    }

    /**
     * Builds an unmodifiable Set from a String[] array.
     *
     * Called at init() for the global list and inside the cache loader for each domain.
     * The resulting sets are the foundation of the O(1) lookup path, replacing the
     * per-request Stream merge and linear array scan from the previous implementation.
     *
     * @param hosts source array (may be null or empty)
     * @return unmodifiable Set; never null
     */
    private static Set<String> buildImmutableSet(String[] hosts) {
        if (hosts == null || hosts.length == 0) {
            return Collections.emptySet();
        }
        Set<String> set = new HashSet<>((int) (hosts.length / 0.75f) + 1);
        for (String h : hosts) {
            if (!StringUtil.isNullOrEmpty(h)) {
                set.add(h);
            }
        }
        return Collections.unmodifiableSet(set);
    }

}