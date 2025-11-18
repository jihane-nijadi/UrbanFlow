# Docker Setup Guide for UrbanFlow

This guide explains how to set up and run PostgreSQL and RabbitMQ using Docker for the UrbanFlow microservices.

## Prerequisites

- Docker Desktop installed on your system
- Docker Compose (comes with Docker Desktop)

## Services Overview

The `docker-compose.yml` file sets up three services:

1. **route-db** - PostgreSQL database for Route Service (port 5432)
2. **ticket-db** - PostgreSQL database for Ticket Service (port 5433)
3. **rabbitmq** - RabbitMQ message broker (ports 5672 for AMQP, 15672 for Management UI)

## Quick Start

### 1. Start All Services

```powershell
# Navigate to the project root
cd C:\Users\dohan\OneDrive\Documents\Nassardevops\UrbanFlow

# Start all services
docker-compose up -d
```

The `-d` flag runs containers in detached mode (background).

### 2. Verify Services are Running

```powershell
# Check container status
docker-compose ps

# View logs
docker-compose logs
```

You should see:
- ✅ route-db running on port 5432
- ✅ ticket-db running on port 5433
- ✅ rabbitmq running on ports 5672 and 15672

### 3. Start Your Microservices

**Route Service:**
```powershell
cd backend-services\route-service
.\mvnw.cmd spring-boot:run
```

**Ticket Service:**
```powershell
cd backend-services\ticketservice\ticket-service
.\mvnw.cmd spring-boot:run
```

## Database Connection Details

### Route Service Database
- **Host:** localhost
- **Port:** 5432
- **Database:** route_db
- **Username:** postgres
- **Password:** Halima1975

### Ticket Service Database
- **Host:** localhost
- **Port:** 5433
- **Database:** ticket_db
- **Username:** postgres
- **Password:** Halima1975

### RabbitMQ
- **AMQP Port:** 5672
- **Management UI:** http://localhost:15672
- **Username:** guest
- **Password:** guest

## Common Commands

### Stop Services
```powershell
docker-compose stop
```

### Stop and Remove Containers
```powershell
docker-compose down
```

### Stop and Remove Everything (including volumes/data)
```powershell
docker-compose down -v
```

### View Logs
```powershell
# All services
docker-compose logs

# Specific service
docker-compose logs route-db
docker-compose logs ticket-db
docker-compose logs rabbitmq

# Follow logs (real-time)
docker-compose logs -f
```

### Restart a Service
```powershell
docker-compose restart route-db
docker-compose restart ticket-db
docker-compose restart rabbitmq
```

### Access PostgreSQL Database

**Route Database:**
```powershell
docker exec -it route-db psql -U postgres -d route_db
```

**Ticket Database:**
```powershell
docker exec -it ticket-db psql -U postgres -d ticket_db
```

**Common PostgreSQL Commands:**
```sql
-- List all tables
\dt

-- Describe a table
\d table_name

-- List all databases
\l

-- Exit
\q
```

## RabbitMQ Management UI

Access the RabbitMQ management interface at:
- **URL:** http://localhost:15672
- **Username:** guest
- **Password:** guest

Here you can:
- Monitor queues and exchanges
- View message rates
- Manage connections
- Check queue bindings

## Troubleshooting

### Port Already in Use

If you get a port conflict error:

```powershell
# Check what's using the port
netstat -ano | findstr :5432
netstat -ano | findstr :5433
netstat -ano | findstr :5672

# Stop the process or change the port in docker-compose.yml
```

### Database Connection Failed

1. Verify Docker containers are running:
   ```powershell
   docker-compose ps
   ```

2. Check container logs:
   ```powershell
   docker-compose logs route-db
   docker-compose logs ticket-db
   ```

3. Verify health check:
   ```powershell
   docker inspect route-db | findstr -i health
   ```

### Reset Database

To start fresh with empty databases:

```powershell
# Stop and remove everything
docker-compose down -v

# Start again
docker-compose up -d
```

## Data Persistence

Data is persisted in Docker volumes:
- `route-db-data` - Route service database data
- `ticket-db-data` - Ticket service database data
- `rabbitmq-data` - RabbitMQ data

These volumes persist even when containers are stopped, ensuring your data is not lost.

## Network

All services are connected via the `urbanflow-network` bridge network, allowing them to communicate with each other using container names as hostnames.

## Health Checks

All services have health checks configured:
- PostgreSQL: Checks if database is ready to accept connections
- RabbitMQ: Runs diagnostics ping

You can check health status:
```powershell
docker-compose ps
```

Healthy services will show `(healthy)` in the status column.
