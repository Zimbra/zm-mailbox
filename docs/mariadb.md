# MariaDB

`MariaDB` is used to store metadata about mailboxes and their contents.

Single-server installs use a local `MariaDB`, which is installed by the Zimbra installer.

Always On clusters use a shared `MariaDB` or `MariaDB Galera Cluster`.

# Configuration

Configure a server to use a certain MariaDB database:

    $ zmprov ms `zmhostname` zimbraMailboxDbConnectionUrl jdbc:mysql://the-mariadb-server:3306/zimbra?user=user&password=pass
    $ zmmailboxdctl restart

Configure the Always On Cluster named "mycluster" to use a certain MariaDB Galera Cluster database:

    $ zmprov maoc mycluster zimbraMailboxDbConnectionUrl jdbc:mysql://mariadb-server-1:3306,mariadb-server-2:3306,mariadb-server-3:3306/zimbra?user=user&password=pass
