import { ActionIcon, Box, Button, Divider, Group, Menu, MultiSelect, NumberInput, Paper, Select, Stack, TextInput, Title } from "@mantine/core";
import { isNotEmpty, useForm } from "@mantine/form";
import { IconPlus, IconRobotFace, IconTrash } from "@tabler/icons-react";
import JsonPathAutocomplete from "./JsonPathAutocomplete/JsonPathAutocomplete.tsx";
import { WorkflowDefinitionRequest, WorkflowDefinitionResponse } from "../api-types.ts";

interface WorkflowFormProps {
    initialValues?: WorkflowDefinitionResponse;
    onSubmit: (values: WorkflowDefinitionRequest, id?: string) => void;
    onCancel: () => void;
}

export function WorkflowForm({ onSubmit, onCancel, initialValues }: WorkflowFormProps) {
    const form = useForm({
        initialValues: {
            repository: { name: "", owner: "" },
            ref: "main",
            maxConcurrentRuns: 5,
            concurrencyGroup: "ONE_JOB_PER_PULL_REQUEST" as "ONE_JOB_PER_PULL_REQUEST" | "ONE_JOB_PER_REPOSITORY",
            filters: { matchingStrategy: "ALL" as "ALL" | "ANY", matchers: [{ path: "", regex: "", type: "event" }] },
            mapping: { fields: [""] },
            check: { name: "", detailsPage: { title: "", summary: "", details: "" } },
            workflowFilename: "",
            enabled: false,
            ...initialValues,
        },
        validate: {
            repository: {
                name: isNotEmpty("Repository name is required"),
                owner: isNotEmpty("Repository owner is required"),
            },
            ref: isNotEmpty("Ref is required"),
            check: {
                name: isNotEmpty("Check name is required"),
                detailsPage: {
                    title: isNotEmpty("Title is required"),
                    summary: isNotEmpty("Summary is required"),
                },
            },
            workflowFilename: isNotEmpty("Workflow file is required"),
            maxConcurrentRuns: (value) => (value < 50 ? null : "Max concurrent runs must be less than 50"),
        },
    });

    const handleSubmit = (values: typeof form.values) => {
        // Remove id from values to create request object
        const { id, ...requestData } = values as any;
        onSubmit(requestData as WorkflowDefinitionRequest, initialValues?.id);
    };

    return (
        <Box component="form" onSubmit={form.onSubmit(handleSubmit)} display="grid">
            <Stack>
                <Title order={4}>Repository Information</Title>
                <Group grow>
                    <TextInput
                        data-autofocus
                        label="Repository Owner"
                        placeholder="e.g., allegro-internal"
                        withAsterisk
                        {...form.getInputProps("repository.owner")}
                    />
                    <TextInput label="Repository Name" placeholder="e.g., security-scan" withAsterisk {...form.getInputProps("repository.name")} />
                </Group>
                <TextInput label="Reference Branch" placeholder="main" withAsterisk {...form.getInputProps("ref")} />
                <Title order={4}>Workflow Information</Title>
                <TextInput label="Workflow id" placeholder="e.g., security-scan" withAsterisk {...form.getInputProps("workflowFilename")} />
                <Group grow>
                    <Select
                        label="Concurrency Group"
                        placeholder="Select concurrency group"
                        data={[
                            { value: "ONE_JOB_PER_PULL_REQUEST", label: "One Job Per Pull Request" },
                            { value: "ONE_JOB_PER_REPOSITORY", label: "One Job Per Repository" },
                        ]}
                        {...form.getInputProps("concurrencyGroup")}
                    />
                    <NumberInput label="Max Concurrent Runs" placeholder="5" min={1} {...form.getInputProps("maxConcurrentRuns")} />
                </Group>

                <Title order={4}>Filters</Title>
                <Select
                    label="Matching Strategy"
                    w="100%"
                    data={[
                        { value: "ALL", label: "Match All" },
                        { value: "ANY", label: "Match Any" },
                    ]}
                    {...form.getInputProps("filters.matchingStrategy")}
                />
                <Stack>
                    {form.values.filters.matchers.map((_, index) => (
                        <Paper key={index} p="sm" withBorder>
                            <Group align="center">
                                <Filter
                                    type={form.values.filters.matchers[index].type}
                                    inputProps={(field) => form.getInputProps(`filters.matchers.${index}.` + field)}
                                />
                                <ActionIcon
                                    flex="0"
                                    mt="lg"
                                    color={form.values.filters.matchers.length <= 1 ? "gray" : "red"}
                                    variant="subtle"
                                    onClick={() => {
                                        if (form.values.filters.matchers.length <= 1) {
                                            form.setFieldValue("filters.matchers", [
                                                {
                                                    path: "",
                                                    regex: "",
                                                    type: "event",
                                                },
                                            ]);
                                            return;
                                        }
                                        const updatedMatchers = [...form.values.filters.matchers];
                                        updatedMatchers.splice(index, 1);
                                        form.setFieldValue("filters.matchers", updatedMatchers);
                                    }}
                                >
                                    <IconTrash size={16} />
                                </ActionIcon>
                            </Group>
                        </Paper>
                    ))}
                    <Menu shadow="md" width={500}>
                        <Menu.Target>
                            <Button leftSection={<IconPlus size={16} />} variant="light">
                                Add Matcher
                            </Button>
                        </Menu.Target>
                        <Menu.Dropdown>
                            <Menu.Label>Matcher type</Menu.Label>
                            <Menu.Item
                                onClick={() => {
                                    form.insertListItem("filters.matchers", {
                                        path: "",
                                        regex: "",
                                        type: "event",
                                    });
                                }}
                                leftSection={<IconRobotFace size={14} />}
                            >
                                Github Event
                            </Menu.Item>
                            <Menu.Item
                                onClick={() => {
                                    form.insertListItem("filters.matchers", {
                                        artifactRegex: "",
                                        type: "dependabot",
                                    });
                                }}
                                leftSection={<IconRobotFace size={14} />}
                            >
                                Dependabot Artifact
                            </Menu.Item>
                            <Menu.Item
                                onClick={() => {
                                    form.insertListItem("filters.matchers", {
                                        file: "",
                                        statuses: [],
                                        type: "file",
                                    });
                                }}
                                leftSection={<IconRobotFace size={14} />}
                            >
                                File Path
                            </Menu.Item>
                        </Menu.Dropdown>
                    </Menu>
                </Stack>

                <Title order={4}>Field Mapping</Title>
                <Stack>
                    {form.values.mapping.fields.map((_, index) => (
                        <Group key={index} align="center">
                            <JsonPathAutocomplete style={{ flex: 1 }} {...form.getInputProps(`mapping.fields.${index}`)} />
                            <ActionIcon
                                color={form.values.mapping.fields.length <= 1 ? "gray" : "red"}
                                variant="subtle"
                                onClick={() => {
                                    if (form.values.mapping.fields.length <= 1) {
                                        form.setFieldValue("mapping.fields", [""]);
                                        return;
                                    }
                                    const updatedFields = [...form.values.mapping.fields];
                                    updatedFields.splice(index, 1);
                                    form.setFieldValue("mapping.fields", updatedFields);
                                }}
                            >
                                <IconTrash size={16} />
                            </ActionIcon>
                        </Group>
                    ))}

                    <Button
                        leftSection={<IconPlus size={16} />}
                        variant="light"
                        onClick={() => {
                            form.insertListItem("mapping.fields", "");
                        }}
                    >
                        Add Field
                    </Button>
                </Stack>
                <Title order={4}>Check Information</Title>
                <TextInput label="Check Name" placeholder="e.g., Security Scan" withAsterisk {...form.getInputProps("check.name")} />
                <Divider label="Details Page" labelPosition="center" />
                <TextInput label="Title" placeholder="e.g., Security Scan 🛡️" withAsterisk {...form.getInputProps("check.detailsPage.title")} />
                <TextInput label="Summary" placeholder="Brief summary of the check" withAsterisk {...form.getInputProps("check.detailsPage.summary")} />
                <TextInput label="Details" placeholder="Additional details about the check" {...form.getInputProps("check.detailsPage.details")} />
                <Group justify="flex-end" mt="lg">
                    <Button variant="default" onClick={onCancel}>
                        Cancel
                    </Button>
                    <Button type="submit" color="blue">
                        {initialValues ? "Update" : "Save"} Workflow
                    </Button>
                </Group>
            </Stack>
        </Box>
    );
}

function Filter({ type, inputProps }: { type: string; inputProps: (field: string) => any }) {
    switch (type) {
        case "event":
            return (
                <>
                    <TextInput flex="1" label="Path" placeholder="$.action" {...inputProps("path")} />
                    <TextInput flex="1" label="Regex" placeholder="opened|synchronize" {...inputProps("regex")} />
                </>
            );
        case "dependabot":
            return <TextInput flex="1" label="Artifact Regex" placeholder="$.action" {...inputProps("artifactRegex")} />;
        case "file":
            return (
                <>
                    <TextInput label="File" placeholder="$.action" {...inputProps("file")} />
                    <MultiSelect flex="1" label="File statuses" data={["added", "modified", "removed"]} {...inputProps("statuses")} />
                </>
            );
        default:
            return null;
    }
}
