$logDirectory = "logs"
$logFile = "$logDirectory/test.log"

New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null

if (Test-Path $logFile) {
    Remove-Item $logFile -Force
}

mvn test 2>&1 | Tee-Object -FilePath $logFile