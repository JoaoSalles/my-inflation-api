#!/usr/bin/env bash
#
# Creates (or repins) the daily scraper as a Fly *scheduled machine*.
#
# A scheduled machine is started by Fly once per period, runs its entrypoint to completion,
# then stops — exactly the batch model. It lives in the same Fly app as the web service, so it
# inherits the app's secrets (DB_URL, DB_PASSWORD, ...) and needs no extra config.
#
# The machine is pinned to a specific image, so re-run this script after every `fly deploy`
# to repin it to the latest code (it destroys the previous scraper machine first, by metadata).
#
# Usage: ./scripts/deploy-scraper-machine.sh
set -euo pipefail

APP="my-inflation-api"
NAME="cronjob-scrap"
REGION="iad"                 # keep the scraper in the same region as the database
SCHEDULE="daily"             # hourly | daily | weekly | monthly (Fly picks the time within the period)
MEMORY_MB=2048               # Chromium (Carrefour/Playwright) OOMs on 1 GB
ENTRYPOINT="java -Xmx1500m -cp app.jar com.salles.root.ScraperJobKt"
ROLE_TAG="scraper-cron"
GROUP="scraper"              # Fly process group (sets fly_process_group metadata)

# Build & push a fresh image from the current working tree, unless SKIP_DEPLOY=1.
# This is what packages ScraperJobKt into app.jar — the pin step below only *points at*
# an existing release, it does not build one.
if [[ "${SKIP_DEPLOY:-0}" != "1" ]]; then
  echo "Building & deploying a fresh image (fly deploy)..."
  fly deploy --app "$APP"
else
  echo "SKIP_DEPLOY=1 set; pinning to the existing release without building."
fi

# Resolve the image currently running for the web app (set by the last `fly deploy`).
echo "Resolving current image for app '$APP'..."
IMG_JSON=$(fly image show --app "$APP" --json)
# `fly image show --json` returns an array of image records; take the first.
# Use the tag form (repo:tag) — passing a @sha256 digest makes `fly machine run`
# re-append the digest and reject it as an invalid identifier.
IMAGE="$(echo "$IMG_JSON" | jq -r '.[0] | "\(.Registry)/\(.Repository):\(.Tag)"')"
if [[ "$IMAGE" == *null* ]]; then
  echo "Could not resolve image ref. Raw 'fly image show --json' output:" >&2
  echo "$IMG_JSON" >&2
  exit 1
fi
echo "Image: $IMAGE"

# Destroy any previous scraper machine so re-runs don't pile up duplicates.
echo "Removing previous scheduled scraper machines (if any)..."
fly machine list --app "$APP" --json \
  | jq -r ".[] | select(.config.metadata.role == \"$ROLE_TAG\") | .id" \
  | while read -r id; do
      [ -n "$id" ] && echo "  destroying $id" && fly machine destroy "$id" --app "$APP" --force
    done

# Create the scheduled machine pinned to the current image.
echo "Creating scheduled scraper machine ($SCHEDULE)..."
fly machine run "$IMAGE" \
  --app "$APP" \
  --name "$NAME" \
  --schedule "$SCHEDULE" \
  --region "$REGION" \
  --vm-memory "$MEMORY_MB" \
  --metadata "role=$ROLE_TAG" \
  --entrypoint "$ENTRYPOINT"

echo "Done. Inspect logs with: fly logs --app $APP"
