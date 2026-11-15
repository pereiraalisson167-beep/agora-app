# Estado do projeto

Primeiro arquivo a ler ao retomar o trabalho.

## O que é

App Android pessoal, feito a partir do protótipo da tela "Agora"
(artefato "Protótipo da tela principal — Agora", de 15/11/2026). Uso só do dono.

## Decisões tomadas (15/11/2026)

| Assunto | Decisão |
|---|---|
| Escopo da versão 1 | Núcleo + lembretes das tarefas com data |
| Ampulheta | Portada para Kotlin nativo, não WebView |
| Dados | Só no celular, **sem backup** (escolha do dono, avisado do risco) |
| Nome / pacote | Agora / `br.com.agora` |
| Entrega | APK nos Releases do GitHub |

## Valores padrão usados (não foram perguntados)

- **Fotos de tarefa ficaram fora da versão 1.** O protótipo tinha foto na tarefa e foto
  "antes/depois" ao concluir. Exigem câmera e FileProvider, e a câmera foi a maior fonte
  de erro no Acalmar. Dá para acrescentar depois sem mexer no resto.
- **Limite de trocas = 2**, como no protótipo (`LIMITE_DE_TROCAS` em `ui/AgoraViewModel.kt`).
- **Fonte do sistema** no lugar de Inter + Space Grotesk, para não embutir arquivos de fonte.
- **Tarefas de exemplo não existem**: o app começa vazio, com o convite para anotar.

## Como está organizado

```
domain/      regras puras, com teste JUnit (escolha, trocas, datas)
data/        Room: tarefas e conclusões do dia
notify/      lembretes por alarme + remarcar depois de reiniciar
ui/          telas em Compose
ui/hourglass/ a ampulheta portada (geometria, camadas, areia, view)
```

A regra da escolha está em `domain/Escolha.kt` e é a mesma do protótipo: filtra por tempo
e energia, tira as tarefas cuja hora ainda não chegou, e ordena por hora marcada →
energia que combina → mais tempo esperando.

## Conferido antes do primeiro build

- A geometria portada foi comparada número a número com o perfil do protótipo: igual até
  a quinta casa (`GeometriaTest.kt` guarda esses valores).
- As camadas foram desenhadas fora do Android e a imagem conferida a olho: madeira, vidro,
  latão e os níveis de areia saíram iguais aos do original.

## Versões publicadas

| Release | Data | O que mudou |
|---|---|---|
| v1.0.0-2 | 15/11/2026 | Primeiro build que compilou. Não foi entregue. |
| **v1.0.0-3** | 15/11/2026 | **Primeira entregue.** Ajustes da revisão das telas. |

Conferido na v1.0.0-3 com androguard: pacote `br.com.agora`, certificado SHA-256
`f310be3f25f0b676…1604f079` (o mesmo da v2) e versionCode 3 > 2 — a atualização
instala por cima.

## Teste no emulador (API 34, pixel_6)

Rodada verde: 24 conferências, nenhuma queda, nenhum aviso do app. O roteiro
(`ci/ui/run.py`) instala a versão publicada, anota duas tarefas e uma com data,
instala a nova por cima e confere que tudo continua; depois percorre escolher,
trocar até acabar, fazer com a ampulheta, parar, concluir (singular e plural),
"nada cabe", pausa de 2 minutos esperando o tempo acabar de verdade e "Mais 5 min"
com a virada fotografada no meio.

Problemas achados na revisão das imagens e corrigidos: botões quebrando em duas
linhas, subtítulo com palavra sozinha, aviso por cima do título, "nada cabe" com
botões espremidos, e o contador "Hoje" que não zerava à meia-noite com o app aberto.

## Instalação fora da loja (situação em 15/11/2026)

Desde 30/09/2026 o Google exige, no Brasil, desenvolvedor verificado para apps
distribuídos pelas lojas participantes (Google Play, Galaxy Store etc.). APK baixado
direto do GitHub ainda instala pelo caminho normal. Se o celular recusar, há o
"fluxo avançado" do Android (modo desenvolvedor, reiniciar, esperar 1 dia,
confirmar). A expansão para todos os apps está prevista para 2027 — aí a conta
gratuita de distribuição limitada (até 20 aparelhos) resolve.

## Pendências

- Instalar no celular (moto g35 5g) e testar de verdade — nada foi testado em
  aparelho real ainda. Pontos a observar: tempo do "Preparando a ampulheta…" na
  primeira tarefa, fluidez da areia, se o lembrete chega na hora marcada.
- Decidir depois, se fizer falta: backup por arquivo, lista de tarefas para
  editar/excluir, fotos antes/depois.
