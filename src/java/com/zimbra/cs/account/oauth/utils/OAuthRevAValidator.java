/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.account.oauth.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthValidator;
import net.oauth.signature.OAuthSignatureMethod;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

/**
 * A simple OAuthValidator for OAuth 1.0 Rev.A, which checks the version, whether the timestamp
 * is close to now and the signature is valid. Each check may be overridden.
 *
 * @author Yutaka Obuchi
 */
public class OAuthRevAValidator implements OAuthValidator {

    /** The default window for timestamps is 5 minutes. */
    public static final long DEFAULT_TIMESTAMP_WINDOW = 5 * 60 * 1000L;

    private static final Log LOG = ZimbraLog.oauth;

    /**
     * Names of parameters that may not appear twice in a valid message.
     * This limitation is specified by OAuth Core <a
     * href="http://oauth.net/core/1.0#anchor7">section 5</a>.
     */
    public static final Set<String> SINGLE_PARAMETERS = constructSingleParameters();

    private static Set<String> constructSingleParameters() {
        Set<String> s = new HashSet<String>();
        for (String p : new String[] { OAuth.OAUTH_CONSUMER_KEY, OAuth.OAUTH_TOKEN, OAuth.OAUTH_TOKEN_SECRET,
                OAuth.OAUTH_CALLBACK, OAuth.OAUTH_SIGNATURE_METHOD, OAuth.OAUTH_SIGNATURE, OAuth.OAUTH_TIMESTAMP,
                OAuth.OAUTH_NONCE, OAuth.OAUTH_VERSION }) {
            s.add(p);
        }
        return Collections.unmodifiableSet(s);
    }

    /**
     * Construct a validator that rejects messages more than five minutes out
     * of date, or with a OAuth version other than 1.0, or with an invalid
     * signature.
     */
    public OAuthRevAValidator() {
        this(DEFAULT_TIMESTAMP_WINDOW, Double.parseDouble(OAuth.VERSION_1_0));
    }

    /**
     * Public constructor.
     *
     * @param timestampWindowSec
     *            specifies, in seconds, the windows (into the past and
     *            into the future) in which we'll accept timestamps.
     * @param maxVersion
     *            the maximum acceptable oauth_version
     */
    public OAuthRevAValidator(long timestampWindowMsec, double maxVersion) {
        this.timestampWindow = timestampWindowMsec;
        this.maxVersion = maxVersion;
    }

    protected final double minVersion = 1.0;
    protected final double maxVersion;
    protected final long timestampWindow;

    /** {@inherit}
     * @throws URISyntaxException */
    @Override
    public void validateMessage(OAuthMessage message, OAuthAccessor accessor)
            throws OAuthException, IOException, URISyntaxException {
        checkSingleParameters(message);
        validateVersion(message);
        validateTimestampAndNonce(message);
        validateSignature(message, accessor);
    }

    public void validateAccTokenMessage(OAuthMessage message, OAuthAccessor accessor)
            throws OAuthException, IOException, URISyntaxException {
        checkSingleParameters(message);
        LOG.debug("validateAccToken:checkShingle done.");
        validateVersion(message);
        LOG.debug("validateAccToken:validateVersion done.");
        validateTimestampAndNonce(message);
        LOG.debug("validateAccToken:validateTimestamp done.");
        validateSignature(message, accessor);
        LOG.debug("validateAccToken:validateSignature done.");
        validateVerifier(message, accessor);
        LOG.debug("validateAccToken:validateVerifier done.");
    }

    public void validateReqTokenMessage(OAuthMessage message, OAuthAccessor accessor)
            throws OAuthException, IOException, URISyntaxException {
        checkSingleParameters(message);
        validateVersion(message);
        validateTimestampAndNonce(message);
        validateSignature(message, accessor);
        validateCallback(message, accessor);
    }

    /** Throw an exception if any SINGLE_PARAMETERS occur repeatedly. */
    protected void checkSingleParameters(OAuthMessage message) throws IOException, OAuthException {
        // Check for repeated oauth_ parameters:
        boolean repeated = false;
        Map<String, Collection<String>> nameToValues = new HashMap<String, Collection<String>>();
        for (Map.Entry<String, String> parameter : message.getParameters()) {
            String name = parameter.getKey();
            if (SINGLE_PARAMETERS.contains(name)) {
                Collection<String> values = nameToValues.get(name);
                if (values == null) {
                    values = new ArrayList<String>();
                    nameToValues.put(name, values);
                } else {
                    repeated = true;
                }
                values.add(parameter.getValue());
            }
        }
        if (repeated) {
            Collection<OAuth.Parameter> rejected = new ArrayList<OAuth.Parameter>();
            for (Map.Entry<String, Collection<String>> p : nameToValues.entrySet()) {
                String name = p.getKey();
                Collection<String> values = p.getValue();
                if (values.size() > 1) {
                    for (String value : values) {
                        rejected.add(new OAuth.Parameter(name, value));
                    }
                }
            }
            OAuthProblemException problem = new OAuthProblemException(OAuth.Problems.PARAMETER_REJECTED);
            problem.setParameter(OAuth.Problems.OAUTH_PARAMETERS_REJECTED, OAuth.formEncode(rejected));
            throw problem;
        }
    }

    protected void validateVersion(OAuthMessage message)
            throws OAuthException, IOException {
        String versionString = message.getParameter(OAuth.OAUTH_VERSION);
        if (versionString != null) {
            double version = Double.parseDouble(versionString);
            if (version < minVersion || maxVersion < version) {
                OAuthProblemException problem = new OAuthProblemException(OAuth.Problems.VERSION_REJECTED);
                problem.setParameter(OAuth.Problems.OAUTH_ACCEPTABLE_VERSIONS, minVersion + "-" + maxVersion);
                throw problem;
            }
        }
    }

    /** This implementation doesn't check the nonce value. */
    protected void validateTimestampAndNonce(OAuthMessage message)
            throws IOException, OAuthProblemException {
        message.requireParameters(OAuth.OAUTH_TIMESTAMP, OAuth.OAUTH_NONCE);
        long timestamp = Long.parseLong(message.getParameter(OAuth.OAUTH_TIMESTAMP)) * 1000L;
        long now = currentTimeMsec();
        long min = now - timestampWindow;
        long max = now + timestampWindow;
        if (timestamp < min || max < timestamp) {
            OAuthProblemException problem = new OAuthProblemException(OAuth.Problems.TIMESTAMP_REFUSED);
            problem.setParameter(OAuth.Problems.OAUTH_ACCEPTABLE_TIMESTAMPS, min + "-" + max);
            throw problem;
        }
    }

    protected void validateSignature(OAuthMessage message, OAuthAccessor accessor)
            throws OAuthException, IOException, URISyntaxException {
        message.requireParameters(OAuth.OAUTH_CONSUMER_KEY,
                OAuth.OAUTH_SIGNATURE_METHOD, OAuth.OAUTH_SIGNATURE);
        OAuthSignatureMethod.newSigner(message, accessor).validate(message);
    }

    protected void validateVerifier(OAuthMessage message, OAuthAccessor accessor)
            throws OAuthException, IOException {
        String verifier = message.getParameter(OAuth.OAUTH_VERIFIER);
        if (!StringUtil.equal(verifier, (String) accessor.getProperty(OAuth.OAUTH_VERIFIER))) {
            LOG.debug("verifier from request("+verifier+") and local memory("+accessor.getProperty(OAuth.OAUTH_VERIFIER)+") should be same.");
            OAuthProblemException problem = new OAuthProblemException("invalid_verifier");
            throw problem;
        }
    }

    // Support oauth_callback here for RevA
    protected void validateCallback(OAuthMessage message, OAuthAccessor accessor)
            throws OAuthException, IOException {

        String callback = message.getParameter(OAuth.OAUTH_CALLBACK);
        if (callback != null && callback != "") {
            LOG.debug("callback is ready.");
            //if(callback=="oob"){
            //oob is not implemented yet
            //}else{
            accessor.setProperty(OAuth.OAUTH_CALLBACK, callback);
            //}
        }else{
            LOG.debug("no callbacks set!");
            OAuthProblemException problem = new OAuthProblemException("no_callback");
            throw problem;
        }
    }

    protected long currentTimeMsec() {
        return System.currentTimeMillis();
    }
}

