#!/usr/bin/env bash
# Generates the dev mTLS material under ./certs/.
# Re-run this script after the existing certs expire (5 years from generation).
# NOT for production use - passwords are hard-coded.

set -euo pipefail
CERT_DIR="$(cd "$(dirname "$0")/.." && pwd)/certs"
mkdir -p "$CERT_DIR"
cd "$CERT_DIR"

PASS="changeit"
DAYS=1825  # 5 years

# 1. CA
openssl req -x509 -newkey rsa:4096 -keyout ca.key -out ca.crt \
    -days "$DAYS" -nodes -subj "/CN=IKO Dev CA"

# 2. Server cert (used by the nginx mTLS sidecar)
openssl req -newkey rsa:4096 -keyout server.key -out server.csr -nodes \
    -subj "/CN=haalcentraal-personen-mtls"
cat > server.ext <<EOF
subjectAltName=DNS:haalcentraal-personen-mtls,DNS:localhost,IP:127.0.0.1
EOF
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
    -out server.crt -days "$DAYS" -extfile server.ext

# 3. Client cert (presented by IKO)
openssl req -newkey rsa:4096 -keyout client.key -out client.csr -nodes \
    -subj "/CN=iko-client"
openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
    -out client.crt -days "$DAYS"

# 4. Client JKS (cert + private key for outbound mTLS)
openssl pkcs12 -export -in client.crt -inkey client.key -out client.p12 \
    -name iko-client -password "pass:$PASS"
rm -f client.jks
keytool -importkeystore -srckeystore client.p12 -srcstoretype PKCS12 \
    -srcstorepass "$PASS" -destkeystore client.jks -deststoretype JKS \
    -deststorepass "$PASS" -noprompt

# 5. Truststore JKS (so IKO trusts the nginx server cert via the dev CA)
rm -f truststore.jks
keytool -import -trustcacerts -alias iko-dev-ca -file ca.crt \
    -keystore truststore.jks -storepass "$PASS" -noprompt

# Clean up intermediates
rm -f server.csr client.csr server.ext ca.srl client.p12 client.crt client.key

echo "Done. Server cert valid until: $(openssl x509 -enddate -noout -in server.crt | cut -d= -f2)"
