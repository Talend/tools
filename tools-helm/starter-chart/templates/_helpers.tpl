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
{{- .Values.global.registryKey | default "talendregistry" }}
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
{{- $envValues := pluck .Values.global.env .Values | first }}
{{- $imageRepositoryName := default .Values.image.repositoryName $envValues.image.repositoryName -}}
{{- $imageTag := default .Values.image.tag $envValues.image.tag -}}
{{- if eq .Values.global.registry "" -}}
    {{- printf "%s/%s:%s" .Values.global.repositoryUser $imageRepositoryName $imageTag -}}
{{else}}
    {{- printf "%s/%s/%s:%s" .Values.global.registry .Values.global.repositoryUser $imageRepositoryName $imageTag -}}
{{- end -}}
{{- end -}}

{{/*
Define the default service service port.(must be shorter than 15 chars)
*/}}
{{- define "<service_name>.servicePortName" -}}
{{- $envValues := pluck .Values.global.env .Values | first }}
{{- default .Chart.Name .Values.nameOverride | trunc 10 | printf "%sport" -}}
{{- end -}}
