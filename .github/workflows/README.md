# CI/CD Pipeline Documentation

Ce dossier contient les workflows GitHub Actions pour automatiser le build, le test et le déploiement d'UrbanFlow.

## Workflows disponibles

### `ci-cd.yml` - Pipeline principal

Ce workflow automatise :
1. **Build des images Docker** pour tous les services
2. **Push vers GitHub Container Registry (GHCR)**
3. **Déploiement sur Kubernetes** (seulement sur la branche `main`)

## Configuration requise

### 1. Secrets GitHub

Configure les secrets suivants dans ton repository GitHub (Settings → Secrets and variables → Actions) :

#### Pour le déploiement Kubernetes :
- `KUBECONFIG` : Configuration kubectl encodée en base64
  ```bash
  # Générer le secret :
  cat ~/.kube/config | base64 -w 0
  ```

#### Alternative pour AWS EKS :
- `AWS_ACCESS_KEY_ID` : Clé d'accès AWS
- `AWS_SECRET_ACCESS_KEY` : Clé secrète AWS
- `AWS_REGION` : Région AWS (ex: `us-east-1`)
- `EKS_CLUSTER_NAME` : Nom du cluster EKS

### 2. Permissions du repository

Le workflow nécessite les permissions suivantes :
- `contents: read` - Pour lire le code
- `packages: write` - Pour push les images Docker vers GHCR

Ces permissions sont configurées automatiquement via `permissions:` dans le workflow.

## Utilisation

### Déclenchement automatique

Le pipeline se déclenche automatiquement :
- Sur chaque push vers `main` ou `develop`
- Sur chaque Pull Request vers `main` ou `develop`

### Déclenchement manuel

Tu peux aussi déclencher le workflow manuellement :
1. Va dans l'onglet "Actions" de ton repository GitHub
2. Sélectionne "CI/CD Pipeline"
3. Clique sur "Run workflow"

## Images Docker générées

Les images sont pushées vers : `ghcr.io/<ton-username>/urbanflow/<service-name>`

Exemples :
- `ghcr.io/jihaneeee/urbanflow/route-service:latest`
- `ghcr.io/jihaneeee/urbanflow/ticket-service:main-abc123`
- `ghcr.io/jihaneeee/urbanflow/frontend:v1.2.3`

### Tags générés

Le workflow génère plusieurs tags :
- `latest` - Pour la branche par défaut (main)
- `<branch-name>` - Nom de la branche
- `<branch-name>-<sha>` - Branche + SHA du commit
- `pr-<number>` - Pour les Pull Requests

## Déploiement Kubernetes

Le déploiement se fait uniquement sur la branche `main` après un push.

### Prérequis

1. **Cluster Kubernetes configuré** et accessible
2. **kubectl configuré** avec accès au cluster
3. **Namespace `urbanflow` créé** :
   ```bash
   kubectl create namespace urbanflow
   ```
4. **Secrets Kubernetes créés** :
   ```bash
   kubectl create secret generic urbanflow-secrets \
     --from-env-file=.env \
     -n urbanflow
   ```

### Personnalisation du déploiement

Pour personnaliser le déploiement, modifie la section `deploy` dans `.github/workflows/ci-cd.yml` :

#### Pour AWS EKS :
```yaml
- name: Configure kubectl for EKS
  uses: aws-actions/configure-aws-credentials@v4
  with:
    aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
    aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
    aws-region: ${{ secrets.AWS_REGION }}

- name: Update kubeconfig
  run: |
    aws eks update-kubeconfig \
      --name ${{ secrets.EKS_CLUSTER_NAME }} \
      --region ${{ secrets.AWS_REGION }}
```

#### Pour Google GKE :
```yaml
- name: Authenticate to Google Cloud
  uses: google-github-actions/auth@v2
  with:
    credentials_json: ${{ secrets.GCP_SA_KEY }}

- name: Set up Cloud SDK
  uses: google-github-actions/setup-gcloud@v2

- name: Configure kubectl
  run: |
    gcloud container clusters get-credentials ${{ secrets.GKE_CLUSTER_NAME }} \
      --zone ${{ secrets.GKE_ZONE }} \
      --project ${{ secrets.GCP_PROJECT_ID }}
```

## Tests

La section de tests est actuellement commentée. Pour l'activer :

1. Décommente la section `test` dans `ci-cd.yml`
2. Ajoute les tests unitaires dans chaque service
3. Configure les dépendances nécessaires (JDK, Node.js, etc.)

## Monitoring

Après le déploiement, vérifie le statut :

```bash
# Vérifier les pods
kubectl get pods -n urbanflow

# Vérifier les services
kubectl get services -n urbanflow

# Voir les logs
kubectl logs -f deployment/route-service -n urbanflow
```

## Troubleshooting

### Les images ne sont pas pushées

- Vérifie que `GITHUB_TOKEN` a les permissions `packages: write`
- Vérifie que le repository est public ou que tu as les droits sur les packages

### Le déploiement Kubernetes échoue

- Vérifie que `KUBECONFIG` est correctement configuré
- Vérifie que le namespace `urbanflow` existe
- Vérifie que les secrets Kubernetes sont créés
- Vérifie les logs du workflow : Actions → Dernier workflow → Job "deploy"

### Les images ne sont pas trouvées

- Vérifie que les images sont bien pushées dans GHCR
- Vérifie que le tag utilisé correspond au SHA du commit
- Vérifie les permissions d'accès aux packages GitHub

