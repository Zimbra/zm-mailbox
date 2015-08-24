# Use of Redis #

`Redis` is used to store shared state for coordination of distributed operations.

Storing state externally enables single-server installs to scale out to multi-server installs without reconfiguration of the 1st server. It also relieves memory pressure on the main process, trading it for some network I/O. See PHP for clarification! Or any Heroku app.

# What about Memcached? #

Use of Redis is not intended to replace use of `Memcached`. You can configure your system to use one or the other, but ideally you'll use both.

  * `Memcached` works better as a cache, and is good at expiring out least-recently-used content.
  * Redis is better as a general purpose service to store some state, or to coordinate distributed operations. It is great for incrementing counters, storing lists of sessions, or keeping maps in memory where both primary and secondary indexes need to be maintaned atomically.

# What about Zookeeper? #

Use of `Zookeeper` made a brief appearance in the codebase for some early proof-of-concept work. It was removed & replaced with Redis, after the overall architecture became inspired by the [Heroku 12-factor methodology](http://12factor.net), which strives towards a single service formation for all installs; whether a single-server install for a small business or a 400-node multi-server install. The heavy memory footprint of Java-based `Zookeeper` was never going to work for the single-server installs, whereas Redis works well in both contexts. This maximizes [dev/prod parity](http://12factor.net/dev-prod-parity), a significant driver of improved test coverage and quality.

# Redis Cluster vs Master/Slave+Sentinel #

In most places where a Redis adapter exists, a RedisCluster adapter also exists, and uses the more restrictive semantics of `Redis Cluster` to perform the given function. Use of `Redis Cluster`, however, is NOT currently supported, since it is not yet clear how Lua-based libraries like [Zimbra qless-java](https://github.com/Zimbra/qless-java) are going to work in a cluster context.

So use of Redis, or Redis Master/Slave+Sentinel for HA, is assumed.

# What is Zimbra qless-java? #

[Zimbra qless-java](https://github.com/Zimbra/qless-java) is our Java port of the Ruby-based [qless](https://github.com/seomoz/qless) job tracking library. It works with Redis or Redis Master/Slave+Sentinel. It probably doesn't work with Redis Cluster.

Zimbra does not yet depend on `qless-java`, or use a `job tracking` service yet, but it's a possibility and solves a real need as the overall system becomes more distributed. Lots of long jobs exist in the system, and don't have adequate visibility (UI) or a managment interface (pause/cancel) yet: HSM blob moves, mailbox moves, backups, restores, reindex jobs, etc.

Qless-based Redis adapters exist in the codebase, complete with unit test coverage, and they run if you have Redis installed locally in your dev environment.

# Redis Adapters & Auto-wiring #

Wherever Redis is used in the system, a Redis adapter is written that implements a pure interface, and alternative Local (in-memory) and Memcached adapter alternatives are always provided as well.

The alternatives exist not only to ensure test coverage, but also to enable the server to be configured with memcached or Redis support disabled.

An example:

  1. **RedisSharedDeliveryCoordinator.java** (1st choice)
  2. **MemcachedSharedDeliveryCoordinator.java** (2nd choice - works most of the time, but is less efficient, and could malfunction if the cache is under heavy pressure and is expiring things out within a few seconds)
  3. **LocalSharedDeliveryCoordinator.java** (the fallback legacy adapter that performs in-process based synchronization)
  4. **RedisQlessSharedDeliveryCoordinator.java** (experimental)

The ZimbraConfig class performs auto-wiring of Redis, Memcached, and Local adapters during Spring application context initialization during the server start-up.

  * For some cache adapter interfaces, Memcached is the first choice, Redis is the fallback, and Local (in-process memory) is the last resort.
  * For other concurrency management interfaces, Redis is the first choice, Memcached is the fallback, and Local (in-process memory) is the last resort.

Ideally, every installation will have both memcached and Redis configured and running, so that the system start-up can choose and auto-wire the best adapter for each individual job.

# Testing

Run all Redis-based unit tests:

```
$ cd ZimbraServer
$ mvn test -Dtest=Redis*
```

Run all Redis Cluster-based unit tests:

```
$ cd ZimbraServer
$ mvn test -Dtest=RedisCluster*
```

Tests will be gracefully skipped if the required backing service is not running on localhost.

# Configure for Redis (dev)

Enable use of Redis:

```
$ cd ZimbraServer
$ ant reset-all
$ zmprov ms `zmhostname` zimbraRedisUrl redis://localhost:6379
$ jetty restart
```

Disable use of Redis:

```
$ zmprov ms `zmhostname` -zimbraRedisUrl
$ jetty restart
```

# Configure for Redis Cluster (dev)

Enable use of Redis Cluster:

```
$ cd ZimbraServer
$ ant reset-all
$ zmprov ms `zmhostname` zimbraRedisUrl redis://localhost:7000
$ zmprov ms `zmhostname` +zimbraRedisUrl redis://localhost:7001
$ zmprov ms `zmhostname` +zimbraRedisUrl redis://localhost:7002
$ zmprov ms `zmhostname` +zimbraRedisUrl redis://localhost:7003
$ zmprov ms `zmhostname` +zimbraRedisUrl redis://localhost:7004
$ zmprov ms `zmhostname` +zimbraRedisUrl redis://localhost:7005
$ jetty restart
```
