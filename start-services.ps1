$services = @(
    "usage-service",
    "percentage-service",
    "energy-producer",
    "energy-user",
    "rest-api"
)

Write-Host "Starting Spring Boot Microservices..."

foreach ($service in $services) {
    Write-Host "Starting $service in a new window..."
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd $service; .\mvnw.cmd spring-boot:run"
}

Write-Host "All Spring Boot services have been started!"
Write-Host "To start the GUI, run: cd energy-gui; mvn compile javafx:run"

