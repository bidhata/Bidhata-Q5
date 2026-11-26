# Build environment for Bidhata-Q5 (LineageOS cm-13.0 = Android 6.0.1).
# cm-13.0 wants a 2015-era host: Ubuntu 14.04 and OpenJDK 7. This image is exactly that,
# so nobody has to downgrade their own machine. Used by tools/build.sh.
FROM ubuntu:14.04
ENV DEBIAN_FRONTEND=noninteractive LC_ALL=C USER=builder
RUN apt-get update && apt-get install -y --no-install-recommends \
      openjdk-7-jdk git-core gnupg flex bison gperf build-essential zip unzip curl \
      zlib1g-dev gcc-multilib g++-multilib libc6-dev-i386 lib32ncurses5-dev lib32z-dev \
      libx11-dev libgl1-mesa-dev libxml2-utils xsltproc python bc lzop pngcrush \
      schedtool rsync ccache imagemagick ca-certificates && \
    rm -rf /var/lib/apt/lists/*
# Jack (the cm-13 Java compiler) needs a real user and a big heap on 8 GB+ hosts
RUN useradd -m -u 1000 builder
ENV JACK_SERVER_VM_ARGUMENTS="-Dfile.encoding=UTF-8 -XX:+TieredCompilation -Xmx6g" \
    USE_CCACHE=1 CCACHE_DIR=/src/.ccache
USER builder
WORKDIR /src
