FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# Variante oficial não-root do Nginx (roda como usuário nginx, uid 101, e escuta 8080 —
# porta <1024 exigiria root). Fecha a parte frontend do gap S9 de docs/criterios-aceite.md.
FROM nginxinc/nginx-unprivileged:1.31-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080
