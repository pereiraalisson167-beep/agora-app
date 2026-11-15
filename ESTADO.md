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

## Pendências

- Rodar o primeiro build e a primeira rodada no emulador.
- Publicar a versão 1.0.0 e instalar no celular (moto g35 5g) — nada foi testado em
  aparelho real ainda.
- Decidir depois, se fizer falta: backup por arquivo, lista de tarefas para editar/excluir,
  fotos antes/depois.
