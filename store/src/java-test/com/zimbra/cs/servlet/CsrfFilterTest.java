package com.zimbra.cs.servlet;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.google.common.cache.LoadingCache;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Test class for {@link CsrfFilter} domain and global CSRF allowed referer hosts configuration.
 */
public class CsrfFilterTest {

    private Provisioning prov;

    private static String[] originalGlobalAllowedHosts;

    private static final String[] SINGLE_HOST = new String[] {"oidc-idp01.example.local" };

    private static final String[] MULTIPLE_HOSTS = new String[] {"idp01.example.local", "idp02.example.local",
            "idp03.example.local" };

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();

        final Provisioning provisioning = Provisioning.getInstance();

        // Preserve original global config to restore after test suite completion
        originalGlobalAllowedHosts = provisioning.getConfig().getCsrfAllowedRefererHosts();

        final Map<String, Object> singleHostAttr = new HashMap<>();
        singleHostAttr.put(Provisioning.A_zimbraCsrfAllowedRefererHosts, SINGLE_HOST);

        final Map<String, Object> multipleHostAttr = new HashMap<>();
        multipleHostAttr.put(Provisioning.A_zimbraCsrfAllowedRefererHosts, MULTIPLE_HOSTS);

        // Setup test domains
        provisioning.createDomain("example.local", singleHostAttr);
        provisioning.createDomain("zimbra.com", multipleHostAttr);
        provisioning.createDomain("empty.example.local", new HashMap<>());
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if (originalGlobalAllowedHosts != null) {
            Provisioning.getInstance().getConfig().setCsrfAllowedRefererHosts(originalGlobalAllowedHosts);
        }
    }

    @Before
    public void setup() throws Exception {
        prov = Provisioning.getInstance();
        // Reset global config before each test method for test isolation
        prov.getConfig().setCsrfAllowedRefererHosts(new String[0]);
    }

    /**
     * Test domain-level configuration with a single allowed referer host.
     */
    @Test
    public void testDomainLevelSingleHostConfig() throws Exception {
        final CsrfFilter filter = new CsrfFilter();
        filter.init(null);

        final Set<String> expected = new HashSet<>(Arrays.asList(SINGLE_HOST));
        final Set<String> actual = getDomainAllowedHosts(filter, "example.local");

        Assert.assertEquals(expected, actual);
    }

    /**
     * Test domain-level configuration with multiple allowed referer hosts.
     */
    @Test
    public void testDomainLevelMultipleHostsConfig() throws Exception {
        final CsrfFilter filter = new CsrfFilter();
        filter.init(null);

        final Set<String> expected = new HashSet<>(Arrays.asList(MULTIPLE_HOSTS));
        final Set<String> actual = getDomainAllowedHosts(filter, "zimbra.com");

        Assert.assertEquals(expected, actual);
    }

    /**
     * Test domain-level configuration for a domain with no configured allowed hosts.
     */
    @Test
    public void testDomainLevelEmptyConfig() throws Exception {
        final CsrfFilter filter = new CsrfFilter();
        filter.init(null);

        final Set<String> actual = getDomainAllowedHosts(filter, "empty.example.local");

        Assert.assertTrue(actual.isEmpty());
    }

    /**
     * Test global-level configuration loading.
     */
    @Test
    public void testGlobalLevelHostConfig() throws Exception {
        prov.getConfig().setCsrfAllowedRefererHosts(MULTIPLE_HOSTS);

        final CsrfFilter filter = new CsrfFilter();
        filter.init(null);

        final Set<String> expected = new HashSet<>(Arrays.asList(MULTIPLE_HOSTS));
        final Set<String> actual = getGlobalAllowedHosts(filter);

        Assert.assertEquals(expected, actual);
    }

    /**
     * Test query for a non-existent domain falling back safely to empty set.
     */
    @Test
    public void testNonExistentDomainLookup() throws Exception {
        final CsrfFilter filter = new CsrfFilter();
        filter.init(null);

        final Set<String> actualDomain = getDomainAllowedHosts(filter, "nonexist.example.local");

        Assert.assertTrue(actualDomain.isEmpty());
    }

    @SuppressWarnings("unchecked")
    private Set<String> getGlobalAllowedHosts(CsrfFilter filter) throws Exception {
        final Field field = CsrfFilter.class.getDeclaredField("allowedRefHostsSet");
        field.setAccessible(true);
        return (Set<String>) field.get(filter);
    }

    @SuppressWarnings("unchecked")
    private Set<String> getDomainAllowedHosts(CsrfFilter filter, String virtualHost) throws Exception {
        final Field field = CsrfFilter.class.getDeclaredField("domainAllowedRefHosts");
        field.setAccessible(true);
        final LoadingCache<String, Set<String>> cache = (LoadingCache<String, Set<String>>) field.get(filter);
        return cache.get(virtualHost);
    }
}
