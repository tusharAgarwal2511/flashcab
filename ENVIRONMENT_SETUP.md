# Environment and deployment setup

## Local development

Each Spring service can load a module-local `.env` file through
`spring.config.import=optional:file:./.env[.properties]`. Start a service with
its module directory as the working directory so `./.env` resolves correctly.
Real `.env` files are ignored by Git; the checked-in `.env.example` files are
safe templates.

On a fresh checkout, copy only the templates for files that do not already
exist. In PowerShell, from the repository root:

```powershell
$envFiles = @(
  '.env',
  'authService\.env',
  'driverService\.env',
  'gatewayService\.env',
  'locationService\.env',
  'matchingService\.env',
  'rideService\.env',
  'riderService\.env'
)
foreach ($file in $envFiles) {
  if (-not (Test-Path $file)) {
    Copy-Item "${file}.example" $file
  }
}
```

The root `.env` supplies Compose interpolation values. Keep `AUTH_JWT_SECRET`
and `GATEWAY_JWT_SECRET` equal; the local template value is only for
development. AuthService and Gateway must use the same secret, containing at
least 32 UTF-8 bytes.

Start the local infrastructure for services launched from an IDE:

```powershell
docker compose -f docker-compose.yaml -f docker-compose.local.yaml up -d mongodb redis mysql kafka
```

The local override publishes infrastructure ports only on `127.0.0.1`.
Run the seven Spring services from their module directories in the IDE.

To run the entire application in containers instead:

```powershell
docker compose up --build -d
```

Only Gateway is published on port `8081` by the default Compose file. Service
containers use Docker DNS names, while IntelliJ/local runs use the `localhost`
URLs in their module `.env` files.

## Environment variables

The service `.env.example` files list each service's local settings, including
ports, database/cache addresses, Kafka bootstrap servers, Feign service URLs,
and AuthService token settings. Compose maps the same settings to container
service names. `RIDE_JPA_DDL_AUTO` defaults to `update`; avoid destructive
schema modes for a deployed database.

The root Compose configuration requires:

- `FLASHCAB_MYSQL_ROOT_PASSWORD` for MySQL initialization.
- `FLASHCAB_MYSQL_APP_PASSWORD` for the limited `flashcab_ride` database user.
- `AUTH_JWT_SECRET` and `GATEWAY_JWT_SECRET`, which must match and be at least
  32 UTF-8 bytes.

On a new MySQL data volume, Compose creates the limited `flashcab_ride` user.
An existing volume initialized before this change will not create that user
automatically; provision it for the `flashcab_rides` database rather than
deleting a volume that may contain data.

For production, provide these as VM/process environment variables or through a
secret manager; do not use the example values. The per-service Compose
`env_file` entries are optional, so a deployment does not need service `.env`
files. Compose also accepts environment-specific URLs such as
`RIDER_MONGODB_URI`, `DRIVER_MONGODB_URI`, `AUTH_MONGODB_URI`,
`RIDE_MYSQL_URL`, `LOCATION_REDIS_HOST`, and the `*_KAFKA_BOOTSTRAP_SERVERS`
variables from the VM environment. Keep database credentials out of source
control, use TLS/authentication for managed services as required, and restrict
the VM firewall to the Gateway port.

Compose's optional `env_file` syntax requires Docker Compose v2.24.0 or newer.

## Authentication and access

- `POST /auth/signup` creates a Rider or Driver account and provisions the
  corresponding profile. Driver signup also requires vehicle type and number.
- `POST /auth/login` returns a signed Bearer JWT.
- Pass the token as `Authorization: Bearer <token>` to protected Gateway APIs.
- Profile endpoints and ride/location operations enforce the user's role and
  ownership. Client-supplied identity headers are discarded by Gateway; trusted
  identity headers are derived from the validated JWT.
- Profile provisioning and service-to-service endpoints under `/internal/**`
  are not Gateway routes. Keep service ports private; publish only Gateway.
