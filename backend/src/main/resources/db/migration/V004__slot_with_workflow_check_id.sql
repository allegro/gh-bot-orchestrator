ALTER TABLE public.workflow_run_availability
    ADD COLUMN workflow_check_id varchar,
    ADD COLUMN workflow_target_repo_owner varchar,
    ADD COLUMN workflow_target_repo_name varchar;
