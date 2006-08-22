# 
# ***** BEGIN LICENSE BLOCK *****
# Version: MPL 1.1
# 
# The contents of this file are subject to the Mozilla Public License
# Version 1.1 ("License"). You may not use this file except in
# compliance with the License. You may obtain a copy of the License at
# http://www.zimbra.com/license
# 
# Software distributed under the License is distributed on an "AS IS"
# basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
# the License for the specific language governing rights and limitations
# under the License.
# 
# The Original Code is: Zimbra Collaboration Suite Server.
# 
# The Initial Developer of the Original Code is Zimbra, Inc.
# Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
# All Rights Reserved.
# 
# Contributor(s):
# 
# ***** END LICENSE BLOCK *****
# 
DROP DATABASE IF EXISTS ${DATABASE_NAME};

CREATE DATABASE ${DATABASE_NAME}
DEFAULT CHARACTER SET utf8;

CREATE TABLE IF NOT EXISTS ${DATABASE_NAME}.mail_item (
   mailbox_id    INTEGER UNSIGNED NOT NULL,
   id            INTEGER UNSIGNED NOT NULL,
   type          TINYINT NOT NULL,           # 1 = folder, 3 = tag, etc.
   parent_id     INTEGER UNSIGNED,
   folder_id     INTEGER UNSIGNED,
   index_id      INTEGER UNSIGNED,
   imap_id       INTEGER UNSIGNED,
   date          INTEGER UNSIGNED NOT NULL,  # stored as a UNIX-style timestamp
   size          INTEGER UNSIGNED NOT NULL,
   volume_id     TINYINT UNSIGNED,
   blob_digest   VARCHAR(28) BINARY,         # reference to blob, meaningful for messages only (type == 5)
   unread        INTEGER UNSIGNED,           # stored separately from the other flags so we can index it
   flags         INTEGER NOT NULL DEFAULT 0,
   tags          BIGINT NOT NULL DEFAULT 0,
   sender        VARCHAR(128),
   subject       TEXT,
   metadata      TEXT,
   mod_metadata  INTEGER UNSIGNED NOT NULL,  # change number for last row modification
   change_date   INTEGER UNSIGNED,           # UNIX-style timestamp for last row modification
   mod_content   INTEGER UNSIGNED NOT NULL,  # change number for last change to "content" (e.g. blob)

   PRIMARY KEY (mailbox_id, id),
   INDEX i_type (mailbox_id, type),          # for looking up folders and tags
   INDEX i_parent_id (mailbox_id, parent_id),# for looking up a parent\'s children
   INDEX i_folder_id_date (mailbox_id, folder_id, date), # for looking up by folder and sorting by date
   INDEX i_index_id (mailbox_id, index_id),  # for looking up based on search results
   INDEX i_unread (mailbox_id, unread),      # there should be a small number of items with unread=TRUE
                                             # no compound index on (unread, date), so we save space at
                                             # the expense of sorting a small number of rows
   INDEX i_date (mailbox_id, date),          # fallback index in case other constraints are not specified
   INDEX i_mod_metadata (mailbox_id, mod_metadata),      # used by the sync code
   INDEX i_tags_date (mailbox_id, tags, date),           # for tag searches
   INDEX i_flags_date (mailbox_id, flags, date),         # for flag searches
   INDEX i_volume_id (mailbox_id, volume_id),            # for the foreign key into the volume table

   CONSTRAINT fk_mail_item_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES zimbra.mailbox(id),
   #CONSTRAINT fk_mail_item_parent_id FOREIGN KEY (mailbox_id, parent_id) REFERENCES ${DATABASE_NAME}.mail_item(mailbox_id, id),
   #CONSTRAINT fk_mail_item_folder_id FOREIGN KEY (mailbox_id, folder_id) REFERENCES ${DATABASE_NAME}.mail_item(mailbox_id, id),
   CONSTRAINT fk_mail_item_volume_id FOREIGN KEY (volume_id) REFERENCES zimbra.volume(id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS ${DATABASE_NAME}.open_conversation (
   mailbox_id  INTEGER UNSIGNED NOT NULL,
   hash        CHAR(28) BINARY NOT NULL,
   conv_id     INTEGER UNSIGNED NOT NULL,

   PRIMARY KEY (mailbox_id, hash),
   INDEX i_conv_id (mailbox_id, conv_id),
   CONSTRAINT fk_open_conversation_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES zimbra.mailbox(id),
   CONSTRAINT fk_open_conversation_conv_id FOREIGN KEY (mailbox_id, conv_id) REFERENCES ${DATABASE_NAME}.mail_item(mailbox_id, id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS ${DATABASE_NAME}.appointment (
   mailbox_id  INTEGER UNSIGNED NOT NULL,
   uid         VARCHAR(255) NOT NULL,
   item_id     INTEGER UNSIGNED NOT NULL,
   start_time  DATETIME NOT NULL,
   end_time    DATETIME,

   PRIMARY KEY (mailbox_id, uid),
   INDEX i_item_id (mailbox_id, item_id),
   CONSTRAINT fk_appointment_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES zimbra.mailbox(id),
   CONSTRAINT fk_appointment_item_id FOREIGN KEY (mailbox_id, item_id) REFERENCES ${DATABASE_NAME}.mail_item(mailbox_id, id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS ${DATABASE_NAME}.tombstone (
   mailbox_id  INTEGER UNSIGNED NOT NULL,
   sequence    INTEGER UNSIGNED NOT NULL,  # change number for deletion
   date        INTEGER UNSIGNED NOT NULL,  # deletion date as a UNIX-style timestamp
   ids         TEXT,

   INDEX i_sequence (mailbox_id, sequence),
   CONSTRAINT fk_tombstone_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES zimbra.mailbox(id)
) ENGINE = InnoDB;
