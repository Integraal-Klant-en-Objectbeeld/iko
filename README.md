# Integraal Klant & Objectbeeld (IKO)

## Docker

The shipped Dockerfile, builds and creates an image that can be used to run IKO. The docker compose file contains some
services that IKO can connect to and setups a Keycloak authentication. Simply run `docker compose up -d` to run it.

To be able to login with Keycloak into the admin panel you will have to add the following to your respective host file.

```text
127.0.0.1 keycloak
```

### Publishing
Choos the platform to use in the build --platform=linux/amd64|linux/arm64
```text
docker build --platform=linux/amd64 .
```

## Development info 

Rename the .env.template to .env
The env.template file contains a SPRING_THYMELEAF_PREFIX=file:src/main/resources/templates/
This is to allow local dev to have no caching when working on HTML. Remove it when running the docker container.

### Admin  

Go to http://localhost:0808/admin to access the admin.
For a full tutorial how to create Aggregated Data Profile go to https://docs.integraal-klant-objectbeeld.nl/admin-configuratie/samengesteld-gegevensprofiel-aanmaken

### Source routes

## More documentation

You can find more documentation [here](./doc/README.md)

### Crypto
IKO uses AES encryption for config storage of connectors. 
Please generate a Base64 key in the env var IKO_CRYPTO_KEY via the .env file for local development.

Kotlin notebook code helps run pieces of code or just ask AI.
```Kotlin
fun generateBase64AesKey(keySize: Int = 256): String {
    val keyGen = KeyGenerator.getInstance("AES")
    keyGen.init(keySize, SecureRandom())
    val secretKey: SecretKey = keyGen.generateKey()
    return Base64.getEncoder().encodeToString(secretKey.encoded)
}

fun main() {
    val base64Key = generateBase64AesKey()
    println("Generated AES key (Base64): $base64Key")
}
main()
```