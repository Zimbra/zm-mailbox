package com.zimbra.cs.util.s3;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.util.ZimbraLog;

public class S3Connection {

    public static boolean connect(String url, String bucketName, String regionName, String awsAccessKey,
            String awsSecretKey) {
        URL endpointUrl;
        boolean s3Connection = false;

        try {
            endpointUrl = new URL(url + "/" + bucketName);

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("x-amz-content-sha256", AWS4SignerBase.EMPTY_BODY_SHA256);

            AWS4SignerForAuthorizationHeader signer = new AWS4SignerForAuthorizationHeader(endpointUrl, "HEAD", "s3",
                    regionName);
            String authorization = signer.computeSignature(headers, null, AWS4SignerBase.EMPTY_BODY_SHA256,
                    awsAccessKey, awsSecretKey);

            headers.put("Authorization", authorization);
            s3Connection = HttpUtils.invokeHttpRequest(endpointUrl, "HEAD", headers);

        } catch (MalformedURLException e) {
            ZimbraLog.mailbox.warn("Unable to parse service endpoint: " + e.getMessage());
        }

        return s3Connection;
    }
}
