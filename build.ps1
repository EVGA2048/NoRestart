param(
    [Parameter(Mandatory = $true)]
    [string]$ServerJar
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$classes = Join-Path $root 'build\classes'

if (-not (Test-Path -LiteralPath $ServerJar)) {
    throw "Server jar not found: $ServerJar"
}

Remove-Item -Recurse -Force (Join-Path $root 'build') -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $classes | Out-Null

$sources = @(Get-ChildItem -Recurse (Join-Path $root 'src\main\java') -Filter *.java | ForEach-Object FullName)
if ($sources.Count -eq 0) {
    throw 'No Java sources found.'
}

& javac -encoding UTF-8 -source 21 -target 21 -cp $ServerJar -d $classes $sources
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

Copy-Item (Join-Path $root 'src\main\resources\plugin.yml') $classes

$jar = Join-Path $root 'build\NoRestart-1.0.0.jar'
& jar --create --file $jar -C $classes .
if ($LASTEXITCODE -ne 0) {
    throw "jar failed with exit code $LASTEXITCODE"
}

Write-Host "Built $jar"
