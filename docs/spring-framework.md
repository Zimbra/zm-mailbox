# Use of Spring Framework

[Spring Framework](http://spring.io) is used to construct singletons, and to auto-wire their references to others.

It is also used for:

  * AMQP and RabbitMQ support, provided by [Spring AMQP](http://projects.spring.io/spring-amqp/). See also: [Messaging with RabbitMQ](http://spring.io/guides/gs/messaging-rabbitmq/).
  * The ActiveSync module.

# Application Context

An `ApplicationContext` is used by the server and CLI tools to construct and manage the lifecycle of singletons. See `com.zimbra.cs.util.Zimbra.java:initAppContext()`.

# Configuration

A Spring configuration class (a class annotated with `@org.springframework.context.annotation.Configuration`) is used to programmatically construct singletons. Zimbra's main configuration class is `com.zimbra.cs.util.ZimbraConfig.java`.

The `ZimbraConfig` class is used to conditionally construct Spring beans depending on whether a given service is installed, or whether a given backing service such as Redis is reachable during start-up.

If any bean construction method throws an exception, start-up of the server or CLI tool fails. To prevent this when a singleton is optional, some bean factory methods return null.

For example, if Zimbra is configured with a `Redis` URL, the `ZimbraConfig` class might attempt to connect to Redis during the construction of `SharedDeliveryCoordinator` bean. If `Redis` can't be reached, a `Memcached` or `Local` (in-process) alternative adapter will be selected.

# Unit Testing

Some unit tests provide a specalized configuration so that mock singletons are used in place of the normal ones that would have been created by the `ZimbraConfig` class. See `MemcachedFoldersAndTagsCacheTest.java` as an example:

```
/**
 * Unit test for {@link MemcachedFoldersAndTagsCache}.
 */
public final class MemcachedFoldersAndTagsCacheTest extends AbstractFoldersAndTagsCacheTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer(MockStoreManager.class, "", LocalConfig.class);
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    /**
     * Use local (in-mem) adapters for everything possible (LocalCachingZimbraConfig), to allow
     * this test to have dedicated use of memcached.
     **/
    @Configuration
    static class LocalConfig extends LocalCachingZimbraConfig {
        @Override
        public ZimbraMemcachedClientConfigurer memcachedClientConfigurer() {
            return new MemcachedOnLocalhostZimbraMemcachedClientConfigurer();
        }
    }
    ...
}
```
