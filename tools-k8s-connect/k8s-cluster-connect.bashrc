# To use this custo add the 3 following line in your bashrc file
# if [ -f ~/projects/tools/tools-k8s-connect/k8s-cluster-connect.bashrc]; then
#    . ~/projects/tools/tools-k8s-connect/k8s-cluster-connect.bashrc
# fi
#

# Example of custom bashrc configuration

alias kl='f(){
    bash /home/$(whoami)/projects/tools/tools-k8s-connect/k8s-cluster-connect.sh $@
    update_k8s_config
    unset -f f;
    };
  f'

update_k8s_config(){
    export KUBECONFIG=$(for i in $(find /home/$(whoami)/.kube -iname '*.config') ; do echo -n :$i; done | cut -c 2-)
}
update_k8s_config

prompt_k8s(){
  k8s_current_ns=$(kubectl config view --minify --output 'jsonpath={..namespace}' 2> /dev/null; echo)
  k8s_current_context=$(kubectl config current-context 2> /dev/null)
    if [[ $? -eq 0 ]] ; then 
        echo -e "(${k8s_current_context}[${k8s_current_ns}]) "; 
    else 
        echo "$USER@$NAME"; 
    fi
}

if [ "$color_prompt" = yes ]; then
    PS1='${debian_chroot:+($debian_chroot)}\[\033[01;32m\]$(prompt_k8s)\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\$ '
else
    PS1='${debian_chroot:+($debian_chroot)}\u@\h:\w\$ '
fi

unset color_prompt force_color_prompt
#PS1+='$(prompt_k8s)$ '

# Git + K8S prompt
if [ -f "$HOME/tools/.bash-git-prompt/gitprompt.sh" ]; then
    GIT_PROMPT_ONLY_IN_REPO=1
    GIT_PROMPT_END=" \$(prompt_k8s) \n\$ "
    source $HOME/tools/.bash-git-prompt/gitprompt.sh
fi

# K8S Custo
source <(kubectl completion bash)
alias k=kubectl
alias k-get-contexts="kubectl config get-contexts -o name"
alias k-use-context="kubectl config use-context"
alias k-unset-current-context="kubectl config unset current-context"

complete -F __start_kubectl k

export PATH=$PATH:~/projects/tools/tools-k8s-connect
