FROM golang:1.26.6-bookworm AS build
WORKDIR /source
COPY deploy/proxy/ ./
RUN CGO_ENABLED=0 go build -mod=readonly -trimpath -o /caddy .

FROM caddy:2.11.4
RUN apk upgrade --no-cache
COPY --from=build /caddy /usr/bin/caddy
