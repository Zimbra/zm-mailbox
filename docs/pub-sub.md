# Use of publish/subscribe

Historically, the product used SOAP for server-to-server (point-to-point or P2P) messaging.

Later, an experimental and proprietary TCP-based `iochannel` mechanism was implemented for lighter-weight server-to-server messaging.

Now, wherever server-to-server messaging is required, a pure Java interface is created to describe the communication requirements, then `Redis` and `RabbitMQ` broker-based adapters are written, as well as broker-less `0mq` and `iochannel` based adapters.

# Broker-based MOM vs Broker-less

By default, `Redis` is configured as the message-oriented middleware (MOM) broker to carry server-to-server messages. Its pub-sub mechanism is used. This should be OK for most small installations.

Other IT environments may prefer to switch to `RabbitMQ` to make use of their existing AMQP infrastructure.

Large operators of 400-server installations, however, will probably not be able to funnel all server-to-server messages through a broker pinch-point, and will need to use P2P messaging. The `Consul` service locator makes P2P messaging possible, since every message has to address its recipients, rather than just address a topic like `account-id` using broker-based MOM.

`iochannel` and `0mq` adapters are the alternatives to enable point-to-point messaging. `0mq` is the mature choice, but where adapters don't yet exist in the codebase. `iochannel` is the experimental choice where adapters have existed in the codebase for some time, but the transport library itself lacks thorough performance tuning and test coverage.

`0mq` libraries have existed in the product since the introduction of `Amavis`. This makes it a convenient choice for use by additional parts of the system that wish to perform server-to-server messaging.

# Auto-wiring on start-up

Wherever server-to-server messaging is used in the system, adapters are written for `Redis`, `RabbitMQ`, `iochannel`, and `0mq` that implements a pure Java interface.

An example:

  1. **RedisMailiboxListenerTransport.java**
  2. **AmqpMailiboxListenerTransport.java**
  3. **IOChannelMailiboxListenerTransport.java**

The ZimbraConfig class performs auto-wiring of various pub/sub-related adapters during Spring application context initialization during the server start-up. Wherever no adapter is specified, the broker-based alternatives are tried as a default, based on availability (I/O tests to detect Redis or RabbitMQ brokers).

# Mailbox Listener pub/sub

The first use of pub/sub in the system was for the MailboxListener Java EventListener class, which publishes PendingNotifications objects to other servers so that they can update their in-process caches based on the latest redolog ops or other changes that have occurred on another host.

To configure Redis as the broker to carry Mailbox Listener pub/sub traffic:

    zmprov ms `zmhostname` zimbraMailboxListenerUrl redis://[password@]redis-host:6379

To configure Redis as the broker, using the default Redis URL:

    zmprov ms `zmhostname` zimbraRedisUrl redis://localhost:6379
    zmprov ms `zmhostname` zimbraMailboxListenerUrl redis:default

To configure AMQP as the broker:

    zmprov ms `zmhostname` zimbraMailboxListenerUrl amqp://[user:pass@]rabbitmq-host:5672/

To configure Redis as the broker, and also send a copy of everything via AMQP for some external integrations that are interested in subscribing to the flow:

    zmprov ms `zmhostname` zimbraMailboxListenerUrl redis:default
    zmprov ms `zmhostname` +zimbraMailboxListenerUrl amqp://[user:pass@]rabbitmq-host:5672/

To configure `iochannel` as the transport:

	zmprov ms `zmhostname` zimbraMailboxListenerUrl iochannel://

To configure `0mq` as the transport (not implemented yet):

	zmprov ms `zmhostname` zimbraMailboxListenerUrl tcp://

Note that for the broker-less transports, the URI's only need to designate a scheme, and not also a host/port. This is because `Consul` is consulted to produce the actual list of endpoint URLs that will be communicated with.

# Why pub/sub for single-server installs?

There are several reasons why pub/sub is still used even for single-server installs:

  1. It allows you to install & configure a 2nd server, to go from a single-server to a multi-server installation, without having to revisit and reconfigure the 1st server.
  2. It ensures the product uses a single service formation for all types of installations, which drives test coverage and quality.
  3. It provides visibility into the internal data flows of the system, creating the potential for custom integration hooks.
