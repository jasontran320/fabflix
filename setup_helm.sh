# Demo script for deploying MySQL replication with Helm
# IMPORTANT: Replace all <PLACEHOLDER> values with your own configuration.
# Do NOT hardcode real credentials in this file. Use Kubernetes Secrets in production.

#!/bin/bash
set -e

# --- Install Helm ---
echo "[*] Installing Helm..."
sudo snap install helm --classic

# --- Deploy MySQL with Helm (Primary + Secondary replication) ---
echo "[*] Deploying MySQL Master/Slave with Helm..."

helm install mysql \
  --set auth.rootPassword='<ROOT_PASSWORD>' \
  --set auth.username='<DB_USER>' \
  --set auth.password='<DB_PASSWORD>' \
  --set auth.replicationPassword='<REPLICATION_PASSWORD>' \
  --set secondary.persistence.enabled=true \
  --set secondary.persistence.size=2Gi \
  --set primary.persistence.enabled=true \
  --set primary.persistence.size=2Gi \
  --set architecture=replication \
  --set secondary.replicaCount=1 \
  oci://registry-1.docker.io/bitnamicharts/mysql

echo "[*] MySQL Helm release started."

# --- Deploy Ingress Controller with Helm ---
helm upgrade --install ingress-nginx ingress-nginx \
  --repo https://kubernetes.github.io/ingress-nginx \
  --namespace ingress-nginx --create-namespace
