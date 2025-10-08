# Demo script for creating a Kubernetes cluster with kOps on AWS
# IMPORTANT: Replace all <PLACEHOLDER> values with your own configuration.
# Do NOT commit real AWS credentials or bucket names into version control.


#!/bin/bash
set -e  # stop on error (except where we override with || true)

# --- Install AWS CLI ---
echo "[*] Installing AWS CLI..."
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
sudo apt-get update -y
sudo apt-get install -y unzip
unzip -o awscliv2.zip
sudo ./aws/install

# --- Configure AWS CLI ---
echo "[*] Configuring AWS CLI..."
aws configure set aws_access_key_id "<YOUR_AWS_ACCESS_KEY_ID>"
aws configure set aws_secret_access_key "<YOUR_AWS_SECRET_ACCESS_KEY>"
aws configure set default.region us-west-2
aws configure set output json

# --- Install kubectl ---
echo "[*] Installing kubectl..."
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# --- Install kOps ---
echo "[*] Installing kOps..."
curl -Lo kops https://github.com/kubernetes/kops/releases/download/$(curl -s https://api.github.com/repos/kubernetes/kops/releases/latest | grep tag_name | cut -d '"' -f 4)/kops-linux-amd64
chmod +x kops
sudo mv kops /usr/local/bin/kops

# --- Export keys for kOps ---
export AWS_ACCESS_KEY_ID=$(aws configure get aws_access_key_id)
export AWS_SECRET_ACCESS_KEY=$(aws configure get aws_secret_access_key)

# --- Define cluster vars ---
export NAME=<YOUR_CLUSTER_NAME>
export STATE_BUCKET=<YOUR_STATE_STORE_BUCKET>
export OIDC_BUCKET=<YOUR_OIDC_BUCKET>
export KOPS_STATE_STORE=s3://$STATE_BUCKET

# --- Create state-store bucket ---
echo "[*] Creating state-store bucket..."
aws s3api create-bucket \
  --bucket $STATE_BUCKET \
  --region us-west-2 \
  --create-bucket-configuration LocationConstraint=us-west-2 || true

# --- Create oidc-store bucket ---
echo "[*] Creating oidc-store bucket..."
aws s3api create-bucket \
  --bucket $OIDC_BUCKET \
  --region us-west-2 \
  --object-ownership BucketOwnerPreferred \
  --create-bucket-configuration LocationConstraint=us-west-2 || true

# --- Configure OIDC bucket for public access ---
echo "[*] Configuring oidc-store bucket..."
aws s3api put-public-access-block \
  --bucket $OIDC_BUCKET \
  --public-access-block-configuration BlockPublicAcls=false,IgnorePublicAcls=false,BlockPublicPolicy=false,RestrictPublicBuckets=false || true

aws s3api put-bucket-acl \
  --bucket $OIDC_BUCKET \
  --acl public-read || true

# --- Create cluster config (not applied yet) ---
echo "[*] Creating cluster config..."
kops create cluster \
  --name=${NAME} \
  --cloud=aws \
  --zones=us-west-2a \
  --discovery-store=s3://${OIDC_BUCKET}/${NAME}/discovery

# --- Apply cluster ---
echo "[*] Applying cluster..."
kops update cluster --name ${NAME} --yes --admin

echo "[✓] Kubernetes cluster creation initiated!"

export KOPS_STATE_STORE=s3://<YOUR_STATE_STORE_BUCKET>
