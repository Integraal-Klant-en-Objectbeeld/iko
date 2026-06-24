/*
 * One-off helper to (re)generate the AES-GCM ciphertext used in the e2e baseline
 * seed (V9999.01.01.1__e2e_baseline.sql) for `connector_instance_config.value`.
 *
 * WHY THIS EXISTS
 * ---------------
 * Connector instance config is encrypted at rest (see crypto/AesGcmEncryptionService.kt).
 * The mounted Flyway seed therefore has to store *ciphertext*, not plaintext, and
 * that ciphertext must be decryptable by the app using the fixed e2e IKO_CRYPTO_KEY
 * that docker-compose-e2e.yaml sets. This script reproduces the exact storage
 * format the app uses so the seed values can be regenerated if the key changes.
 *
 * STORAGE FORMAT (must match AesGcmEncryptionService)
 * ---------------------------------------------------
 *   Base64( IV[12 bytes] || CIPHERTEXT || GCM_TAG[16 bytes] )
 *   - AES-256-GCM (key is a 32-byte Base64 string)
 *   - 12-byte random IV, 128-bit auth tag appended after the ciphertext
 * The Java/JCE side reads IV = first 12 bytes and treats the remainder
 * (ciphertext + tag) as one blob, which is exactly this layout.
 *
 * USAGE
 * -----
 *   node e2e/seed/encrypt-config.mjs
 *   # or with an override key / custom values:
 *   IKO_CRYPTO_KEY=<base64-aes-256-key> node e2e/seed/encrypt-config.mjs "value to encrypt"
 *
 * Each run produces a *fresh* random IV, so the Base64 output differs every time;
 * any of those outputs is a valid ciphertext for the same plaintext. Paste the
 * printed values into the seed SQL and update the plaintext comment next to them.
 */

import { randomBytes, createCipheriv, createDecipheriv } from "node:crypto";

// Fixed e2e key — keep in sync with IKO_CRYPTO_KEY in docker-compose-e2e.yaml
// (the same key application-test.yml uses).
const KEY_B64 =
    process.env.IKO_CRYPTO_KEY ?? "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=";

const IV_LENGTH = 12;
const TAG_LENGTH = 16;

const key = Buffer.from(KEY_B64, "base64");

function encrypt(plainText) {
    const iv = randomBytes(IV_LENGTH);
    const cipher = createCipheriv("aes-256-gcm", key, iv, {
        authTagLength: TAG_LENGTH,
    });
    const ciphertext = Buffer.concat([
        cipher.update(plainText, "utf8"),
        cipher.final(),
    ]);
    const tag = cipher.getAuthTag();
    return Buffer.concat([iv, ciphertext, tag]).toString("base64");
}

function decrypt(b64) {
    const buf = Buffer.from(b64, "base64");
    const iv = buf.subarray(0, IV_LENGTH);
    const tag = buf.subarray(buf.length - TAG_LENGTH);
    const ciphertext = buf.subarray(IV_LENGTH, buf.length - TAG_LENGTH);
    const decipher = createDecipheriv("aes-256-gcm", key, iv, {
        authTagLength: TAG_LENGTH,
    });
    decipher.setAuthTag(tag);
    return Buffer.concat([decipher.update(ciphertext), decipher.final()]).toString(
        "utf8",
    );
}

// Default plaintext values used by the BRP personen connector instance, or the
// single value passed as a CLI argument.
const values =
    process.argv.length > 2
        ? process.argv.slice(2)
        : [
              "http://haalcentraal-personen:5010",
              "file:/openapi-specs/haalcentraal-brp-personen.yaml",
          ];

for (const plain of values) {
    const ct = encrypt(plain);
    const roundtrip = decrypt(ct);
    if (roundtrip !== plain) {
        throw new Error(`Round-trip mismatch for "${plain}"`);
    }
    console.log(`plaintext : ${plain}`);
    console.log(`ciphertext: ${ct}`);
    console.log("");
}
