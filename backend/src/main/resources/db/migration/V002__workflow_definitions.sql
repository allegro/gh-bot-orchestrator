CREATE TABLE workflow_definitions
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_filename   TEXT    NOT NULL,
    ref                 TEXT    NOT NULL,
    max_concurrent_runs INT     NOT NULL,
    concurrency_group   TEXT,
    repository_name     TEXT    NOT NULL,
    repository_owner    TEXT    NOT NULL,
    check_name          TEXT    NOT NULL,
    check_title         TEXT,
    check_summary       TEXT,
    check_details       TEXT,
    enabled             BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workflow_filters
(
    id                SERIAL PRIMARY KEY,
    workflow_id       UUID NOT NULL REFERENCES workflow_definitions (id) ON DELETE CASCADE,
    matching_strategy TEXT NOT NULL,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workflow_json_path_matchers
(
    id         SERIAL PRIMARY KEY,
    filter_id  INT  NOT NULL REFERENCES workflow_filters (id) ON DELETE CASCADE,
    path       TEXT NOT NULL,
    regex      TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workflow_file_matchers
(
    id         SERIAL PRIMARY KEY,
    filter_id  INT    NOT NULL REFERENCES workflow_filters (id) ON DELETE CASCADE,
    pattern    TEXT   NOT NULL,
    statuses   TEXT[] NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workflow_dependabot_matchers
(
    id             SERIAL PRIMARY KEY,
    filter_id      INT  NOT NULL REFERENCES workflow_filters (id) ON DELETE CASCADE,
    artifact_regex TEXT NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workflow_field_mappings
(
    id          SERIAL PRIMARY KEY,
    workflow_id UUID NOT NULL REFERENCES workflow_definitions (id) ON DELETE CASCADE,
    json_path   TEXT NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE workflow_run_availability
    RENAME COLUMN workflow_label TO workflow_id;

ALTER TABLE workflow_run_availability
    ALTER COLUMN workflow_id TYPE UUID USING workflow_id::UUID;

CREATE OR REPLACE FUNCTION delete_scheduled_tasks_on_workflow_removal()
    RETURNS TRIGGER AS
$$
BEGIN
    DELETE
    FROM scheduled_tasks
    WHERE task_data ->> 'workflowId' = OLD.id::text;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_delete_scheduled_tasks
    AFTER DELETE
    ON workflow_definitions
    FOR EACH ROW
EXECUTE FUNCTION delete_scheduled_tasks_on_workflow_removal();
