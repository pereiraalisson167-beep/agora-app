#!/usr/bin/env bash
# Publica o APK gerado como um Release do GitHub (arquivo agora.apk).
set -euo pipefail

APK="$(ls app/build/outputs/apk/release/*.apk | head -n 1)"
VERSION="$(grep -oE 'versionName = "[^"]+"' app/build.gradle.kts | head -n 1 | cut -d'"' -f2)"
BUILD="${GITHUB_RUN_NUMBER}"
cp "$APK" agora.apk
ls -la agora.apk

if [ "${SIGNED:-false}" = "true" ]; then
  gh release create "v${VERSION}-${BUILD}" agora.apk \
    --target "${GITHUB_SHA}" \
    --title "Agora ${VERSION} (versão ${BUILD})" \
    --notes "Baixe o arquivo **agora.apk** abaixo e abra no celular para instalar ou atualizar. Suas tarefas são mantidas na atualização." \
    --latest
else
  gh release create "teste-${BUILD}" agora.apk \
    --target "${GITHUB_SHA}" \
    --title "TESTE ${VERSION} (${BUILD}) - assinatura temporária" \
    --notes "Versão apenas para teste: não aceita atualização por cima. Cadastre o segredo KEYSTORE_PASSWORD no repositório para gerar a versão definitiva." \
    --prerelease
fi
