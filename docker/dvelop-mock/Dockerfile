# Custom WireMock image with canned d.velop responses for local development
# (identityprovider-api, dvelop-dmsapp, dashboard-app-api). Never used in
# production/cluster profiles; opt-in via the "mock" Compose profile.
FROM wiremock/wiremock:3.13.1@sha256:d61e7720f89483fdef5366843b58d1dfd06bcce5828179c9f2f54de5c28354b0

COPY mappings /home/wiremock/mappings
COPY __files /home/wiremock/__files

EXPOSE 8080
