-- Workflow definition
INSERT INTO public.workflow_definitions (id, workflow_filename, ref, enabled, max_concurrent_runs, concurrency_group, repository_name, repository_owner, check_name, check_title, check_summary, check_details) VALUES ('cfffce42-fc1f-4977-90d4-bc7abf8da4ed', 'some-workflow.yml', 'main', 'true', 5, 'ONE_JOB_PER_PULL_REQUEST', 'some-repository', 'some-owner', 'some-check', 'Some check', 'Some summary', 'Some details') ON CONFLICT (id) DO NOTHING;

-- Filter
INSERT INTO public.workflow_filters (id, workflow_id, matching_strategy) VALUES (1001, 'cfffce42-fc1f-4977-90d4-bc7abf8da4ed', 'ALL') ON CONFLICT (id) DO NOTHING;

-- Field mapping
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1001, 'cfffce42-fc1f-4977-90d4-bc7abf8da4ed', '$.repository.name') ON CONFLICT (id) DO NOTHING;

-- JSON path matcher
INSERT INTO public.workflow_json_path_matchers (id, filter_id, path, regex) VALUES (1001, 1001, '$.action', 'opened|synchronize') ON CONFLICT (id) DO NOTHING;

-- Reset sequences past the highest seeded ID so GUI-created rows don't collide
SELECT setval(pg_get_serial_sequence('public.workflow_filters', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM public.workflow_filters), 1001), true);
SELECT setval(pg_get_serial_sequence('public.workflow_field_mappings', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM public.workflow_field_mappings), 1001), true);
SELECT setval(pg_get_serial_sequence('public.workflow_file_matchers', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM public.workflow_file_matchers), 1), true);
SELECT setval(pg_get_serial_sequence('public.workflow_json_path_matchers', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM public.workflow_json_path_matchers), 1001), true);
SELECT setval(pg_get_serial_sequence('public.workflow_dependabot_matchers', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM public.workflow_dependabot_matchers), 1), true);
