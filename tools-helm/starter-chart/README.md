# <Service_name>

<provide short description of service here>

## TL;DR;

```bash
$ helm install <chart_folder_location>
```

### Introduction
<provide more detailed information about the service here>

### Prerequisites
- Kubernetes v1.8+
- <other dependencies or prerequisites>

### Installing the chart
To install the chart with the release name <service_name> you need to 

```$ helm install --name <service_name> <proj_root_folder>/deploy/kubernetes/<service_name>```

The command deploys the <service_name> Service on the Kubernetes cluster in the default configuration. The [configuration](#configuration) section lists the parameters that can be configured during installation.

### Uninstalling the chart

To uninstall/delete the <service_name> deployment:

```$ helm delete --purge <service_name>```

This command removes all the Kubernetes components associated with the chart and deletes the release.

### Configuration

The chart uses two global values which need to be set before installing this chart: ```global.env``` and ```global.infraReleaseName```.

The following tables lists the configurable parameters of the Platform Configuration Service chart and their default values. 
These values can be configured independently for different releases (i.e. prod, qa, dev, etc...)

Parameter                      | Description	                                    | Default
-------------------------------|--------------------------------------------------|--------------------------------
`registryKey`                  | k8s secret for the docker registry               | talendregistry
`replicaCount`                 | Number of containers running in parallel         | 1
`global.registry`              | Docker registry                                  | registry.datapwn.com
`global.repositoryUser`        | GitHub user name                                 | talend
`image.repositoryName`         | GitHub repo name                                 | 
`image.tag`                    | Image tag/version                                | 
`image.pullPolicy`             | Image pull policy	                              | IfNotPresent
`service.name`                 | k8s service name                                 | 

You can override these values at runtime using the `--set key=value[,key=value]` argument to `helm install`. For example,

```bash
$ helm install --name <service_name> \
  --set <parameter1>=<value1>,<parameter2>=<value2> \
    <proj_root_folder>/deploy/kubernetes/<service_name>
```

The above command deploys the <service_name> Service in the k8s cluster and sets the values of ...

Alternatively, a YAML file that specifies the values for the parameters can be provided while installing the chart. For example,

```bash
$ helm install --name <release_name> -f <path>/values.yaml <proj_root_folder>/deploy/kubernetes/<service_name>
```

> **Tip**: You can use the default [values.yaml](values.yaml)


### Persistence - Volumes

<explain persistance requirements/definition here>

