#!/usr/bin/env sh

CONSUMER="$1"

API_KEY=vault read "apikey/apigw/dev/helse-reverse-proxy/${CONSUMER}_q1" -format=json | jq '.data | {"key": .["x-nav-apiKey"]} | .key'
echo "APIKEY=$API_KEY"


