# Agora

App Android para decidir **o que fazer agora**, quando escolher é a parte difícil.

Você diz como está a sua energia e quanto tempo tem. O app mostra **uma tarefa só** —
não uma lista. Na hora de fazer, não há cronômetro com números: há uma ampulheta, e a
areia desce no tempo da tarefa.

## Baixar

<https://github.com/pereiraalisson167-beep/agora-app/releases/latest> → arquivo `agora.apk`.

Instala por cima da versão anterior e mantém as tarefas.

## As telas

| Tela | O que faz |
|---|---|
| **Agora** | Energia (Zumbi / Moderado / Foco) + tempo (5 / 15 / 45 min+) → uma tarefa, com o porquê dela. Duas trocas, e depois é essa mesmo. |
| **Fazendo** | A ampulheta. "Concluí", "Parar" e, quando o tempo acaba, "Mais 5 min" — que vira a ampulheta e começa um ciclo novo. |
| **Feito** | A tarefa riscada e quantas saíram hoje. |
| **Anotar tarefa** | Título, tempo e energia. Depois pergunta se tem data; se tiver, pede dia, mês, ano, horário e uma observação, e marca o lembrete. |

Quando nada cabe no momento, o app oferece uma pausa de 2 minutos em vez de insistir.

## Como é feito

- Kotlin + Jetpack Compose (Material 3), Room para guardar as tarefas, minSdk 26.
- As tarefas ficam **só no celular**. Não há backup nem nuvem: desinstalar apaga tudo.
- Os lembretes usam alarme do sistema e voltam sozinhos depois que o celular reinicia.
- A ampulheta é desenhada pixel por pixel em Kotlin (`ui/hourglass/`), portada do projeto
  original: madeira, vidro, latão e areia, com o nível calculado por volume de verdade.
  As peças fixas são desenhadas uma vez por tamanho de tela e ficam guardadas; a cada
  quadro mudam só a areia, o fio que cai e os grãos soltos.

## Compilar

Não há SDK Android nesta máquina: **tudo roda no GitHub Actions**.

- Enviar para `dev` → roda o teste no emulador ("Conferir telas").
- Enviar para `main` → compila, testa e publica o APK em Releases.

As imagens das telas de cada rodada ficam no ramo `ui-screens`.
