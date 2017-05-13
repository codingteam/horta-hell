How to publish Docker image
===========================

Register at the [Docker Hub][docker-hub].

Run [`publish.ps1`][publish-script] script passing it a package version:

```console
$ src/docker/publish.ps1 0.17.4
```

If you want to change the script parameters, consult its' help section:

```console
$ Get-Help src/docker/publish.ps1
```

[publish-script]: ../src/docker/publish.ps1

[docker-hub]: https://hub.docker.com
