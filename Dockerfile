FROM openjdk:13-alpine

# copy the packaged jar file into our docker image
COPY ./build/install/ ./

# set the startup command to execute the jar
CMD ["/ZeroGcHttpServer/bin/ZeroGcHttpServer"]