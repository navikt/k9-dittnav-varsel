#!/usr/bin/env sh

echo "Appyling oneshot file"
curl -X PUT "http://igroup:itest@localhost:8840/api/v1/oneshot" -H  "Accept: application/json" -H  "Content-Type: application/json" --data "@oneshot.json" -v
