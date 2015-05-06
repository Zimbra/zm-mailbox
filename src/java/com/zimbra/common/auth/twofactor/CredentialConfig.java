package com.zimbra.common.auth.twofactor;

public class CredentialConfig {
    private int secretLength;
    private Encoding secretEncoding;
    private int scratchCodeLength;
    private Encoding scratchCodeEncoding;
    private int numScratchCodes;


    public enum Encoding {
        BASE32, BASE64;
    }

    public CredentialConfig setEncoding(Encoding encoding) {
        this.secretEncoding = encoding;
        return this;
    }

    public CredentialConfig setScratchCodeEncoding(Encoding encoding) {
        this.scratchCodeEncoding = encoding;
        return this;
    }

    public CredentialConfig setNumScratchCodes(int n) {
        numScratchCodes = n;
        return this;
    }

    public CredentialConfig setScratchCodeLength(int n) {
        scratchCodeLength = n;
        return this;
    }

    public CredentialConfig setSecretLength(int n) {
        secretLength = n;
        return this;
    }

    public int getSecretLength() {
        return secretLength;
    }

    public int getNumScratchCodes() {
        return numScratchCodes;
    }

    public int getScratchCodeLength() {
        return scratchCodeLength;
    }

    public Encoding getEncoding() {
        return secretEncoding;
    }

    public Encoding getScratchCodeEncoding() {
        return scratchCodeEncoding;
    }

    public int getBytesPerSecret() {
        return getBytesPerCodeLength(secretEncoding, secretLength);
    }

    private static int getBytesPerCodeLength(Encoding encoding, int n) {
        switch(encoding) {
            case BASE32:
                return (n/ 8) * 5;
            case BASE64:
                return (n / 4) * 3;
            default:
                return 0;
        }
    }

    public int getBytesPerScratchCode() {
        return getBytesPerCodeLength(scratchCodeEncoding, scratchCodeLength);
    }
}
