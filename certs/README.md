# Dev mTLS certificates

DEV USE ONLY. These certs are intentionally committed so local development works
out of the box (`docker compose up` + `./gradlew bootRun`). Do not reuse them in
any deployed environment.

- `ca.{crt,key}` - local CA, signs server + client.
- `server.{crt,key}` - nginx mTLS sidecar at `haalcentraal-personen-mtls:8443`.
  SANs: `haalcentraal-personen-mtls`, `localhost`, `127.0.0.1`.
- `client.jks` - client cert + private key presented by IKO. Password: `changeit`.
- `truststore.jks` - contains the dev CA so IKO trusts the nginx server cert.
  Password: `changeit`.

Regenerate (e.g. after 5-year expiry) with `./scripts/generate-dev-certs.sh`
from the repo root.
