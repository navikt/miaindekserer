ARG BASE_IMAGE_PREFIX=""

FROM ${BASE_IMAGE_PREFIX}maven as builder
ADD . /source
WORKDIR /source
RUN mvn package -DskipTests

FROM navikt/java:8
COPY --from=builder /source/target/miaindekserer /app
ENV MAIN_CLASS no.nav.fo.miaindekserer.MainKt
