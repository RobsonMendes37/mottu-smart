#!/bin/sh
set -e

# ==============================================================================
# MOTTU SMART OPS — MINIO BUCKET INITIALIZATION SCRIPT
# ==============================================================================

MINIO_HOST="${MINIO_HOST:-minio}"
MINIO_PORT="${MINIO_PORT:-9000}"
ACCESS_KEY="${S3_ACCESS_KEY:-minioadmin}"
SECRET_KEY="${S3_SECRET_KEY:-minioadmin123}"
BUCKET_NAME="${S3_BUCKET_NAME:-mottu-inspections}"

echo "=============================================================================="
echo "Mottu Smart Ops: Initializing MinIO Bucket '${BUCKET_NAME}'"
echo "Target: http://${MINIO_HOST}:${MINIO_PORT}"
echo "=============================================================================="

# Wait for MinIO service to become ready
echo "Waiting for MinIO server..."
until mc alias set localminio "http://${MINIO_HOST}:${MINIO_PORT}" "${ACCESS_KEY}" "${SECRET_KEY}" > /dev/null 2>&1; do
  echo "MinIO is not reachable yet, waiting 2s..."
  sleep 2
done

echo "Connected to MinIO successfully."

# 1. Create the bucket idempotently
echo "Ensuring bucket '${BUCKET_NAME}' exists..."
mc mb --ignore-existing "localminio/${BUCKET_NAME}"

# 2. Set anonymous download policy so photos can be viewed via GET / photoDownloadUrl
echo "Setting anonymous download policy on '${BUCKET_NAME}'..."
mc anonymous set download "localminio/${BUCKET_NAME}"

echo "Bucket '${BUCKET_NAME}' configured successfully!"
exit 0
