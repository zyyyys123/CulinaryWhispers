#!/bin/bash
set -e

echo "Waiting for Doris FE to be ready..."
# Loop until we can connect to Doris FE
until mysql -h doris-fe -P 9030 -u root --skip-password -e "SELECT 1"; do
  echo "Doris FE is not ready yet. Retrying in 5 seconds..."
  sleep 5
done

echo "Doris FE is ready."

# Check if BE is already added
if mysql -h doris-fe -P 9030 -u root --skip-password -e "SHOW BACKENDS" | grep -q "doris-be"; then
  echo "Backend doris-be already registered."
else
  echo "Registering Backend doris-be..."
  mysql -h doris-fe -P 9030 -u root --skip-password -e "ALTER SYSTEM ADD BACKEND 'doris-be:9050';"
  echo "Backend registered."
fi

echo "Preparing initialization SQL..."
# Replace replication_num = 3 with replication_num = 1 for single-node setup
sed 's/"replication_num" = "3"/"replication_num" = "1"/g' /sql/doris.sql > /tmp/doris_init.sql

echo "Executing initialization SQL..."
mysql -h doris-fe -P 9030 -u root --skip-password < /tmp/doris_init.sql

echo "Doris initialization completed successfully."
