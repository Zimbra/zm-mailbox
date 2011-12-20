--
-- ***** BEGIN LICENSE BLOCK *****
-- Zimbra Collaboration Suite Server
-- Copyright (C) 2011 Zimbra, Inc.
--
-- The contents of this file are subject to the Zimbra Public License
-- Version 1.3 ("License"); you may not use this file except in
-- compliance with the License.  You may obtain a copy of the License at
-- http://www.zimbra.com/license.
--
-- Software distributed under the License is distributed on an "AS IS"
-- basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
-- ***** END LICENSE BLOCK *****
--

-- HSQLDB is for unit test. The scheme should be logically identical with MySQL except no index is needed for unit test.

CREATE SCHEMA zimbra;
SET SCHEMA zimbra;
SET INITIAL SCHEMA zimbra;

CREATE TABLE volume (
   id                     IDENTITY,
   type                   TINYINT NOT NULL,
   name                   VARCHAR(255) NOT NULL,
   path                   VARCHAR(255) NOT NULL,
   file_bits              SMALLINT NOT NULL,
   file_group_bits        SMALLINT NOT NULL,
   mailbox_bits           SMALLINT NOT NULL,
   mailbox_group_bits     SMALLINT NOT NULL,
   compress_blobs         BOOLEAN NOT NULL,
   compression_threshold  BIGINT NOT NULL,

   CONSTRAINT i_name UNIQUE (name),
   CONSTRAINT i_path UNIQUE (path)
);

CREATE TABLE current_volumes (
   message_volume_id            TINYINT NOT NULL,
   secondary_message_volume_id  TINYINT,
   index_volume_id              TINYINT NOT NULL,
   next_mailbox_id              INTEGER NOT NULL,

   CONSTRAINT fk_current_volumes_message_volume_id FOREIGN KEY (message_volume_id) REFERENCES volume(id),
   CONSTRAINT fk_current_volumes_secondary_message_volume_id FOREIGN KEY (secondary_message_volume_id) REFERENCES volume(id),
   CONSTRAINT fk_current_volumes_index_volume_id FOREIGN KEY (index_volume_id) REFERENCES volume(id)
);

INSERT INTO volume (id, type, name, path, file_bits, file_group_bits, mailbox_bits, mailbox_group_bits, compress_blobs, compression_threshold)
  VALUES (1, 1, 'message1', 'build/test/store', 12, 8, 12, 8, 0, 4096);
INSERT INTO volume (id, type, name, path, file_bits, file_group_bits, mailbox_bits, mailbox_group_bits, compress_blobs, compression_threshold)
  VALUES (2, 10, 'index1', 'build/test/index', 12, 8, 12, 8, 0, 4096);
INSERT INTO current_volumes (message_volume_id, index_volume_id, next_mailbox_id) VALUES (1, 2, 1);

CREATE TABLE mailbox (
   id                  INTEGER NOT NULL PRIMARY KEY,
   group_id            INTEGER NOT NULL,
   account_id          VARCHAR(127) NOT NULL,
   index_volume_id     TINYINT NOT NULL,
   item_id_checkpoint  INTEGER DEFAULT 0 NOT NULL,
   contact_count       INTEGER DEFAULT 0,
   size_checkpoint     BIGINT DEFAULT 0 NOT NULL,
   change_checkpoint   INTEGER DEFAULT 0 NOT NULL,
   tracking_sync       INTEGER DEFAULT 0 NOT NULL,
   tracking_imap       BOOLEAN DEFAULT 0 NOT NULL,
   last_backup_at      INTEGER,
   comment             VARCHAR(255),
   last_soap_access    INTEGER DEFAULT 0 NOT NULL,
   new_messages        INTEGER DEFAULT 0 NOT NULL,
   idx_deferred_count  INTEGER DEFAULT 0 NOT NULL, -- deprecated
   highest_indexed     VARCHAR(21), -- deprecated
   version             VARCHAR(16),

   CONSTRAINT i_account_id UNIQUE (account_id),
   CONSTRAINT fk_mailbox_index_volume_id FOREIGN KEY (index_volume_id) REFERENCES volume(id)
);

CREATE TABLE deleted_account (
   email       VARCHAR(255) NOT NULL PRIMARY KEY,
   account_id  VARCHAR(127) NOT NULL,
   mailbox_id  INTEGER NOT NULL,
   deleted_at  INTEGER NOT NULL
);

CREATE TABLE mailbox_metadata (
   mailbox_id  INTEGER NOT NULL,
   section     VARCHAR(64) NOT NULL,
   metadata    VARCHAR(255),

   CONSTRAINT pk_mailbox_metadata PRIMARY KEY (mailbox_id, section),
   CONSTRAINT fk_metadata_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES mailbox(id) ON DELETE CASCADE
);

CREATE TABLE out_of_office (
   mailbox_id  INTEGER NOT NULL,
   sent_to     VARCHAR(255) NOT NULL,
   sent_on     DATETIME NOT NULL,

   CONSTRAINT pk_out_of_office PRIMARY KEY (mailbox_id, sent_to),
   CONSTRAINT fk_out_of_office_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES mailbox(id) ON DELETE CASCADE
);

CREATE TABLE config (
   name         VARCHAR(255) NOT NULL PRIMARY KEY,
   value        VARCHAR(255),
   description  VARCHAR(255),
   modified     TIMESTAMP
);

CREATE TABLE table_maintenance (
   database_name       VARCHAR(64) NOT NULL,
   table_name          VARCHAR(64) NOT NULL,
   maintenance_date    DATETIME NOT NULL,
   last_optimize_date  DATETIME,
   num_rows            INTEGER NOT NULL,

   CONSTRAINT pk_table_maintenance PRIMARY KEY (table_name, database_name)
);

CREATE TABLE service_status (
   server   VARCHAR(255) NOT NULL,
   service  VARCHAR(255) NOT NULL,
   time     DATETIME,
   status   BOOLEAN,

   CONSTRAINT i_server_service UNIQUE (server, service)
);

CREATE TABLE scheduled_task (
   class_name       VARCHAR(255) NOT NULL,
   name             VARCHAR(255) NOT NULL,
   mailbox_id       INTEGER NOT NULL,
   exec_time        DATETIME,
   interval_millis  INTEGER,
   metadata         VARCHAR(255),

   CONSTRAINT pk_scheduled_task PRIMARY KEY (name, mailbox_id, class_name),
   CONSTRAINT fk_st_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES mailbox(id) ON DELETE CASCADE,
);

CREATE TABLE mobile_devices (
   mailbox_id          INTEGER NOT NULL,
   device_id           VARCHAR(64) NOT NULL,
   device_type         VARCHAR(64) NOT NULL,
   user_agent          VARCHAR(64),
   protocol_version    VARCHAR(64),
   provisionable       BOOLEAN DEFAULT 0 NOT NULL,
   status              TINYINT DEFAULT 0 NOT NULL,
   policy_key          INTEGER,
   recovery_password   VARCHAR(64),
   first_req_received  INTEGER NOT NULL,
   last_policy_update  INTEGER,
   remote_wipe_req     INTEGER,
   remote_wipe_ack     INTEGER,
   policy_values       VARCHAR(512),
   last_used_date      DATE,
   deleted_by_user     BOOLEAN DEFAULT 0 NOT NULL,
   model               VARCHAR(64),
   imei                VARCHAR(64),
   friendly_name       VARCHAR(512),
   os                  VARCHAR(64),
   os_language         VARCHAR(64),
   phone_number        VARCHAR(64),
   unapproved_appl_list VARCHAR(512),
   approved_appl_list   VARCHAR(512),  

   CONSTRAINT pk_mobile_devices PRIMARY KEY (mailbox_id, device_id),
   CONSTRAINT fk_mobile_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES mailbox(id) ON DELETE CASCADE,
);

CREATE TABLE pending_acl_push (
   mailbox_id  INTEGER NOT NULL,
   item_id     INTEGER NOT NULL,
   date        BIGINT NOT NULL,

   PRIMARY KEY (mailbox_id, item_id, date),
   CONSTRAINT fk_pending_acl_push_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES mailbox(id) ON DELETE CASCADE
);
