# Use of Consul

`Consul` is used as a service locator.

Prior to `Consul`, you would need to query LDAP to obtain a list of hosts, or a list of instances of a given service. This information wasn't always actionable, since LDAP only knows about what hosts and services were installed, and it doesn't know whether a given host is currently powered up or if a service instance is healthy, stopped, crashed, or busy and unresponsive.

Zimbra registers health check scripts for each service with `Consul`, which invokes them periodically to determine an accurate health status of each service instance.

# How Consul is Installed

`Consul` runs on every Zimbra node, giving each node access to the single logical global database. When querying Consul, a Zimbra node always connects to the instance running at `localhost`.

`Consul` runs in either `server` or `client` mode. Server mode performs persistent storage and multi-master replication.

Consul server-mode is installed on every LDAP MMR node.

# Common curl queries

Get a unique list of known services:

```
$ curl http://127.0.0.1:8500/v1/catalog/services

{"consul":[],
"zimbra-imap":["9.0.x","9.0.0","ssl"],
"zimbra-lmtp":["9.0.x","9.0.0"],
"zimbra-mailstore":["9.0.x","9.0.0"],
"zimbra-mailstoreadmin":["9.0.x","9.0.0","ssl"],
"zimbra-pop3":["9.0.x","9.0.0","ssl"],
"zimbra-redolog":["ssl"],
"zimbra-web":["9.0.x","9.0.0","ssl"],
"zimbra-webadmin":["9.0.x","9.0.0","ssl"]}
```

Get a list of instances of a given service (the mailstore SOAP service in this example):

```
$ curl http://127.0.0.1:8500/v1/catalog/service/zimbra-mailstore

[{"Node":"Davids-MacBook-Pro.local","Address":"192.168.0.12","ServiceID":"zimbra-mailstore:7070","ServiceName":"zimbra-mailstore","ServiceTags":["9.0.x","9.0.0"],"ServiceAddress":"","ServicePort":7070}]
```

Get a list of healthy mailstore service instances (2 healthy instances are shown):

```
$ curl http://localhost:8500/v1/health/service/zimbra-mailstore

[{"Node":{"Node":"zdev-vm065.eng.zimbra.com","Address":"10.137.29.65"},"Service":{"ID":"zimbra:zimbra-mailstore:443","Service":"zimbra-mailstore","Tags":["9.0.x","9.0.0","ssl""],"Address":"","Port":443},"Checks":[{"Node":"zdev-vm065.eng.zimbra.com","CheckID":"service:zimbra-mailstore:443","Name":"Service 'zimbra-mailstore' check","Status":"critical","Notes":"","Output":"","ServiceID":"zimbra-mailstore:443","ServiceName":"zimbra-mailstore"},{"Node":"zdev-vm065.eng.zimbra.com","CheckID":"serfHealth","Name":"Serf Health Status","Status":"passing","Notes":"","Output":"Agent alive and reachable","ServiceID":"","ServiceName":""}]},

{"Node":{"Node":"zdev-vm064.eng.zimbra.com","Address":"10.137.29.64"},"Service":{"ID":"zimbra-mailstore:443","Service":"zimbra-mailstore","Tags":["9.0.x","9.0.0","ssl"],"Address":"","Port":443},"Checks":[{"Node":"zdev-vm064.eng.zimbra.com","CheckID":"service:zimbra-mailstore:443","Name":"Service 'zimbra-mailstore' check","Status":"critical","Notes":"","Output":"","ServiceID":"zimbra-mailstore:443","ServiceName":"zimbra-mailstore"},{"Node":"zdev-vm064.eng.zimbra.com","CheckID":"serfHealth","Name":"Serf Health Status","Status":"passing","Notes":"","Output":"Agent alive and reachable","ServiceID":"","ServiceName":""}]}]
```
