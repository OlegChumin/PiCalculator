param(
    [switch]$FreePort,
    [switch]$Rebuild
)

$ErrorActionPreference = "Stop"

$imageName = "pi-calculator-app"
$containerName = "pi-calculator-container"
$url = "http://localhost:8181/"
$port = 8181

if ($FreePort) {
    $listeners = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue
    $owningProcesses = $listeners | Select-Object -ExpandProperty OwningProcess -Unique

    foreach ($owningProcess in $owningProcesses) {
        if ($owningProcess) {
            Stop-Process -Id $owningProcess -Force -ErrorAction SilentlyContinue
        }
    }
}

function Wait-ForApplication {
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
}

$existingImageId = docker images -q $imageName
if (-not $existingImageId -or $Rebuild) {
    docker build -t $imageName .
}

$existingContainerId = docker ps -aq -f "name=^${containerName}$"
$runningContainerId = docker ps -q -f "name=^${containerName}$"

if ($Rebuild -and $existingContainerId) {
    docker rm -f $containerName | Out-Null
    $existingContainerId = $null
    $runningContainerId = $null
}

if ($runningContainerId) {
    Wait-ForApplication
}

if ($existingContainerId) {
    docker start $containerName | Out-Null
    Wait-ForApplication
}

docker run -d --name $containerName -p ${port}:8181 $imageName | Out-Null
Wait-ForApplication
