## Kubernetes Manifests

This folder contains the resources to run UrbanFlow on a Kubernetes cluster.

### Layout

```
kubernetes/
├── base/        # Cluster-agnostic manifests (namespace, config, Deployments, Services)
├── dev/         # Environment-specific overlays (image tags, replica counts, ingress host)
└── prod/        # Production overrides (autoscaling, stricter resources, secrets references)
```

The `base` directory is bundled with a `kustomization.yaml` so you can test locally:

```bash
kubectl apply -k infrastructure/kubernetes/base
```

Before applying:

1. Replace placeholder secret values in `secret-placeholders.yaml` (or better, generate a sealed secret / external secret source).
2. Point service images to your registry (`ghcr.io/urbanflow/*` is a placeholder).
3. Decide whether databases run outside the cluster; if so, update the JDBC URLs accordingly.

Use the `dev` and `prod` overlays to set replica counts, ingress hosts, resource limits, and image tags aligned with each environment. Add Helm charts or Kustomize patches here as the deployment strategy evolves.

