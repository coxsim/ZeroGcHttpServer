Zero-GC HTTP Server
===

Basic implementation of an HTTP server with the intention of allocating little to no memory at runtime.

Building
---

See: https://docs.docker.com/docker-for-mac/multi-arch/

    $ docker buildx build --platform linux/amd64 -t coxsim/zero-gc-http-server:latest --push .
    $ docker run -p 8081:8080 --rm docker.io/coxsim/zero-gc-http-server:latest /ZeroGcHttpServer/bin/ZeroGcHttpServer

