#!/bin/bash
# Script pour créer les secrets Kubernetes à partir du fichier .env
# Usage: ./scripts/setup-k8s-secrets.sh [namespace]

set -e

NAMESPACE=${1:-urbanflow}
ENV_FILE=${2:-.env}

if [ ! -f "$ENV_FILE" ]; then
    echo "❌ Fichier .env non trouvé: $ENV_FILE"
    echo "💡 Crée un fichier .env à partir de env.template"
    exit 1
fi

echo "📦 Création du namespace $NAMESPACE (s'il n'existe pas)..."
kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

echo "🔐 Création des secrets Kubernetes à partir de $ENV_FILE..."

# Créer le secret depuis le fichier .env
kubectl create secret generic urbanflow-secrets \
    --from-env-file="$ENV_FILE" \
    --namespace="$NAMESPACE" \
    --dry-run=client -o yaml | kubectl apply -f -

echo "✅ Secrets créés avec succès dans le namespace $NAMESPACE"
echo ""
echo "📋 Vérification:"
kubectl get secret urbanflow-secrets -n "$NAMESPACE"

echo ""
echo "💡 Pour voir les clés du secret:"
echo "   kubectl get secret urbanflow-secrets -n $NAMESPACE -o jsonpath='{.data}' | jq 'keys'"

