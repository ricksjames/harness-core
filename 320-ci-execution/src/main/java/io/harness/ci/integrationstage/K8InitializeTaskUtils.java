package io.harness.ci.integrationstage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.quantity.unit.DecimalQuantityUnit;
import io.harness.beans.quantity.unit.StorageQuantityUnit;
import io.harness.beans.serializer.RunTimeInputHandler;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.beans.yaml.extended.infrastrucutre.OSType;
import io.harness.beans.yaml.extended.infrastrucutre.k8.Capabilities;
import io.harness.beans.yaml.extended.infrastrucutre.k8.SecurityContext;
import io.harness.beans.yaml.extended.infrastrucutre.k8.Toleration;
import io.harness.beans.yaml.extended.volumes.CIVolume;
import io.harness.beans.yaml.extended.volumes.EmptyDirYaml;
import io.harness.beans.yaml.extended.volumes.HostPathYaml;
import io.harness.beans.yaml.extended.volumes.PersistentVolumeClaimYaml;
import io.harness.ci.utils.QuantityUtils;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.ContainerCapabilities;
import io.harness.delegate.beans.ci.pod.ContainerSecurityContext;
import io.harness.delegate.beans.ci.pod.EmptyDirVolume;
import io.harness.delegate.beans.ci.pod.HostPathVolume;
import io.harness.delegate.beans.ci.pod.ImageDetailsWithConnector;
import io.harness.delegate.beans.ci.pod.PVCVolume;
import io.harness.delegate.beans.ci.pod.PodToleration;
import io.harness.delegate.beans.ci.pod.PodVolume;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.k8s.model.ImageDetails;
import io.harness.ng.core.NGAccess;
import io.harness.pms.yaml.ParameterField;
import io.harness.stateutils.buildstate.ConnectorUtils;
import io.harness.yaml.core.timeout.Timeout;
import io.harness.yaml.extended.ci.container.ContainerResource;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import static io.harness.beans.serializer.RunTimeInputHandler.UNRESOLVED_PARAMETER;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveIntegerParameter;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveStringParameter;
import static io.harness.common.CICommonPodConstants.POD_NAME_PREFIX;
import static io.harness.common.CIExecutionConstants.POD_MAX_WAIT_UNTIL_READY_SECS;
import static io.harness.common.CIExecutionConstants.VOLUME_PREFIX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.Character.toLowerCase;
import static java.lang.String.format;
import static org.apache.commons.lang3.CharUtils.isAsciiAlphanumeric;


@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class K8InitializeTaskUtils {
    static final String SOURCE = "123456789bcdfghjklmnpqrstvwxyz";
    static final Integer RANDOM_LENGTH = 8;
    private static final SecureRandom random = new SecureRandom();
    @Inject
    private ConnectorUtils connectorUtils;


    public String generatePodName(String identifier) {
        return POD_NAME_PREFIX + "-" + getK8PodIdentifier(identifier) + "-"
                + generateRandomAlphaNumericString(RANDOM_LENGTH);
    }

    private String getK8PodIdentifier(String identifier) {
        StringBuilder sb = new StringBuilder(15);
        for (char c : identifier.toCharArray()) {
            if (isAsciiAlphanumeric(c)) {
                sb.append(toLowerCase(c));
            }
            if (sb.length() == 15) {
                return sb.toString();
            }
        }
        return sb.toString();
    }

    private static String generateRandomAlphaNumericString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(SOURCE.charAt(random.nextInt(SOURCE.length())));
        }
        return sb.toString();
    }

    public List<PodVolume> convertDirectK8Volumes(K8sDirectInfraYaml k8sDirectInfraYaml) {
        List<PodVolume> podVolumes = new ArrayList<>();

        List<CIVolume> volumes = k8sDirectInfraYaml.getSpec().getVolumes().getValue();
        if (isEmpty(volumes)) {
            return podVolumes;
        }

        int index = 0;
        for (CIVolume volume : volumes) {
            String volumeName = format("%s%d", VOLUME_PREFIX, index);
            if (volume.getType() == CIVolume.Type.EMPTY_DIR) {
                podVolumes.add(convertEmptyDir(volumeName, (EmptyDirYaml) volume));
            } else if (volume.getType() == CIVolume.Type.HOST_PATH) {
                podVolumes.add(convertHostPath(volumeName, (HostPathYaml) volume));
            } else if (volume.getType() == CIVolume.Type.PERSISTENT_VOLUME_CLAIM) {
                podVolumes.add(convertPVCVolume(volumeName, (PersistentVolumeClaimYaml) volume));
            }

            index++;
        }
        return podVolumes;
    }

    private EmptyDirVolume convertEmptyDir(String volumeName, EmptyDirYaml emptyDirYaml) {
        EmptyDirVolume.EmptyDirVolumeBuilder emptyDirVolumeBuilder = EmptyDirVolume.builder()
                .name(volumeName)
                .mountPath(emptyDirYaml.getMountPath().getValue())
                .medium(emptyDirYaml.getSpec().getMedium().getValue());
        String sizeStr = emptyDirYaml.getSpec().getSize().getValue();
        if (isNotEmpty(sizeStr)) {
            emptyDirVolumeBuilder.sizeMib(QuantityUtils.getStorageQuantityValueInUnit(sizeStr, StorageQuantityUnit.Mi));
        }
        return emptyDirVolumeBuilder.build();
    }

    private HostPathVolume convertHostPath(String volumeName, HostPathYaml hostPathYaml) {
        return HostPathVolume.builder()
                .name(volumeName)
                .mountPath(hostPathYaml.getMountPath().getValue())
                .path(hostPathYaml.getSpec().getPath().getValue())
                .hostPathType(hostPathYaml.getSpec().getType().getValue())
                .build();
    }

    private PVCVolume convertPVCVolume(String volumeName, PersistentVolumeClaimYaml pvcYaml) {
        return PVCVolume.builder()
                .name(volumeName)
                .mountPath(pvcYaml.getMountPath().getValue())
                .claimName(pvcYaml.getSpec().getClaimName().getValue())
                .build();
    }

    public List<PodToleration> getPodTolerations(ParameterField<List<Toleration>> parameterizedTolerations) {
        List<PodToleration> podTolerations = new ArrayList<>();
        List<Toleration> tolerations = RunTimeInputHandler.resolveTolerations(parameterizedTolerations);
        if (tolerations == null) {
            return podTolerations;
        }

        for (Toleration toleration : tolerations) {
            String effect = resolveStringParameter("effect", null, "infrastructure", toleration.getEffect(), false);
            String key = resolveStringParameter("key", null, "infrastructure", toleration.getKey(), false);
            String operator = resolveStringParameter("operator", null, "infrastructure", toleration.getOperator(), false);
            String value = resolveStringParameter("value", null, "infrastructure", toleration.getValue(), false);
            Integer tolerationSeconds = resolveIntegerParameter(toleration.getTolerationSeconds(), null);

            validateTolerationEffect(effect);
            validateTolerationOperator(operator);

            podTolerations.add(PodToleration.builder()
                    .effect(effect)
                    .key(key)
                    .operator(operator)
                    .value(value)
                    .tolerationSeconds(tolerationSeconds)
                    .build());
        }
        return podTolerations;
    }

    private void validateTolerationEffect(String effect) {
        if (isNotEmpty(effect)) {
            if (!effect.equals("NoSchedule") && !effect.equals("PreferNoSchedule") && !effect.equals("NoExecute")) {
                throw new CIStageExecutionException(format("Invalid value %s for effect in toleration", effect));
            }
        }
    }

    private void validateTolerationOperator(String operator) {
        if (isNotEmpty(operator)) {
            if (!operator.equals("Equal") && !operator.equals("Exists")) {
                throw new CIStageExecutionException(format("Invalid value %s for operator in toleration", operator));
            }
        }
    }

    public int getPodWaitUntilReadTimeout(K8sDirectInfraYaml k8sDirectInfraYaml) {
        ParameterField<String> timeout = k8sDirectInfraYaml.getSpec().getInitTimeout();

        int podWaitUntilReadyTimeout = POD_MAX_WAIT_UNTIL_READY_SECS;
        if (timeout != null && timeout.fetchFinalValue() != null && isNotEmpty((String) timeout.fetchFinalValue())) {
            long timeoutInMillis = Timeout.fromString((String) timeout.fetchFinalValue()).getTimeoutInMillis();
            podWaitUntilReadyTimeout = (int) (timeoutInMillis / 1000);
        }
        return podWaitUntilReadyTimeout;
    }

    public ContainerSecurityContext getCtrSecurityContext(SecurityContext securityContext, OSType os) {
        if (securityContext == null || os == OSType.Windows) {
            return ContainerSecurityContext.builder().build();
        }
        return ContainerSecurityContext.builder()
                .allowPrivilegeEscalation(securityContext.getAllowPrivilegeEscalation().getValue())
                .privileged(securityContext.getPrivileged().getValue())
                .procMount(securityContext.getProcMount().getValue())
                .readOnlyRootFilesystem(securityContext.getReadOnlyRootFilesystem().getValue())
                .runAsNonRoot(securityContext.getRunAsNonRoot().getValue())
                .runAsGroup(securityContext.getRunAsGroup().getValue())
                .runAsUser(resolveIntegerParameter(securityContext.getRunAsUser(), null))
                .capabilities(getCtrCapabilities(securityContext.getCapabilities().getValue()))
                .build();
    }

    private ContainerCapabilities getCtrCapabilities(Capabilities capabilities) {
        if (capabilities == null) {
            return ContainerCapabilities.builder().build();
        }

        return ContainerCapabilities.builder()
                .add(capabilities.getAdd().getValue())
                .drop(capabilities.getDrop().getValue())
                .build();
    }

    public ImageDetailsWithConnector getContainerImageInfo(String image, String connectorRef, ConnectorDetails harnessInternalImageConnector, boolean isHarnessManagedImage, NGAccess ngAccess) {
        ImageDetails imageDetails = IntegrationStageUtils.getImageInfo(image);
        ConnectorDetails connectorDetails = null;
        if (connectorRef != null) {
            connectorDetails = connectorUtils.getConnectorDetails(
                    ngAccess, connectorRef);
        }

        ConnectorDetails imgConnector = connectorDetails;
        if (isHarnessManagedImage) {
            imgConnector = harnessInternalImageConnector;
        }
        String fullyQualifiedImageName =
                IntegrationStageUtils.getFullyQualifiedImageName(imageDetails.getName(), imgConnector);
        imageDetails.setName(fullyQualifiedImageName);
        return ImageDetailsWithConnector.builder().imageConnectorDetails(imgConnector).imageDetails(imageDetails).build();
    }
}
