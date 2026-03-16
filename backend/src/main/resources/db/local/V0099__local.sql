-- Workflow definitions (UUIDs — no sequence collision risk)
INSERT INTO public.workflow_definitions (id, workflow_filename, ref, enabled, max_concurrent_runs, concurrency_group, repository_name, repository_owner, check_name, check_title, check_summary, check_details) VALUES ('cfffce42-fc1f-4977-90d4-bc7abf8da4ed', 'test-workflow.yml', 'main', 'true', 5, 'ONE_JOB_PER_PULL_REQUEST', 'github-workflow-orchestrator-staging', 'allegro-internal', 'Some validation', 'Some validation', 'Your summary', 'Your details') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_definitions (id, workflow_filename, ref, enabled, max_concurrent_runs, concurrency_group, repository_name, repository_owner, check_name, check_title, check_summary, check_details) VALUES ('f0c563aa-c066-4856-a2b8-608925dc215e', 'sast.yml', 'main', 'true', 5, 'ONE_JOB_PER_PULL_REQUEST', 'security-scan', 'allegro-internal', 'Security Scan', 'Security Scan 🛡️', 'Your PR is being scanned by some vulnerability scanners like semgrep. Any interesting findings will be presented as comment.', 'In case of any doubts / suggestions - don''t hesitate to contact us on #it-security.') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_definitions (id, workflow_filename, ref, enabled, max_concurrent_runs, concurrency_group, repository_name, repository_owner, check_name, check_title, check_summary, check_details) VALUES ('f59edd15-76f9-48b3-8f82-c391b4271009', 'dependabot-obt-upgrade.yml', 'master', 'true', 5, 'ONE_JOB_PER_PULL_REQUEST', 'opbox-build-tools', 'allegro-internal', 'Opbox Build Tools Migrator', 'Opbox Build Tools Migrator from properties', 'Opbox Build Tools Migrator', 'In case of any doubts / suggestions - don''t hesitate to contact us on #help-lego.') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_definitions (id, workflow_filename, ref, enabled, max_concurrent_runs, concurrency_group, repository_name, repository_owner, check_name, check_title, check_summary, check_details) VALUES ('ed7b9b56-0781-479f-bb99-eac10439a3f5', 'validate-tycho.yml', 'main', 'true', 5, 'ONE_JOB_PER_PULL_REQUEST', 'github-workflow-orchestrator-staging', 'allegro-internal', 'Tycho validation', 'Tycho validation', 'Your tycho.yaml is being validated', 'In case of any doubts / suggestions - don''t hesitate to contact us on #help-vulcan.') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_definitions (id, workflow_filename, ref, enabled, max_concurrent_runs, concurrency_group, repository_name, repository_owner, check_name, check_title, check_summary, check_details) VALUES ('feb4c0e1-6c29-487d-9e93-c3ea86c08146', 'configuration-validation.yml', 'main', 'true', 5, 'ONE_JOB_PER_PULL_REQUEST', 'github-workflow-orchestrator-staging', 'allegro-internal', 'Configuration validation', 'Configuration validation', 'Checking your configuration repo', 'In case of any doubts / suggestions - don''t hesitate to contact us on #help-skylab-eden.') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_definitions (id, workflow_filename, ref, enabled, max_concurrent_runs, concurrency_group, repository_name, repository_owner, check_name, check_title, check_summary, check_details) VALUES ('43da8f0b-7cd5-4fb9-8d31-ae2656bc9d3c', 'validator.yml', 'master', 'true', 5, 'ONE_JOB_PER_PULL_REQUEST', 'monitoring-resources-manager', 'allegro-internal', 'Monitoring Validator', 'Monitoring Validator ⚙️', 'Validating monitoring configurations in your PR. Results will be presented as a comment.', 'Contact the monitoring team for questions or suggestions on #help-monitoring.') ON CONFLICT (id) DO NOTHING;
-- Observer - reacts to check_run conclusion=success
INSERT INTO public.workflow_definitions (id, workflow_filename, ref, enabled, max_concurrent_runs, concurrency_group, repository_name, repository_owner, check_name, check_title, check_summary, check_details) VALUES ('94fcfecf-5afb-4738-ad8b-655be9d4b775', 'observer.yml', 'master', 'true', 5, 'ONE_JOB_PER_PULL_REQUEST', 'observer', 'allegro-develop', 'Observer', 'Observer', 'Supervisor check - at least one CI check succeeded', 'In case of any doubts / suggestions - don''t hesitate to contact us on #help-vulcan.') ON CONFLICT (id) DO NOTHING;

-- Filters (id 1001-1006)
INSERT INTO public.workflow_filters (id, workflow_id, matching_strategy) VALUES (1001, 'cfffce42-fc1f-4977-90d4-bc7abf8da4ed', 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_filters (id, workflow_id, matching_strategy) VALUES (1002, 'f0c563aa-c066-4856-a2b8-608925dc215e', 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_filters (id, workflow_id, matching_strategy) VALUES (1003, 'f59edd15-76f9-48b3-8f82-c391b4271009', 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_filters (id, workflow_id, matching_strategy) VALUES (1004, 'ed7b9b56-0781-479f-bb99-eac10439a3f5', 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_filters (id, workflow_id, matching_strategy) VALUES (1005, 'feb4c0e1-6c29-487d-9e93-c3ea86c08146', 'ALL') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_filters (id, workflow_id, matching_strategy) VALUES (1006, '43da8f0b-7cd5-4fb9-8d31-ae2656bc9d3c', 'ALL') ON CONFLICT (id) DO NOTHING;
-- Observer
INSERT INTO public.workflow_filters (id, workflow_id, matching_strategy) VALUES (1100, '94fcfecf-5afb-4738-ad8b-655be9d4b775', 'ALL') ON CONFLICT (id) DO NOTHING;

-- Field mappings (id 1001-1028)
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1001, 'cfffce42-fc1f-4977-90d4-bc7abf8da4ed', '$.repository.name') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1002, 'f0c563aa-c066-4856-a2b8-608925dc215e', '$.repository.name') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1003, 'f0c563aa-c066-4856-a2b8-608925dc215e', '$.repository.topics') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1004, 'f0c563aa-c066-4856-a2b8-608925dc215e', '$.repository.owner.login') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1005, 'f0c563aa-c066-4856-a2b8-608925dc215e', '$.pull_request.head.ref') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1006, 'f0c563aa-c066-4856-a2b8-608925dc215e', '$.pull_request.number') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1007, 'f0c563aa-c066-4856-a2b8-608925dc215e', '$.pull_request.base.sha') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1008, 'f0c563aa-c066-4856-a2b8-608925dc215e', '$.pull_request.updated_at') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1009, 'f59edd15-76f9-48b3-8f82-c391b4271009', '$.repository.name') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1010, 'f59edd15-76f9-48b3-8f82-c391b4271009', '$.repository.owner.login') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1011, 'f59edd15-76f9-48b3-8f82-c391b4271009', '$.pull_request.head.ref') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1012, 'f59edd15-76f9-48b3-8f82-c391b4271009', '$.pull_request.number') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1013, 'f59edd15-76f9-48b3-8f82-c391b4271009', '$.pull_request.base.sha') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1014, 'f59edd15-76f9-48b3-8f82-c391b4271009', '$.pull_request.updated_at') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1015, 'ed7b9b56-0781-479f-bb99-eac10439a3f5', '$.repository.name') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1016, 'feb4c0e1-6c29-487d-9e93-c3ea86c08146', '$.repository.name') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1017, 'feb4c0e1-6c29-487d-9e93-c3ea86c08146', '$.repository.owner.login') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1018, 'feb4c0e1-6c29-487d-9e93-c3ea86c08146', '$.pull_request.head.ref') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1019, 'feb4c0e1-6c29-487d-9e93-c3ea86c08146', '$.pull_request.number') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1020, 'feb4c0e1-6c29-487d-9e93-c3ea86c08146', '$.pull_request.base.sha') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1021, 'feb4c0e1-6c29-487d-9e93-c3ea86c08146', '$.pull_request.updated_at') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1022, '43da8f0b-7cd5-4fb9-8d31-ae2656bc9d3c', '$.repository.name') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1023, '43da8f0b-7cd5-4fb9-8d31-ae2656bc9d3c', '$.repository.topics') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1024, '43da8f0b-7cd5-4fb9-8d31-ae2656bc9d3c', '$.repository.owner.login') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1025, '43da8f0b-7cd5-4fb9-8d31-ae2656bc9d3c', '$.pull_request.head.ref') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1026, '43da8f0b-7cd5-4fb9-8d31-ae2656bc9d3c', '$.pull_request.number') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1027, '43da8f0b-7cd5-4fb9-8d31-ae2656bc9d3c', '$.pull_request.base.sha') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1028, '43da8f0b-7cd5-4fb9-8d31-ae2656bc9d3c', '$.pull_request.updated_at') ON CONFLICT (id) DO NOTHING;
-- Observer
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1100, '94fcfecf-5afb-4738-ad8b-655be9d4b775', '$.repository.name') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1101, '94fcfecf-5afb-4738-ad8b-655be9d4b775', '$.check_run.head_sha') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1102, '94fcfecf-5afb-4738-ad8b-655be9d4b775', '$.check_run.conclusion') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1103, '94fcfecf-5afb-4738-ad8b-655be9d4b775', '$.check_run.name') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_field_mappings (id, workflow_id, json_path) VALUES (1104, '94fcfecf-5afb-4738-ad8b-655be9d4b775', '$.repository.owner.login') ON CONFLICT (id) DO NOTHING;

-- File matchers (id 1001-1002, referencing filter IDs 1004, 1006)
INSERT INTO public.workflow_file_matchers (id, filter_id, pattern, statuses) VALUES (1001, 1004, '.*tycho.yaml', '{added,modified}') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_file_matchers (id, filter_id, pattern, statuses) VALUES (1002, 1006, '^monitoring/.*$', '{added,modified,removed}') ON CONFLICT (id) DO NOTHING;

-- JSON path matchers (id 1001-1011, referencing filter IDs 1001-1006)
INSERT INTO public.workflow_json_path_matchers (id, filter_id, path, regex) VALUES (1001, 1001, '$.action', 'opened|synchronize') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_json_path_matchers (id, filter_id, path, regex) VALUES (1002, 1002, '$.action', 'opened|synchronize') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_json_path_matchers (id, filter_id, path, regex) VALUES (1003, 1002, '$.pull_request.user.login', '^(?!dependabot[bot]).*$') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_json_path_matchers (id, filter_id, path, regex) VALUES (1004, 1003, '$.action', 'opened|synchronize') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_json_path_matchers (id, filter_id, path, regex) VALUES (1005, 1003, '$.pull_request.user.login', 'dependabot\[bot\]') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_json_path_matchers (id, filter_id, path, regex) VALUES (1006, 1004, '$.action', 'opened|synchronize') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_json_path_matchers (id, filter_id, path, regex) VALUES (1007, 1004, '$.repository.name', 'andamio-autoupgrade-watchdog') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_json_path_matchers (id, filter_id, path, regex) VALUES (1008, 1005, '$.action', 'opened|synchronize') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_json_path_matchers (id, filter_id, path, regex) VALUES (1009, 1005, '$.repository.topics', '^config$') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_json_path_matchers (id, filter_id, path, regex) VALUES (1010, 1005, '$.repository.name', '(github-workflow-orchestrator|skycaptcha)-config') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_json_path_matchers (id, filter_id, path, regex) VALUES (1011, 1006, '$.action', 'opened|synchronize|closed') ON CONFLICT (id) DO NOTHING;
-- Observer
INSERT INTO public.workflow_json_path_matchers (id, filter_id, path, regex) VALUES (1100, 1100, '$.action', 'completed') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_json_path_matchers (id, filter_id, path, regex) VALUES (1101, 1100, '$.check_run.conclusion', 'success') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_json_path_matchers (id, filter_id, path, regex) VALUES (1102, 1100, '$.check_run.app.slug', 'github-actions') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.workflow_json_path_matchers (id, filter_id, path, regex) VALUES (1103, 1100, '$.check_run.name', '^(?!Observer).*$') ON CONFLICT (id) DO NOTHING;

-- Dependabot matchers (id 1001, referencing filter ID 1003)
INSERT INTO public.workflow_dependabot_matchers (id, filter_id, artifact_regex) VALUES (1001, 1003, '.*opbox-build-tools.*') ON CONFLICT (id) DO NOTHING;

-- Reset sequences past the highest seeded ID so GUI-created rows don't collide
SELECT setval(pg_get_serial_sequence('public.workflow_filters', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM public.workflow_filters), 1100), true);
SELECT setval(pg_get_serial_sequence('public.workflow_field_mappings', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM public.workflow_field_mappings), 1104), true);
SELECT setval(pg_get_serial_sequence('public.workflow_file_matchers', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM public.workflow_file_matchers), 1002), true);
SELECT setval(pg_get_serial_sequence('public.workflow_json_path_matchers', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM public.workflow_json_path_matchers), 1103), true);
SELECT setval(pg_get_serial_sequence('public.workflow_dependabot_matchers', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM public.workflow_dependabot_matchers), 1001), true);
