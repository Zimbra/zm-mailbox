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

DELETE FROM *{DATABASE_NAME}.mail_item;
DELETE FROM *{DATABASE_NAME}.mail_item_dumpster;
DELETE FROM *{DATABASE_NAME}.mail_address;
DELETE FROM *{DATABASE_NAME}.revision;
DELETE FROM *{DATABASE_NAME}.revision_dumpster;
DELETE FROM *{DATABASE_NAME}.open_conversation;
DELETE FROM *{DATABASE_NAME}.appointment;
DELETE FROM *{DATABASE_NAME}.appointment_dumpster;
DELETE FROM *{DATABASE_NAME}.tombstone;
DELETE FROM *{DATABASE_NAME}.pop3_message;
DELETE FROM *{DATABASE_NAME}.imap_folder;
DELETE FROM *{DATABASE_NAME}.imap_message;
DELETE FROM *{DATABASE_NAME}.data_source_item;

DELETE FROM ZIMBRA.mailbox;
DELETE FROM ZIMBRA.current_volumes;
DELETE FROM ZIMBRA.deleted_account;
DELETE FROM ZIMBRA.mailbox_metadata;
DELETE FROM ZIMBRA.out_of_office;
DELETE FROM ZIMBRA.config;
DELETE FROM ZIMBRA.table_maintenance;
DELETE FROM ZIMBRA.service_status;
DELETE FROM ZIMBRA.scheduled_task;
DELETE FROM ZIMBRA.mobile_devices;

INSERT INTO current_volumes (message_volume_id, index_volume_id, next_mailbox_id) VALUES (1, 2, 1);
