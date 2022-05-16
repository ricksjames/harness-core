<#macro common>
# Account Id to which the delegate will be connecting
accountId: ${accountId}

# Secret identifier associated with the account
delegateToken: ${delegateToken}

# Short 6 character identifier of the account
accountIdShort: ${kubernetesAccountLabel}
delegateName: ${delegateName}
delegateType: ${delegateType}
delegateDockerImage: ${delegateDockerImage}
managerHostAndPort: ${managerHostAndPort}
</#macro>

<#macro ngSpecific>
nextGen: true
delegateTags:${delegateTags}
delegateDescription:${delegateDescription}
k8sPermissionsType: ${k8sPermissionsType}
delegateReplicas: ${delegateReplicas}
delegateCpu: ${delegateCpu}
delegateRam: ${delegateRam}
delegateNamespace: ${delegateNamespace}
</#macro>

<#macro cgSpecific>
delegateProfile: "${delegateProfile}"
</#macro>