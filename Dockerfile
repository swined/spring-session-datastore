FROM maven AS build

WORKDIR /build
COPY . .
RUN mvn source:jar deploy -DaltDeploymentRepository=internal.repo::default::file:///repo

FROM nginx

COPY --from=build /repo /static/
COPY nginx.conf /etc/nginx/conf.d/default.conf
RUN nginx -t