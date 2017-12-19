## ToDo List for Kubernetes & Helm


## Open Tasks
1. Naming convention for secrets (and everthing else)
   - Release name 
     - MUST be unique for the whole cluster [See Helm issue 2060](https://github.com/kubernetes/helm/issues/2060)
     - for development it should be equal to the branch name
     - for production it could be something like: prod, dev, qa (need input from DevOps)
   - Namespaces (equal to the Release Name)
1. What are the use cases for application persistence? Are there any?


## In-Progress Tasks

1. How do we work with databases in/from a k8s cluster?
   - Use a k8s service to connect to the database - independent from where the database is deployed (inside or outside the k8s cluster)
1. How do we provide access to databases? Where do we store users, passwords, keys, etc...?
   - Define secrets in k8s and access them as environment variables or as files from pods
   - DevOps are responsible for creating the secrets
1. Define a starter chart 
   - Configuration Service will serve as template for the starter chart
1. Define a location for charts
   - Projects Charts are stored in each project's GitHub repository
   - Infrastructure Chart and Infrastructure Chart dependencies are stored in a central GitHub repository [talend/helm-charts-infrastructure](https://github.com/Talend/helm-charts-infrastructure)
   - Platform Chart is stored in a common Platform repository
   - Starter Chart is stored in the repository [talend/tools](https://github.com/Talend/tools.git)
1. Create a chart registry [DEVOPS-3279](https://jira.talendforge.org/browse/DEVOPS-3279)
   - Run chartmuseum locally and download/upload your own charts there 
       docker run --rm -it \
       -p 8080:8080 \
       -v ~/.aws:/root/.aws:ro \
       chartmuseum/chartmuseum:latest \
       --debug --port=8080 \
       --storage="amazon" \
       --storage-amazon-bucket="us-east-1-helm-registry-dev" \
       --storage-amazon-prefix="/dev" \
       --storage-amazon-region="us-east-1"
1. Do we use the default release names or should we define our own?
   - It is recommended to define our own release names - Do NOT use Helm default release names

## Done Tasks

1. Snapshot versions in charts - do we need to do something here?
   - In general we don't use the version qualifier at all
1. Chart versioning
   - The version of the chart should be the same with the version of the app. This should be updated/synchronized at build time.
1. Reference value files from the requirement files
   - This is not possible with Helm
   - An external tool is required for dynamic dependencies
1. Templates - _helpers.tpl
   - Template names are global - need special attention here because templates with same name can overwrite each other (the last loaded template will overwrite the previous ones)
   - We can have only one single _helpers.tpl in the parent chart and reference templates defined inside it from all subcharts
1. Local docker environment vs minikube docker environment
   - we can point the local docker env to the minikube docker env
   - we can't point the the minikube docker env to the local docker env
   - we can install a docker registry in minikube and reference it from the local docker env
     - this implies that minikube needs to run all the time
1. Values files
   - Values files should be kept at chart level and parent charts should not need to know about values in subcharts 
   - Each ```values.yaml``` file should have different sections corresponding to different environments as described below:
	 
    ```
	    prod:
	      cpu:
	      memory:
	    eval:
	      cpu:
	      memory:
     dev:
       cpu:
       memory:
    ```
	The values will be accessed like this:

     ``` 
     {{- $envValues := pluck .Values.global.env .Values | first | default .Values.dev }}
     cpu: {{ $envValues.cpu }}
	   memory: {{ $envValues.memory }}
     ```  
   
     __PLEASE note that the global variable ```env``` MUST be declared and it MUST be different than nil.__
1. Manging chart dependencies
   See section "Managing Chart Dependencies" below
1. Define structure for full/partial stacks
   See section "Managing Chart Dependencies" below


## Managing Chart Dependencies

A Helm chart can depend on one of more charts which can then depend on other charts. Dependencies can be defined through the ```requirements.yaml``` file or through the ```charts``` folder. If two charts share a dependency, Helm will install that dependency two times, which is in most situations undesired. The following lists provide an overview of software modules/components/applications used, respectively developed, by Talend. 

__Infrastructure__
- PostgreSQL
- MongoDB
- Redis
- Kafka
- Zookeeper

__Platform__
- Configuration
- IAM (IDP, SCIM, Provisioning)
- Rating
- Sharing
- Tcomp (?) 
- Dataset

__Applications__
- Data Streams
  - Data Streams RT
- Data Catalog
- Data Preparation
  - Data Preparation RT

In the next paragraphs, we will explain how this list could be represented by Helm charts and how different deployment stacks could be developed using Helm charts.

There will be two parent charts for the Infrastructure and the Platform categories.

Each component belonging to the Infrastructure category will have its own chart. The Infrastructure Chart will have a hard dependency to all of its components charts. This dependency will be defined in the ```Infrastructure_Chart/requirements.yaml``` file as follows:

```
dependencies:
- name: mongodb
  version: 3.4.9
  repository: https://talend-charts.storage.api.com
  condition: global.mongodb.enabled
  tags:
    - mongodb
```
Each dependency will have a condition pointing to a global value named ```global.<Chart.Name>.enabled```. Similarly, each dependency will have a tag named ```<Chart.Name>```. The condition allows component charts to be enabled/disabled as desired from any other chart which uses the Infrastructure Chart. The tag is only useful for the Infrastructure chart as this value can't be propagated to a higher level in a chart hierarchy.

The Platform Chart follows the same pattern as the Infrastructure chart. There will be no hard dependency between the Platform chart and the Infrastructure chart. Platform subcharts which depend on Infrastructure subcharts will use reference them through either ```.Values.global.infraReleaseName-.Chart.Name``` (in case the infrastructure charts are deployed under a different release name than the Platform charts) or just ```.Release.Name-.Chart.Name```.

For applications, there will be no parent chart but each application will have its own chart. The applications charts will not have hard dependencies on Infrastructure or Platform charts. In order to test an application, a stack chart need to be created which has hard dependencies on Infrastructure, Platform and Application charts. The following example shows the ```requirements.yaml``` file of a stack chart:
```
dependencies:
- name: infrastructure
  version: 0.1.0
  repository: https://talend-charts.storage.api.com
  condition: global.infrastructure.enabled
  tags:
    - infrastructure
- name: platform
  version: 0.1.0
  repository: https://talend-charts.storage.api.com
  condition: global.platform.enabled
  tags:
    - platform
- name: data-streams
  version: 0.1.0
  repository: https://talend-charts.storage.api.com
  condition: global.data-streams.enabled
  tags:
    - data-streams
```

When a stack chart is installed it is recommended to install it in its own k8s namespace. In this case the namespace could be set to the release name.
```helm install <chart_folder> --name <release_name> --namespace <release_name> --set global.env=[prod | env | dev]```

For development and testing purposes the Infrastructure chart and the Platform chart could be installed separately so that they can be reused by multiple consumer charts. In this case the parameter ```.Values.global.infraReleaseName``` resp. ```.Values.global.platformReleaseName```need to be set at installation time.
```
helm install <chart_folder> --name <release_name> --namespace <release_name> \
   --set global.env=[prod | env | dev],\
   .Values.global.infraReleaseName=<infrastructure_release_name>,\
   .Values.global.infraReleaseName=<infrastructure_release_name>   

```

-------------------------------------------------------------------------------------------------------------------------------

The following list is the output of a meeting between Francis, Seb and Iosif on Nov 16th
	
	1. Starter chart which can be used for all projects
	2. Decide on the version of the chart
		a. Start version
		b. Is the chart version equal to the project version?
		c. Can we reference the version in the values.yaml file?
		d. Can we import the version from somewhere?
	3. Dependencies
		a. Can we template the dependencies?
		b. Shall we have all dependencies listed and the consumer will remove what he/she doesn't needs?
	4. Global chart names versus local names
		a. Names must be unique
		b. Reference local name globally
	5. Can we reference values file from the requirements file?
		a. We don't want to repeat the same value all the time (i.e. repository)
	6. How do we deal with snapshot versions in dependencies?
	7. Use aggregate charts
		a. Valid only for core services
	8. _helpers.tpl
		a. Do we need this in every chart?
		b. Can we define it in a global place?
		c. Have a build action which will copy this file from a remote location into here and will   put the right values into it
		d. Read the required values from an external file - which could be part each project in github
		e. Have a global file for general stuff and local files for local stuff
	9. Configmaps
		a. We should not pre-assign ports
		b. Use kubernetes dns
		c. Use SRV record in dns
		d. Define here variables used in multiple pods
	10. Local docker env vs minikube docker env
		a. Is there a way to reference in both ways?
	11. Remote debug
		a. Can be defined in the deplyoments.yaml
	12. Set the jvm settings in the docker parameters
		a. Jvm is not aware of the memory limits of the container
	13. Need to be able to set resources for containers
		a. Usually defined in values.yaml but they can be overwritten in a consumer values file
	14. K8s namespace vs chart release versions
	15. Do we work with the default helm charts release names or shall we provide our own release names?