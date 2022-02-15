--
-- ***** BEGIN LICENSE BLOCK *****
-- Zimbra Collaboration Suite Server
-- Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
--
-- This program is free software: you can redistribute it and/or modify it under
-- the terms of the GNU General Public License as published by the Free Software Foundation,
-- version 2 of the License.
--
-- This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
-- without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
-- See the GNU General Public License for more details.
-- You should have received a copy of the GNU General Public License along with this program.
-- If not, see <https://www.gnu.org/licenses/>.
-- ***** END LICENSE BLOCK *****
--

-- HSQLDB is for unit test. The scheme should be logically identical with MySQL except no index is needed for unit test.

CREATE SCHEMA *{DATABASE_NAME};

CREATE TABLE *{DATABASE_NAME}.mail_item (
   mailbox_id    INTEGER NOT NULL,
   id            INTEGER NOT NULL,
   type          TINYINT NOT NULL,
   parent_id     INTEGER,
   folder_id     INTEGER,
   prev_folders  VARCHAR(255),               -- e.g. "101:2;110:5", before mod_metadata 101, this item was in folder 2, before 110, it was in 5
   index_id      INTEGER,
   imap_id       INTEGER,
   date          INTEGER NOT NULL,
   size          BIGINT NOT NULL,
   locator       VARCHAR(1024),
   blob_digest   VARCHAR(44),
   unread        INTEGER,
   flags         INTEGER DEFAULT 0 NOT NULL,
   tags          BIGINT DEFAULT 0 NOT NULL,
   tag_names     VARCHAR(255),
   sender        VARCHAR(128),
   recipients    VARCHAR(128),
   subject       VARCHAR(255),
   name          VARCHAR(255),
   metadata      VARCHAR(1000000),
   mod_metadata  INTEGER NOT NULL,
   change_date   INTEGER,
   mod_content   INTEGER NOT NULL,
   uuid          VARCHAR(127),               -- e.g. "d94e42c4-1636-11d9-b904-4dd689d02402"

   CONSTRAINT pk_mail_item PRIMARY KEY (mailbox_id, id),
   CONSTRAINT i_name_folder_id UNIQUE (mailbox_id, folder_id, name),
   CONSTRAINT fk_mail_item_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES zimbra.mailbox(id),
   CONSTRAINT fk_mail_item_parent_id FOREIGN KEY (mailbox_id, parent_id) REFERENCES mail_item(mailbox_id, id),
   CONSTRAINT fk_mail_item_folder_id FOREIGN KEY (mailbox_id, folder_id) REFERENCES mail_item(mailbox_id, id)
);

CREATE TABLE *{DATABASE_NAME}.mail_item_dumpster (
   mailbox_id    INTEGER NOT NULL,
   id            INTEGER NOT NULL,
   type          TINYINT NOT NULL,
   parent_id     INTEGER,
   folder_id     INTEGER,
   prev_folders  VARCHAR(255),               -- e.g. "101:2;110:5", before mod_metadata 101, this item was in folder 2, before 110, it was in 5
   index_id      INTEGER,
   imap_id       INTEGER,
   date          INTEGER NOT NULL,
   size          BIGINT NOT NULL,
   locator       VARCHAR(1024),
   blob_digest   VARCHAR(44),
   unread        INTEGER,
   flags         INTEGER DEFAULT 0 NOT NULL,
   tags          BIGINT DEFAULT 0 NOT NULL,
   tag_names     VARCHAR(255),
   sender        VARCHAR(128),
   recipients    VARCHAR(128),
   subject       VARCHAR(255),
   name          VARCHAR(255),
   metadata      VARCHAR(255),
   mod_metadata  INTEGER NOT NULL,
   change_date   INTEGER,
   mod_content   INTEGER NOT NULL,
   uuid          VARCHAR(127),               -- e.g. "d94e42c4-1636-11d9-b904-4dd689d02402"

   CONSTRAINT pk_mail_item_dumpster PRIMARY KEY (mailbox_id, id),
   CONSTRAINT fk_mail_item_dumpster_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES zimbra.mailbox(id)
);

CREATE TABLE *{DATABASE_NAME}.revision (
   mailbox_id    INTEGER NOT NULL,
   item_id       INTEGER NOT NULL,
   version       INTEGER NOT NULL,
   date          INTEGER NOT NULL,
   size          BIGINT NOT NULL,
   locator       VARCHAR(1024),
   blob_digest   VARCHAR(44),
   name          VARCHAR(255),
   metadata      VARCHAR(255),
   mod_metadata  INTEGER NOT NULL,
   change_date   INTEGER,
   mod_content   INTEGER NOT NULL,

   CONSTRAINT pk_revision PRIMARY KEY (mailbox_id, item_id, version),
   CONSTRAINT fk_revision_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES zimbra.mailbox(id),
   CONSTRAINT fk_revision_item_id FOREIGN KEY (mailbox_id, item_id)
      REFERENCES mail_item(mailbox_id, id) ON DELETE CASCADE
);

CREATE TABLE *{DATABASE_NAME}.revision_dumpster (
   mailbox_id    INTEGER NOT NULL,
   item_id       INTEGER NOT NULL,
   version       INTEGER NOT NULL,
   date          INTEGER NOT NULL,
   size          BIGINT NOT NULL,
   locator       VARCHAR(1024),
   blob_digest   VARCHAR(44),
   name          VARCHAR(255),
   metadata      VARCHAR(255),
   mod_metadata  INTEGER NOT NULL,
   change_date   INTEGER,
   mod_content   INTEGER NOT NULL,

   CONSTRAINT pk_revision_dumpster PRIMARY KEY (mailbox_id, item_id, version),
   CONSTRAINT fk_revision_dumpster_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES zimbra.mailbox(id),
   CONSTRAINT fk_revision_dumpster_item_id FOREIGN KEY (mailbox_id, item_id)
      REFERENCES mail_item_dumpster(mailbox_id, id) ON DELETE CASCADE
);

CREATE TABLE *{DATABASE_NAME}.tag (
   mailbox_id    INTEGER NOT NULL,
   id            INTEGER NOT NULL,
   name          VARCHAR(128) NOT NULL,
   color         BIGINT,
   item_count    INTEGER DEFAULT 0 NOT NULL,
   unread        INTEGER DEFAULT 0 NOT NULL,
   listed        BOOLEAN DEFAULT FALSE NOT NULL,
   sequence      INTEGER NOT NULL,
   policy        VARCHAR(1024),

   CONSTRAINT pk_tag PRIMARY KEY (mailbox_id, id),
   CONSTRAINT i_tag_name UNIQUE (mailbox_id, name),
   CONSTRAINT fk_tag_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES zimbra.mailbox(id)
);

CREATE TABLE *{DATABASE_NAME}.tagged_item (
   mailbox_id    INTEGER NOT NULL,
   tag_id        INTEGER NOT NULL,
   item_id       INTEGER NOT NULL,

   CONSTRAINT i_tagged_item UNIQUE (mailbox_id, tag_id, item_id),
   CONSTRAINT fk_tagged_item_tag FOREIGN KEY (mailbox_id, tag_id) REFERENCES tag(mailbox_id, id) ON DELETE CASCADE,
   CONSTRAINT fk_tagged_item_item FOREIGN KEY (mailbox_id, item_id) REFERENCES mail_item(mailbox_id, id) ON DELETE CASCADE
);

CREATE TABLE *{DATABASE_NAME}.open_conversation (
   mailbox_id  INTEGER NOT NULL,
   hash        CHAR(28) NOT NULL,
   conv_id     INTEGER NOT NULL,

   CONSTRAINT pk_open_conversation PRIMARY KEY (mailbox_id, hash),
   CONSTRAINT fk_open_conversation_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES zimbra.mailbox(id),
   CONSTRAINT fk_open_conversation_conv_id FOREIGN KEY (mailbox_id, conv_id)
      REFERENCES mail_item(mailbox_id, id) ON DELETE CASCADE
);

CREATE TABLE *{DATABASE_NAME}.appointment (
   mailbox_id  INTEGER NOT NULL,
   uid         VARCHAR(255) NOT NULL,
   item_id     INTEGER NOT NULL,
   start_time  DATETIME NOT NULL,
   end_time    DATETIME,

   CONSTRAINT pk_appointment PRIMARY KEY (mailbox_id, uid),
   CONSTRAINT i_appointment_item_id UNIQUE (mailbox_id, item_id),
   CONSTRAINT fk_appointment_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES zimbra.mailbox(id),
   CONSTRAINT fk_appointment_item_id FOREIGN KEY (mailbox_id, item_id)
      REFERENCES mail_item(mailbox_id, id) ON DELETE CASCADE
);

CREATE TABLE *{DATABASE_NAME}.appointment_dumpster (
   mailbox_id  INTEGER NOT NULL,
   uid         VARCHAR(255) NOT NULL,
   item_id     INTEGER NOT NULL,
   start_time  DATETIME NOT NULL,
   end_time    DATETIME,

   CONSTRAINT pk_appointment_dumpster PRIMARY KEY (mailbox_id, uid),
   CONSTRAINT i_appointment_dumpster_item_id UNIQUE (mailbox_id, item_id),
   CONSTRAINT fk_appointment_dumpster_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES zimbra.mailbox(id),
   CONSTRAINT fk_appointment_dumpster_item_id FOREIGN KEY (mailbox_id, item_id)
      REFERENCES mail_item_dumpster(mailbox_id, id) ON DELETE CASCADE
);

CREATE TABLE *{DATABASE_NAME}.tombstone (
   mailbox_id  INTEGER NOT NULL,
   sequence    INTEGER NOT NULL,
   date        INTEGER NOT NULL,
   type        TINYINT,
   ids         VARCHAR(255),

   CONSTRAINT fk_tombstone_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES zimbra.mailbox(id)
);

CREATE TABLE *{DATABASE_NAME}.pop3_message (
   mailbox_id     INTEGER NOT NULL,
   data_source_id CHAR(36) NOT NULL,
   uid            VARCHAR(255) NOT NULL,
   item_id        INTEGER NOT NULL,

   CONSTRAINT pk_pop3_message PRIMARY KEY (mailbox_id, item_id),
   CONSTRAINT i_uid_pop3_id UNIQUE (uid, data_source_id),
   CONSTRAINT fk_pop3_message_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES zimbra.mailbox(id)
);

CREATE TABLE *{DATABASE_NAME}.imap_folder (
   mailbox_id     INTEGER NOT NULL,
   item_id        INTEGER NOT NULL,
   data_source_id CHAR(36) NOT NULL,
   local_path     VARCHAR(1000) NOT NULL,
   remote_path    VARCHAR(1000) NOT NULL,
   uid_validity   INTEGER,

   CONSTRAINT pk_imap_folder PRIMARY KEY (mailbox_id, item_id),
   CONSTRAINT i_local_path UNIQUE (local_path, data_source_id, mailbox_id),
   CONSTRAINT i_remote_path UNIQUE (remote_path, data_source_id, mailbox_id),
   CONSTRAINT fk_imap_folder_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES zimbra.mailbox(id) ON DELETE CASCADE
);

CREATE TABLE *{DATABASE_NAME}.imap_message (
   mailbox_id     INTEGER NOT NULL,
   imap_folder_id INTEGER NOT NULL,
   uid            BIGINT NOT NULL,
   item_id        INTEGER NOT NULL,
   flags          INTEGER DEFAULT 0 NOT NULL,

   CONSTRAINT pk_imap_message PRIMARY KEY (mailbox_id, item_id),
   CONSTRAINT i_uid_imap_id UNIQUE (mailbox_id, imap_folder_id, uid),
   CONSTRAINT fk_imap_message_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES zimbra.mailbox(id) ON DELETE CASCADE,
   CONSTRAINT fk_imap_message_imap_folder_id FOREIGN KEY (mailbox_id, imap_folder_id)
      REFERENCES imap_folder(mailbox_id, item_id) ON DELETE CASCADE
);

CREATE TABLE *{DATABASE_NAME}.data_source_item (
   mailbox_id     INTEGER NOT NULL,
   data_source_id CHAR(36) NOT NULL,
   item_id        INTEGER NOT NULL,
   folder_id      INTEGER DEFAULT 0 NOT NULL,
   remote_id      VARCHAR(255) NOT NULL,
   metadata       VARCHAR(255),

   CONSTRAINT pk_data_source_item PRIMARY KEY (mailbox_id, item_id),
   CONSTRAINT i_remote_id UNIQUE (mailbox_id, data_source_id, remote_id),
   CONSTRAINT fk_data_source_item_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES zimbra.mailbox(id) ON DELETE CASCADE
);

CREATE TABLE *{DATABASE_NAME}.event (
   mailbox_id    INTEGER NOT NULL,
   account_id    VARCHAR(36) NOT NULL,  -- user performing the action (email address or guid)
   item_id       INTEGER NOT NULL,  -- itemId for the event
   folder_id     INTEGER NOT NULL,  -- folderId for the item in the event
   op            TINYINT NOT NULL,  -- operation
   ts            INTEGER NOT NULL,  -- timestamp
   version       INTEGER,           -- version of the item
   user_agent    VARCHAR(128),      -- identifier of device if available
   arg           VARCHAR(10240),    -- operation specific argument

   CONSTRAINT fk_event_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES zimbra.mailbox(id) ON DELETE CASCADE
);

CREATE TABLE *{DATABASE_NAME}.watch (
   mailbox_id   INTEGER NOT NULL,
   target       VARCHAR(36) NOT NULL,  -- watch target account id
   item_id      INTEGER NOT NULL,  -- target item id

   CONSTRAINT pk_watch PRIMARY KEY (mailbox_id, target, item_id)
);