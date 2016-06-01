param (
    $HortaArtifact,
    $VolumePath,
    $docker = 'docker'
)

Copy-Item $HortaArtifact .\horta-hell.jar -Force
& $docker build -t=codingteam/horta-hell .
if (-not $?) {
  exit 1
}

& $docker stop horta-hell
& $docker rm horta-hell
& $docker run -d --name horta-hell -v ${VolumePath}:/data codingteam/horta-hell
