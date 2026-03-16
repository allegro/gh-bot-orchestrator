import { createFileRoute } from "@tanstack/react-router";
import { Button, Group, Stack, Title } from "@mantine/core";
import { createWorkflowDefinition, getWorkflowsDefinitions, updateWorkflowDefinition } from "../api.ts";
import { useMutation, useSuspenseQuery } from "@tanstack/react-query";
import FullScreenLoader from "../components/FullScreenLoader.tsx";
import { useTranslation } from "react-i18next";
import { WorkflowsTable } from "../components/WorkflowsTable.tsx";
import { IconPlus } from "@tabler/icons-react";
import { modals } from "@mantine/modals";
import { WorkflowForm } from "../components/WorkflowForm.tsx";
import { WorkflowDefinitionRequest, WorkflowDefinitionResponse } from "../api-types.ts";

export const Route = createFileRoute("/")({
    component: Dashboard,
    pendingComponent: FullScreenLoader,
});

function Dashboard() {
    const workflowsQuery = useSuspenseQuery(getWorkflowsDefinitions());
    const { t } = useTranslation();

    const createMutation = useMutation({
        mutationFn: createWorkflowDefinition,
        onSuccess: () => {
            workflowsQuery.refetch();
            modals.closeAll();
        },
    });

    const updateMutation = useMutation({
        mutationFn: ({ id, workflow }: { id: string; workflow: WorkflowDefinitionRequest }) => updateWorkflowDefinition(id, workflow),
        onSuccess: () => {
            workflowsQuery.refetch();
            modals.closeAll();
        },
    });

    const toggleMutation = useMutation({
        mutationFn: ({ id, workflow }: { id: string; workflow: WorkflowDefinitionRequest }) => updateWorkflowDefinition(id, workflow),
        onSuccess: () => {
            workflowsQuery.refetch();
        },
    });

    const handleSubmit = (workflow: WorkflowDefinitionRequest, id?: string) => {
        if (id) {
            updateMutation.mutate({ id, workflow });
        } else {
            createMutation.mutate(workflow);
        }
    };

    const handleToggle = (workflow: WorkflowDefinitionResponse) => {
        const { id, ...workflowData } = workflow;
        const updatedWorkflow: WorkflowDefinitionRequest = {
            ...workflowData,
            enabled: !workflow.enabled,
        };
        toggleMutation.mutate({ id, workflow: updatedWorkflow });
    };

    return (
        <Stack>
            <Group justify="space-between">
                <Title order={2}>{t("dashboard.title")}</Title>
                <AddWorkflowButton onSubmit={handleSubmit} />
            </Group>
            <WorkflowsTable
                workflows={workflowsQuery.data}
                onEdit={(workflow) =>
                    modals.open({
                        title: "Edit workflow",
                        size: "xl",
                        centered: true,
                        closeOnClickOutside: false,
                        children: <WorkflowForm initialValues={workflow} onSubmit={handleSubmit} onCancel={() => modals.closeAll()} />,
                    })
                }
                onToggle={handleToggle}
            />
        </Stack>
    );
}

function AddWorkflowButton({ onSubmit }: { onSubmit: (workflow: WorkflowDefinitionRequest) => void }) {
    return (
        <Button
            leftSection={<IconPlus size={16} />}
            onClick={() => {
                modals.open({
                    title: "Add workflow",
                    size: "xl",
                    centered: true,
                    closeOnClickOutside: false,
                    children: <WorkflowForm onSubmit={onSubmit} onCancel={() => modals.closeAll()} />,
                });
            }}
        >
            Create Workflow
        </Button>
    );
}
