# Use of Solr & SolrCloud

`Solr` is used as a backing service for full text search. All searchable content is sent to Solr for indexing and all search requests initiated internally, via a mail client or a SOAP client trigger a request to `Solr`.

Zimbra supports bundled `Solr` or externally managed `SolrCloud` services in support of search functionality. Proior to using `Solr`, this used to be provided in-process by Lucene.

# Configuration

Use `Solr` (a WAR deployed to Jetty):

    $ zmprov ms `zmhostname` zimbraIndexURL solr:http://localhost:7983/solr
    $ zmmailboxdctl restart

Use `SolrCloud` by pointing Zimbra at the SolrCloud installation's ZooKeeper endpoint:

    $ zmprov ms `zmhostname` zimbraIndexURL solrcloud:http://host:8983/solr
    $ zmmailboxdctl restart
