package software.wings.helpers.ext.helm;

import io.harness.delegate.task.helm.HelmCommandResponse;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequestParams;
import software.wings.helpers.ext.helm.request.HelmReleaseHistoryCommandRequestParams;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequestParams;
import software.wings.helpers.ext.helm.response.HelmReleaseHistoryCommandResponse;

import java.io.IOException;

public interface HelmDeployDelegateService {

    /**
     * Deploy helm command response.
     *
     * @param commandRequest       the command request
     * @return the helm command response
     */
    HelmCommandResponse deploy(HelmInstallCommandRequestParams commandRequest) throws IOException;

    /**
     * Rollback helm command response.
     *
     * @param commandRequest       the command request
     * @return the helm command response
     */
    HelmCommandResponse rollback(HelmRollbackCommandRequestParams commandRequest);

    /**
     * Release history helm release history command response.
     *
     * @param helmCommandRequest the helm command request
     * @return the helm release history command response
     */
    HelmReleaseHistoryCommandResponse releaseHistory(HelmReleaseHistoryCommandRequestParams helmCommandRequest);


}
