<#macro ngSpecific>
# Mention tags that will be used to identify delegate
tags: "${delegateTags}"
description: "${delegateDescription}"

# Specify access for delegate, CLUSTER_ADMIN, CLUSTER_VIEWER and NAMESPACE_ADMIN are valid entries.
k8sPermissionsType: ${k8sPermissionsType}

# Namespace where delegate will be installed
namespace: ${delegateNamespace}

# Resource Configuration
replicas: ${delegateReplicas}
cpu: ${delegateCpu}
memory: ${delegateRam}

# Need to run something specific before delegate starts, enter your script in initScripts.
initScript: ""

# Specify JAVA_OPTS
javaOpts: "-Xms64M"

logStreamingServiceBaseUrl: "${logStreamingServiceBaseUrl}"
upgraderDockerImage: "${upgraderDockerImage}"
</#macro>

<#macro cgSpecific>
delegateProfile: "${delegateProfile}"
</#macro>