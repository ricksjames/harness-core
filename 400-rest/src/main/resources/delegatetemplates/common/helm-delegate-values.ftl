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

# Proxy settings if the delegate will be running behind proxy
# Default value for proxyManager is false (delegate will directly connect to manager)
proxyManager: "false"
proxyHost: ""
proxyPort: ""

# Use Base64 encoded username and password
# you can also set these parameters using --set flag during chart installation.
proxyUser: ""
proxyPassword: ""

# Allowed values are http or https
proxyScheme: ""

# Enter a comma separated list of suffixes for which proxy is not
# required. Do not use leading wildcards (.company.com,specifichost)
# (optional):
noProxy: ""

# If the proxy doesn't support web socket (wss) protocol then set it
# to true.
pollForTasks: "false"

</#macro>

<#macro cgSpecific>
delegateProfile: "${delegateProfile}"
</#macro>