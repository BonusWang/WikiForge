FROM node:22-alpine AS build
WORKDIR /workspace
COPY orchestration-ui/package*.json ./
RUN npm ci
COPY orchestration-ui ./
RUN npm run build

FROM nginx:1.27-alpine
COPY orchestration-ui/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html
EXPOSE 80
