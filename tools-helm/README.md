# Getting started with Helm charts

This document provides instructions about how to create a new Helm chart starting from the Talend [starter-chart](https://github.com/Talend/tools/tree/iiosif/ARCH-15_helm_charts/tools-helm/starter-chart)


### Introduction
The starter-chart is a template chart which can be used as the start point for any Helm Chart. It contains the most basic Kubernetes objects which are required for every Kubernetes application (i.e. deployments, services and configmaps). The starter chart defines one 
single pod and one single docker container for the pod.

_Note: The starter chart is work in progress and will be enhanced with new Kubernetes objects_

### Pre-install
Copy the starter-chart folder to your project folder and follow the next steps:
1. Rename the folder to your service name/project name (e.g. tpsvc-config)
1. Go through all the files inside the folder and look for placeholders of the form ```<some_name>```. Replace these placeholders with your own values.
1. Configure the build system to replace the placeholder ```@service.version@``` from the ```values.yaml``` and ```Chart.yaml``` with the
service's version number. 
1. Update the values.yaml file with additional ```labels:values``` required for your chart
1. Add your service's environment variables to the file ```templates/configmap.yaml```
1. Enhance the chart with new Kubernetes objects (only if necessary)
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