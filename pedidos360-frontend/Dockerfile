FROM nginx:alpine

RUN apk add --no-cache openssl \
  && mkdir -p /etc/nginx/ssl \
  && openssl req -x509 -nodes -days 825 -newkey rsa:2048 \
     -keyout /etc/nginx/ssl/privkey.pem \
     -out /etc/nginx/ssl/fullchain.pem \
     -subj "/CN=pedidos360"

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist/pedidos360-frontend/browser /usr/share/nginx/html

EXPOSE 80 443
