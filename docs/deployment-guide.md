# Deployment Guide — AWS EC2

This guide deploys the full stack (backend, PostgreSQL, Redis, Kafka,
Nginx) to a single EC2 instance using Docker Compose. It is the
appropriate starting point for a small-to-medium deployment; splitting
each service onto managed infrastructure (RDS, ElastiCache, MSK) is a
natural next step once traffic justifies it; a short note on that
migration path is at the end of this guide.

## 1. Provision the instance

- **AMI**: Ubuntu Server 24.04 LTS
- **Instance type**: `t3.large` (2 vCPU / 8 GB) is a reasonable floor for
  running Postgres, Redis, Kafka+Zookeeper and the backend JVM on one
  host; size up if you skip step 6 (managed services) later.
- **Storage**: 30 GB gp3 minimum — Kafka's log segments and Postgres data
  both live on this instance's disk in the single-instance topology.
- **Security group**:
  - `22/tcp` (SSH) — restrict to your IP or a bastion, never `0.0.0.0/0`
  - `80/tcp` and `443/tcp` (HTTP/HTTPS) — open to the internet, this is
    Nginx
  - `5432` (Postgres), `6379` (Redis), `9092` (Kafka), `8090` (Kafka UI),
    `9090` (Prometheus), `3001` (Grafana) — `docker-compose.yml` does
    publish these to the Docker host for local development convenience
    (connecting a database client, checking Kafka UI, viewing Grafana),
    but on an actual EC2 deployment these ports should **not** be opened
    in the security group. Restricting them at the security-group level
    is what actually enforces "only Nginx is reachable from the
    internet" in production — the compose file's port mappings alone
    don't provide that isolation, since they bind to the host's network
    interface regardless of firewall rules layered on top.

## 2. Install Docker

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

sudo usermod -aG docker $USER
newgrp docker
```

## 3. Deploy the application

```bash
git clone <your-repository-url> backend-control-plane
cd backend-control-plane

cp .env.example .env
nano .env   # fill in real DB password, JWT secret, SMTP credentials, etc.

docker compose up -d --build
docker compose ps      # confirm every service is healthy
docker compose logs -f backend   # tail startup logs; Flyway runs automatically
```

The backend is now reachable through Nginx on port 80. Confirm with:

```bash
curl http://<ec2-public-ip>/actuator/health
```

## 4. Put a domain and TLS certificate in front of it

Once you have a domain pointed at the instance's Elastic IP, obtain a
certificate with Certbot and terminate TLS at Nginx:

```bash
sudo apt-get install -y certbot python3-certbot-nginx
```

Since this project's `nginx.conf` runs inside the `nginx` container
rather than directly on the host, the simplest approach is to run Certbot
against a host-level Nginx or Caddy instance that reverse-proxies to the
Dockerized Nginx on an internal port, or to switch the `nginx` service to
mount a certificate volume and add a `listen 443 ssl;` server block once
you've obtained certificates via the DNS-01 or webroot challenge. Either
way, only the change to `nginx/nginx.conf` and the `nginx` service's port
mapping in `docker-compose.yml` is required — no backend changes.

## 5. Keep it running

- **Restart policy**: every service in `docker-compose.yml` already sets
  `restart: unless-stopped`, so a reboot of the instance brings the whole
  stack back up automatically.
- **Updates**: `git pull && docker compose up -d --build` rebuilds only
  the `backend` image (Postgres/Redis/Kafka images are pulled, not
  built) and performs a rolling restart of that one container.
- **Backups**: snapshot the `cp-postgres-data` and `cp-attachments`
  named volumes on a schedule (`docker run --rm -v cp-postgres-data:/data
  -v $(pwd):/backup alpine tar czf /backup/postgres-backup.tar.gz /data`
  is a simple cron-friendly approach).
- **Log rotation**: Docker's default `json-file` log driver grows
  unbounded; add `logging: driver: json-file, options: {max-size: "10m",
  max-file: "3"}` to each service if disk space is a concern on a small
  instance.

## 6. Migrating to managed services (later)

When a single EC2 instance stops being enough:

- **RDS for PostgreSQL** replaces the `postgres` service — point
  `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` at the RDS endpoint, nothing else
  changes since the application only talks to Postgres through Spring
  Data JPA.
- **ElastiCache for Redis** replaces the `redis` service the same way,
  via `REDIS_HOST`/`REDIS_PORT`.
- **MSK (Managed Streaming for Kafka)** replaces `kafka`/`zookeeper` via
  `KAFKA_BOOTSTRAP_SERVERS`.
- The `backend` container itself moves onto **ECS Fargate** or an **EC2
  Auto Scaling Group behind an Application Load Balancer**, at which
  point the `nginx` container's job (routing, rate limiting) is taken
  over by the ALB and an AWS WAF, and `nginx/nginx.conf` retires.

None of this requires application code changes — every external
dependency is already configured purely through environment variables
(see `.env.example`), which is exactly what makes this migration
mechanical rather than a rewrite.
