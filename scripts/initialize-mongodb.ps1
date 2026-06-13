[CmdletBinding()]
param(
    [string]$MongoUri = $(if ($env:SPRING_DATA_MONGODB_URI) {
        $env:SPRING_DATA_MONGODB_URI
    } else {
        "mongodb://localhost:27017"
    }),
    [string]$Database = $(if ($env:SPRING_DATA_MONGODB_DATABASE) {
        $env:SPRING_DATA_MONGODB_DATABASE
    } else {
        "audit_platform"
    })
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command mongosh -ErrorAction SilentlyContinue)) {
    throw "mongosh was not found. Install MongoDB Shell and ensure it is available on PATH."
}

$scriptPath = Join-Path $PSScriptRoot "initialize-mongodb.js"
if (-not (Test-Path -LiteralPath $scriptPath)) {
    throw "MongoDB initialization script was not found at $scriptPath"
}

$previousDatabase = $env:MONGODB_DATABASE
try {
    $env:MONGODB_DATABASE = $Database
    Write-Host "Initializing MongoDB database '$Database'..."
    & mongosh $MongoUri --quiet --file $scriptPath
    if ($LASTEXITCODE -ne 0) {
        throw "MongoDB initialization failed with exit code $LASTEXITCODE."
    }
} finally {
    $env:MONGODB_DATABASE = $previousDatabase
}
