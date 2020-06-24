#!/usr/bin/perl

use strict;
use warnings;

use Config;
use Cwd;
use Data::Dumper;
use File::Basename;
use File::Copy;
use File::Path qw/make_path/;
use Getopt::Long;
use Getopt::Std;
use IPC::Cmd qw/run can_run/;
use Term::ANSIColor;

my %DEFINES = ();

my $sc_name = basename("$0");
my $usage   = "usage: $sc_name -v package_version -r package_release\n";
our($opt_v, $opt_r);

getopts('v:r:') or die "$usage";

die "$usage" if (!$opt_v);
die "$usage" if (!$opt_r);
my $version = "$opt_v";
$version =~ s/_/./g;
my $revision = $opt_r;


sub parse_defines()
{
   Die("wrong commandline options")
     if ( !GetOptions( "defines=s" => \%DEFINES ) );
}

sub cpy_file($$)
{
   my $src_file = shift;
   my $des_file = shift;

   my $des_dir = dirname($des_file);

   make_path($des_dir)
     if ( !-d $des_dir );

   Die("copy '$src_file' -> '$des_file' failed!")
     if ( !copy( $src_file, $des_file ) );
}

sub git_timestamp_from_dirs($)
{
   my $dirs = shift || [];

   print Dumper($dirs);

   my $ts;
   if ( $dirs && @$dirs )
   {
      foreach my $dir (@$dirs)
      {
         chomp( my $ts_new = `git log --pretty=format:%ct -1 '$dir'` );
         Die("failed to get git timestamp from $dir")
            if(! defined $ts_new);
         $ts = $ts_new
           if ( !defined $ts || $ts_new > $ts );
      }
   }

   return $ts;
}


my %PKG_GRAPH = (
   "zimbra-mbox-service" => {
      summary   => "Zimbra Mailbox Service",
      version   => "$version",
      revision  => $revision,
      hard_deps => [
         "zimbra-mbox-war",
         "zimbra-mbox-conf",
         "zimbra-common-core-jar"
      ],
      soft_deps => [
         "zimbra-common-mbox-conf",
         "zimbra-common-mbox-db",
         "zimbra-common-mbox-native-lib",
         "zimbra-common-mbox-conf-msgs",
         "zimbra-common-mbox-conf-rights",
         "zimbra-common-mbox-conf-attrs",
         "zimbra-common-mbox-docs",
      ],
      other_deps => [ "zimbra-store-components", ],
      file_list  => [],
      stage_fun  => sub { &stage_zimbra_mbox_service(@_); },
   },

   "zimbra-mbox-war" => {
      summary    => "Zimbra Mailbox Service War",
      version    => "$version",
      revision   => $revision,
      hard_deps  => [],
      soft_deps  => [],
      other_deps => ["zimbra-store-components"],
      replaces   => ["zimbra-store"],
      file_list  => ['/opt/zimbra/*'],
      stage_fun  => sub { &stage_zimbra_mbox_war(@_); },
   },

   "zimbra-mbox-conf" => {
      summary    => "Zimbra Mailbox Service Configuration",
      version    => "$version",
      revision   => $revision,
      hard_deps  => [],
      soft_deps  => [],
      other_deps => ["zimbra-store-components"],
      replaces   => ["zimbra-store"],
      file_list  => ['/opt/zimbra/*'],
      stage_fun  => sub { &stage_zimbra_mbox_conf(@_); },
   },

   "zimbra-common-mbox-conf" => {
      summary    => "Zimbra Core Mailbox Configuration",
      version    => "$version",
      revision   => $revision,
      hard_deps  => [],
      soft_deps  => [],
      other_deps => ["zimbra-core-components"],
      replaces   => ["zimbra-core"],
      file_list  => ['/opt/zimbra/*'],
      stage_fun  => sub { &stage_zimbra_common_mbox_conf(@_); },
   },

   "zimbra-common-mbox-db" => {
      summary    => "Zimbra Core Mailbox DB Files",
      version    => "$version",
      revision   => $revision,
      hard_deps  => [],
      soft_deps  => [],
      other_deps => ["zimbra-core-components"],
      replaces   => ["zimbra-core"],
      file_list  => ['/opt/zimbra/*'],
      stage_fun  => sub { &stage_zimbra_common_mbox_db(@_); },
   },

   "zimbra-common-mbox-native-lib" => {
      summary    => "Zimbra Core Mailbox Native Libs",
      version    => "$version",
      revision   => $revision,
      hard_deps  => [],
      soft_deps  => [],
      other_deps => ["zimbra-core-components"],
      replaces   => ["zimbra-core"],
      file_list  => ['/opt/zimbra/*'],
      stage_fun  => sub { &stage_zimbra_common_mbox_native_lib(@_); },
   },

   "zimbra-common-mbox-conf-msgs" => {
      summary    => "Zimbra Core Mailbox Message Locale Files",
      version    => "$version",
      revision   => $revision,
      hard_deps  => [],
      soft_deps  => [],
      other_deps => ["zimbra-core-components"],
      replaces   => ["zimbra-core"],
      file_list  => ['/opt/zimbra/*'],
      stage_fun  => sub { &stage_zimbra_common_mbox_conf_msgs(@_); },
   },

   "zimbra-common-mbox-conf-rights" => {
      summary    => "Zimbra Core Mailbox Rights Configuration",
      version    => "$version",
      revision   => $revision,
      hard_deps  => [],
      soft_deps  => [],
      other_deps => ["zimbra-core-components"],
      replaces   => ["zimbra-core"],
      file_list  => ['/opt/zimbra/*'],
      stage_fun  => sub { &stage_zimbra_common_mbox_conf_rights(@_); },
   },

   "zimbra-common-mbox-conf-attrs" => {
      summary    => "Zimbra Core Mailbox Attributes Configuration",
      version    => "$version",
      revision   => $revision,
      hard_deps  => [],
      soft_deps  => [],
      other_deps => ["zimbra-core-components"],
      replaces   => ["zimbra-core"],
      file_list  => ['/opt/zimbra/*'],
      stage_fun  => sub { &stage_zimbra_common_mbox_conf_attrs(@_); },
   },

   "zimbra-common-mbox-docs" => {
      summary    => "Zimbra Core Mailbox Docs",
      version    => "$version",
      revision   => $revision,
      hard_deps  => [],
      soft_deps  => [],
      other_deps => ["zimbra-core-components"],
      replaces   => ["zimbra-core"],
      file_list  => ['/opt/zimbra/*'],
      stage_fun  => sub { &stage_zimbra_common_mbox_docs(@_); },
   },

   "zimbra-common-core-jar" => {
      summary    => "Zimbra Core Jars",
      version    => "$version",
      revision   => $revision,
      hard_deps  => [],
      soft_deps  => [],
      other_deps => ["zimbra-core-components"],
      replaces   => ["zimbra-core"],
      file_list  => ['/opt/zimbra/*'],
      stage_fun  => sub { &stage_zimbra_common_core_jars(@_); },
   },
);


sub stage_zimbra_mbox_war($)
{
   my $stage_base_dir = shift;

   make_path("$stage_base_dir/opt/zimbra/jetty_base/webapps/service");
   System("cd $stage_base_dir/opt/zimbra/jetty_base/webapps/service && jar -xf @{[getcwd()]}/store/build/service.war");
   cpy_file( "store/conf/web.xml.production", "$stage_base_dir/opt/zimbra/jetty_base/etc/service.web.xml.in" );

   return ["."];
}


sub stage_zimbra_common_mbox_conf()
{
   my $stage_base_dir = shift;

   cpy_file( "milter-conf/conf/milter.log4j.properties",    "$stage_base_dir/opt/zimbra/conf/milter.log4j.properties" );
   cpy_file( "milter-conf/conf/mta_milter_options.in",      "$stage_base_dir/opt/zimbra/conf/mta_milter_options.in" );
   cpy_file( "store-conf/conf/datasource.xml",              "$stage_base_dir/opt/zimbra/conf/datasource.xml" );
   cpy_file( "store-conf/conf/localconfig.xml.production",  "$stage_base_dir/opt/zimbra/conf/localconfig.xml" );
   cpy_file( "store-conf/conf/log4j.properties.production", "$stage_base_dir/opt/zimbra/conf/log4j.properties.in" );
   cpy_file( "store-conf/conf/stats.conf.in",               "$stage_base_dir/opt/zimbra/conf/stats.conf.in" );
   cpy_file( "store/conf/unbound.conf.in",                  "$stage_base_dir/opt/zimbra/conf/unbound.conf.in" );

   return [ "store-conf", "store", "milter-conf" ];
}


sub stage_zimbra_mbox_conf()
{
   my $stage_base_dir = shift;

   cpy_file( "store-conf/conf/globs2",                             "$stage_base_dir/opt/zimbra/conf/globs2" );
   cpy_file( "store-conf/conf/magic",                              "$stage_base_dir/opt/zimbra/conf/magic" );
   cpy_file( "store-conf/conf/magic.zimbra",                       "$stage_base_dir/opt/zimbra/conf/magic.zimbra" );
   cpy_file( "store-conf/conf/globs2.zimbra",                      "$stage_base_dir/opt/zimbra/conf/globs2.zimbra" );
   cpy_file( "store-conf/conf/spnego_java_options.in",             "$stage_base_dir/opt/zimbra/conf/spnego_java_options.in" );
   cpy_file( "store-conf/conf/contacts/zimbra-contact-fields.xml", "$stage_base_dir/opt/zimbra/conf/zimbra-contact-fields.xml" );
   cpy_file( "store-conf/conf/common-passwords.txt",               "$stage_base_dir/opt/zimbra/conf/common-passwords.txt" );

   return ["store-conf/conf"];
}

sub stage_zimbra_common_mbox_db()
{
   my $stage_base_dir = shift;

   cpy_file( "store/build/dist/versions-init.sql", "$stage_base_dir/opt/zimbra/db/versions-init.sql" );

   return ["store"];
}

sub stage_zimbra_common_mbox_native_lib()
{
   my $stage_base_dir = shift;

   cpy_file( "native/build/libzimbra-native.so", "$stage_base_dir/opt/zimbra/lib/libzimbra-native.so" );

   return ["native"];
}

sub stage_zimbra_common_mbox_conf_msgs()
{
   my $stage_base_dir = shift;

   cpy_file( "store-conf/conf/msgs/L10nMsg.properties",           "$stage_base_dir/opt/zimbra/conf/msgs/L10nMsg.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg.properties",             "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights.properties",       "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_ar.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_ar.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_da.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_da.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_de.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_de.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_en_AU.properties", "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_en_AU.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_en_GB.properties", "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_en_GB.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_es.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_es.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_eu.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_eu.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_fr.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_fr.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_fr_CA.properties", "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_fr_CA.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_hi.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_hi.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_hu.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_hu.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_in.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_in.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_it.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_it.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_iw.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_iw.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_ja.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_ja.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_ko.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_ko.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_lo.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_lo.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_ms.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_ms.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_nl.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_nl.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_pl.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_pl.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_pt.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_pt.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_pt_BR.properties", "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_pt_BR.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_ro.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_ro.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_ru.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_ru.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_sl.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_sl.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_sv.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_sv.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_th.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_th.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_tr.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_tr.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_uk.properties",    "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_uk.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_zh_CN.properties", "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_zh_CN.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_zh_HK.properties", "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_zh_HK.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsgRights_zh_TW.properties", "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsgRights_zh_TW.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_ar.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_ar.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_da.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_da.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_de.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_de.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_en.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_en.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_en_AU.properties",       "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_en_AU.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_en_GB.properties",       "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_en_GB.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_es.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_es.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_eu.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_eu.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_fr.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_fr.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_fr_CA.properties",       "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_fr_CA.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_fr_FR.properties",       "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_fr_FR.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_hi.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_hi.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_hu.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_hu.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_in.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_in.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_it.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_it.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_iw.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_iw.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_ja.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_ja.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_ko.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_ko.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_lo.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_lo.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_ms.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_ms.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_nl.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_nl.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_pl.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_pl.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_pt.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_pt.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_pt_BR.properties",       "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_pt_BR.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_ro.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_ro.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_ru.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_ru.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_sl.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_sl.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_sv.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_sv.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_th.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_th.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_tr.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_tr.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_uk.properties",          "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_uk.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_zh_CN.properties",       "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_zh_CN.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_zh_HK.properties",       "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_zh_HK.properties" );
   cpy_file( "store-conf/conf/msgs/ZsMsg_zh_TW.properties",       "$stage_base_dir/opt/zimbra/conf/msgs/ZsMsg_zh_TW.properties" );

   return ["store-conf/conf/msgs"];
}

sub stage_zimbra_common_mbox_conf_rights()
{
   my $stage_base_dir = shift;

   cpy_file( "store-conf/conf/rights/adminconsole-ui.xml",                        "$stage_base_dir/opt/zimbra/conf/rights/adminconsole-ui.xml" );
   cpy_file( "store-conf/conf/rights/zimbra-rights-adminconsole-domainadmin.xml", "$stage_base_dir/opt/zimbra/conf/rights/zimbra-rights-adminconsole-domainadmin.xml" );
   cpy_file( "store-conf/conf/rights/zimbra-rights-adminconsole.xml",             "$stage_base_dir/opt/zimbra/conf/rights/zimbra-rights-adminconsole.xml" );
   cpy_file( "store-conf/conf/rights/zimbra-rights-domainadmin.xml",              "$stage_base_dir/opt/zimbra/conf/rights/zimbra-rights-domainadmin.xml" );
   cpy_file( "store-conf/conf/rights/zimbra-rights-roles.xml",                    "$stage_base_dir/opt/zimbra/conf/rights/zimbra-rights-roles.xml" );
   cpy_file( "store-conf/conf/rights/zimbra-rights.xml",                          "$stage_base_dir/opt/zimbra/conf/rights/zimbra-rights.xml" );
   cpy_file( "store-conf/conf/rights/zimbra-user-rights.xml",                     "$stage_base_dir/opt/zimbra/conf/rights/zimbra-user-rights.xml" );

   return ["store-conf/conf/rights"];
}

sub stage_zimbra_common_mbox_conf_attrs()
{
   my $stage_base_dir = shift;

   cpy_file( "store/conf/attrs/amavisd-new-attrs.xml", "$stage_base_dir/opt/zimbra/conf/attrs/amavisd-new-attrs.xml" );
   cpy_file( "store/conf/attrs/zimbra-attrs.xml",      "$stage_base_dir/opt/zimbra/conf/attrs/zimbra-attrs.xml" );
   cpy_file( "store/conf/attrs/zimbra-ocs.xml",        "$stage_base_dir/opt/zimbra/conf/attrs/zimbra-ocs.xml" );
   cpy_file( "store/build/dist/conf/attrs/zimbra-attrs-schema",   "$stage_base_dir/opt/zimbra/conf/zimbra-attrs-schema" );

   return ["store/conf/attrs"];
}

sub stage_zimbra_common_mbox_docs()
{
   my $stage_base_dir = shift;

   cpy_file( "store/docs/INSTALL-DEV-MAC-UBUNTU-VM.md",         "$stage_base_dir/opt/zimbra/docs/INSTALL-DEV-MAC-UBUNTU-VM.md" );
   cpy_file( "store/docs/INSTALL-DEV-MULTISERVER.txt",          "$stage_base_dir/opt/zimbra/docs/INSTALL-DEV-MULTISERVER.txt" );
   cpy_file( "store/docs/INSTALL-DEV-UBUNTU12_64.txt",          "$stage_base_dir/opt/zimbra/docs/INSTALL-DEV-UBUNTU12_64.txt" );
   cpy_file( "store/docs/INSTALL-OSX.md",                       "$stage_base_dir/opt/zimbra/docs/INSTALL-OSX.md" );
   cpy_file( "store/docs/INSTALL-SVN-WIN32.txt",                "$stage_base_dir/opt/zimbra/docs/INSTALL-SVN-WIN32.txt" );
   cpy_file( "store/docs/INSTALL-VOICE.txt",                    "$stage_base_dir/opt/zimbra/docs/INSTALL-VOICE.txt" );
   cpy_file( "store/docs/INSTALL-win.txt",                      "$stage_base_dir/opt/zimbra/docs/INSTALL-win.txt" );
   cpy_file( "store/docs/Notification.md",                      "$stage_base_dir/opt/zimbra/docs/Notification.md" );
   cpy_file( "store/docs/OAuthConsumer.txt",                    "$stage_base_dir/opt/zimbra/docs/OAuthConsumer.txt" );
   cpy_file( "store/docs/RedoableOperations.txt",               "$stage_base_dir/opt/zimbra/docs/RedoableOperations.txt" );
   cpy_file( "store/docs/ServerLocalization.txt",               "$stage_base_dir/opt/zimbra/docs/ServerLocalization.txt" );
   cpy_file( "store/docs/abook.md",                             "$stage_base_dir/opt/zimbra/docs/abook.md" );
   cpy_file( "store/docs/accesscontrol.txt",                    "$stage_base_dir/opt/zimbra/docs/accesscontrol.txt" );
   cpy_file( "store/docs/acl.md",                               "$stage_base_dir/opt/zimbra/docs/acl.md" );
   cpy_file( "store/docs/admin_soap_white_list.txt",            "$stage_base_dir/opt/zimbra/docs/admin_soap_white_list.txt" );
   cpy_file( "store/docs/alarm.md",                             "$stage_base_dir/opt/zimbra/docs/alarm.md" );
   cpy_file( "store/docs/autoprov.txt",                         "$stage_base_dir/opt/zimbra/docs/autoprov.txt" );
   cpy_file( "store/docs/caches.txt",                           "$stage_base_dir/opt/zimbra/docs/caches.txt" );
   cpy_file( "store/docs/cal-todos.md",                         "$stage_base_dir/opt/zimbra/docs/cal-todos.md" );
   cpy_file( "store/docs/certauth.txt",                         "$stage_base_dir/opt/zimbra/docs/certauth.txt" );
   cpy_file( "store/docs/changepasswordlistener.txt",           "$stage_base_dir/opt/zimbra/docs/changepasswordlistener.txt" );
   cpy_file( "store/docs/clienturls.txt",                       "$stage_base_dir/opt/zimbra/docs/clienturls.txt" );
   cpy_file( "store/docs/customauth-hosted.txt",                "$stage_base_dir/opt/zimbra/docs/customauth-hosted.txt" );
   cpy_file( "store/docs/customauth.txt",                       "$stage_base_dir/opt/zimbra/docs/customauth.txt" );
   cpy_file( "store/docs/dav.txt",                              "$stage_base_dir/opt/zimbra/docs/dav.txt" );
   cpy_file( "store/docs/delegatedadmin.txt",                   "$stage_base_dir/opt/zimbra/docs/delegatedadmin.txt" );
   cpy_file( "store/docs/extensions.md",                        "$stage_base_dir/opt/zimbra/docs/extensions.md" );
   cpy_file( "store/docs/externalldapauth.txt",                 "$stage_base_dir/opt/zimbra/docs/externalldapauth.txt" );
   cpy_file( "store/docs/familymailboxes.md",                   "$stage_base_dir/opt/zimbra/docs/familymailboxes.md" );
   cpy_file( "store/docs/file-upload.txt",                      "$stage_base_dir/opt/zimbra/docs/file-upload.txt" );
   cpy_file( "store/docs/freebusy-interop.md",                  "$stage_base_dir/opt/zimbra/docs/freebusy-interop.md" );
   cpy_file( "store/docs/gal.txt",                              "$stage_base_dir/opt/zimbra/docs/gal.txt" );
   cpy_file( "store/docs/groups.md",                            "$stage_base_dir/opt/zimbra/docs/groups.md" );
   cpy_file( "store/docs/idn.txt",                              "$stage_base_dir/opt/zimbra/docs/idn.txt" );
   cpy_file( "store/docs/jetty.txt",                            "$stage_base_dir/opt/zimbra/docs/jetty.txt" );
   cpy_file( "store/docs/junk-notjunk.md",                      "$stage_base_dir/opt/zimbra/docs/junk-notjunk.md" );
   cpy_file( "store/docs/krb5.txt",                             "$stage_base_dir/opt/zimbra/docs/krb5.txt" );
   cpy_file( "store/docs/ldap.txt",                             "$stage_base_dir/opt/zimbra/docs/ldap.txt" );
   cpy_file( "store/docs/ldap_replication_howto.txt",           "$stage_base_dir/opt/zimbra/docs/ldap_replication_howto.txt" );
   cpy_file( "store/docs/lockout.txt",                          "$stage_base_dir/opt/zimbra/docs/lockout.txt" );
   cpy_file( "store/docs/logging.md",                           "$stage_base_dir/opt/zimbra/docs/logging.md" );
   cpy_file( "store/docs/login.txt",                            "$stage_base_dir/opt/zimbra/docs/login.txt" );
   cpy_file( "store/docs/mysql-monitoring.txt",                 "$stage_base_dir/opt/zimbra/docs/mysql-monitoring.txt" );
   cpy_file( "store/docs/notes.txt",                            "$stage_base_dir/opt/zimbra/docs/notes.txt" );
   cpy_file( "store/docs/open_source_licenses_zcs-windows.txt", "$stage_base_dir/opt/zimbra/docs/open_source_licenses_zcs-windows.txt" );
   cpy_file( "store/docs/pop-imap.txt",                         "$stage_base_dir/opt/zimbra/docs/pop-imap.txt" );
   cpy_file( "store/docs/postfix-ldap-tables.txt",              "$stage_base_dir/opt/zimbra/docs/postfix-ldap-tables.txt" );
   cpy_file( "store/docs/postfix-split-domain.md",              "$stage_base_dir/opt/zimbra/docs/postfix-split-domain.md" );
   cpy_file( "store/docs/preauth.md",                           "$stage_base_dir/opt/zimbra/docs/preauth.md" );
   cpy_file( "store/docs/qatests.txt",                          "$stage_base_dir/opt/zimbra/docs/qatests.txt" );
   cpy_file( "store/docs/query.md",                             "$stage_base_dir/opt/zimbra/docs/query.md" );
   cpy_file( "store/docs/rest-admin.txt",                       "$stage_base_dir/opt/zimbra/docs/rest-admin.txt" );
   cpy_file( "store/docs/rest.txt",                             "$stage_base_dir/opt/zimbra/docs/rest.txt" );
   cpy_file( "store/docs/rights-adminconsole.txt",              "$stage_base_dir/opt/zimbra/docs/rights-adminconsole.txt" );
   cpy_file( "store/docs/rights-ext.txt",                       "$stage_base_dir/opt/zimbra/docs/rights-ext.txt" );
   cpy_file( "store/docs/rights.txt",                           "$stage_base_dir/opt/zimbra/docs/rights.txt" );
   cpy_file( "store/docs/share.md",                             "$stage_base_dir/opt/zimbra/docs/share.md" );
   cpy_file( "store/docs/snmp.txt",                             "$stage_base_dir/opt/zimbra/docs/snmp.txt" );
   cpy_file( "store/docs/soap-admin.txt",                       "$stage_base_dir/opt/zimbra/docs/soap-admin.txt" );
   cpy_file( "store/docs/soap-calendar.txt",                    "$stage_base_dir/opt/zimbra/docs/soap-calendar.txt" );
   cpy_file( "store/docs/soap-context-extension.txt",           "$stage_base_dir/opt/zimbra/docs/soap-context-extension.txt" );
   cpy_file( "store/docs/soap-document.txt",                    "$stage_base_dir/opt/zimbra/docs/soap-document.txt" );
   cpy_file( "store/docs/soap-im.txt",                          "$stage_base_dir/opt/zimbra/docs/soap-im.txt" );
   cpy_file( "store/docs/soap-mobile.txt",                      "$stage_base_dir/opt/zimbra/docs/soap-mobile.txt" );
   cpy_file( "store/docs/soap-right.txt",                       "$stage_base_dir/opt/zimbra/docs/soap-right.txt" );
   cpy_file( "store/docs/soap-waitset.txt",                     "$stage_base_dir/opt/zimbra/docs/soap-waitset.txt" );
   cpy_file( "store/docs/soap.txt",                             "$stage_base_dir/opt/zimbra/docs/soap.txt" );
   cpy_file( "store/docs/spnego.txt",                           "$stage_base_dir/opt/zimbra/docs/spnego.txt" );
   cpy_file( "store/docs/sync.txt",                             "$stage_base_dir/opt/zimbra/docs/sync.txt" );
   cpy_file( "store/docs/testharness.txt",                      "$stage_base_dir/opt/zimbra/docs/testharness.txt" );
   cpy_file( "store/docs/urls.md",                              "$stage_base_dir/opt/zimbra/docs/urls.md" );
   cpy_file( "store/docs/using-gdb.txt",                        "$stage_base_dir/opt/zimbra/docs/using-gdb.txt" );
   cpy_file( "store/docs/webdav-mountpoint.txt",                "$stage_base_dir/opt/zimbra/docs/webdav-mountpoint.txt" );
   cpy_file( "store/docs/zdesktop-dev-howto.txt",               "$stage_base_dir/opt/zimbra/docs/zdesktop-dev-howto.txt" );

   return ["store/docs"];
}

sub stage_zimbra_common_core_jars()
{
   my $stage_base_dir = shift;

   cpy_file( "store/build/dist/zm-store.jar",                    "$stage_base_dir/opt/zimbra/lib/jars/zimbrastore.jar");
   cpy_file( "soap/build/dist/zm-soap.jar",                      "$stage_base_dir/opt/zimbra/lib/jars/zimbrasoap.jar");
   cpy_file( "client/build/dist/zm-client.jar",                  "$stage_base_dir/opt/zimbra/lib/jars/zimbraclient.jar");
   cpy_file( "common/build/dist/zm-common.jar",                  "$stage_base_dir/opt/zimbra/lib/jars/zimbracommon.jar");
   cpy_file( "native/build/dist/zm-native.jar",                  "$stage_base_dir/opt/zimbra/lib/jars/zimbra-native.jar");
   return ["."];
}

sub stage_zimbra_mbox_service(%)
{
   my $stage_base_dir = shift;

   return ["."];
}

sub make_package($)
{
   my $pkg_name = shift;

   my $pkg_info = $PKG_GRAPH{$pkg_name};

   print Dumper($pkg_info);

   my $stage_fun = $pkg_info->{stage_fun};

   my $stage_base_dir = "build/stage/$pkg_name";

   make_path($stage_base_dir) if ( !-d $stage_base_dir );

   my $timestamp = git_timestamp_from_dirs( &$stage_fun($stage_base_dir) );

   $pkg_info->{_version_ts} = $pkg_info->{version} . ( $timestamp ? ( "." . $timestamp ) : "" );

   my @cmd = (
      "../zm-pkg-tool/pkg-build.pl",
      "--out-type=binary",
      "--pkg-name=$pkg_name",
      "--pkg-version=$pkg_info->{_version_ts}",
      "--pkg-release=$pkg_info->{revision}",
      "--pkg-summary=$pkg_info->{summary}"
   );

   if ( $pkg_info->{file_list} )
   {
      foreach my $expr ( @{ $pkg_info->{file_list} } )
      {
         print "stage_base_dir = $stage_base_dir\n";
         print "expr = $expr\n";

         my $dir_expr = "$stage_base_dir$expr";

         foreach my $entry (`find $dir_expr -type f`)
         {
            chomp($entry);
            $entry =~ s@$stage_base_dir@@;

            push( @cmd, "--pkg-installs=$entry" );
         }
      }
   }

   push( @cmd, @{ [ map { "--pkg-replaces=$_"; } @{ $pkg_info->{replaces} } ] } )                                                              if ( $pkg_info->{replaces} );
   push( @cmd, @{ [ map { "--pkg-depends=$_"; } @{ $pkg_info->{other_deps} } ] } )                                                             if ( $pkg_info->{other_deps} );
   push( @cmd, @{ [ map { "--pkg-depends=$_ (>= $PKG_GRAPH{$_}->{version})"; } @{ $pkg_info->{soft_deps} } ] } )                               if ( $pkg_info->{soft_deps} );
   push( @cmd, @{ [ map { "--pkg-depends=$_ (>= $PKG_GRAPH{$_}->{_version_ts}-$PKG_GRAPH{$_}->{revision})"; } @{ $pkg_info->{hard_deps} } ] } ) if ( $pkg_info->{hard_deps} );

   System(@cmd);
}

sub depth_first_traverse_package($)
{
   my $pkg_name = shift;

   my $pkg_info = $PKG_GRAPH{$pkg_name} || Die("package configuration error: '$pkg_name' not found");

   return
     if ( $pkg_info->{_state} && $pkg_info->{_state} eq "BUILT" );

   Die("dependency loop detected...")
     if ( $pkg_info->{_state} && $pkg_info->{_state} eq "BUILDING" );

   $pkg_info->{_state} = 'BUILDING';

   foreach my $dep_pkg_name ( ( sort @{ $pkg_info->{hard_deps} }, sort @{ $pkg_info->{soft_deps} } ) )
   {
      depth_first_traverse_package($dep_pkg_name);
   }

   make_package($pkg_name);

   $pkg_info->{_state} = 'BUILT';
}

sub main
{
   parse_defines();

   # cleanup
   system( "rm", "-rf", "build/stage" );
   system( "rm", "-rf", "build/dist" );

   foreach my $pkg_name ( sort keys %PKG_GRAPH )
   {
      depth_first_traverse_package($pkg_name);
   }
}

sub System(@)
{
   my $cmd_str = "@_";

   print color('green') . "#: pwd=@{[Cwd::getcwd()]}" . color('reset') . "\n";
   print color('green') . "#: $cmd_str" . color('reset') . "\n";

   $! = 0;
   my ( $success, $error_message, $full_buf, $stdout_buf, $stderr_buf ) = run( command => \@_, verbose => 1 );

   Die( "cmd='$cmd_str'", $error_message )
     if ( !$success );

   return { msg => $error_message, out => $stdout_buf, err => $stderr_buf };
}

sub Run(%)
{
   my %args  = (@_);
   my $chdir = $args{cd};
   my $child = $args{child};

   my $child_pid = fork();

   Die("FAILURE while forking")
     if ( !defined $child_pid );

   if ( $child_pid != 0 )    # parent
   {
      local $?;

      while ( waitpid( $child_pid, 0 ) == -1 ) { }

      Die( "child $child_pid died", einfo($?) )
        if ( $? != 0 );
   }
   else
   {
      Die( "chdir to '$chdir' failed", einfo($?) )
        if ( $chdir && !chdir($chdir) );

      $! = 0;
      &$child;
      exit(0);
   }
}

sub einfo()
{
   my @SIG_NAME = split( / /, $Config{sig_name} );

   return "ret=" . ( $? >> 8 ) . ( ( $? & 127 ) ? ", sig=SIG" . $SIG_NAME[ $? & 127 ] : "" );
}

sub Die($;$)
{
   my $msg  = shift;
   my $info = shift || "";
   my $err  = "$!";

   print "\n";
   print "\n";
   print "=========================================================================================================\n";
   print color('red') . "FAILURE MSG" . color('reset') . " : $msg\n";
   print color('red') . "SYSTEM ERR " . color('reset') . " : $err\n"  if ($err);
   print color('red') . "EXTRA INFO " . color('reset') . " : $info\n" if ($info);
   print "\n";
   print "=========================================================================================================\n";
   print color('red');
   print "--Stack Trace--\n";
   my $i = 1;

   while ( ( my @call_details = ( caller( $i++ ) ) ) )
   {
      print $call_details[1] . ":" . $call_details[2] . " called from " . $call_details[3] . "\n";
   }
   print color('reset');
   print "\n";
   print "=========================================================================================================\n";

   die "END";
}

##############################################################################################

main();
