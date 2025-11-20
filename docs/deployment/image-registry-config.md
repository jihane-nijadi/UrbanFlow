# Configuration du Registry d'Images Docker

Ce document explique comment les images Docker sont configurées et publiées pour UrbanFlow.

## Format des Images

Les images Docker sont publiées avec le format suivant :

```
ghcr.io/<github-username>/urbanflow/<service-name>:<tag>
```

### Exemples

Si ton repository GitHub est `jihaneeee/UrbanFlow`, les images seront :

- `ghcr.io/jihaneeee/urbanflow/route-service:latest`
- `ghcr.io/jihaneeee/urbanflow/ticket-service:latest`
- `ghcr.io/jihaneeee/urbanflow/user-service:latest`
- `ghcr.io/jihaneeee/urbanflow/payment-service:latest`
- `ghcr.io/jihaneeee/urbanflow/geolocation-service:latest`
- `ghcr.io/jihaneeee/urbanflow/frontend:latest`

## Tags Générés

Le pipeline CI/CD génère plusieurs tags pour chaque image :

### Sur la branche `main` :
- `latest` - Tag principal utilisé en production
- `main` - Tag de branche
- `main-<sha>` - Branche + SHA du commit (ex: `main-abc1234`)

### Sur d'autres branches :
- `<branch-name>` - Nom de la branche
- `<branch-name>-<sha>` - Branche + SHA du commit

### Sur les Pull Requests :
- `pr-<number>` - Numéro de la PR

## Configuration dans Kubernetes

### Manifests de Base

Les manifests Kubernetes dans `infrastructure/kubernetes/base/` utilisent des images génériques :

```yaml
image: ghcr.io/urbanflow/route-service:latest
```

Ces images sont des **placeholders** qui seront remplacés automatiquement par le pipeline CI/CD.

### Overlay Production

Le fichier `infrastructure/kubernetes/prod/kustomization.yaml` contient la configuration des images qui sera mise à jour par le pipeline :

```yaml
images:
  - name: ghcr.io/urbanflow/route-service
    newName: ghcr.io/urbanflow/route-service
    newTag: latest
```

Le pipeline CI/CD remplace automatiquement ces valeurs avec :

```yaml
images:
  - name: ghcr.io/urbanflow/route-service
    newName: ghcr.io/<github-username>/urbanflow/route-service
    newTag: latest  # ou <sha> selon la branche
```

## Comment ça Fonctionne

### 1. Build des Images

Quand tu push sur `main` ou `develop`, le pipeline :

1. Build les 6 images Docker en parallèle
2. Push vers GitHub Container Registry (GHCR)
3. Génère plusieurs tags (latest, branch-name, sha, etc.)

### 2. Déploiement sur Kubernetes

Sur push vers `main`, le pipeline :

1. Met à jour `infrastructure/kubernetes/prod/kustomization.yaml` avec les nouveaux tags
2. Build les manifests Kubernetes avec `kustomize build`
3. Déploie sur le cluster avec `kubectl apply`

### 3. Remplacement des Images

Le pipeline utilise `kustomize edit set image` pour remplacer :

```bash
kustomize edit set image \
  ghcr.io/urbanflow/route-service=ghcr.io/jihaneeee/urbanflow/route-service:latest \
  ...
```

Cela met à jour automatiquement la section `images:` dans `kustomization.yaml`.

## Changer le Registry

### Utiliser un autre Registry (Docker Hub, ECR, etc.)

1. **Modifier le workflow CI/CD** (`.github/workflows/ci-cd.yml`) :

```yaml
env:
  REGISTRY: docker.io  # ou registry.aws.com, gcr.io, etc.
  IMAGE_PREFIX: your-username/urbanflow  # ou votre organisation
```

2. **Mettre à jour les manifests de base** :

Remplace `ghcr.io/urbanflow` par ton registry dans tous les fichiers :
- `infrastructure/kubernetes/base/apps-*.yaml`

3. **Mettre à jour kustomization.yaml** :

```yaml
images:
  - name: ghcr.io/urbanflow/route-service
    newName: docker.io/your-username/urbanflow/route-service
    newTag: latest
```

### Utiliser AWS ECR

```yaml
env:
  REGISTRY: <account-id>.dkr.ecr.<region>.amazonaws.com
  IMAGE_PREFIX: urbanflow
```

Et ajouter l'authentification ECR dans le workflow :

```yaml
- name: Configure AWS credentials
  uses: aws-actions/configure-aws-credentials@v4
  with:
    aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
    aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
    aws-region: us-east-1

- name: Login to Amazon ECR
  id: login-ecr
  uses: aws-actions/amazon-ecr-login@v2
```

### Utiliser Google Container Registry (GCR)

```yaml
env:
  REGISTRY: gcr.io
  IMAGE_PREFIX: <project-id>/urbanflow
```

## Vérifier les Images Publiées

### Sur GitHub

1. Va dans ton repository GitHub
2. Clique sur **Packages** (à droite)
3. Tu verras toutes les images : `urbanflow-route-service`, `urbanflow-ticket-service`, etc.

### Via Docker CLI

```bash
# Login à GHCR
echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin

# Lister les images
docker images | grep urbanflow

# Pull une image
docker pull ghcr.io/username/urbanflow/route-service:latest
```

### Via kubectl

```bash
# Voir les images utilisées par les pods
kubectl get pods -n urbanflow -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.containers[*].image}{"\n"}{end}'

# Voir les images d'un déploiement spécifique
kubectl describe deployment route-service -n urbanflow | grep Image
```

## Rollback

Pour revenir à une version précédente :

### Option 1 : Via le tag SHA

Si tu connais le SHA du commit :

```bash
cd infrastructure/kubernetes/prod
kustomize edit set image \
  ghcr.io/urbanflow/route-service=ghcr.io/username/urbanflow/route-service:<sha>
kubectl apply -k .
```

### Option 2 : Via kubectl rollout

```bash
# Voir l'historique des déploiements
kubectl rollout history deployment/route-service -n urbanflow

# Rollback vers la version précédente
kubectl rollout undo deployment/route-service -n urbanflow

# Rollback vers une version spécifique
kubectl rollout undo deployment/route-service -n urbanflow --to-revision=2
```

## Troubleshooting

### ❌ ImagePullBackOff

**Problème** : Kubernetes ne peut pas pull l'image

**Solutions** :
1. Vérifie que l'image existe dans le registry
2. Vérifie les permissions d'accès au registry
3. Configure un `imagePullSecrets` si le registry est privé :

```yaml
spec:
  imagePullSecrets:
    - name: ghcr-secret
```

Créer le secret :

```bash
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=USERNAME \
  --docker-password=$GITHUB_TOKEN \
  -n urbanflow
```

### ❌ Les images ne sont pas mises à jour

**Problème** : Le déploiement utilise toujours l'ancienne image

**Solutions** :
1. Vérifie que le pipeline a bien mis à jour `kustomization.yaml`
2. Force le redéploiement : `kubectl rollout restart deployment/<service> -n urbanflow`
3. Vérifie les tags dans le registry

### ❌ Permission denied sur GHCR

**Problème** : Impossible de push/pull les images

**Solutions** :
1. Vérifie que `GITHUB_TOKEN` a les permissions `packages: write`
2. Vérifie que le repository n'est pas privé avec restrictions
3. Va dans Settings → Actions → General → Workflow permissions → Read and write permissions

