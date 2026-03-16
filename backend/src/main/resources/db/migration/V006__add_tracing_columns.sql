ALTER TABLE scheduled_tasks ADD COLUMN traceparent TEXT NULL;
ALTER TABLE scheduled_tasks_log ADD COLUMN traceparent TEXT NULL;

ALTER TABLE scheduled_tasks ADD COLUMN tracing_mode TEXT NULL;
ALTER TABLE scheduled_tasks_log ADD COLUMN tracing_mode TEXT NULL;
