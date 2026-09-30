FROM golang:1.26.6-bookworm AS build
RUN git clone --depth 1 --branch 1.19 https://github.com/tianon/gosu.git /source
WORKDIR /source
RUN CGO_ENABLED=0 go build -mod=readonly -trimpath -o /gosu .

FROM mysql:8.4
# mysql and mysqldump remain available; MySQL Shell and its bundled Python are unused.
RUN microdnf remove -y mysql-shell && microdnf upgrade -y && microdnf clean all
COPY --from=build /gosu /usr/local/bin/gosu
RUN gosu mysql id && mysql --version && mysqldump --version
