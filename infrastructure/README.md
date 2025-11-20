## Infrastructure Layout

This directory holds all deployment-as-code assets that take UrbanFlow from Docker Compose to production environments.

```
infrastructure/
└── kubernetes/
    ├── base/   # Common manifests (namespaces, configmaps, secrets templates, shared services)
    ├── dev/    # Overlays / values for development clusters
    └── prod/   # Overlays / values for production clusters
```

### Usage

1. Start by authoring cluster-agnostic manifests in `kubernetes/base`.
2. Layer environment-specific overrides (image tags, replica counts, resource limits) inside `kubernetes/dev` and `kubernetes/prod`.
3. Reference these folders from your CI/CD pipeline (e.g., `kubectl apply -k infrastructure/kubernetes/prod` or Helm charts stored inside each folder).

Add additional subdirectories (Helm charts, Terraform, etc.) under `infrastructure/` as the project grows.

