package software.wings.helpers.ext.k8s.request;

import io.harness.delegate.task.helm.HelmCommandFlag;
import io.harness.expression.Expression;
import io.harness.expression.ExpressionReflectionUtils;
import io.harness.manifest.CustomManifestSource;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.appmanifest.StoreType;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.kustomize.KustomizeConfig;

import java.util.List;

import static io.harness.expression.Expression.ALLOW_SECRETS;

@Data
@Builder
public class K8sDelegateManifestConfig implements ExpressionReflectionUtils.NestedAnnotationResolver {
    StoreType manifestStoreTypes;
    List<EncryptedDataDetail> encryptedDataDetails;

    // Applies to GitFileConfig
    private GitFileConfig gitFileConfig;
    private GitConfig gitConfig;

    // Applies only to HelmRepoConfig
    private HelmChartConfigParams helmChartConfigParams;

    // Applies only to Kustomize
    private KustomizeConfig kustomizeConfig;

    // Applies only to Custom/CustomOpenshiftTemplate
    @Expression(ALLOW_SECRETS) private CustomManifestSource customManifestSource;

    @Expression(ALLOW_SECRETS) private HelmCommandFlag helmCommandFlag;

    private boolean customManifestEnabled;

    private boolean bindValuesAndManifestFetchTask;

    private boolean optimizedFilesFetch;

    private boolean shouldSaveManifest;
}
