package com.zimbra.cs.account.auth.ropc;

/**
 * Contract for all ROPC provider handlers.
 */
public interface IRopcHandler {

    /**
     * Returns the unique provider name/type for this handler.
     * okta, keycloak
     *  @return provider name
     */
    String getName();

    /**
     * Authenticates the user via ROPC flow.
     * @param request the authentication request
     * @return true if authentication is successful, false otherwise   ← this line was added
     */
    boolean authenticate(IRopcAuthRequest request);
}
