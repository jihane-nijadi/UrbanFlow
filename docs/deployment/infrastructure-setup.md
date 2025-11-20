## Production Infrastructure Setup

This guide captures the second deployment step: picking and preparing the runtime environment that will host the UrbanFlow Docker stack. It assumes you have already parameterized the services (see `env.template`) and now need a secure server to run `docker compose up -d`.

### 1. Choose the target environment

The stack currently runs well on a single Docker host. Pick one of:

- **Managed VM (recommended to start)** – e.g. AWS EC2 t3.large / Azure B2ms / GCP e2-standard-2 with Ubuntu 22.04 LTS, 2 vCPU, 8 GB RAM, 50 GB SSD.
- **Docker-ready PaaS** – AWS Lightsail, DigitalOcean Droplet, etc.
- **Kubernetes** – defer until you need autoscaling/high availability; manifests can be generated later.

Decide on:

| Item | Minimum recommendation |
| --- | --- |
| OS | Ubuntu 22.04 LTS |
| CPU/RAM | 2 vCPU / 8 GB |
| Disk | 50 GB SSD (expand if you keep long-lived DB volumes) |
| Network | Static public IP + DNS record (`app.yourdomain.com`) |
| TLS | Let’s Encrypt via reverse proxy (nginx/caddy/traefik) |

### 2. Provision and harden the host

1. **Create the VM** in your cloud provider, attach a security group/firewall that allows:
   - SSH (22) from your admin IP
   - HTTP/HTTPS (80/443) for the frontend/API once TLS is configured
   - Optional admin ports (RabbitMQ 15672, pgAdmin) – prefer to keep these closed externally.
2. **Initialize the OS**:
   ```bash
   sudo apt update && sudo apt upgrade -y
   sudo adduser urbanflow && sudo usermod -aG sudo urbanflow
   ```
   Log in as `urbanflow` via SSH.
3. **Install Docker + Compose plugin**:
   ```bash
   curl -fsSL https://get.docker.com | sh
   sudo usermod -aG docker urbanflow
   docker --version
   docker compose version
   ```
4. **Enable a basic firewall** (UFW example):
   ```bash
   sudo ufw default deny incoming
   sudo ufw default allow outgoing
   sudo ufw allow OpenSSH
   sudo ufw allow 80/tcp
   sudo ufw allow 443/tcp
   sudo ufw enable
   ```
5. **Install support tooling** – `git`, `curl`, `fail2ban`, `unattended-upgrades`, monitoring agents as needed.

### 3. Prepare networking & certificates

1. Point your DNS (e.g. `app.example.com`) to the VM public IP.
2. Pick a TLS termination strategy:
   - Use a reverse proxy container (nginx, Caddy, Traefik) within `docker-compose.yml`.
   - Or install nginx on the host and forward to the Compose stack.
3. Issue certificates via Let’s Encrypt:
   ```bash
   sudo snap install --classic certbot
   sudo certbot certonly --standalone -d app.example.com
   ```
   (If you use an in-stack reverse proxy, mount `/etc/letsencrypt` into the container and automate renewals.)

### 4. Manage secrets and configuration

1. Copy `env.template` from the repo to the server and rename to `.env`:
   ```bash
   scp env.template urbanflow@vm-ip:~/UrbanFlow/.env
   ```
2. Edit `.env` on the server with real passwords, JWT secrets, and public URLs.
3. If you have a secrets manager (AWS Secrets Manager, HashiCorp Vault), store the values there and render them into `.env` during deployment.

### 5. Validate the host before deploying

Run these commands to ensure the server is ready:

```bash
docker info
docker run --rm hello-world
df -h
free -m
```

Document any custom firewall/NAT rules or corporate policies in this file so other engineers can reproduce the environment.

---

**Next step:** once the host is ready, copy the repository (or CI pipeline artifacts) onto it, log in, and run `docker compose pull && docker compose up -d` using the `.env` you just configured. That will be Step 3 (publish & deploy images).


