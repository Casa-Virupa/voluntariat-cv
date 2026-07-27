CREATE TABLE `admin_user` (
	`email` text PRIMARY KEY NOT NULL,
	`display_name` text,
	`role` text NOT NULL,
	`added_by` text NOT NULL,
	`added_at` integer NOT NULL,
	`disabled_at` integer
);
--> statement-breakpoint
CREATE TABLE `admin_user_area` (
	`email` text NOT NULL,
	`area` text NOT NULL,
	PRIMARY KEY(`email`, `area`),
	FOREIGN KEY (`email`) REFERENCES `admin_user`(`email`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE TABLE `app_setting` (
	`key` text PRIMARY KEY NOT NULL,
	`value_json` text NOT NULL,
	`updated_at` integer NOT NULL,
	`updated_by` text
);
--> statement-breakpoint
CREATE TABLE `audit_log` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`actor_email` text NOT NULL,
	`action` text NOT NULL,
	`entity` text NOT NULL,
	`entity_id` text,
	`before_json` text,
	`after_json` text,
	`at` integer NOT NULL
);
--> statement-breakpoint
CREATE INDEX `ix_audit_at` ON `audit_log` (`at`);--> statement-breakpoint
CREATE TABLE `charge_locked` (
	`doc_id` text NOT NULL,
	`item` text NOT NULL,
	`user_id` text NOT NULL,
	`service_date` text NOT NULL,
	`qty` integer NOT NULL,
	`unit_price_cents` integer NOT NULL,
	`amount_cents` integer NOT NULL,
	`locked_at` integer NOT NULL,
	PRIMARY KEY(`doc_id`, `item`)
);
--> statement-breakpoint
CREATE INDEX `ix_charge_locked_user` ON `charge_locked` (`user_id`,`service_date`);--> statement-breakpoint
CREATE TABLE `commitment_rule` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`scope_kind` text NOT NULL,
	`scope_value` text,
	`area` text NOT NULL,
	`period_kind` text NOT NULL,
	`target_minutes` integer NOT NULL,
	`valid_from` text NOT NULL,
	`valid_to` text,
	`note` text,
	`created_by` text NOT NULL,
	`created_at` integer NOT NULL
);
--> statement-breakpoint
CREATE INDEX `ix_commitment_lookup` ON `commitment_rule` (`area`,`scope_kind`,`scope_value`,`valid_from`);--> statement-breakpoint
CREATE TABLE `fs_booking` (
	`doc_id` text PRIMARY KEY NOT NULL,
	`user_id` text NOT NULL,
	`ts_epoch` integer NOT NULL,
	`service_date` text NOT NULL,
	`has_lunch` integer DEFAULT false NOT NULL,
	`has_dinner` integer DEFAULT false NOT NULL,
	`sleep` integer DEFAULT false NOT NULL,
	`meals_raw_json` text DEFAULT '[]' NOT NULL,
	`shift_count` integer DEFAULT 0 NOT NULL,
	`total_minutes` integer DEFAULT 0 NOT NULL,
	`anomaly_flags` integer DEFAULT 0 NOT NULL,
	`raw_json` text NOT NULL,
	`doc_hash` text NOT NULL,
	`first_seen_at` integer NOT NULL,
	`last_seen_at` integer NOT NULL,
	`deleted_at` integer
);
--> statement-breakpoint
CREATE INDEX `ix_booking_date` ON `fs_booking` (`service_date`);--> statement-breakpoint
CREATE INDEX `ix_booking_user_date` ON `fs_booking` (`user_id`,`service_date`);--> statement-breakpoint
CREATE INDEX `ix_booking_anomaly` ON `fs_booking` (`anomaly_flags`);--> statement-breakpoint
CREATE TABLE `fs_booking_shift` (
	`doc_id` text NOT NULL,
	`seq` integer NOT NULL,
	`slot` text NOT NULL,
	`kind` text NOT NULL,
	`area` text NOT NULL,
	`start_sec` integer,
	`end_sec` integer,
	`minutes` integer DEFAULT 0 NOT NULL,
	PRIMARY KEY(`doc_id`, `seq`),
	FOREIGN KEY (`doc_id`) REFERENCES `fs_booking`(`doc_id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `ix_shift_area` ON `fs_booking_shift` (`area`);--> statement-breakpoint
CREATE TABLE `fs_payment` (
	`doc_id` text PRIMARY KEY NOT NULL,
	`user_id` text NOT NULL,
	`year` integer NOT NULL,
	`month` integer NOT NULL,
	`paid` integer DEFAULT false NOT NULL,
	`amount_cents` integer DEFAULT 0 NOT NULL,
	`amount_raw` real DEFAULT 0 NOT NULL,
	`raw_json` text NOT NULL,
	`doc_hash` text NOT NULL,
	`first_seen_at` integer NOT NULL,
	`last_seen_at` integer NOT NULL,
	`deleted_at` integer
);
--> statement-breakpoint
CREATE INDEX `ix_payment_period` ON `fs_payment` (`user_id`,`year`,`month`);--> statement-breakpoint
CREATE TABLE `fs_user` (
	`uid` text PRIMARY KEY NOT NULL,
	`name` text DEFAULT '' NOT NULL,
	`email` text DEFAULT '' NOT NULL,
	`role` text DEFAULT 'unknown' NOT NULL,
	`volunteer_type` text,
	`onboarding_completed` integer DEFAULT false NOT NULL,
	`is_member` integer DEFAULT false NOT NULL,
	`raw_json` text NOT NULL,
	`doc_hash` text NOT NULL,
	`first_seen_at` integer NOT NULL,
	`last_seen_at` integer NOT NULL,
	`deleted_at` integer
);
--> statement-breakpoint
CREATE INDEX `ix_fs_user_email` ON `fs_user` (`email`);--> statement-breakpoint
CREATE INDEX `ix_fs_user_role` ON `fs_user` (`role`,`volunteer_type`);--> statement-breakpoint
CREATE TABLE `fs_user_area` (
	`uid` text NOT NULL,
	`area` text NOT NULL,
	PRIMARY KEY(`uid`, `area`)
);
--> statement-breakpoint
CREATE INDEX `ix_fs_user_area` ON `fs_user_area` (`area`);--> statement-breakpoint
CREATE TABLE `ledger_entry` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`user_id` text NOT NULL,
	`kind` text NOT NULL,
	`effective_date` text NOT NULL,
	`amount_cents` integer NOT NULL,
	`method` text,
	`note` text,
	`created_by` text NOT NULL,
	`created_at` integer NOT NULL,
	`voids_id` integer,
	`external_ref` text
);
--> statement-breakpoint
CREATE INDEX `ix_ledger_user_date` ON `ledger_entry` (`user_id`,`effective_date`);--> statement-breakpoint
CREATE UNIQUE INDEX `ux_ledger_external_ref` ON `ledger_entry` (`external_ref`);--> statement-breakpoint
CREATE TABLE `period_close` (
	`user_id` text NOT NULL,
	`period_kind` text NOT NULL,
	`period_year` integer NOT NULL,
	`period_index` integer NOT NULL,
	`closed_at` integer NOT NULL,
	`closed_by` text NOT NULL,
	`total_cents` integer NOT NULL,
	PRIMARY KEY(`user_id`, `period_kind`, `period_year`, `period_index`)
);
--> statement-breakpoint
CREATE TABLE `price_rule` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`item` text NOT NULL,
	`volunteer_type` text,
	`is_member` integer,
	`unit_price_cents` integer NOT NULL,
	`valid_from` text NOT NULL,
	`valid_to` text,
	`note` text,
	`created_by` text NOT NULL,
	`created_at` integer NOT NULL
);
--> statement-breakpoint
CREATE INDEX `ix_price_lookup` ON `price_rule` (`item`,`valid_from`);--> statement-breakpoint
CREATE TABLE `sync_run` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`mode` text NOT NULL,
	`trigger` text NOT NULL,
	`triggered_by` text,
	`status` text NOT NULL,
	`started_at` integer NOT NULL,
	`heartbeat_at` integer NOT NULL,
	`finished_at` integer,
	`window_from` text,
	`read_users` integer DEFAULT 0 NOT NULL,
	`read_bookings` integer DEFAULT 0 NOT NULL,
	`read_payments` integer DEFAULT 0 NOT NULL,
	`inserted_bookings` integer DEFAULT 0 NOT NULL,
	`deleted_bookings` integer DEFAULT 0 NOT NULL,
	`anomalies` integer DEFAULT 0 NOT NULL,
	`error_message` text
);
--> statement-breakpoint
CREATE UNIQUE INDEX `ux_sync_single_run` ON `sync_run` (`status`) WHERE "sync_run"."status" = 'running';--> statement-breakpoint
CREATE INDEX `ix_sync_started` ON `sync_run` (`started_at`);--> statement-breakpoint
CREATE TABLE `writeback_log` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`run_id` integer,
	`payment_doc_id` text,
	`user_id` text NOT NULL,
	`year` integer NOT NULL,
	`month` integer NOT NULL,
	`expected_amount_cents` integer,
	`expected_paid` integer,
	`new_amount_cents` integer NOT NULL,
	`new_paid` integer NOT NULL,
	`status` text NOT NULL,
	`error` text,
	`created_at` integer NOT NULL
);
--> statement-breakpoint
CREATE INDEX `ix_writeback_period` ON `writeback_log` (`user_id`,`year`,`month`);