package com.zimbra.cs.account.auth.ropc;

/**
 * ROPC handler implementation for Okta.
 */
public class OktaRopcHandler implements IRopcHandler {

    public static final String NAME = "okta";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean authenticate(IRopcAuthRequest request) {
        // Stub implementation for this iteration.
        return true;
    }
}
