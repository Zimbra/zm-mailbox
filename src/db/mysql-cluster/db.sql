--
-- ***** BEGIN LICENSE BLOCK *****
-- Zimbra Collaboration Suite Server
-- Copyright (C) 2013, 2014 Zimbra, Inc.
-- 
-- This program is free software: you can redistribute it and/or modify it under
-- the terms of the GNU General Public License as published by the Free Software Foundation,
-- version 2 of the License.
-- 
-- This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
-- without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
-- See the GNU General Public License for more details.
-- You should have received a copy of the GNU General Public License along with this program.
-- If not, see <http://www.gnu.org/licenses/>.
-- ***** END LICENSE BLOCK *****
--
CREATE DATABASE zimbra;
ALTER DATABASE zimbra DEFAULT CHARACTER SET utf8;

USE zimbra;

GRANT ALL ON zimbra.* TO 'zimbra' IDENTIFIED BY 'zimbra';
GRANT ALL ON zimbra.* TO 'zimbra'@'localhost' IDENTIFIED BY 'zimbra';
GRANT ALL ON zimbra.* TO 'zimbra'@'localhost.localdomain' IDENTIFIED BY 'zimbra';
GRANT ALL ON zimbra.* TO 'root'@'localhost.localdomain' IDENTIFIED BY 'zimbra';

-- The zimbra user needs to be able to create and drop databases and perform
-- backup and restore operations.  Give
-- zimbra root access for now to keep things simple until there is a need
-- to add more security.
GRANT ALL ON *.* TO 'zimbra' WITH GRANT OPTION;
GRANT ALL ON *.* TO 'zimbra'@'localhost' WITH GRANT OPTION;
GRANT ALL ON *.* TO 'zimbra'@'localhost.localdomain' WITH GRANT OPTION;
GRANT ALL ON *.* TO 'root'@'localhost.localdomain' WITH GRANT OPTION;

-- -----------------------------------------------------------------------
-- volumes
-- -----------------------------------------------------------------------

-- list of known volumes
CREATE TABLE volume (
   id                     TINYINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
   type                   TINYINT NOT NULL,   -- 1 = primary msg, 2 = secondary msg, 10 = index
   name                   VARCHAR(255) NOT NULL,
   path                   TEXT NOT NULL,
   file_bits              SMALLINT NOT NULL,
   file_group_bits        SMALLINT NOT NULL,
   mailbox_bits           SMALLINT NOT NULL,
   mailbox_group_bits     SMALLINT NOT NULL,
   compress_blobs         BOOLEAN NOT NULL,
   compression_threshold  BIGINT NOT NULL,
   metadata               MEDIUMTEXT,

   UNIQUE INDEX i_name (name),
   UNIQUE INDEX i_path (path(255))   -- Index prefix length of 255 is the max prior to MySQL 4.1.2.  Should be good enough.
) ENGINE = NDBCLUSTER;

-- This table has only one row.  It points to message and index volumes
-- to use for newly provisioned mailboxes.
CREATE TABLE current_volumes (
   message_volume_id            TINYINT UNSIGNED NOT NULL,
   secondary_message_volume_id  TINYINT UNSIGNED,
   index_volume_id              TINYINT UNSIGNED NOT NULL,
   next_mailbox_id              INTEGER UNSIGNED NOT NULL,

   INDEX i_message_volume_id (message_volume_id),
   INDEX i_secondary_message_volume_id (secondary_message_volume_id),
   INDEX i_index_volume_id (index_volume_id),

   CONSTRAINT fk_current_volumes_message_volume_id FOREIGN KEY (message_volume_id) REFERENCES volume(id),
   CONSTRAINT fk_current_volumes_secondary_message_volume_id FOREIGN KEY (secondary_message_volume_id) REFERENCES volume(id),
   CONSTRAINT fk_current_volumes_index_volume_id FOREIGN KEY (index_volume_id) REFERENCES volume(id)
) ENGINE = NDBCLUSTER;

INSERT INTO volume (id, type, name, path, file_bits, file_group_bits,
    mailbox_bits, mailbox_group_bits, compress_blobs, compression_threshold)
  VALUES (1, 1, 'message1', '/opt/zimbra/store', 12, 8, 12, 8, 0, 4096);
INSERT INTO volume (id, type, name, path, file_bits, file_group_bits,
    mailbox_bits, mailbox_group_bits, compress_blobs, compression_threshold)
  VALUES (2, 10, 'index1',   '/opt/zimbra/index', 12, 8, 12, 8, 0, 4096);

INSERT INTO current_volumes (message_volume_id, index_volume_id, next_mailbox_id) VALUES (1, 2, 1);
COMMIT;

create table volume_blobs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  volume_id TINYINT NOT NULL,
  mailbox_id INTEGER NOT NULL,
  item_id INTEGER NOT NULL,
  revision INTEGER NOT NULL,
  blob_digest VARCHAR(44),
  processed BOOLEAN default false,
  
  INDEX i_blob_digest (blob_digest),
  
  CONSTRAINT uc_blobinfo UNIQUE (volume_id,mailbox_id,item_id,revision)
  -- FK constraints disabled for now; maybe enable them in 9.0 when we have time to deal with delete cases
  -- CONSTRAINT fk_volume_blobs_volume_id FOREIGN KEY (volume_id) REFERENCES volume(id),
  -- CONSTRAINT fk_volume_blobs_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES mailbox(id)
);

-- -----------------------------------------------------------------------
-- mailbox info
-- -----------------------------------------------------------------------

CREATE TABLE mailbox (
   id                  INTEGER UNSIGNED NOT NULL PRIMARY KEY,
   group_id            INTEGER UNSIGNED NOT NULL,  -- mailbox group
   account_id          VARCHAR(127) NOT NULL,      -- e.g. "d94e42c4-1636-11d9-b904-4dd689d02402"
   index_volume_id     TINYINT UNSIGNED NOT NULL,
   item_id_checkpoint  INTEGER UNSIGNED NOT NULL DEFAULT 0,
   contact_count       INTEGER UNSIGNED DEFAULT 0,
   size_checkpoint     BIGINT UNSIGNED NOT NULL DEFAULT 0,
   change_checkpoint   INTEGER UNSIGNED NOT NULL DEFAULT 0,
   tracking_sync       INTEGER UNSIGNED NOT NULL DEFAULT 0,
   tracking_imap       BOOLEAN NOT NULL DEFAULT 0,
   last_backup_at      INTEGER UNSIGNED,           -- last full backup time, UNIX-style timestamp
   comment             VARCHAR(255),               -- usually the main email address originally associated with the mailbox
   last_soap_access    INTEGER UNSIGNED NOT NULL DEFAULT 0,
   new_messages        INTEGER UNSIGNED NOT NULL DEFAULT 0,
   idx_deferred_count  INTEGER NOT NULL DEFAULT 0, -- deprecated
   highest_indexed     VARCHAR(21), -- deprecated
   version             VARCHAR(16),
   last_purge_at       INTEGER UNSIGNED NOT NULL DEFAULT 0,
   itemcache_checkpoint INTEGER UNSIGNED NOT NULL DEFAULT 0,

   UNIQUE INDEX i_account_id (account_id),
   INDEX i_index_volume_id (index_volume_id),
   INDEX i_last_backup_at (last_backup_at, id),

   CONSTRAINT fk_mailbox_index_volume_id FOREIGN KEY (index_volume_id) REFERENCES volume(id)
) ENGINE = NDBCLUSTER;

-- -----------------------------------------------------------------------
-- deleted accounts
-- -----------------------------------------------------------------------

CREATE TABLE deleted_account (
   email       VARCHAR(255) NOT NULL PRIMARY KEY,
   account_id  VARCHAR(127) NOT NULL,
   mailbox_id  INTEGER UNSIGNED NOT NULL,
   deleted_at  INTEGER UNSIGNED NOT NULL      -- UNIX-style timestamp
) ENGINE = NDBCLUSTER;

-- -----------------------------------------------------------------------
-- mailbox metadata info
-- -----------------------------------------------------------------------

CREATE TABLE mailbox_metadata (
   mailbox_id  INTEGER UNSIGNED NOT NULL,
   section     VARCHAR(64) NOT NULL,       -- e.g. "imap"
   metadata    MEDIUMTEXT,

   PRIMARY KEY (mailbox_id, section),

   CONSTRAINT fk_metadata_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES mailbox(id) ON DELETE CASCADE
) ENGINE = NDBCLUSTER;

-- -----------------------------------------------------------------------
-- out-of-office reply history
-- -----------------------------------------------------------------------

CREATE TABLE out_of_office (
   mailbox_id  INTEGER UNSIGNED NOT NULL,
   sent_to     VARCHAR(255) NOT NULL,
   sent_on     DATETIME NOT NULL,

   PRIMARY KEY (mailbox_id, sent_to),
   INDEX i_sent_on (sent_on),

   CONSTRAINT fk_out_of_office_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES mailbox(id) ON DELETE CASCADE
) ENGINE = NDBCLUSTER;

-- -----------------------------------------------------------------------
-- etc.
-- -----------------------------------------------------------------------

-- table for global config params
CREATE TABLE config (
   name         VARCHAR(255) NOT NULL PRIMARY KEY,
   value        TEXT,
   description  TEXT,
   modified     TIMESTAMP
) ENGINE = NDBCLUSTER;

-- table for tracking database table maintenance
CREATE TABLE table_maintenance (
   database_name       VARCHAR(64) NOT NULL,
   table_name          VARCHAR(64) NOT NULL,
   maintenance_date    DATETIME NOT NULL,
   last_optimize_date  DATETIME,
   num_rows            INTEGER UNSIGNED NOT NULL,

   PRIMARY KEY (table_name, database_name)
) ENGINE = NDBCLUSTER;

CREATE TABLE service_status (
   server   VARCHAR(255) NOT NULL,
   service  VARCHAR(255) NOT NULL,
   time     DATETIME,
   status   BOOLEAN,

   UNIQUE INDEX i_server_service (server(100), service(100))
) ENGINE = MyISAM;

-- Tracks scheduled tasks
CREATE TABLE scheduled_task (
   class_name       VARCHAR(255) BINARY NOT NULL,
   name             VARCHAR(255) NOT NULL,
   mailbox_id       INTEGER UNSIGNED NOT NULL,
   exec_time        DATETIME,
   interval_millis  INTEGER UNSIGNED,
   metadata         MEDIUMTEXT,

   PRIMARY KEY (name, mailbox_id, class_name),
   CONSTRAINT fk_st_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES mailbox(id) ON DELETE CASCADE,
   INDEX i_mailbox_id (mailbox_id)
) ENGINE = NDBCLUSTER;

-- Mobile Devices
CREATE TABLE mobile_devices (
   mailbox_id          INTEGER UNSIGNED NOT NULL,
   device_id           VARCHAR(64) NOT NULL,
   device_type         VARCHAR(64) NOT NULL,
   user_agent          VARCHAR(64),
   protocol_version    VARCHAR(64),
   provisionable       BOOLEAN NOT NULL DEFAULT 0,
   status              TINYINT UNSIGNED NOT NULL DEFAULT 0,
   policy_key          INTEGER UNSIGNED,
   recovery_password   VARCHAR(64),
   first_req_received  INTEGER UNSIGNED NOT NULL,
   last_policy_update  INTEGER UNSIGNED,
   remote_wipe_req     INTEGER UNSIGNED,
   remote_wipe_ack     INTEGER UNSIGNED,
   policy_values       VARCHAR(512),
   last_used_date      DATE,
   deleted_by_user     BOOLEAN NOT NULL DEFAULT 0,
   model               VARCHAR(64),
   imei                VARCHAR(64),
   friendly_name       VARCHAR(512),
   os                  VARCHAR(64),
   os_language         VARCHAR(64),
   phone_number        VARCHAR(64),
   unapproved_appl_list TEXT NULL,
   approved_appl_list   TEXT NULL,
   
   PRIMARY KEY (mailbox_id, device_id),
   CONSTRAINT fk_mobile_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES mailbox(id) ON DELETE CASCADE,
   INDEX i_last_used_date (last_used_date)
) ENGINE = NDBCLUSTER;

-- Tracks ACLs to be pushed to LDAP
CREATE TABLE pending_acl_push (
   mailbox_id  INTEGER UNSIGNED NOT NULL,
   item_id     INTEGER UNSIGNED NOT NULL,
   date        BIGINT UNSIGNED NOT NULL,

   PRIMARY KEY (mailbox_id, item_id, date),
   CONSTRAINT fk_pending_acl_push_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES mailbox(id) ON DELETE CASCADE,
   INDEX i_date (date)
) ENGINE = NDBCLUSTER;
