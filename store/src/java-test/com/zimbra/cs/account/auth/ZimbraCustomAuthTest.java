package com.zimbra.cs.account.auth;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.zimbra.cs.account.Account;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for {@link ZimbraCustomAuth} — the registry and default behaviour.
 *
 * The static {@code mHandlers} map is lazily initialised and shared across
 * tests; the {@code static} initializer in {@code ZimbraCustomAuth} registers
 * "hosted" automatically, so we work around name collisions by using unique
 * handler names per test.
 *
 * No mocks required — ZimbraLog calls are benign.
 */
public class ZimbraCustomAuthTest {

    /**
     * Minimal concrete subclass used as a test fixture.
     */
    private static class StubAuth extends ZimbraCustomAuth {
        boolean authenticateCalled;
        boolean shouldThrow;

        @Override
        public void authenticate(Account acct, String password,
                Map<String, Object> context, List<String> args) throws Exception {
            if (shouldThrow) {
                throw new Exception("stub auth failure");
            }
            authenticateCalled = true;
        }
    }

    private StubAuth stub;

    @Before
    public void setUp() {
        stub = new StubAuth();
    }

    // =========================================================================
    // register + getHandler
    // =========================================================================

    @Test
    public void register_newHandler_canBeRetrievedByName() {
        String name = "test-handler-" + System.nanoTime();
        ZimbraCustomAuth.register(name, stub);
        assertSame(stub, ZimbraCustomAuth.getHandler(name));
    }

    @Test
    public void getHandler_unknownName_returnsNull() {
        assertNull(ZimbraCustomAuth.getHandler("this-handler-does-not-exist-xyz"));
    }

    @Test
    public void register_duplicateName_firstRegistrationWins() {
        String name = "dup-handler-" + System.nanoTime();
        StubAuth first  = new StubAuth();
        StubAuth second = new StubAuth();

        ZimbraCustomAuth.register(name, first);
        ZimbraCustomAuth.register(name, second); // duplicate — should be ignored

        assertSame("first registration must be kept", first, ZimbraCustomAuth.getHandler(name));
    }

    @Test
    public void register_multipleDistinctNames_allRetrievable() {
        long ts = System.nanoTime();
        String nameA = "handler-a-" + ts;
        String nameB = "handler-b-" + ts;
        StubAuth authA = new StubAuth();
        StubAuth authB = new StubAuth();

        ZimbraCustomAuth.register(nameA, authA);
        ZimbraCustomAuth.register(nameB, authB);

        assertSame(authA, ZimbraCustomAuth.getHandler(nameA));
        assertSame(authB, ZimbraCustomAuth.getHandler(nameB));
    }

    // =========================================================================
    // Static initializer: "hosted" handler is pre-registered
    // =========================================================================

    @Test
    public void staticInit_hostedHandlerIsRegistered() {
        assertNotNull("'hosted' handler must be auto-registered", ZimbraCustomAuth.getHandler("hosted"));
    }

    // =========================================================================
    // checkPasswordAging — default implementation
    // =========================================================================

    @Test
    public void checkPasswordAging_defaultReturnsFalse() {
        assertFalse(stub.checkPasswordAging());
    }

    // =========================================================================
    // authenticate — delegated to concrete subclass
    // =========================================================================

    @Test
    public void authenticate_success_doesNotThrow() throws Exception {
        stub.authenticate(null, "pass", null, Arrays.asList("arg1"));
        // If no exception thrown, authentication succeeded per contract
    }

    @Test(expected = Exception.class)
    public void authenticate_failure_throwsException() throws Exception {
        stub.shouldThrow = true;
        stub.authenticate(null, "pass", null, Arrays.asList("arg1"));
    }
}
