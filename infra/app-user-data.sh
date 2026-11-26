#!/bin/bash
set -euo pipefail

region="${aws_region}"
db_host="${db_host}"
app_commit="20003b0d7d2a6172ac18df070310321545375f6d"
source_dir="/opt/ums/source"
ca_bundle="/opt/ums/rds-ca.pem"

dnf install -y docker git awscli-2
systemctl enable --now docker

mkdir -p "$source_dir"
git -C "$source_dir" init
git -C "$source_dir" fetch --depth 1 \
  https://github.com/BoDe-117/university-management-system.git "$app_commit"
git -C "$source_dir" checkout --detach FETCH_HEAD
docker build --tag "ums-app:$app_commit" "$source_dir"

curl --fail --silent --show-error --location \
  "https://truststore.pki.rds.amazonaws.com/$region/$region-bundle.pem" \
  --output "$ca_bundle"
chmod 0644 "$ca_bundle"

read_parameter() {
  aws ssm get-parameter --region "$region" --name "$1" --with-decryption \
    --query 'Parameter.Value' --output text --no-cli-pager
}

SPRING_DATASOURCE_PASSWORD="$(read_parameter /ums/app/db-password)"
JWT_SECRET="$(read_parameter /ums/app/jwt-secret)"
SPRING_DATASOURCE_URL="jdbc:postgresql://$db_host:5432/umsdb?sslmode=verify-full&sslrootcert=/etc/ums/rds-ca.pem"
SPRING_DATASOURCE_USERNAME="umsadmin"
SERVER_PORT=8080
export SPRING_DATASOURCE_PASSWORD JWT_SECRET SPRING_DATASOURCE_URL
export SPRING_DATASOURCE_USERNAME SERVER_PORT

docker run --detach --name ums-app --restart unless-stopped \
  --publish 127.0.0.1:8080:8080 \
  --mount "type=bind,src=$ca_bundle,dst=/etc/ums/rds-ca.pem,readonly" \
  --env SPRING_DATASOURCE_URL --env SPRING_DATASOURCE_USERNAME \
  --env SPRING_DATASOURCE_PASSWORD --env JWT_SECRET --env SERVER_PORT \
  "ums-app:$app_commit"
