# Use of memcached

`memcached` is used as an out-of-process cache, in order to reduce the memory requirements of the main Java process, and in order to increase the capacity of what can be cached to reduce the load on the database.

A `memcached` instance can be shared by multiple Zimbra nodes. Keys always include an `account-id`, when appropriate, to ensure that any `memcached` instance works as a shared resource.

# What about Redis? #

`Redis` is also supported in the codebase, and is meant to complement, rather than replace, `memcached`. You can configure your system to use one or the other, but ideally you'll use both.

  * `memcached` works better as a cache, and is good at expiring out least-recently-used content.
  * Redis is better as a general purpose service to store some state, or to coordinate distributed operations. It is great for incrementing counters, storing lists of sessions, or keeping maps in memory where both primary and secondary indexes need to be maintaned atomically.

# Memcached Cache Adapters & Auto-wiring #

Wherever `memcached` is used in the system, a Memcached adapter is written that implements a pure interface, and alternative Local (in-memory) and Redis adapter alternatives are always provided as well.

The alternatives exist not only to ensure test coverage, but also to enable the server to be configured with `memcached` or Redis support disabled.

An example:

  1. **MemcachedMailboxDataCache.java** (1st choice)
  2. **RedisMailboxDataCache.java** (2nd choice - works well but could fail on a put if Redis is configured with a small memory cap like 4 MB that doesn't fit all the mailbox data; `memcached` would have never failed on a put, it would have expired something to make room)
  3. **LocalMailboxDataCache.java** (the fallback adapter that performs legacy in-process based caching)
  4. **RedisClusterMailboxDataCache.java** (experimental)

The ZimbraConfig class performs auto-wiring of Redis, `memcached`, and Local adapters during Spring application context initialization during the server start-up.

  * For some cache adapter interfaces, `memcached` is the first choice, Redis is the fallback, and Local (in-process memory) is the last resort.
  * For other concurrency management interfaces, Redis is the first choice, `memcached` is the fallback, and Local (in-process memory) is the last resort.

Ideally, every installation will have both `memcached` and Redis configured and running, so that the system start-up can choose and auto-wire the best adapter for each individual job.

# Testing

Run all memcached-based unit tests:

```
$ cd ZimbraServer
$ mvn test -Dtest=Memcache*
```

Tests will be gracefully skipped if the required backing service is not running on localhost.

# Configure for memcached (dev)

Enable use of memcached:

```
$ cd ZimbraServer
$ ant reset-all
$ zmprov ms `zmhostname` zimbraMemcachedClientServerList localhost
$ jetty restart
```

Disable use of memcached:

```
$ zmprov ms `zmhostname` zimbraMemcachedClientServerList ""
$ jetty restart
```
