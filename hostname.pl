#!/usr/bin/perl
use strict;
use warnings;
use Net::Domain;

sub getHostName {
   return Net::Domain::hostfqdn;
}
my $hostname = getHostName();
print ${hostname};