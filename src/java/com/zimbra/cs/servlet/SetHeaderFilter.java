package com.zimbra.cs.servlet;

import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZAttrProvisioning;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.Log;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/** Sets headers for request. */
public class SetHeaderFilter implements Filter {

	//
	// Constants
	//

	public static final String P_RESPONSE_HEADERS_ENABLED = "zimbraResponseHeader.enabled";

	public static final Pattern RE_HEADER = Pattern.compile("^([^:]+):\\s+(.*)$");
	public static final String UNKNOWN_HEADER_NAME = "X-Zimbra-Unknown-Header";

	private static final Map<String,KeyValue[]> responseHeaders = Collections.synchronizedMap(new HashMap<String,KeyValue[]>());
	private static final KeyValue[] NO_HEADERS = {};

	private static boolean isAlreadyFiltering = false;

	//
	// Data
	//

	protected boolean isResponseHeadersEnabled = true;

	private Log logger;

	//
	// Constructors
	//

	public SetHeaderFilter() {
		this(ZimbraLog.misc);
	}
	protected SetHeaderFilter(Log logger) {
		this.logger = logger;
	}

	//
	// Filter methods
	//

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
	throws IOException, ServletException {
		if (this.doFilter(request, response)) {
			chain.doFilter(request, response);
		}
	}

	public void init(FilterConfig filterConfig) throws ServletException {
		String s = filterConfig.getInitParameter(P_RESPONSE_HEADERS_ENABLED);
		if (s != null) {
			this.isResponseHeadersEnabled = Boolean.parseBoolean(s.trim().toLowerCase());
		}
	}

	public void destroy() { }

	//
	// Protected methods
	//

	public boolean doFilter(ServletRequest request, ServletResponse response)
	throws IOException, ServletException {
		this.addZimbraResponseHeaders(request, response);
		return true;
	}

	protected void addZimbraResponseHeaders(ServletRequest request, ServletResponse response)
	throws IOException, ServletException {
		if (!this.isResponseHeadersEnabled) return;
		try {
			HttpServletRequest httpRequest = (HttpServletRequest)request;
			HttpServletResponse httpResponse = (HttpServletResponse)response;

			String serverName = getServerName(httpRequest);
			KeyValue[] headers = getResponseHeaders(serverName);
			if (headers != null) {
				this.addHeaders(httpResponse, headers);
			}
		}
		finally {
			isAlreadyFiltering = false;
		}
	}

	protected String getServerName(HttpServletRequest request) {
		return HttpUtil.getVirtualHost(request);
	}

	protected KeyValue[] getResponseHeaders(String serverName) {
		KeyValue[] headers = responseHeaders.get(serverName);
		if (headers == null) {
			boolean filtering;
			synchronized (this.getClass()) {
				filtering = isAlreadyFiltering;
				isAlreadyFiltering = true;
			}
			if (!filtering) {
				headers = NO_HEADERS;
				try {
					SoapProvisioning provisioning = new SoapProvisioning();
					String soapUri =
						LC.zimbra_admin_service_scheme.value() +
						LC.zimbra_zmprov_default_soap_server.value() +
						':' +
						LC.zimbra_admin_service_port.intValue() +
						AdminConstants.ADMIN_SERVICE_URI
					;
					provisioning.soapSetURI(soapUri);
					Entry info = provisioning.getDomainInfo(Provisioning.DomainBy.virtualHostname, serverName);
					if (info == null) {
						info = provisioning.getConfig();
					}
					if (info != null) {
						String[] values = info.getMultiAttr(ZAttrProvisioning.A_zimbraResponseHeader, true);
						headers = new KeyValue[values.length];
						for (int i = 0; i < values.length; i++) {
							String value = values[i];
							Matcher matcher = RE_HEADER.matcher(value);
							if (matcher.matches()) {
								headers[i] = new KeyValue(matcher.group(1), matcher.group(2));
							}
							else {
								headers[i] = new KeyValue(value);
							}
						}
					}
				}
				catch (Exception e) {
					this.error("Unable to get domain config", e);
				}
				responseHeaders.put(serverName, headers);
			}
		}
		return headers;
	}

	protected void addHeaders(HttpServletResponse response, KeyValue[] headers) {
		if (headers == null) return;
		for (KeyValue header : headers) {
			this.addHeader(response, header);
		}
	}

	protected void addHeader(HttpServletResponse response, KeyValue header) {
		response.addHeader(header.key, header.value);
	}

	protected boolean isDebugEnabled() {
		return this.logger.isDebugEnabled();
	}
	protected boolean isWarnEnabled() {
		return this.logger.isWarnEnabled();
	}
	protected boolean isErrorEnabled() {
		return this.logger.isErrorEnabled();
	}

	protected void debug(String message) {
		this.logger.debug(message);
	}
	protected void warn(String message) {
		this.logger.warn(message);
	}
	protected void error(String message, Throwable t) {
		this.logger.error(message, t);
	}

	//
	// Classes
	//

	protected static class KeyValue {
		// Data
		public String key;
		public String value;
		// Constructors
		public KeyValue(String value) {
			this(SetHeaderFilter.UNKNOWN_HEADER_NAME, value);
		}
		public KeyValue(String key, String value) {
			this.key = key;
			this.value = value;
		}
	}

} // class SetHeaderFilter
