# Guide de Configuration CI/CD

Ce guide explique comment configurer le pipeline CI/CD pour UrbanFlow.

## Vue d'ensemble

Le pipeline CI/CD automatise :
1. ✅ **Build** des images Docker pour tous les services
2. ✅ **Push** vers GitHub Container Registry (GHCR)
3. ✅ **Déploiement** automatique sur Kubernetes (branche `main`)

## Étape 1 : Configuration GitHub

### 1.1 Activer GitHub Actions

Les workflows GitHub Actions sont dans `.github/workflows/`. Ils sont automatiquement activés quand tu push le code.

### 1.2 Configurer les secrets GitHub

Va dans ton repository GitHub → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**

#### Secrets requis pour Kubernetes :

**Option A : KUBECONFIG (pour cluster existant)**
```
Nom: KUBECONFIG
Valeur: [Contenu de ~/.kube/config encodé en base64]
```

Pour générer :
```bash
cat ~/.kube/config | base64 -w 0
# Sur macOS :
cat ~/.kube/config | base64
```

**Option B : AWS EKS**
```
Nom: AWS_ACCESS_KEY_ID
Valeur: [Ton access key ID]

Nom: AWS_SECRET_ACCESS_KEY
Valeur: [Ton secret access key]

Nom: AWS_REGION
Valeur: us-east-1

Nom: EKS_CLUSTER_NAME
Valeur: urbanflow-cluster
```

**Option C : Google GKE**
```
Nom: GCP_SA_KEY
Valeur: [Service account key JSON]

Nom: GCP_PROJECT_ID
Valeur: [Ton project ID]

Nom: GKE_CLUSTER_NAME
Valeur: urbanflow-cluster

Nom: GKE_ZONE
Valeur: us-central1-a
```

## Étape 2 : Préparer le cluster Kubernetes

### 2.1 Créer le namespace

```bash
kubectl create namespace urbanflow
```

### 2.2 Créer les secrets Kubernetes

Utilise le script helper :

```bash
# Créer un fichier .env à partir du template
cp env.template .env
# Édite .env avec tes vraies valeurs

# Créer les secrets dans Kubernetes
./scripts/setup-k8s-secrets.sh
```

Ou manuellement :

```bash
kubectl create secret generic urbanflow-secrets \
  --from-env-file=.env \
  -n urbanflow
```

### 2.3 Vérifier la configuration

```bash
kubectl get secrets -n urbanflow
kubectl get configmap -n urbanflow
```

## Étape 3 : Personnaliser le workflow

### 3.1 Modifier le registry d'images

Par défaut, les images sont pushées vers `ghcr.io/<username>/urbanflow/...`

Pour changer, modifie dans `.github/workflows/ci-cd.yml` :

```yaml
env:
  REGISTRY: ghcr.io  # Change pour docker.io, registry.aws.com, etc.
  IMAGE_PREFIX: ${{ github.repository_owner }}/urbanflow
```

### 3.2 Modifier le déploiement Kubernetes

Édite la section `deploy` dans `.github/workflows/ci-cd.yml` selon ton environnement.

#### Pour AWS EKS :

Remplace la section "Configure kubectl" par :

```yaml
- name: Configure AWS credentials
  uses: aws-actions/configure-aws-credentials@v4
  with:
    aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
    aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
    aws-region: ${{ secrets.AWS_REGION }}

- name: Update kubeconfig for EKS
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

- name: Configure kubectl for GKE
  run: |
    gcloud container clusters get-credentials ${{ secrets.GKE_CLUSTER_NAME }} \
      --zone ${{ secrets.GKE_ZONE }} \
      --project ${{ secrets.GCP_PROJECT_ID }}
```

## Étape 4 : Tester le pipeline

### 4.1 Test local (optionnel)

Tu peux tester le build localement :

```bash
# Build une image
docker build -t ghcr.io/username/urbanflow/route-service:test \
  ./backend-services/route-service

# Test avec docker-compose
docker-compose build
docker-compose up -d
```

### 4.2 Déclencher le pipeline

**Méthode 1 : Push sur main/develop**
```bash
git add .
git commit -m "Setup CI/CD pipeline"
git push origin main
```

**Méthode 2 : Déclenchement manuel**
1. Va dans GitHub → **Actions**
2. Sélectionne **CI/CD Pipeline**
3. Clique sur **Run workflow**

### 4.3 Vérifier les résultats

1. Va dans **Actions** pour voir le workflow en cours
2. Clique sur le workflow pour voir les détails
3. Vérifie que toutes les étapes passent :
   - ✅ Build and push (6 jobs en parallèle)
   - ✅ Deploy (seulement sur main)

## Étape 5 : Vérifier le déploiement

Après le déploiement, vérifie que tout fonctionne :

```bash
# Vérifier les pods
kubectl get pods -n urbanflow

# Vérifier les services
kubectl get services -n urbanflow

# Voir les logs d'un service
kubectl logs -f deployment/route-service -n urbanflow

# Vérifier les événements
kubectl get events -n urbanflow --sort-by='.lastTimestamp'
```

## Étape 6 : Accéder aux images Docker

Les images sont disponibles dans GitHub Container Registry :

1. Va dans ton repository GitHub
2. Clique sur **Packages** (à droite)
3. Tu verras toutes les images : `urbanflow-route-service`, `urbanflow-ticket-service`, etc.

Pour pull une image localement :

```bash
# Login à GHCR
echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin

# Pull une image
docker pull ghcr.io/username/urbanflow/route-service:latest
```

## Troubleshooting

### ❌ Les images ne sont pas pushées

**Problème** : Permission denied lors du push

**Solution** :
1. Vérifie que `GITHUB_TOKEN` a les permissions `packages: write`
2. Vérifie que le repository n'est pas en mode "private" avec restrictions
3. Va dans Settings → Actions → General → Workflow permissions → Read and write permissions

### ❌ Le déploiement Kubernetes échoue

**Problème** : `kubectl: command not found` ou erreur de connexion

**Solution** :
1. Vérifie que `KUBECONFIG` est correctement configuré dans les secrets
2. Teste la connexion localement : `kubectl get nodes`
3. Vérifie que le secret est bien encodé en base64

### ❌ Les pods ne démarrent pas

**Problème** : ImagePullBackOff ou CrashLoopBackOff

**Solution** :
1. Vérifie que les images existent dans GHCR
2. Vérifie que les secrets Kubernetes sont créés
3. Vérifie les logs : `kubectl describe pod <pod-name> -n urbanflow`

### ❌ Les tags d'images ne sont pas mis à jour

**Problème** : Le déploiement utilise toujours `latest`

**Solution** :
1. Vérifie que le workflow met bien à jour les tags dans `kustomization.yaml`
2. Vérifie que le SHA du commit correspond au tag de l'image
3. Regarde les logs du job "Update image tags"

## Prochaines étapes

Une fois le CI/CD configuré :

1. ✅ Configure un **Ingress** pour exposer les services publiquement
2. ✅ Configure **cert-manager** pour HTTPS automatique
3. ✅ Configure **Horizontal Pod Autoscaler (HPA)** pour l'auto-scaling
4. ✅ Configure **Prometheus** et **Grafana** pour le monitoring
5. ✅ Active les **tests** dans le pipeline CI/CD

Voir `docs/deployment/` pour plus de guides.

