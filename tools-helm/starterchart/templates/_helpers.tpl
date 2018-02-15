{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
Truncate at 63 chars characters due to limitations of the DNS system.
*/}}
{{- define "<service_name>.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "<service_name>.fullname" -}}
{{- $name := (include "<service_name>.name" .) -}}
{{- printf "%s-%s" .Release.Name $name -}}
{{- end -}}

{{/*
Create a default chart name including the version number
*/}}
{{- define "<service_name>.chart" -}}
{{- $name := (include "<service_name>.name" .) -}}
{{- printf "%s-%s" $name .Chart.Version | replace "+" "_" -}}
{{- end -}}

{{/*
Define the docker registry key.
*/}}
{{- define "<service_name>.registryKey" -}}
{{- .Values.global.registryKey | default "" -}}
{{- end -}}

{{/*
Define labels which are used throughout the chart files
*/}}
{{- define "<service_name>.labels" -}}
app: {{ include "<service_name>.fullname" . }}
chart: {{ include "<service_name>.chart" . }}
release: {{ .Release.Name }}
heritage: {{ .Release.Service }}
{{- end -}}

{{/*
Define the docker image.
*/}}
{{- define "<service_name>.image" -}}
{{- if eq (default "" .Values.image.registry) "" -}}
    {{- printf "%s:%s" .Values.image.path (default .Values.global.<appNameVariable> .Values.image.tag | default "latest" ) -}}
{{else}}
    {{- printf "%s/%s:%s" .Values.image.registry .Values.image.path (default .Values.global.<appNameVariable> .Values.image.tag | default "latest" ) -}}
{{- end -}}
{{- end -}}

{{/*
Create a default fully qualified service name.
Truncate at 63 chars characters due to limitations of the DNS system.
*/}}
{{- define "<service_name>.service.name" -}}
{{- $name := .Values.service.name| trunc 63 | trimSuffix "-" -}}
{{- printf "%s-%s" .Release.Name $name -}}
{{- end -}}