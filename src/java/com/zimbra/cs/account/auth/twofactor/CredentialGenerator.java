package com.zimbra.cs.account.auth.twofactor;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Base64;

import com.zimbra.common.auth.twofactor.CredentialConfig;
import com.zimbra.common.auth.twofactor.CredentialConfig.Encoding;
import com.zimbra.common.service.ServiceException;

public class CredentialGenerator {
    private CredentialConfig config;

    public CredentialGenerator(CredentialConfig config) {
        this.config = config;
    }

    protected byte[] generateBytes(int n) throws ServiceException {
        byte[] bytes = new byte[n];
        try {
			SecureRandom.getInstance("SHA1PRNG").nextBytes(bytes);
		} catch (NoSuchAlgorithmException e) {
			throw ServiceException.FAILURE("error generating random bytes", e);
		}
        return bytes;
    }

    private byte[] mask(byte[] bytes) {
        byte[] masked = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            masked[i] = (byte) (bytes[i] & 0x7F);
        }
        return masked;
    }

    public TOTPCredentials generateCredentials() throws ServiceException {
        byte[] secretBytes = generateBytes(config.getBytesPerSecret());
        String encoded = encodeBytes(mask(secretBytes), config.getEncoding());
        List<String> scratchCodes = generateScratchCodes();
        return new TOTPCredentials(encoded, scratchCodes);
    }

    public List<String> generateScratchCodes() throws ServiceException {
        Set<String> scratchCodeSet = new HashSet<String>();
        while (scratchCodeSet.size() < config.getNumScratchCodes()) {
            scratchCodeSet.add(generateScratchCode());
        }
        List<String> scratchCodes = new ArrayList<String>(scratchCodeSet.size());
        scratchCodes.addAll(scratchCodeSet);
        return scratchCodes;
    }

    private String generateScratchCode() throws ServiceException {
        byte[] randomBytes = generateBytes(config.getBytesPerScratchCode());
        return encodeBytes(mask(randomBytes), config.getScratchCodeEncoding());
    }

    protected String encodeBytes(byte[] bytes, Encoding encoding) {
        byte[] encoded;
        switch (encoding) {
            case BASE32:
                encoded = new Base32().encode(bytes);
                return new String(encoded).toUpperCase();
            case BASE64:
                encoded = Base64.encodeBase64(bytes);
                return new String(encoded).toUpperCase();
            default:
                return null;
        }
    }
}
