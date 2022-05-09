package software.wings.helpers.ext.helm;

import io.harness.delegate.task.helm.HelmCommandResponse;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequestParams;
import software.wings.helpers.ext.helm.request.HelmReleaseHistoryCommandRequestParams;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequestParams;
import software.wings.helpers.ext.helm.response.HelmReleaseHistoryCommandResponse;

import java.io.IOException;

public class HelmDeployDelegateServiceImpl implements HelmDeployDelegateService{


    @Override
    public HelmCommandResponse deploy(HelmInstallCommandRequestParams commandRequest) throws IOException {
        return null;
    }

    @Override
    public HelmCommandResponse rollback(HelmRollbackCommandRequestParams commandRequest) {
        return null;
    }

    @Override
    public HelmReleaseHistoryCommandResponse releaseHistory(HelmReleaseHistoryCommandRequestParams helmCommandRequest) {
        return null;
    }
}
