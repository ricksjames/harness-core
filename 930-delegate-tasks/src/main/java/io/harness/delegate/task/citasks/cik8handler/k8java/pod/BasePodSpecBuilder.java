package io.harness.delegate.task.citasks.cik8handler.k8java.pod;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ci.pod.ContainerParams;
import io.harness.delegate.beans.ci.pod.HostAliasParams;
import io.harness.delegate.beans.ci.pod.PVCParams;
import io.harness.delegate.beans.ci.pod.PodParams;
import io.harness.delegate.task.citasks.cik8handler.k8java.container.ContainerSpecBuilder;
import io.harness.delegate.task.citasks.cik8handler.k8java.container.ContainerSpecBuilderResponse;

import com.google.inject.Inject;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1EmptyDirVolumeSourceBuilder;
import io.kubernetes.client.openapi.models.V1HostAlias;
import io.kubernetes.client.openapi.models.V1HostAliasBuilder;
import io.kubernetes.client.openapi.models.V1LocalObjectReference;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimVolumeSourceBuilder;
import io.kubernetes.client.openapi.models.V1PodBuilder;
import io.kubernetes.client.openapi.models.V1PodFluent;
import io.kubernetes.client.openapi.models.V1PodSecurityContext;
import io.kubernetes.client.openapi.models.V1PodSecurityContextBuilder;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.openapi.models.V1VolumeBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An abstract class to generate K8 pod spec based on parameters provided to it. It builds a minimal pod spec essential
 * for creating a pod. This class can be extended to generate a generic pod spec.
 */
@OwnedBy(HarnessTeam.CI)
public abstract class BasePodSpecBuilder {
  @Inject private ContainerSpecBuilder containerSpecBuilder;

  public V1PodBuilder createSpec(PodParams<ContainerParams> podParams) {
    V1PodFluent.SpecNested<V1PodBuilder> podBuilderSpecNested = getBaseSpec(podParams);
    decorateSpec(podParams, podBuilderSpecNested);
    return podBuilderSpecNested.endSpec();
  }

  /**
   * Builds on minimal pod spec generated by getBaseSpec method.
   */
  protected abstract void decorateSpec(
      PodParams<ContainerParams> podParams, V1PodFluent.SpecNested<V1PodBuilder> podBuilderSpecNested);

  private V1PodFluent.SpecNested<V1PodBuilder> getBaseSpec(PodParams<ContainerParams> podParams) {
    List<V1LocalObjectReference> imageSecrets = new ArrayList<>();

    Set<V1Volume> volumesToCreate = new HashSet<>();
    Map<String, String> volumeToPVCMap = getPVC(podParams.getPvcParamList());
    Map<String, V1LocalObjectReference> imageSecretByName = new HashMap<>();
    List<V1Container> containers =
        getContainers(podParams.getContainerParamsList(), volumesToCreate, volumeToPVCMap, imageSecretByName);
    List<V1Container> initContainers =
        getContainers(podParams.getInitContainerParamsList(), volumesToCreate, volumeToPVCMap, imageSecretByName);

    imageSecretByName.forEach((imageName, imageSecret) -> imageSecrets.add(imageSecret));

    return new V1PodBuilder()
        .withNewMetadata()
        .withName(podParams.getName())
        .withLabels(podParams.getLabels())
        .withAnnotations(podParams.getAnnotations())
        .withNamespace(podParams.getNamespace())
        .endMetadata()
        .withNewSpec()
        .withContainers(containers)
        .withInitContainers(initContainers)
        .withImagePullSecrets(imageSecrets)
        .withHostAliases(getHostAliases(podParams.getHostAliasParamsList()))
        .withVolumes(new ArrayList<>(volumesToCreate))
        .withSecurityContext(getSecurityContext(podParams));
  }

  private V1PodSecurityContext getSecurityContext(PodParams<ContainerParams> podParams) {
    V1PodSecurityContext podSecurityContext = null;
    if (podParams.getRunAsUser() != null) {
      podSecurityContext = new V1PodSecurityContextBuilder().withRunAsUser((long) podParams.getRunAsUser()).build();
    }
    return podSecurityContext;
  }

  private List<V1Container> getContainers(List<ContainerParams> containerParamsList, Set<V1Volume> volumesToCreate,
      Map<String, String> volumeToPVCMap, Map<String, V1LocalObjectReference> imageSecretByName) {
    List<V1Container> containers = new ArrayList<>();
    if (containerParamsList == null) {
      return containers;
    }

    for (ContainerParams containerParams : containerParamsList) {
      if (containerParams.getVolumeToMountPath() != null) {
        containerParams.getVolumeToMountPath().forEach(
            (volumeName, volumeMountPath) -> volumesToCreate.add(getVolume(volumeName, volumeToPVCMap)));
      }

      ContainerSpecBuilderResponse containerSpecBuilderResponse = containerSpecBuilder.createSpec(containerParams);
      containers.add(containerSpecBuilderResponse.getContainerBuilder().build());
      if (containerSpecBuilderResponse.getImageSecret() != null) {
        V1LocalObjectReference imageSecret = containerSpecBuilderResponse.getImageSecret();
        imageSecretByName.put(imageSecret.getName(), imageSecret);
      }
      if (containerSpecBuilderResponse.getVolumes() != null) {
        List<V1Volume> volumes = containerSpecBuilderResponse.getVolumes();
        volumesToCreate.addAll(volumes);
      }
    }
    return containers;
  }

  private Map<String, String> getPVC(List<PVCParams> pvcParamsList) {
    Map<String, String> volumeToPVCMap = new HashMap<>();
    if (pvcParamsList == null) {
      return volumeToPVCMap;
    }

    for (PVCParams pvcParam : pvcParamsList) {
      volumeToPVCMap.put(pvcParam.getVolumeName(), pvcParam.getClaimName());
    }
    return volumeToPVCMap;
  }

  private V1Volume getVolume(String volumeName, Map<String, String> volumeToPVCMap) {
    if (volumeToPVCMap.containsKey(volumeName)) {
      return new V1VolumeBuilder()
          .withName(volumeName)
          .withPersistentVolumeClaim(
              new V1PersistentVolumeClaimVolumeSourceBuilder().withClaimName(volumeToPVCMap.get(volumeName)).build())
          .build();
    } else {
      return new V1VolumeBuilder()
          .withName(volumeName)
          .withEmptyDir(new V1EmptyDirVolumeSourceBuilder().build())
          .build();
    }
  }

  private List<V1HostAlias> getHostAliases(List<HostAliasParams> hostAliasParamsList) {
    List<V1HostAlias> hostAliases = new ArrayList<>();
    if (hostAliasParamsList == null) {
      return hostAliases;
    }

    hostAliasParamsList.forEach(hostAliasParams
        -> hostAliases.add(new V1HostAliasBuilder()
                               .withHostnames(hostAliasParams.getHostnameList())
                               .withIp(hostAliasParams.getIpAddress())
                               .build()));
    return hostAliases;
  }
}
