create table public.comment_callback_urls
(
    id                  uuid primary key not null,
    slot_id             uuid             not null,
    owner               text             not null,
    repository          text             not null,
    pull_request_number integer          not null,
    body                text,
    github_comment_id   bigint
);
create unique index comment_callback_urls_slot_id_idx on comment_callback_urls using btree (slot_id);

create table public.scheduled_tasks
(
    task_name            text                     not null,
    task_instance        text                     not null,
    task_data            bytea,
    execution_time       timestamp with time zone not null,
    picked               boolean                  not null,
    picked_by            text,
    last_success         timestamp with time zone,
    last_failure         timestamp with time zone,
    consecutive_failures integer,
    last_heartbeat       timestamp with time zone,
    version              bigint                   not null,
    created_at           timestamp with time zone not null default now(),
    resource_id          text,
    trace_id             text,
    request_id           text,
    primary key (task_name, task_instance)
);

create index scheduled_tasks_resource_id_idx on scheduled_tasks using btree (resource_id)
    WHERE (resource_id IS NOT NULL);

create index scheduled_tasks_execution_time_idx on scheduled_tasks using btree (execution_time);

create index scheduled_tasks_last_heartbeat_idx on scheduled_tasks using btree (last_heartbeat);

create table public.scheduled_tasks_log
(
    task_name            text                     not null,
    task_instance        text                     not null,
    task_data            text,
    scheduled_at         timestamp with time zone,
    task_status          text                     not null,
    last_success         timestamp with time zone,
    consecutive_failures integer                  not null,
    last_failure         timestamp with time zone,
    created_at           timestamp with time zone not null default now(),
    resource_id          text,
    request_id           text,
    trace_id             text,
    primary key (task_name, task_instance)
);

create index scheduled_tasks_log_last_success_idx on scheduled_tasks_log using btree (last_success)
    WHERE (last_success IS NOT NULL);

create index scheduled_tasks_log_task_status_idx on scheduled_tasks_log using btree (task_status);

create table public.workflow_run_availability
(
    slot_id                    uuid primary key            not null,
    workflow_label             text                        not null,
    concurrency_group          text                        not null,
    change_timestamp           timestamp without time zone not null,
    workflow_concurrency_group integer                     not null,
    finished_at                timestamp without time zone
);

create unique index workflow_run_availability_workflow_label_concurrency_group_idx on workflow_run_availability
    using btree (workflow_label, concurrency_group) WHERE (finished_at IS NULL);

create unique index workflow_run_availability_workflow_label_workflow_concurren_idx on workflow_run_availability
    using btree (workflow_label, workflow_concurrency_group) WHERE (finished_at IS NULL);

