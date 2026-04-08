GH Bot Orchestrator
===================

A service that makes building custom GitHub bots easy!

## Usage

1. Install [GH Bot Orchestrator GitHub App](TODO) in your organization
2. Deploy `gh-bot-orchestrator` to your infrastructure (see [deployment](#deployment))
3. Register the bot via web UI to react on given GitHub events
4. Create a workflow in a repository within your organization that looks like this:

```yaml
on:
  workflow_dispatch:
    inputs:
      comment-callback-url:
        type: text
  # other inputs omitted for brevity

jobs:
  hello-world-bot:
    runs-on: ubuntu-latest
    steps:
      - name: Post PR comment
        uses: allegro/gh-bot-orchestrator/post-comment
        with:
          comment-callback-url: ${{ inputs.comment-callback-url }}
          message: Hello, I'm the bot!
```

## Features

- a dedicated PR check is created for the bot execution
- a bot can update the status of that check via provided `check-callback-url`
- a bot can post PR comments via provided `comment-callback-url`
- a bot can check out the original repository via provided readonly `github-clone-token`
- a bot can do whatever possible via GitHub API (but a custom GitHub app is required for that)
- a bot can access the original GitHub event
- a bot can access list of files changed in a PR (it's not available in the event)
- a bot can access Dependabot metadata (which dependency was bumped to which version)

## Use cases
- run automatic code migrations for every dependabot PR bumping a specific dependency (see [allwrite](https://github.com/allegro/allwrite))
- validate common config file correctness whenever it's modified
- perform a security scan on the changed files
- automatically add label when PR is approved (e.g. `_<username> : 👍`)

## Deployment

### Prerequisites

- Kubernetes 1.24+
- Helm 3.x
- Docker daemon (for building the image)
- A [GitHub App](https://docs.github.com/en/apps/creating-github-apps) with the required permissions installed in your organization

### Building the container image

The project uses [Spring Boot's Cloud Native Buildpacks](https://docs.spring.io/spring-boot/gradle-plugin/packaging-oci-image.html) integration to build OCI images -- no Dockerfile needed.

Build and tag the image:

```bash
./gradlew bootBuildImage
```

This produces `gh-workflow-orchestrator:<project-version>` in your local Docker daemon.

To customize the image name (e.g. for pushing to a private registry):

```bash
./gradlew bootBuildImage --imageName=myregistry.example.com/my-org/gh-workflow-orchestrator:v1.0.0
```

Push the image to your container registry so your Kubernetes cluster can pull it:

```bash
docker push myregistry.example.com/my-org/gh-workflow-orchestrator:v1.0.0
```

### Installing with Helm

The Helm chart is located at `helm/gh-bot-orchestrator/`. It deploys the application together with a bundled PostgreSQL instance by default.

#### Quick start (bundled PostgreSQL)

```bash
helm install gwo ./helm/gh-bot-orchestrator \
  --set image.repository=myregistry.example.com/my-org/gh-workflow-orchestrator \
  --set image.tag=v1.0.0 \
  --set app.baseUrl=https://gwo.example.com \
  --set app.github.auth.appId=123456 \
  --set app.github.auth.installationId=12345678 \
  --set-file app.github.auth.privateKey=path/to/private-key.pem
```

> Use `--set-file` for the private key since PEM keys are multi-line.

#### Using an external database

Disable the bundled PostgreSQL and point to your own:

```bash
helm install gwo ./helm/gh-bot-orchestrator \
  --set postgresql.enabled=false \
  --set externalDatabase.host=my-postgres.example.com \
  --set externalDatabase.port=5432 \
  --set externalDatabase.database=gwo \
  --set externalDatabase.username=gwo \
  --set externalDatabase.password=supersecret \
  --set image.repository=myregistry.example.com/my-org/gh-workflow-orchestrator \
  --set image.tag=v1.0.0 \
  --set app.baseUrl=https://gwo.example.com \
  --set app.github.auth.appId=123456 \
  --set app.github.auth.installationId=12345678 \
  --set-file app.github.auth.privateKey=path/to/private-key.pem
```

#### Using pre-existing secrets

For production / GitOps workflows, create secrets externally and reference them:

```bash
# Create secrets ahead of time
kubectl create secret generic my-github-secret \
  --from-literal=github-app-id=123456 \
  --from-literal=github-installation-id=12345678 \
  --from-file=github-private-key=path/to/private-key.pem

kubectl create secret generic my-db-secret \
  --from-literal=postgresql-password=supersecret

# Reference them during install
helm install gwo ./helm/gh-bot-orchestrator \
  --set app.github.auth.existingSecret=my-github-secret \
  --set postgresql.auth.existingSecret=my-db-secret \
  --set image.repository=myregistry.example.com/my-org/gh-workflow-orchestrator \
  --set image.tag=v1.0.0 \
  --set app.baseUrl=https://gwo.example.com
```

#### Enabling ingress

```bash
helm install gwo ./helm/gh-bot-orchestrator \
  --set ingress.enabled=true \
  --set ingress.className=nginx \
  --set ingress.hosts[0].host=gwo.example.com \
  --set ingress.hosts[0].paths[0].path=/ \
  --set ingress.hosts[0].paths[0].pathType=Prefix \
  --set ingress.tls[0].secretName=gwo-tls \
  --set ingress.tls[0].hosts[0]=gwo.example.com \
  # ... other flags
```

### Deploying to minikube (local development)

```bash
minikube start

# Build the image on your local Docker daemon, then load it into minikube
./gradlew bootBuildImage
minikube image load gh-workflow-orchestrator:unspecified

# Install with image.pullPolicy=Never to use the local image
helm install gwo ./helm/gh-bot-orchestrator \
  --set image.tag=unspecified \
  --set image.pullPolicy=Never \
  --set app.github.auth.appId=123456 \
  --set app.github.auth.installationId=12345678 \
  --set app.github.auth.privateKey="dummy-key-for-testing"

# Access the app via port-forward
kubectl port-forward svc/gwo-gh-bot-orchestrator 8080:8080
```

Then open http://localhost:8080.

### Key configuration reference

| Parameter | Description | Default |
|---|---|---|
| `image.repository` | Container image repository | `gh-workflow-orchestrator` |
| `image.tag` | Image tag (defaults to Chart `appVersion`) | `""` |
| `image.pullPolicy` | Image pull policy | `IfNotPresent` |
| `app.baseUrl` | Base URL reachable from GitHub Actions runners | `http://localhost:8080` |
| `app.github.auth.appId` | GitHub App ID | `""` |
| `app.github.auth.installationId` | GitHub App installation ID | `""` |
| `app.github.auth.privateKey` | GitHub App PEM private key | `""` |
| `app.github.auth.existingSecret` | Name of existing secret with GitHub credentials | `""` |
| `postgresql.enabled` | Deploy a bundled PostgreSQL instance | `true` |
| `postgresql.auth.database` | PostgreSQL database name | `mydatabase` |
| `postgresql.auth.username` | PostgreSQL username | `myuser` |
| `postgresql.auth.password` | PostgreSQL password | `secret` |
| `postgresql.auth.existingSecret` | Name of existing secret with DB password | `""` |
| `postgresql.persistence.enabled` | Enable persistent storage for PostgreSQL | `true` |
| `postgresql.persistence.size` | PVC size | `5Gi` |
| `externalDatabase.host` | External database host (when `postgresql.enabled=false`) | `""` |
| `externalDatabase.port` | External database port | `5432` |
| `ingress.enabled` | Enable ingress | `false` |
| `ingress.className` | Ingress class name | `""` |

See [`values.yaml`](helm/gh-bot-orchestrator/values.yaml) for the full list of configurable parameters.

### Upgrading

```bash
helm upgrade gwo ./helm/gh-bot-orchestrator --reuse-values \
  --set image.tag=v2.0.0
```

### Uninstalling

```bash
helm uninstall gwo

# The PostgreSQL PVC is retained by default. Delete it manually if needed:
kubectl delete pvc gwo-gh-bot-orchestrator-postgresql
```
