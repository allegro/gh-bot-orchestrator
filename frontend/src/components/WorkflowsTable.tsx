import { ActionIcon, Anchor, Badge, Group, Table, Text, Tooltip } from "@mantine/core";
import { IconEdit, IconPower, IconTrash } from "@tabler/icons-react";
import { WorkflowDefinitionResponse } from "../api-types.ts";

interface WorkflowsTableProps {
    workflows: WorkflowDefinitionResponse[];
    onEdit: (workflow: WorkflowDefinitionResponse) => void;
    onToggle: (workflow: WorkflowDefinitionResponse) => void;
}

export function WorkflowsTable({ onEdit, workflows, onToggle }: WorkflowsTableProps) {
    const rows = workflows.map((workflow) => (
        <Table.Tr key={workflow.id}>
            <Table.Td>
                <Text>{workflow.repository.owner}</Text>
            </Table.Td>
            <Table.Td>
                <Text>{workflow.repository.name}</Text>
            </Table.Td>
            <Table.Td>
                <Anchor href={`https://github.com/${workflow.repository.owner}/${workflow.repository.name}/actions/workflows/${workflow.workflowFilename}`}>
                    {workflow.workflowFilename}
                </Anchor>
            </Table.Td>
            <Table.Td>
                <Badge>{workflow.ref}</Badge>
            </Table.Td>
            <Table.Td>
                <Text size="sm">
                    {Intl.DateTimeFormat("pl", {
                        dateStyle: "short",
                        timeStyle: "short",
                    }).format(new Date())}
                </Text>
            </Table.Td>
            <Table.Td>
                <Group gap={4} justify="flex-end">
                    <Tooltip label={workflow.enabled ? "Disable" : "Enable"}>
                        <WorkflowToggleButton state={workflow.enabled} onClick={() => onToggle(workflow)} />
                    </Tooltip>
                    <Tooltip label="Edit">
                        <ActionIcon variant="subtle" color="blue" onClick={() => onEdit(workflow)}>
                            <IconEdit size={16} />
                        </ActionIcon>
                    </Tooltip>
                    <Tooltip label="Delete">
                        <ActionIcon variant="subtle" color="red" onClick={console.log}>
                            <IconTrash size={16} />
                        </ActionIcon>
                    </Tooltip>
                </Group>
            </Table.Td>
        </Table.Tr>
    ));

    return (
        <Table striped highlightOnHover>
            <Table.Thead>
                <Table.Tr>
                    <Table.Th>Owner</Table.Th>
                    <Table.Th>Name</Table.Th>
                    <Table.Th>Workflow</Table.Th>
                    <Table.Th>Branch</Table.Th>
                    <Table.Th>Last run</Table.Th>
                    <Table.Th style={{ width: 200 }} />
                </Table.Tr>
            </Table.Thead>
            <Table.Tbody>{rows}</Table.Tbody>
        </Table>
    );
}

function WorkflowToggleButton({ state, onClick }: { state: boolean; onClick: () => void }) {
    if (state)
        return (
            <ActionIcon variant="subtle" color="green" onClick={onClick}>
                <IconPower size={16} />
            </ActionIcon>
        );
    return (
        <ActionIcon variant="subtle" color="red" onClick={onClick}>
            <IconPower size={16} />
        </ActionIcon>
    );
}
