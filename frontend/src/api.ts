import { queryOptions } from "@tanstack/react-query";
import axios from "axios";
import { notifications } from "@mantine/notifications";
import { WorkflowDefinitionRequest, WorkflowDefinitionResponse } from "./api-types.ts";

axios.interceptors.request.use(async (config) => {
    return config;
});

axios.interceptors.response.use(
    (response) => response,
    (error) => {
        notifications.show({
            title: "Error",
            message: error.response?.data?.message || "Could not fetch data",
            color: "red",
        });
        return Promise.reject(error);
    },
);

export function getWorkflowsDefinitions() {
    return queryOptions({
        queryKey: ["workflows-definitions"],
        queryFn: async (): Promise<WorkflowDefinitionResponse[]> => {
            return await axios.get<WorkflowDefinitionResponse[]>(`/api/workflows`).then((response) => response.data);
        },
    });
}

export async function createWorkflowDefinition(workflow: WorkflowDefinitionRequest): Promise<WorkflowDefinitionResponse> {
    const response = await axios.post<WorkflowDefinitionResponse>("/api/workflows", workflow);
    notifications.show({
        title: "Success",
        message: "Workflow created successfully",
        color: "green",
    });
    return response.data;
}

export async function updateWorkflowDefinition(id: string, workflow: WorkflowDefinitionRequest): Promise<WorkflowDefinitionResponse> {
    const response = await axios.put<WorkflowDefinitionResponse>(`/api/workflows/${id}`, workflow);
    notifications.show({
        title: "Success",
        message: "Workflow updated successfully",
        color: "green",
    });
    return response.data;
}

export async function deleteWorkflowDefinition(id: string): Promise<void> {
    await axios.delete(`/api/workflows/${id}`);
    notifications.show({
        title: "Success",
        message: "Workflow deleted successfully",
        color: "green",
    });
}
