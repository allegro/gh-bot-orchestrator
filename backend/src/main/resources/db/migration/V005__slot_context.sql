CREATE TABLE public.slot_context (
    slot_id uuid primary key,
    repo_owner varchar,
    repo_name varchar,
    ref varchar,
    check_run_id varchar,
    check_title varchar,
    pull_request_number bigint,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
