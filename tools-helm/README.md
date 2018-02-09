# Getting started with Helm charts

This document provides instructions about how to create a new Helm chart starting from the Talend [starter-chart](https://github.com/Talend/tools/tree/iiosif/ARCH-15_helm_charts/tools-helm/starter-chart)


### Introduction
The starter-chart is a template chart which can be used as the start point for any Helm Chart. It contains the most basic Kubernetes objects which are required for every Kubernetes application (i.e. deployments, services and configmaps). The starter chart defines one single pod and one single docker container for the pod.

_Note: The starter chart is work in progress and will be enhanced with new Kubernetes objects_

### General recommendations

1. Chart names shall not contain dashes (Note: Helm has a problem with values which contain dashes)
1. Dependencies tags could contain dashes as long as their values are not used inside the templates 
1. For common charts (charts which are intended to be consumed by upper level charts), it is recommended to tag all dependencies with the name of the chart followed by ```-standalone```. This allows consumer charts to easily disable all the dependencies of the common charts. 
```tags:
     commonchart-standalone: false
```
1. When using aliases for dependencies it is not possible to import values from child charts into the parent chart. This is an [issue] (https://github.com/kubernetes/helm/issues/3457)which might be solved in one of the next releases.
Templates
1. When working with templates always use the ```include``` function. Do NOT use the ```template``` function.

### General information

1. Common charts used by Talend products are grouped in two main categories: Infrastructure and Platform, as showed below. 
```
|-- infrastructure
   |-- livy
   |-- kafka
   |-- zookeeper
   |-- mongodb
   |-- postgresql
   |-- ...
|-- platform
   |-- iam
   |-- iamscim
   |-- configsvc
   |-- sharing
   |-- provisioning
   |-- tcomp
   |-- ...
```
This construct provides several advantages:
- allows to import all values of the low-level charts (i.e. kafka, zookeeper, etc..) into an upper chart (right now Helm doesn't allow to import all values of a child chart into a parent chart)
- provides deduplication of services/charts
- provides a place for common templates which are required by upper level charts (i.e. "infrastructure.postgresql.url" which returns the Postgresql URL)
- allows easy separation of charts in different helm releases and namespaces
Whenever an application chart (or any upper level chart) needs one of the charts hosted by the infrastructure chart, the application shall add the infrastructure charts as dependency to its own chart. By default, the infrastructure charts are disabled through tags. It is the application's responsibility to enable the required infrastructure charts. The ```README.md``` file of each chart provides information about the content of the chart and about the values and templates exposed by the chart.


### Pre-install
Copy the starter-chart folder to your project folder and follow the next steps:
1. Rename the folder to your service name/project name (e.g. tpsvc-config)
1. Go through all the files inside the folder and look for placeholders of the form ```<service_name>```. Replace these placeholders with your own values.
1. Replace <appNameVariable> in the files values.yaml and /templates/_helpers.tpl with a name specific to your chart(s). This variable should be the same for
charts that are part of the same release/versioning cycle (i.e. streamsVersion could be used for all charts belonging to data streams). This is a global value and must be set to the docker image version of the chart
1. Configure the build system to replace the placeholder ```@service.version@``` from the ```values.yaml``` and ```Chart.yaml``` with the
service's version number. 
1. Update the values.yaml file with additional ```labels:values``` required for your chart
1. Add your service's environment variables to the file ```templates/configmap.yaml```
1. Enhance the chart with new Kubernetes objects (if necessary)
1. Create a secret for the Talend docker registry as follows:
   ```
   kubectl create secret docker-registry talend-docker-registry --docker-server=registry.datapwn.com --docker-username=<you user name> --docker-password="<your password>" --docker-email=<your email>
   ```
1. Test the chart before installation
   ```
   helm install <chart_folder> --name <release_name> —debug —dry-run 
   ```

### Install
1. Install the chart on your Kubernetes cluster with the following command:
   ```
   helm install <chart_folder> --name <release_name> —debug —dry-run 
   ```

### Post-install
1. Upload the chart to the chartmuseum (ToDo)

### Additional info
1. Wiki page [Kubernetes tips and useful commands](https://wiki.talend.com/display/rd/Kubernetes+tips+and+useful+commands)
1. Check [ToDo.md](https://github.com/Talend/tools/blob/iiosif/ARCH-15_helm_charts/tools-helm/ToDo.md) for background information related to the starter chart
1. [Talend Kubernets and Helm policy](https://github.com/Talend/policies/blob/iiosif/ARCH-15_kubernetes_policy/official/KubernetesPolicy.md)


Naming

1. Chart's names must not contain dashes
1. Tags name could contain dashes and when they do they should start with the current chart's name

Templates

1. When working with templates always use the ```include``` function. Do NOT use the ```template``` function.
