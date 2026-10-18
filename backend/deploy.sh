#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
tag="${1:-}"
if [[ ! "$tag" =~ ^[a-zA-Z0-9][a-zA-Z0-9_.-]{0,127}$ ]]; then
  echo 'Usage: bash deploy.sh <commit-sha-or-image-tag>' >&2
  exit 2
fi
export AWS_REGION="${AWS_REGION:-ap-southeast-1}"
registry='331545064056.dkr.ecr.ap-southeast-1.amazonaws.com'
export FORME_IMAGE="$registry/forme-backend:$tag"

for command in docker python3 flock; do
  command -v "$command" >/dev/null || { echo "Missing command: $command" >&2; exit 1; }
done
aws_cli="$(command -v aws || true)"
if [[ -z "$aws_cli" && -x /usr/local/bin/aws ]]; then aws_cli=/usr/local/bin/aws; fi
[[ -n "$aws_cli" ]] || { echo 'AWS CLI is missing.' >&2; exit 1; }
[[ -f .env && -f docker-compose.yml ]] || { echo 'Missing backend/.env or docker-compose.yml.' >&2; exit 1; }
chmod 600 .env
mkdir -p .deploy
exec 9>.deploy/deploy.lock
flock -n 9 || { echo 'Another deployment is running.' >&2; exit 1; }

# Validate values without sourcing the file or printing credentials.
python3 - <<'PY'
from pathlib import Path
import sys
values = {}
for line in Path('.env').read_text(encoding='utf-8-sig').splitlines():
    if not line.strip() or line.lstrip().startswith('#'):
        continue
    key, sep, value = line.partition('=')
    if sep:
        values[key.strip()] = value
for key in ('DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'JWT_SECRET'):
    value = values.get(key, '')
    if not value or 'THAY_' in value or value.startswith(('"', "'")):
        sys.exit(f'{key}: provide a real unquoted value in backend/.env.')
if not values['DB_URL'].startswith('jdbc:mysql:'):
    sys.exit('DB_URL must use MySQL.')
if len(values['JWT_SECRET'].encode()) < 32 or values['JWT_SECRET'].startswith('local-development-secret'):
    sys.exit('JWT_SECRET must be a new random secret of at least 32 bytes.')
PY

if docker info >/dev/null 2>&1; then
  docker_cmd=(docker)
elif sudo -n docker info >/dev/null 2>&1; then
  docker_cmd=(sudo -n docker)
else
  echo 'Cannot access Docker. Start Docker and grant access or passwordless sudo for deployment.' >&2
  exit 1
fi
compose_version="$("${docker_cmd[@]}" compose version --short)"
python3 - "$compose_version" <<'PY'
import re, sys
version = re.search(r'(\d+)\.(\d+)', sys.argv[1])
if not version or tuple(map(int, version.groups())) < (2, 30):
    sys.exit('Docker Compose >= 2.30 is required (docker compose version).')
PY

# /dev/null prevents Compose from interpolating secrets in its automatic .env.
# The container reads backend/.env through env_file format: raw instead.
compose=("${docker_cmd[@]}" compose --env-file /dev/null -p forme -f docker-compose.yml)
"${compose[@]}" config --quiet
"$aws_cli" ecr get-login-password --region "$AWS_REGION" \
  | "${docker_cmd[@]}" login --username AWS --password-stdin "$registry"
"${compose[@]}" pull backend

mkdir -p uploads/product-images
if [[ "$(stat -c '%u:%g' uploads/product-images)" != '10001:10001' ]]; then
  if [[ "$EUID" -eq 0 ]]; then chown 10001:10001 uploads/product-images
  else sudo -n chown 10001:10001 uploads/product-images; fi
fi

previous=''
container_id="$("${compose[@]}" ps -aq backend)"
if [[ -n "$container_id" ]]; then
  previous="$("${docker_cmd[@]}" inspect --format '{{.Config.Image}}' "$container_id")"
fi
if [[ -n "$previous" ]]; then printf '%s\n' "$previous" > .deploy/previous-image; fi

echo "Deploying $FORME_IMAGE on port 8081..."
if "${compose[@]}" up -d --no-build --pull never --force-recreate --wait --wait-timeout 240 backend; then
  printf '%s\n' "$FORME_IMAGE" > .deploy/current-image
  echo 'Deployment healthy. Swagger: http://56.10.115.197:8081/swagger-ui.html'
else
  echo 'Deployment failed. Inspect the forme backend container with docker logs; see project-data/DEPLOY_EC2.md.' >&2
  if [[ -n "$previous" ]]; then
    echo "Restoring $previous..." >&2
    export FORME_IMAGE="$previous"
    if "${compose[@]}" up -d --no-build --pull never --force-recreate --wait --wait-timeout 240 backend; then
      printf '%s\n' "$previous" > .deploy/current-image
      echo 'Previous image restored. Database migrations are not rolled back.' >&2
    else
      echo 'Rollback also failed; inspect the backend container logs.' >&2
    fi
  else
    "${compose[@]}" stop backend || true
    echo 'First deployment failed; no previous image exists. Failed container retained for logs.' >&2
  fi
  exit 1
fi
