FROM node:22-alpine

ENV NODE_ENV=production \
    CHROME_BIN=/usr/bin/chromium-browser \
    PUPPETEER_SKIP_DOWNLOAD=true \
    PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true

RUN apk add --no-cache chromium ca-certificates ttf-freefont font-noto-emoji

WORKDIR /usr/src/app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev --ignore-scripts && npm cache clean --force

COPY --chown=node:node server.js swagger.json ./
COPY --chown=node:node src ./src
RUN mkdir -p sessions uploads .wwebjs_cache \
    && chown -R node:node /usr/src/app

USER node
EXPOSE 3000
CMD ["node", "server.js"]
