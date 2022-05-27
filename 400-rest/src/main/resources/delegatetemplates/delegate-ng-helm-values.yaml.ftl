<#import "common/helm-delegate-values.ftl" as helmValues>
# This harness-delegate-values.yaml file is compatible harness-delegate-ng helm chart

# You can download the harness-delegate helm chart at
# https://app.harness.io/storage/harness-download/harness-helm-charts/

# To add Harness helm repo with name harness:
# helm repo add harness https://app.harness.io/storage/harness-download/harness-helm-charts/

# To install the chart with the release name my-release and this values.yaml
# helm install --name my-release harness/harness-delegate-ng -f harness-delegate-values.yaml

# Account Id to which the delegate will be connecting
accountId: ${accountId}

# Secret identifier associated with the account
delegateToken: ${delegateToken}

delegateName: ${delegateName}
delegateDockerImage: ${delegateDockerImage}
managerEndpoint: ${managerHostAndPort}

<@helmValues.ngSpecific />