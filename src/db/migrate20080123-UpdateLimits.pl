#!/usr/bin/perl
use strict;
use lib "/opt/zimbra/zimbramon/lib";
use Zimbra::Util::Common;
use File::Grep qw (fgrep);

my $in= "/etc/security/limits.conf";
my $out="/tmp/limits.conf.$$";

if ( -f $in ) {
	print "Updating limits for zimbra user\n";
	if ( fgrep { /^zimbra/ } "${in}" ) {
		open(IN, "<$in") || die("Cannot open file for reading");
		open(OUT,">$out") || die("Cannot open file for writing");
		while(<IN>) {
			if($_ =~ /^zimbra soft/) {
				print OUT "zimbra soft nofile 524288\n";
			} elsif ($_ =~ /^zimbra hard/) {
				print OUT "zimbra hard nofile 524288\n";
			} else {
				print OUT $_;
			}
       		 }
		close(IN);
		close(OUT);
  	}
	if ( -s $out) {
		my $rc=0xffff & system("/bin/mv -f $in $in.bak");
		if ($rc != 0) {
			print "Warning: Failed to backup $in\n";
		}
		$rc=0xffff & system("/bin/mv -f $out $in");
		if ($rc != 0) {
			print "Failed to move $out to $in\nRestoring old configuration\n";
			$rc=0xffff & system("/bin/mv -f $in.bak $in");
			if ($rc != 0) {
				print "Failed to restore backup\n";
			}
		} else {
			 system("/bin/rm -f $in.bak");
		}
	}
}
