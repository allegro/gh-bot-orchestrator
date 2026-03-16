ALTER TABLE public.comment_callback_urls
    ADD COLUMN target_owner text;

ALTER TABLE public.comment_callback_urls
    ADD COLUMN target_repository text;

CREATE OR REPLACE FUNCTION delete_scheduled_tasks_on_workflow_removal()
    RETURNS TRIGGER AS
$$
BEGIN
DELETE
FROM scheduled_tasks
WHERE convert_from(task_data, 'UTF8')::jsonb ->> 'workflowId' = OLD.id::text;
RETURN OLD;
END;
$$ LANGUAGE plpgsql;
