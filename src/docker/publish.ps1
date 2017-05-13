<#
.SYNOPSIS
    Rebuild the application, build Docker image and publish it to the Docker Hub.
.PARAMETER Version
    A version used for publishing of a Docker image.
.PARAMETER DockerId
    Docker Hub username.
.PARAMETER SourceRoot
    The root directory of the source code.
.PARAMETER sbt
    A path to the sbt executable.
.PARAMETER docker
    A path to the Docker executable.
#>
param (
    [Parameter(Mandatory=$true)]
    [string] $Version,
    [string] $DockerId = 'revenrof',

    [string] $SourceRoot = "$PSScriptRoot/../../",

    [string] $sbt = 'sbt',
    [string] $docker = 'docker'
)

$ErrorActionPreference = 'Stop'

function inPath($path, $action) {
    try {
        Set-Location $path
        & $action
    } finally {
        Pop-Location
    }
}

function exec($command) {
    Write-Host "[exec] $command $args" -ForegroundColor White
    & $command $args
    if (!$?) {
        throw "[error] $command $args = $LASTEXITCODE"
    }
}

inPath $SourceRoot {
    exec $sbt clean assembly
}

Copy-Item -Force $PSScriptRoot/../../target/scala-2.11/horta-hell-assembly.jar $PSScriptRoot/horta-hell.jar
exec $docker build -t=codingteam/horta-hell $PSScriptRoot

$tag = "$DockerId/horta-hell:$Version"
exec $docker login
exec $docker tag codingteam/horta-hell $tag
exec $docker push $tag
