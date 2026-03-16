/* eslint-disable */
/* tslint:disable */
// @ts-nocheck
/*
 * ---------------------------------------------------------------
 * ## THIS FILE WAS GENERATED VIA SWAGGER-TYPESCRIPT-API        ##
 * ##                                                           ##
 * ## AUTHOR: acacode                                           ##
 * ## SOURCE: https://github.com/acacode/swagger-typescript-api ##
 * ---------------------------------------------------------------
 */

type UtilRequiredKeys<T, K extends keyof T> = Omit<T, K> & Required<Pick<T, K>>;

export interface CanceledTasksResponse {
    /** @uniqueItems true */
    canceledTasks: string[];
}

export interface CheckResponse {
    detailsPage: DetailsPageResponse;
    name: string;
}

export interface CheckRunUpdateRequest {
    detailsPage?: DetailsPageUpdateRequest;
    status: string;
}

export interface CommentDto {
    body: string;
    mode?: string;
}

export type DependabotMatcherResponse = UtilRequiredKeys<MatcherResponse, "type"> & {
    artifactRegex: string;
};

export interface DetailsPageResponse {
    details: string;
    summary: string;
    title: string;
}

export interface DetailsPageUpdateRequest {
    customUrl?: string;
    details?: string;
    summary: string;
    title: string;
}

export type Errors = object;

export interface ErrorsHolder {
    errors?: Errors;
}

export type FileMatcherResponse = UtilRequiredKeys<MatcherResponse, "type"> & {
    file: string;
    /** @uniqueItems true */
    statuses: string[];
};

export interface FiltersResponse {
    matchers: (DependabotMatcherResponse | FileMatcherResponse | JsonPathMatcherResponse)[];
    matchingStrategy: "ALL" | "ANY";
}

export interface GithubPrincipal {
    /** @format int64 */
    id: number;
    login: string;
    type?: string;
}

export interface GithubPullRequest {
    base: GithubRef;
    body?: string;
    head: GithubRef;
    html_url: string;
    /** @format int32 */
    number: number;
    title: string;
    updated_at: string;
    url: string;
    user: GithubPrincipal;
}

export interface GithubRef {
    ref: string;
    repo: GithubRepo;
    sha: string;
}

export interface GithubRepo {
    full_name: string;
    /** @format int64 */
    id: number;
    name: string;
    owner: GithubPrincipal;
    /** @uniqueItems true */
    topics: string[];
}

export interface Id {
    name: string;
    owner: string;
}

export type JsonPathMatcherResponse = UtilRequiredKeys<MatcherResponse, "type"> & {
    path: string;
    regex: string;
};

export interface MappingResponse {
    fields: string[];
}

export interface MatcherResponse {
    type: string;
}

export interface OwnerDto {
    login: string;
}

export interface PullRequestEvent {
    event: PullRequestEventPayload;
}

export interface PullRequestEventPayload {
    action: string;
    pull_request: GithubPullRequest;
    repository: GithubRepo;
    sender: GithubPrincipal;
}

export interface Repository {
    fullName: string;
    id: Id;
}

export interface RepositoryDto {
    name: string;
    owner: OwnerDto;
}

export interface RepositoryResponse {
    name: string;
    owner: string;
}

export interface WorkflowDefinitionResponse {
    check: CheckResponse;
    concurrencyGroup: "ONE_JOB_PER_REPOSITORY" | "ONE_JOB_PER_PULL_REQUEST";
    enabled: boolean;
    filters: FiltersResponse;
    id: string;
    mapping: MappingResponse;
    /** @format int32 */
    maxConcurrentRuns: number;
    ref: string;
    repository: RepositoryResponse;
    workflowFilename: string;
}

export type WorkflowDefinitionRequest = Omit<WorkflowDefinitionResponse, "id">;

export interface WorkflowRunDto {
    display_title: string;
    head_branch: string;
    id: string;
    path: string;
    repository: RepositoryDto;
}

export interface WorkflowRunFinishedEvent {
    action: string;
    event: WorkflowRunFinishedEventDto;
    path: string;
    ref: string;
    repository: Repository;
    slotId: string;
}

export interface WorkflowRunFinishedEventDto {
    action: string;
    workflow_run: WorkflowRunDto;
}
