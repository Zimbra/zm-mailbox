# Always On Clusters

An `Always On Cluster` is a group of servers that use a shared configuration in LDAP.

`Always On Clusters` enable zero data loss during outages by using external backing services to store state. All of the backing services supported by Zimbra support HA configurations, and are tolerant of single node failures.

When a new Zimbra server is installed, and then joined to an existing cluster, it begins to use the cluster's configuration for MariaDB, Redis, LDAP, Solr, Scality, and so on.

Whatever settings that are not configured at the LDAP cluster config are then read from the LDAP server config or the LDAP global config.

## Create an Always On Cluster

Creation of the Always On Cluster creates an LDAP group identified by a GUID called the `alwaysOnClusterId`.

To create a cluster called "production" (in an environment where others might have been called "test" or "staging"):

    $ zmprov caoc production
    c13773da-94fd-43b1-9117-5a36ec392c76

## Configure backing services for a cluster

    $ zmprov maoc production zimbraRedisURL redis://redis-host:6379
    $ zmprov maoc production zimbraMemcachedClientServerList memcached-host:11211
    $ zmprov maoc production zimbraIndexURL solrcloud:http://solrcloud-host:8983/solr
    $ zmprov maoc production zimbraMailboxDbConnectionUrl jdbc:mysql://host1:port1,host2:port2,host3:port3/zimbra
    ...

## Join a Zimbra node to a cluster:

	$ zmprov ms `zmhostname` zimbraAlwaysOnClusterId c13773da-94fd-43b1-9117-5a36ec392c76
	$ jetty restart
