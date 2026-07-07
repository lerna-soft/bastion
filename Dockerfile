FROM ubuntu:22.04

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    unzip \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/opt/jdk17
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

WORKDIR /src

CMD ["./gradlew", "assembleDebug"]
