package com.zimbra.cs.account.auth.twofactor;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Base64;

import com.zimbra.cs.account.auth.twofactor.CredentialConfig.Encoding;

public class CredentialGenerator {
    private CredentialConfig config;

    public CredentialGenerator(CredentialConfig config) {
        this.config = config;
    }

    protected byte[] generateBytes(int n) {
        byte[] bytes = new byte[n];
        new Random().nextBytes(bytes);
        return bytes;
    }

    private byte[] mask(byte[] bytes) {
        byte[] masked = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            masked[i] = (byte) (bytes[i] & 0x7F);
        }
        return masked;
    }

    public TOTPCredentials generateCredentials() {
        byte[] secretBytes = generateBytes(config.getBytesPerSecret());
        String encoded = encodeBytes(mask(secretBytes), config.getEncoding());
        Set<String> scratchCodes = generateScratchCodes();
        return new TOTPCredentials(encoded, scratchCodes);
    }

    protected Set<String> generateScratchCodes() {
        Set<String> scratchCodes = new HashSet<String>();
        while (scratchCodes.size() < config.getNumScratchCodes()) {
            scratchCodes.add(generateScratchCode());
        }
        return scratchCodes;
    }

    private String generateScratchCode() {
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
                return new String(encoded);
            default:
                return null;
        }
    }
}
