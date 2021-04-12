# Helper script to connect to Talend K8S clusters

## Requirements
This scripts assumes that `okta-awscli`, `aws` and `az` cli tools are installed.

## k8s-cluster-connect.sh
Script to launch to connect to a K8S cluster

```
Usage: k8s-cluster-connect.sh [-h] [-v] [-b] region

Helper script to connect to Talend K8S clusters.

region : value should be in dev, int, int-admin, qa-admin, ci-admin, at, at-admin, staging, staging-admin, us, eu, ap, us-admin, eu-admin, ap-admin, az-sandbox, az-sandbox-admin, az-staging, az-staging-admin,az-prodanz, az-prodanz-admin, az-produs, az-produs-admin
Available options:

-h, --help      Print this help and exit
-v, --verbose   Print script debug info
-b, --bashrc   Display an example of lines to add in your bashrc file to ease kubernetes usage
```

## k8s-cluster-connect.bashrc
Example of scriptlet to include in bashrc.

To use this scriptlet add the 3 following line in your bashrc file

```
if [ -f ~/projects/tools/tools-k8s-connect/k8s-cluster-connect.bashrc]; then
   . ~/projects/tools/tools-k8s-connect/k8s-cluster-connect.bashrc
fi
```

You can use `kl` alias to connect even more easily to the K8S clusters.
