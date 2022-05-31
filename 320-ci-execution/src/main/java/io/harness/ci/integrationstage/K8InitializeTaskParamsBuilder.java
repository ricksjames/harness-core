package io.harness.ci.integrationstage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.stepinfo.InitializeStepInfo;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.delegate.beans.ci.k8s.CIK8InitializeTaskParams;
import io.harness.delegate.beans.ci.pod.CIK8ContainerParams;
import io.harness.delegate.beans.ci.pod.CIK8PodParams;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.ng.core.NGAccess;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.stateutils.buildstate.CodebaseUtils;
import io.harness.stateutils.buildstate.ConnectorUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static io.harness.beans.serializer.RunTimeInputHandler.resolveIntegerParameter;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveMapParameter;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveStringParameter;
import static java.lang.String.format;
import static java.util.Collections.singletonList;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class K8InitializeTaskParamsBuilder {
    @Inject
    private ConnectorUtils connectorUtils;
    @Inject private K8InitializeTaskUtils k8InitializeTaskUtils;
    @Inject
    CodebaseUtils codebaseUtils;


    public CIK8InitializeTaskParams getK8InitializeTaskParams(InitializeStepInfo initializeStepInfo, Infrastructure infrastructure, Ambiance ambiance) {
        if (infrastructure == null) {
            throw new CIStageExecutionException("Input infrastructure can not be empty");
        }

        if (infrastructure.getType() != Infrastructure.Type.KUBERNETES_DIRECT || infrastructure.getType() != Infrastructure.Type.KUBERNETES_HOSTED) {
            throw new CIStageExecutionException(format("Invalid infrastructure type: %s", infrastructure.getType()));
        }

        if (infrastructure.getType() == Infrastructure.Type.KUBERNETES_DIRECT) {
            return buildK8DirectTaskParams(initializeStepInfo, (K8sDirectInfraYaml)infrastructure, ambiance);
        }
        return null;
    }

    private CIK8InitializeTaskParams buildK8DirectTaskParams(InitializeStepInfo initializeStepInfo, K8sDirectInfraYaml k8sDirectInfraYaml, Ambiance ambiance) {
        NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
        String clusterName = k8sDirectInfraYaml.getSpec().getConnectorRef().getValue();
        ConnectorDetails k8sConnector = connectorUtils.getConnectorDetails(ngAccess, clusterName);
        return CIK8InitializeTaskParams.builder()
                .k8sConnector(k8sConnector)
                .cik8PodParams(getDirectPodParams(initializeStepInfo, k8sDirectInfraYaml, ambiance))
                .podMaxWaitUntilReadySecs(k8InitializeTaskUtils.getPodWaitUntilReadTimeout(k8sDirectInfraYaml))
                .build();
    }

    private CIK8PodParams<CIK8ContainerParams> getDirectPodParams(InitializeStepInfo initializeStepInfo, K8sDirectInfraYaml k8sDirectInfraYaml, Ambiance ambiance) {
        String podName = k8InitializeTaskUtils.generatePodName(initializeStepInfo.getStageIdentifier());
        Map<String, String> annotations = resolveMapParameter(
                "annotations", "K8InitializeStep", "stageSetup", k8sDirectInfraYaml.getSpec().getAnnotations(), false);
        Map<String, String> labels =
                resolveMapParameter("labels", "K8InitializeStep", "stageSetup", k8sDirectInfraYaml.getSpec().getLabels(), false);
        Map<String, String> nodeSelector = resolveMapParameter(
                "nodeSelector", "K8InitializeStep", "stageSetup", k8sDirectInfraYaml.getSpec().getNodeSelector(), false);
        Integer stageRunAsUser = resolveIntegerParameter(k8sDirectInfraYaml.getSpec().getRunAsUser(), null);
        String serviceAccountName = resolveStringParameter(
                "serviceAccountName", "K8InitializeStep", "stageSetup", k8sDirectInfraYaml.getSpec().getServiceAccountName(), false);

        NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
        ConnectorDetails gitConnector =
                codebaseUtils.getGitConnector(ngAccess, initializeStepInfo.getCiCodebase(), initializeStepInfo.isSkipGitClone());
        return CIK8PodParams.<CIK8ContainerParams>builder()
                .name(podName)
                .namespace(k8sDirectInfraYaml.getSpec().getNamespace().getValue())
                .labels(labels)
                .serviceAccountName(serviceAccountName)
                .annotations(annotations)
                .nodeSelector(nodeSelector)
                .runAsUser(stageRunAsUser)
                .automountServiceAccountToken(k8sDirectInfraYaml.getSpec().getAutomountServiceAccountToken().getValue())
                .priorityClassName(k8sDirectInfraYaml.getSpec().getPriorityClassName().getValue())
                .tolerations(k8InitializeTaskUtils.getPodTolerations(k8sDirectInfraYaml.getSpec().getTolerations()))
                .gitConnector(gitConnector)
                .containerParamsList(containerParamsList)
                //.pvcParamList(pvcParamsList)
                .initContainerParamsList(singletonList(null))
                .volumes(k8InitializeTaskUtils.convertDirectK8Volumes(k8sDirectInfraYaml))
                .build();
    }


}
