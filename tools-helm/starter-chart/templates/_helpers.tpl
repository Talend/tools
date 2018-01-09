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
{{- .Values.global.registryKey | default "talendregistry" -}}
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
Define the default service port.(must be shorter than 15 chars and must contain only lowercase letters)
*/}}
{{- define "<service_name>.servicePortName" -}}
{{- $name := (include "tpsvc-config.name" .) -}}
{{- default .Chart.Name $name | trunc 10 | printf "%sport" -}}
{{- end -}}

{{/*
Define the docker registry value
*/}}
{{- define "<service_name>.imageRegistry" -}}
{{- $envValues := pluck .Values.global.env .Values | first }}
{{- $imageRegistry := default .Values.image $envValues.image | pluck "registry" | first | default .Values.image.registry -}}
{{- if empty $imageRegistry -}}
    {{- "" -}}
{{else}}
   {{- $imageRegistry -}}
{{- end -}}
{{- end -}}

{{/*
Define the docker image.
*/}}
{{- define "<service_name>.image" -}}
{{- $envValues := pluck .Values.global.env .Values | first }}
{{- $imageRegistry := include "<service_name>.imageRegistry" . -}}
{{- $imagePath := default .Values.image $envValues.image | pluck "path" | first | default .Values.image.path -}}
{{- if eq $imageRegistry "" -}}
    {{- $imagePath -}}
{{else}}
    {{- printf "%s/%s" $imageRegistry $imagePath -}}
{{- end -}}
{{- end -}}

