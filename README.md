# Shopping Mall Management System

A server-rendered mall back office and storefront. Java 21, Maven 3.10.0, Spring Boot 3.5.16, and MySQL 8.4.13. Money is stored as `DECIMAL` and shown in INR. Passwords are BCrypt. Payments are simulated: the pay form says "Simulated payment. Nothing is charged. This form does not ask for a card number."

This repository also contains an existing Python social-automation project. The mall application lives at the repository root beside it and does not replace those files.

## Run

Install Java 21, Maven is provided by `./mvnw`, MySQL 8.4.13 (Docker Compose is included), and the `mysql` command-line client.

Export the four application settings. Do not commit them. `SM_DB_URL` has no default in the application properties.

```bash
export SM_DB_URL='jdbc:mysql://localhost:3306/shopping_mall?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
export SM_DB_USER='the database user'
export SM_DB_PASSWORD='the database password'
export SM_DB_ROOT_PASSWORD='the database root password'
export SM_UPLOAD_DIR="$PWD/mall-uploads"
mkdir -p "$SM_UPLOAD_DIR"
```

`SM_DB_ROOT_PASSWORD` is used only by Docker Compose. The application reads `SM_DB_URL`, `SM_DB_USER`, `SM_DB_PASSWORD`, and `SM_UPLOAD_DIR`.

Start MySQL:

```bash
docker compose up -d
```

Compose creates the `shopping_mall` database. `sql/schema.sql` uses `CREATE DATABASE IF NOT EXISTS` so the same file can run when the database is already there. The salary month column is `` `year_month` `` because `year_month` is not a valid unquoted identifier on MySQL 8.4.

Choose a dev password of 8 to 72 characters with a letter and a digit, export it as `SM_DEV_SEED_PASSWORD`, and print a BCrypt strength-10 hash:

```bash
export SM_DEV_SEED_PASSWORD='your chosen dev password'
./mvnw -q -DskipTests exec:java -Dexec.mainClass=com.shoppingmall.util.DevPasswordHash
```

The command prints one hash and nothing else. Export that hash, then load the schema and seed:

```bash
export SM_DEV_SEED_PASSWORD_HASH='the hash printed above'
sql/load.sh
```

`sql/seed.sql` stores only the token `__SEED_PASSWORD_HASH__`. `sql/load.sh` substitutes the hash when it matches `^\$2a\$10\$[./A-Za-z0-9]{53}$`. If the variable is missing or does not match, the script loads the schema and prints:

```text
Set SM_DEV_SEED_PASSWORD_HASH to a BCrypt strength 10 hash of the dev password.
```

The default profile is `dev` (session cookie not marked Secure, password-reset tokens written to the log). For production:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=prod
```

Otherwise:

```bash
./mvnw spring-boot:run
```

Open http://localhost:8080.

## Demo accounts

All eight accounts use the password you chose above. Nothing in this repository records that password or its hash.

| Email | Role | Status |
| --- | --- | --- |
| admin@mall.local | Admin | Active |
| owner.fashion@mall.local | Shop owner, Urban Thread | Active |
| owner.food@mall.local | Shop owner, Spice Court | Active |
| owner.circuit@mall.local | Shop owner, Circuit Hub | Active |
| owner.pending@mall.local | Shop owner, Pending Tailors | Waiting for approval |
| customer.riya@mall.local | Customer | Active, kurta in the cart |
| customer.arjun@mall.local | Customer | Active, cable on the wishlist |
| inactive.customer@mall.local | Customer | Inactive, cannot sign in |

Admin opens `/admin/dashboard`, an approved shop owner opens `/owner/dashboard`, and a customer opens `/customer/dashboard`.

## Tests

```bash
./mvnw -B test
```

Integration tests start MySQL 8.4.13 with Testcontainers and check `SELECT VERSION()`.

The schema, routes, and module rules are in [docs/architecture.md](docs/architecture.md). A Postman collection of the main pages is in `postman/shopping-mall.postman_collection.json`.
