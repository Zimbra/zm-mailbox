package com.zimbra.cs.util.s3;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import com.zimbra.common.util.ZimbraLog;

/**
 * Various Http helper routines
 */
public class HttpUtils {

    /**
     * Makes a http request to the specified endpoint
     */
    public static boolean invokeHttpRequest(URL endpointUrl, String httpMethod, Map<String, String> headers) {
        HttpURLConnection connection = null;
        boolean status = false;
        ZimbraLog.mailbox.info("==================");
        try {
            ZimbraLog.mailbox.info("endpointUrl: " + endpointUrl);
            ZimbraLog.mailbox.info("httpMethod: " + httpMethod);
            ZimbraLog.mailbox.info("headers: " + headers);
            connection = createHttpConnection(endpointUrl, httpMethod, headers);
            ZimbraLog.mailbox.info("httpStatus: " + connection.getResponseCode());
            if(connection.getResponseCode() == 200) {
                status = true;
            }
        } catch (Exception e) {
            ZimbraLog.mailbox.warn("Request failed. " + e.getMessage(), e);
        }
        ZimbraLog.mailbox.info("==================");
        return status;
    }

    public static HttpURLConnection createHttpConnection(URL endpointUrl, String httpMethod,
            Map<String, String> headers) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) endpointUrl.openConnection();
            connection.setRequestMethod(httpMethod);
            if (headers != null) {
                ZimbraLog.mailbox.info(">>> Request headers:");
                for (String headerKey : headers.keySet()) {
                    ZimbraLog.mailbox.info(headerKey + ": " + headers.get(headerKey));
                    connection.setRequestProperty(headerKey, headers.get(headerKey));
                }
            }
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(false);
        } catch (Exception e) {
            ZimbraLog.mailbox.warn("Cannot create connection. " + e.getMessage(), e);
        }
        return connection;
    }

    public static String urlEncode(String url, boolean keepPathSlash) {
        String encoded = null;
        try {
            encoded = URLEncoder.encode(url, "UTF-8");
            if (keepPathSlash) {
                encoded = encoded.replace("%2F", "/");
            }
        } catch (UnsupportedEncodingException e) {
            ZimbraLog.mailbox.warn("UTF-8 encoding is not supported.", e);
        }
        return encoded;
    }
}
