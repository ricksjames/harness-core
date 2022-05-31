package io.harness.ci.integrationstage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.steps.stepinfo.InitializeStepInfo;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.beans.yaml.extended.infrastrucutre.OSType;
import io.harness.delegate.beans.ci.k8s.CIK8InitializeTaskParams;
import io.harness.delegate.beans.ci.pod.CIK8ContainerParams;
import io.harness.delegate.beans.ci.pod.CIK8PodParams;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.PodVolume;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.ng.core.NGAccess;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.stateutils.buildstate.CodebaseUtils;
import io.harness.stateutils.buildstate.ConnectorUtils;
import io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider;
import io.harness.util.HarnessImageUtils;
import io.harness.util.PortFinder;
import io.harness.yaml.extended.ci.codebase.CodeBase;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.harness.beans.serializer.RunTimeInputHandler.resolveIntegerParameter;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveMapParameter;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveStringParameter;
import static io.harness.common.CIExecutionConstants.PORT_STARTING_RANGE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.Collections.singletonList;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class K8InitializeTaskParamsBuilder {
    @Inject
    private ConnectorUtils connectorUtils;
    @Inject private K8InitializeTaskUtils k8InitializeTaskUtils;
    @Inject private K8InitializeStepUtils k8InitializeStepUtils;
    @Inject private K8InitializeServiceUtils k8InitializeServiceUtils;
    @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;
    @Inject private HarnessImageUtils harnessImageUtils;
    @Inject private InternalContainerParamsProvider internalContainerParamsProvider;



    @Inject
    CodebaseUtils codebaseUtils;


    public CIK8InitializeTaskParams getK8InitializeTaskParams(InitializeStepInfo initializeStepInfo, Infrastructure infrastructure, Ambiance ambiance) {
        if (infrastructure == null) {
            throw new CIStageExecutionException("Input infrastructure can not be empty");
        }

        if (infrastructure.getType() != Infrastructure.Type.KUBERNETES_DIRECT || infrastructure.getType() != Infrastructure.Type.KUBERNETES_HOSTED) {
            throw new CIStageExecutionException(format("Invalid infrastructure type: %s", infrastructure.getType()));
        }

        K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
                ambiance, RefObjectUtils.getSweepingOutputRefObject(ContextElement.podDetails));
        if (infrastructure.getType() == Infrastructure.Type.KUBERNETES_DIRECT) {
            return buildK8DirectTaskParams(initializeStepInfo, k8PodDetails, (K8sDirectInfraYaml)infrastructure, ambiance);
        }
        return null;
    }

    private CIK8InitializeTaskParams buildK8DirectTaskParams(InitializeStepInfo initializeStepInfo, K8PodDetails k8PodDetails, K8sDirectInfraYaml k8sDirectInfraYaml, Ambiance ambiance) {
        NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
        String clusterName = k8sDirectInfraYaml.getSpec().getConnectorRef().getValue();
        ConnectorDetails k8sConnector = connectorUtils.getConnectorDetails(ngAccess, clusterName);
        return CIK8InitializeTaskParams.builder()
                .k8sConnector(k8sConnector)
                .cik8PodParams(getDirectPodParams(initializeStepInfo, k8PodDetails, k8sDirectInfraYaml, ambiance))
                .podMaxWaitUntilReadySecs(k8InitializeTaskUtils.getPodWaitUntilReadTimeout(k8sDirectInfraYaml))
                .build();
    }

    private CIK8PodParams<CIK8ContainerParams> getDirectPodParams(InitializeStepInfo initializeStepInfo, K8PodDetails k8PodDetails, K8sDirectInfraYaml k8sDirectInfraYaml, Ambiance ambiance) {
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

        Map<String, String> buildLabels = k8InitializeTaskUtils.getBuildLabels(ambiance, k8PodDetails);
        if (isNotEmpty(labels)) {
            buildLabels.putAll(labels);
        }

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

    private List<CIK8ContainerParams> getStageContainers(InitializeStepInfo initializeStepInfo, K8PodDetails k8PodDetails, Infrastructure infrastructure, List<PodVolume> volumes, String logPrefix, Ambiance ambiance) {
        List<String> sharedPaths = k8InitializeTaskUtils.getSharedPaths(initializeStepInfo);
        Map<String, String> volumeToMountPath = k8InitializeTaskUtils.getVolumeToMountPath(sharedPaths, volumes);
        OSType os = k8InitializeTaskUtils.getOS(infrastructure);
        String accountId = AmbianceUtils.getAccountId(ambiance);
        NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
        Map<String, String> logEnvVars = k8InitializeTaskUtils.getLogServiceEnvVariables(k8PodDetails, accountId);
        Map<String, String> tiEnvVars = k8InitializeTaskUtils.getTIServiceEnvVariables(accountId);
        Map<String, String> stoEnvVars = k8InitializeTaskUtils.getSTOServiceEnvVariables(accountId);

        CodeBase ciCodebase = initializeStepInfo.getCiCodebase();
        ConnectorDetails gitConnector =
                codebaseUtils.getGitConnector(ngAccess, ciCodebase, initializeStepInfo.isSkipGitClone());
        Map<String, String> gitEnvVars = codebaseUtils.getGitEnvVariables(gitConnector, ciCodebase);
        Map<String, String> runtimeCodebaseVars = codebaseUtils.getRuntimeCodebaseVars(ambiance);

        Map<String, String> commonEnvVars = k8InitializeTaskUtils.getCommonStepEnvVariables(
                k8PodDetails, gitEnvVars, runtimeCodebaseVars, k8InitializeTaskUtils.getWorkDir(), logPrefix, ambiance);

        ConnectorDetails harnessInternalImageConnector =
                harnessImageUtils.getHarnessImageConnectorDetailsForK8(ngAccess, infrastructure);
        CIK8ContainerParams setupAddOnContainerParams = internalContainerParamsProvider.getSetupAddonContainerParams(
                harnessInternalImageConnector, volumeToMountPath, k8InitializeTaskUtils.getWorkDir(),
                k8InitializeTaskUtils.getCtrSecurityContext(infrastructure), ngAccess.getAccountIdentifier(), os);

        Integer stageCpuRequest = k8InitializeStepUtils.getStageCpuRequest(initializeStepInfo.getExecutionElementConfig().getSteps(), accountId);
        Integer stageMemoryRequest = k8InitializeStepUtils.getStageMemoryRequest(initializeStepInfo.getExecutionElementConfig().getSteps(), accountId);
        CIK8ContainerParams liteEngineContainerParams = internalContainerParamsProvider.getLiteEngineContainerParams(harnessInternalImageConnector, new HashMap<>(),
                k8PodDetails, stageCpuRequest, stageMemoryRequest, logEnvVars, tiEnvVars, stoEnvVars, volumeToMountPath,
                k8InitializeTaskUtils.getWorkDir(), k8InitializeTaskUtils.getCtrSecurityContext(infrastructure), logPrefix, ambiance);
    }

    private List<CIK8ContainerParams> convertStageContainerDefinitions(InitializeStepInfo initializeStepInfo, Map<String, String> volumeToMountPath, Ambiance ambiance) {
        NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
        Map<String, ConnectorDetails> githubApiTokenFunctorConnectors =
                k8InitializeTaskUtils.resolveGitAppFunctor(ngAccess, initializeStepInfo, ambiance);


    }

    private List<ContainerDefinitionInfo> getStageContainerDefinitions(InitializeStepInfo initializeStepInfo, Infrastructure infrastructure, Ambiance ambiance) {
        OSType os = k8InitializeTaskUtils.getOS(infrastructure);
        Set<Integer> usedPorts = new HashSet<>();
        PortFinder portFinder = PortFinder.builder().startingPort(PORT_STARTING_RANGE).usedPorts(usedPorts).build();

        StageElementConfig stageElementConfig = StageElementConfig.builder()
                .type("CI")
                .identifier(initializeStepInfo.getStageIdentifier())
                .variables(initializeStepInfo.getVariables())
                .stageType(initializeStepInfo.getStageElementConfig())
                .build();
        CIExecutionArgs ciExecutionArgs = CIExecutionArgs.builder()
                .runSequence(String.valueOf(ambiance.getMetadata().getRunSequence()))
                .executionSource(initializeStepInfo.getExecutionSource())
                .build();
        List<ContainerDefinitionInfo> serviceCtrDefinitionInfos = k8InitializeServiceUtils.createServiceContainerDefinitions(stageElementConfig, portFinder, os);
        List<ContainerDefinitionInfo> stepCtrDefinitionInfos =
                k8InitializeStepUtils.createStepContainerDefinitions(initializeStepInfo.getExecutionElementConfig().getSteps(), stageElementConfig, ciExecutionArgs, portFinder, AmbianceUtils.getAccountId(ambiance), os);

        List<ContainerDefinitionInfo> containerDefinitionInfos = new ArrayList<>();
        containerDefinitionInfos.addAll(serviceCtrDefinitionInfos);
        containerDefinitionInfos.addAll(stepCtrDefinitionInfos);
        return containerDefinitionInfos;
    }


}
