This harness-delegate-values.yaml file is compatible with version 1.0.2
of the harness-delegate helm chart.

You can download the harness-delegate helm chart at
https://app.harness.io/storage/harness-download/harness-helm-charts/

To add Harness helm repo with name harness:
helm repo add harness https://app.harness.io/storage/harness-download/harness-helm-charts/

To install the chart with the release name my-release and this values.yaml
helm install --name my-release harness/harness-delegate -f harness-delegate-values.yaml