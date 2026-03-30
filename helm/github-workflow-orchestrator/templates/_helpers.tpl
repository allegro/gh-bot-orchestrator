{{/*
Expand the name of the chart.
*/}}
{{- define "gh-bot-orchestrator.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "gh-bot-orchestrator.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "gh-bot-orchestrator.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels.
*/}}
{{- define "gh-bot-orchestrator.labels" -}}
helm.sh/chart: {{ include "gh-bot-orchestrator.chart" . }}
{{ include "gh-bot-orchestrator.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels.
*/}}
{{- define "gh-bot-orchestrator.selectorLabels" -}}
app.kubernetes.io/name: {{ include "gh-bot-orchestrator.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Service account name.
*/}}
{{- define "gh-bot-orchestrator.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "gh-bot-orchestrator.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
PostgreSQL fully qualified name.
*/}}
{{- define "gh-bot-orchestrator.postgresql.fullname" -}}
{{- printf "%s-postgresql" (include "gh-bot-orchestrator.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
PostgreSQL host.
*/}}
{{- define "gh-bot-orchestrator.postgresql.host" -}}
{{- if .Values.postgresql.enabled }}
{{- include "gh-bot-orchestrator.postgresql.fullname" . }}
{{- else }}
{{- .Values.externalDatabase.host }}
{{- end }}
{{- end }}

{{/*
PostgreSQL port.
*/}}
{{- define "gh-bot-orchestrator.postgresql.port" -}}
{{- if .Values.postgresql.enabled }}
{{- .Values.postgresql.port }}
{{- else }}
{{- .Values.externalDatabase.port }}
{{- end }}
{{- end }}

{{/*
PostgreSQL database name.
*/}}
{{- define "gh-bot-orchestrator.postgresql.database" -}}
{{- if .Values.postgresql.enabled }}
{{- .Values.postgresql.auth.database }}
{{- else }}
{{- .Values.externalDatabase.database }}
{{- end }}
{{- end }}

{{/*
PostgreSQL username.
*/}}
{{- define "gh-bot-orchestrator.postgresql.username" -}}
{{- if .Values.postgresql.enabled }}
{{- .Values.postgresql.auth.username }}
{{- else }}
{{- .Values.externalDatabase.username }}
{{- end }}
{{- end }}

{{/*
PostgreSQL JDBC URL.
*/}}
{{- define "gh-bot-orchestrator.postgresql.jdbcUrl" -}}
{{- printf "jdbc:postgresql://%s:%s/%s" (include "gh-bot-orchestrator.postgresql.host" .) (include "gh-bot-orchestrator.postgresql.port" . | toString) (include "gh-bot-orchestrator.postgresql.database" .) }}
{{- end }}

{{/*
PostgreSQL secret name (for password).
*/}}
{{- define "gh-bot-orchestrator.postgresql.secretName" -}}
{{- if .Values.postgresql.enabled }}
  {{- if .Values.postgresql.auth.existingSecret }}
    {{- .Values.postgresql.auth.existingSecret }}
  {{- else }}
    {{- include "gh-bot-orchestrator.postgresql.fullname" . }}
  {{- end }}
{{- else }}
  {{- if .Values.externalDatabase.existingSecret }}
    {{- .Values.externalDatabase.existingSecret }}
  {{- else }}
    {{- printf "%s-external-db" (include "gh-bot-orchestrator.fullname" .) | trunc 63 | trimSuffix "-" }}
  {{- end }}
{{- end }}
{{- end }}

{{/*
GitHub App secret name.
*/}}
{{- define "gh-bot-orchestrator.github.secretName" -}}
{{- if .Values.app.github.auth.existingSecret }}
{{- .Values.app.github.auth.existingSecret }}
{{- else }}
{{- printf "%s-github" (include "gh-bot-orchestrator.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{/*
PostgreSQL labels.
*/}}
{{- define "gh-bot-orchestrator.postgresql.labels" -}}
helm.sh/chart: {{ include "gh-bot-orchestrator.chart" . }}
{{ include "gh-bot-orchestrator.postgresql.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
PostgreSQL selector labels.
*/}}
{{- define "gh-bot-orchestrator.postgresql.selectorLabels" -}}
app.kubernetes.io/name: {{ include "gh-bot-orchestrator.name" . }}-postgresql
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: database
{{- end }}
