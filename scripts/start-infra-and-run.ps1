param(
    [switch]$StartServices
)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

Write-Host "Starting infra containers (rabbitmq, ticket-db, route-db, route-service)..."
docker-compose up -d rabbitmq ticket-db route-db route-service

$containers = @(
    @{ name='urbanflow-ticket-db'; service='ticket-db' },
    @{ name='urbanflow-route-db'; service='route-db' },
    @{ name='urbanflow-rabbitmq'; service='rabbitmq' },
    @{ name='urbanflow-route-service'; service='route-service' }
)

foreach ($c in $containers) {
    Write-Host "Waiting for $($c.name) to be healthy..."
    for ($i=0; $i -lt 60; $i++) {
        $status = docker inspect --format='{{json .State.Health.Status}}' $c.name 2>$null
        if ($status -and $status -match '"healthy"') {
            Write-Host "$($c.name) is healthy."
            break
        }
        Start-Sleep -Seconds 2
    }
    if ($i -ge 60) {
        Write-Warning "$($c.name) did not become healthy in time. Check logs with: docker-compose logs $($c.service)"
    }
}

if ($StartServices) {
    Write-Host "Building and starting services (ticket-service, payment-service)..."
    docker-compose build ticket-service payment-service
    docker-compose up -d ticket-service payment-service
    Write-Host "Waiting briefly for services to start..."
    Start-Sleep -Seconds 8
    docker-compose ps
}

Write-Host "Done. Use 'docker-compose logs -f <service>' to follow logs. Example: docker-compose logs -f ticket-service"
