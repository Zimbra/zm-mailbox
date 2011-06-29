/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.common.filters;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.zimbra.common.util.MapUtil;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import java.util.regex.Pattern;

public class GZIPFilter implements Filter {


    private static final String P_COMPRESSABLE_MIME_TYPES = "compressableMimeTypes";
    private static final String P_NO_COMPRESSION_USER_AGENTS = "noCompressionUserAgents";
    private static final String P_USER_AGENT_CACHE_SIZE = "userAgentCacheSize";

    /*
      compression="on"
      compressionMinSize="1024"
      compressableMimeType="text/html,text/plain,text/css"
      noCompressionUserAgents=".*MSIE 6.*"
     */
    private String[] mCompressableMimeTypes;
    private List<Pattern> mNoCompressionUserAgents;
    private Map mUAMap;

    public void init(FilterConfig filterConfig) {
        mNoCompressionUserAgents = new ArrayList<Pattern>();

        String mimeTypes = filterConfig.getInitParameter(P_COMPRESSABLE_MIME_TYPES);
        mCompressableMimeTypes = mimeTypes != null ? mimeTypes.split(",") : new String[0];

        String badUA = filterConfig.getInitParameter(P_NO_COMPRESSION_USER_AGENTS);
        if (badUA != null) {
            for (String ua: badUA.split(",")) {
                mNoCompressionUserAgents.add(Pattern.compile(ua, Pattern.CASE_INSENSITIVE));
            }
        }

        int cacheSize = 20;
        String cacheSizeStr = filterConfig.getInitParameter(P_USER_AGENT_CACHE_SIZE);
        if (cacheSizeStr != null)
        	cacheSize = Integer.parseInt(cacheSizeStr);
        mUAMap = MapUtil.newLruMap(cacheSize);
    }

    public void destroy() { }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
         if (!(req instanceof HttpServletRequest)) {
             chain.doFilter(req, res);
             return;
         }

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (isCompressable(request)) {
            GZIPResponseWrapper wrappedResponse =
                    new GZIPResponseWrapper(response);
            chain.doFilter(req, wrappedResponse);
            wrappedResponse.finishResponse();
        } else {
            chain.doFilter(req, res);
        }
    }

    boolean isCompressable(HttpServletRequest request) {
    	String ae        = request.getHeader("accept-encoding");
    	String userAgent = request.getHeader("user-agent");
    	if (ae == null || ae.indexOf("gzip") == -1)
    		return false;
    	if (mNoCompressionUserAgents.isEmpty() || userAgent == null)
    		return true;

    	String cachedResult = (String) mUAMap.get(userAgent);
    	if (cachedResult != null)
    		return cachedResult.equals("yes");
    	for (Pattern p : mNoCompressionUserAgents) {
    		if (p.matcher(userAgent).matches()) {
    			synchronized (mUAMap) {
    				mUAMap.put(userAgent, "no");
    			}
    			return false;
    		}
    	}
    	synchronized (mUAMap) {
    		mUAMap.put(userAgent, "yes");
    	}
    	return true;
    }

    boolean isCompressable(String ct) {
        if (mCompressableMimeTypes == null || mCompressableMimeTypes.length == 0)
            return true;
        else if (ct != null) {
            for (String compCT :  mCompressableMimeTypes)
                if (ct.startsWith(compCT)) return true;
        }
        return false;
    }

    public class GZIPResponseWrapper extends HttpServletResponseWrapper {

        private HttpServletResponse mResponse = null;
        private ServletOutputStream mOutput = null;
        private PrintWriter mWriter = null;
        private boolean mCompress = false;

        public GZIPResponseWrapper(HttpServletResponse httpServletResponse) {
            super(httpServletResponse);
            mResponse = httpServletResponse;
            checkCompress(mResponse.getContentType());
        }

        public void checkCompress(String ct) {
            if (!mCompress) {
                mCompress = isCompressable(ct);
                if (mCompress)
                    mResponse.setHeader("Content-Encoding", "gzip");
            }
        }

        public void setContentType(String ct) {
            checkCompress(ct);
            super.setContentType(ct);
        }

        void finishResponse() throws IOException {
            if (mWriter != null)
                mWriter.close();
            else if (mOutput != null)
                mOutput.close();
        }

        public void flushBuffer() throws IOException {
            if (mWriter != null)
                mWriter.flush();
            else if (mOutput != null)
                mOutput.flush();
        }

        public ServletOutputStream getOutputStream() throws IOException {
            if (mOutput == null) {
                if (mWriter != null)
                    throw new IllegalStateException("getWriter() has already been called!");
                mOutput = mCompress ? new GZIPResponseStream(mResponse) : mResponse.getOutputStream();
            }
            return mOutput;
        }

        public PrintWriter getWriter() throws IOException {
            if (mWriter == null)  {
                if (mOutput != null)
                    throw new IllegalStateException("getOutputStream() has already been called!");
                mWriter = mCompress ?
                        new PrintWriter(new OutputStreamWriter(new GZIPResponseStream(mResponse), mResponse.getCharacterEncoding())) :
                        mResponse.getWriter();
            }
            return mWriter;
        }

        public void setContentLength(int length) {}

    }

    public static class GZIPResponseStream extends ServletOutputStream {
        protected GZIPOutputStream mOutput = null;
        protected HttpServletResponse mResponse = null;

        public GZIPResponseStream(HttpServletResponse response) throws IOException {
            super();
            mResponse = response;
            mOutput  = new GZIPOutputStream(mResponse.getOutputStream(), 8192);
        }
        
        public void flush() throws IOException {
            mOutput.flush();
        }

        public void write(int b) throws IOException {
            byte[] buff = new byte[1];
            buff[0] = (byte)(b & 0xff);
            write(buff, 0, 1);
        }

        public void write(byte b[]) throws IOException {
            write(b, 0, b.length);
        }

        public void write(byte b[], int off, int len) throws IOException {
            mOutput.write(b, off, len);
        }

        public void close() throws IOException {
            mOutput.close();
        }
    }
}
