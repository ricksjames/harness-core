<#import "common/helm-delegate-values.ftl" as helmValues>
<@helmValues.common />

<@helmValues.ngSpecific />

<#if isImmutable == "false">
watcherStorageUrl: ${watcherStorageUrl}
watcherCheckLocation: ${watcherCheckLocation}
delegateStorageUrl: ${delegateStorageUrl}
delegateCheckLocation: ${delegateCheckLocation}
useCdn: ${useCdn}
jreVersion: ${jreVersion}
<#if useCdn == "true">
cdnUrl: ${cdnUrl}
remoteWatcherUrlCdn: ${remoteWatcherUrlCdn}
</#if>
</#if>