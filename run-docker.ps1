$ErrorActionPreference = "Stop"

$imageName = "pi-calculator-app"
$containerName = "pi-calculator-container"
$url = "http://localhost:8181/"

docker build -t $imageName .

$existingContainerId = docker ps -aq -f "name=^${containerName}$"
if ($existingContainerId) {
    docker rm -f $containerName | Out-Null
}

docker run -d --name $containerName -p 8181:8181 $imageName | Out-Null

for ($i = 0; $i -lt 30; $i++) {
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 2
        if ($response.StatusCode -eq 200) {
            Start-Process $url
            Write-Output "Application is available at $url"
            exit 0
        }
    } catch {
        Start-Sleep -Seconds 1
    }
}

Write-Error "Container started, but the application did not become ready at $url in time."
