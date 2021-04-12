#!/usr/bin/env bash
# Storing if shell script is called from source or not
[[ $_ != $0 ]] && SOURCED=true || SOURCED=false

set -Eeuo pipefail
trap cleanup SIGINT SIGTERM ERR EXIT

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd -P)


# Source 
# - https://github.com/Talend/sre-documentation/blob/master/Infrastructure/kubernetes_access.md
# - https://wiki.talend.com/pages/viewpage.action?pageId=26380077




usage() {
  cat <<EOF
Usage: $(basename "${BASH_SOURCE[0]}") [-h] [-v] [-b] region 

Helper script to connect to Talend K8S clusters.

region : value should be in dev, int, int-admin, qa-admin, ci-admin, at, at-admin, staging, staging-admin, us, eu, ap, us-admin, eu-admin, ap-admin, az-sandbox, az-sandbox-admin, az-staging, az-staging-admin,az-prodanz, az-prodanz-admin, az-produs, az-produs-admin
Available options:

-h, --help      Print this help and exit
-v, --verbose   Print script debug info
-b, --bashrc   Display an example of lines to add in your bashrc file to ease kubernetes usage
EOF
  # -f, --flag      Some flag description
  # -p, --param     Some param description
  die ""
}



cleanup() {
  trap - SIGINT SIGTERM ERR EXIT
  # script cleanup here
}

setup_colors() {
  if [[ -t 2 ]] && [[ -z "${NO_COLOR-}" ]] && [[ "${TERM-}" != "dumb" ]]; then
    NOFORMAT='\033[0m' RED='\033[0;31m' GREEN='\033[0;32m' ORANGE='\033[0;33m' BLUE='\033[0;34m' PURPLE='\033[0;35m' CYAN='\033[0;36m' YELLOW='\033[1;33m'
  else
    NOFORMAT='' RED='' GREEN='' ORANGE='' BLUE='' PURPLE='' CYAN='' YELLOW=''
  fi
}

msg() {
  echo >&2 -e "${1-}"
}

die() {
  local msg=$1
  local code=${2-1} # default exit status 1
  msg "${RED}$msg${NOFORMAT}"
  [[ $SOURCED = true ]] && return "$code" || exit "$code"
}

parse_params() {
  # default values of variables set from params
  flag=0
  param=''

  while :; do
    case "${1-}" in
    -h | --help) usage ;;
    -v | --verbose) set -x ;;
    -b | --bashrc) bashrc ;;
    --no-color) NO_COLOR=1 ;;
    -f | --flag) flag=1 ;; # example flag
    -p | --param) # example named parameter
      param="${2-}"
      shift
      ;;
    -?*) die "Unknown option: $1" ;;
    *) break ;;
    esac
    shift
  done

  args=("$@")

  # check required params and arguments
  # [[ -z "${region-}" ]] && die "Missing required parameter: param"
  [[ ${#args[@]} -eq 0 ]] && die "Missing script arguments. Use --help option to display usage."

  return 0
}

check_deps() {
  if ! command -v okta-awscli &> /dev/null
  then
   echo "You have first to create ~/.okta-aws file with the following content"
   cat <<EOF
[default]
base-url = talend.okta.com
duration = 43200
username = <your okta user name>
factor = OKTA
app-link = https://talend.okta.com/home/amazon_aws/0oamkfs1qoOsgGdhq0x7/272
role = arn:aws:iam::054713022081:role/AWSAccountUser
EOF
   die "okta-awscli could not be found"
  fi
  if ! command -v aws &> /dev/null
  then
      die "aws could not be found"
  fi
  if ! command -v az &> /dev/null
  then
      die "az could not be found"
  fi
}

setup_colors
parse_params "$@"
check_deps

# script logic here
# msg "${RED}Read parameters:${NOFORMAT}"
# msg "- flag: ${flag}"
# msg "- arguments: ${args[*]-}"
# msg "${args[0]}"
#
# Helper function to update k
update_k8s_config(){
        export KUBECONFIG=$(for i in $(find $HOME/.kube -iname '*.config') ; do echo -n ":$i"; done | cut -c 2-)
}

K8S_REGION=${args[0]}

# Setting the default values
AWS_REGION=us-east-1
AWS_ACCOUNT=054713022081
ROLE_PREFIX="eks_developer_"
AZURE=false

# Changing the defualt shell background color
printf %b '\e]11;#7700ff\a'

case $K8S_REGION in
        int | dev)
            OKTA_ROLE="arn:aws:iam::054713022081:role\/AWSAccountUser"
            sed -i "s/role =.*/role = $OKTA_ROLE/g" ~/.okta-aws
            ;;
        int-admin| qa-admin| ci-admin)         
            sed -i 's/role =.*/role = arn:aws:iam::054713022081:role\/AWSAccountAdmin/g' ~/.okta-aws
            ;;
        at)
            AWS_ACCOUNT=946092842050
            sed -i 's/role =.*/role = arn:aws:iam::946092842050:role\/AWSAccountPowerUser/g' ~/.okta-aws
            ;;
        at-admin)
            AWS_ACCOUNT=946092842050
            sed -i 's/role =.*/role = arn:aws:iam::946092842050:role\/AWSAccountAdmin/g' ~/.okta-aws
            ;;
  
        staging)
            AWS_ACCOUNT=676807884358
            sed -i 's/role =.*/role = arn:aws:iam::676807884358:role\/AWSAccountAdmin/g' ~/.okta-aws
            ;;
        us|eu|ap)
            AWS_ACCOUNT=172292293482
            sed -i 's/role =.*/role = arn:aws:iam::172292293482:role\/AWSAccountAuditor/g' ~/.okta-aws
            ;;
        us-admin|eu-admin|ap-admin)
            AWS_ACCOUNT=172292293482
            sed -i 's/role =.*/role = arn:aws:iam::172292293482:role\/AWSAccountAdmin/g' ~/.okta-aws
            ;;
        az-*)
            AZURE=true
            ;;
        *)
           die "${RED}Region parameter as an improper value. Use --help flag for usage.${NOFORMAT}"
           ;;
esac

# Setting the role prefix for admin connections
case $K8S_REGION in
        dev| *-admin)
              ROLE_PREFIX="eks_admin_"
              ;;  
esac

case $K8S_REGION in
        int-admin| qa-admin| ci-admin)
                printf %b '\e]11;#143d18\a'
                ;; 
        at-admin| staging-admin)
                printf %b '\e]11;#001eff\a'
                ;;   
        *-admin)
                printf %b '\e]11;#ff0000\a'
                ;;  
esac

case $K8S_REGION in
        dev)
                CLUSTER_NAME=aws_dev
                ;;
        int| int-admin)
                CLUSTER_NAME=integration_stable
                ;;
        ci)
                CLUSTER_NAME=ci_stable
                ;;
        qa-admin)
                CLUSTER_NAME=qa_stable
                ;;
        at| at-admin)
                CLUSTER_NAME=at_stable
                ;;
        staging| staging-admin)
                CLUSTER_NAME=staging_stable
                ;;
        us| us-admin)
                CLUSTER_NAME=us_stable
                ;;
        eu| eu-admin)
                CLUSTER_NAME=eu_stable
                AWS_REGION=eu-central-1
                ;;
        ap| ap-admin)
                CLUSTER_NAME=apacpr_stable
                AWS_REGION=ap-northeast-1
                ;;
        az-sandbox| az-sandbox-admin)
                CLUSTER_NAME=azure-sandbox-kubernetes-vmss
                SUBSCRIPTION="Engineering - Sandbox"
                ;;
        az-staging| az-staging-admin)
                CLUSTER_NAME=azure-staging-kubernetes-vmss
                SUBSCRIPTION="Engineering - Staging"
                ;;
        az-prodanz| az-prodanz-admin)
                CLUSTER_NAME=azure-anz-production-kubernetes-vmss
                SUBSCRIPTION="Engineering - Prod - ANZ"
                ;;
        az-produs| az-produs-admin)
                CLUSTER_NAME=azure-produs-kubernetes-vmss
                SUBSCRIPTION="Engineering - Prod - US"
                ;;
        *)
                die "${RED}Region parameter as an improper value or script need to be fixed. Use --help flag for usage.${NOFORMAT}"
esac


# Silently create the talend subdirectory in ~/.kube 
mkdir -p ~/.kube/talend
NEW_KUBECONFIG="$HOME/.kube/talend/$K8S_REGION.config"
[[ -f $NEW_KUBECONFIG ]] && K8S_FILE_CREATED=false || K8S_FILE_CREATED=true

if [ "$AZURE" = true ] ; then
        RESOURCE_GROUP=kubernetes-vmss
        msg "Connecting to Azure"
        az login
        # kubectl config set current-context "$NAME-admin"
        if [ "$AZURE" = true ]; then
                case $K8S_REGION in    
                        *-admin)
                                az aks get-credentials --subscription "$SUBSCRIPTION" --resource-group "$RESOURCE_GROUP"  --name "$CLUSTER_NAME" --file "$NEW_KUBECONFIG" --overwrite-existing --admin
                                CLUSTER_NAME="$CLUSTER_NAME-admin"
                                ;;
                        *)
                                az aks get-credentials --subscription "$SUBSCRIPTION" --resource-group "$RESOURCE_GROUP"  --name "$CLUSTER_NAME" --file "$NEW_KUBECONFIG" --overwrite-existing
                                ;;
                esac
        fi
        # CONTEXT=$CLUSTER_NAME
else
        case $K8S_REGION in
        *-admin)
                OLD_CTX=$(echo $K8S_REGION | cut -d'-' -f 1)
                OLD_CTX="$HOME/.kube/talend/$OLD_CTX"
                mv "$OLD_CTX.config" "$OLD_CTX.old" || true
                echo "aws-admin"
                ;;
        *)
                OLD_ADMIN_CTX="$HOME/.kube/talend/$K8S_REGION-admin"
                echo "Renaming $OLD_ADMIN_CTX.config to $OLD_ADMIN_CTX.old"
                mv "$OLD_ADMIN_CTX.config" "$OLD_ADMIN_CTX.admin.old" || true
                ;;    
        esac

        # ARN_ROLE=$(cat $HOME/.okta-aws | grep role | cut -d =  -f 2)
        oktastatus=0
        aws --profile $K8S_REGION sts get-caller-identity > /dev/null || oktastatus=$?
        if [ $oktastatus -ne 0 ]; then
                msg "Connecting to okta"
                okta-awscli -f --profile $K8S_REGION
        fi
        AWS_ACCOUNT_ID="$(aws --profile $K8S_REGION sts get-caller-identity --query Account --output text)"
        
        DEBUG_LINE="$CLUSTER_NAME arn:aws:iam::$AWS_ACCOUNT_ID:role/$ROLE_PREFIX$CLUSTER_NAME $AWS_REGION $NEW_KUBECONFIG"
        
        # If config does not exist we got to generate it with the following commands
        if [ ! -f $NEW_KUBECONFIG ]; then
                msg "Generating new kubeconfig file $NEW_KUBECONFIG"
                # aws --profile $K8S_REGION sts get-caller-identity | more
                aws --profile $K8S_REGION eks update-kubeconfig --name "$CLUSTER_NAME" --role-arn arn:aws:iam::$AWS_ACCOUNT_ID:role/$ROLE_PREFIX$CLUSTER_NAME --region "$AWS_REGION" --kubeconfig "$NEW_KUBECONFIG"     
        fi
        
        # CONTEXT=$K8S_REGION
fi
# Update the kubconfig
update_k8s_config
# Renaming the context
if [ "$K8S_FILE_CREATED" = true ] ; then
  OLD_CONTEXT=$(cat $NEW_KUBECONFIG | grep "current-context:" | sed "s/current-context: //")
  kubectl --kubeconfig $NEW_KUBECONFIG config rename-context $OLD_CONTEXT $K8S_REGION
fi
kubectl config set current-context "$K8S_REGION"
# kubectl config use-context "$K8S_REGION"
kubectl config set-context "$K8S_REGION" --namespace=app
# 
# Command to test credentials

if [ "$SOURCED" = true ] ; then
    msg "KUBECONFIG environement variable updated to $KUBECONFIG"    
else
    if [ "$K8S_FILE_CREATED" = true ] ; then
      msg "${ORANGE}---"
      msg "To use the new K8S config file generated update your KUBECONFIG environement variable with command"
      msg "KUBECONFIG=\$(for i in \$(find $HOME/.kube -iname '*.config') ; do echo -n ":\$i"; done | cut -c 2-)"
      msg "This should be added to your .bashrc file"
      msg "---${NOFORMAT}"
    fi
fi

msg "${GREEN}"
msg "Your are now connected to $K8S_REGION with app as default namespace"
msg "You can access k8s cluster with commands like: kubectl get pods -n app"
msg "${NOFORMAT}"