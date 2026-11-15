#!/usr/bin/env python3
"""Percorre as telas do app Agora no emulador, salvando uma imagem de cada uma.

Usado pelo workflow "Conferir telas". Uso:

    run.py <apk novo> [apk publicado]

Com o APK publicado, o roteiro começa nele (anota duas tarefas, uma com data marcada),
instala a versão nova por cima e confere que as tarefas continuam lá. Depois percorre
escolher, fazer com a ampulheta, parar, concluir, a pausa e o "Mais 5 min".

Nada aqui depende da hora do dia: a data marcada é sempre o dia seguinte ao do aparelho.
"""
import datetime
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

OUT = "ui-out"
PKG = "br.com.agora"
os.makedirs(OUT, exist_ok=True)
steps = open(os.path.join(OUT, "steps.txt"), "w")
failures = []
MAX_FAILURES = 8


class Abort(Exception):
    """Falhas demais: o roteiro se perdeu e não adianta continuar."""


def fail(description):
    failures.append(description)
    if len(failures) >= MAX_FAILURES:
        raise Abort()


def adb(*args, timeout=90):
    try:
        return subprocess.run(("adb",) + args, capture_output=True, timeout=timeout)
    except subprocess.TimeoutExpired:
        return subprocess.CompletedProcess(args, 1, b"", b"timeout")


def note(msg):
    print(msg, flush=True)
    steps.write(msg + "\n")
    steps.flush()


def nodes():
    for _ in range(6):
        adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
        raw = adb("exec-out", "cat", "/sdcard/ui.xml").stdout.decode("utf-8", "ignore")
        try:
            return list(ET.fromstring(raw).iter("node"))
        except ET.ParseError:
            time.sleep(1)
    return []


def bounds(node):
    return tuple(map(int, re.findall(r"\d+", node.get("bounds", "[0,0][0,0]"))))


def center(node):
    x1, y1, x2, y2 = bounds(node)
    return (x1 + x2) // 2, (y1 + y2) // 2


def label_of(node):
    return (node.get("text") or "") or (node.get("content-desc") or "")


def matches(node, text=None, regex=None):
    for value in (node.get("text") or "", node.get("content-desc") or ""):
        if not value:
            continue
        if (regex and re.fullmatch(regex, value, re.S)) or (text is not None and value == text):
            return True
    return False


def find(text=None, regex=None):
    return [n for n in nodes() if matches(n, text, regex)]


def screen_size():
    out = adb("shell", "wm", "size").stdout.decode()
    m = re.search(r"(\d+)x(\d+)", out)
    return (int(m.group(1)), int(m.group(2))) if m else (1080, 2400)


def scroll(up=True):
    """Rola o conteúdo da tela (up=True mostra o que está mais abaixo)."""
    w, h = screen_size()
    y1, y2 = (int(h * 0.68), int(h * 0.32)) if up else (int(h * 0.32), int(h * 0.68))
    adb("shell", "input", "swipe", str(w // 2), str(y1), str(w // 2), str(y2), "450")
    time.sleep(1.0)


def to_top():
    for _ in range(4):
        scroll(up=False)


def press_wait_dialog():
    """Aviso do sistema ("... isn't responding") por cima do app: manda esperar."""
    waiting = find("Wait")
    if waiting:
        x, y = center(waiting[0])
        adb("shell", "input", "tap", str(x), str(y))
        time.sleep(2)
        return True
    return False


def tap(text=None, regex=None, last=False, nth=None, wait=12, optional=False, scrolls=4):
    """Toca no elemento com o texto (ou descrição) indicado. Se não estiver à vista, rola a tela."""
    label = text or regex
    deadline = time.time() + wait
    scrolled = 0
    while True:
        found = find(text, regex)
        if found and (nth is None or nth < len(found)):
            node = found[nth] if nth is not None else (found[-1] if last else found[0])
            x, y = center(node)
            adb("shell", "input", "tap", str(x), str(y))
            note(f"toque: {label} ({x},{y})")
            time.sleep(1.5)
            return True
        if press_wait_dialog():
            continue
        if time.time() > deadline:
            if scrolled < scrolls:
                scroll(up=True)
                scrolled += 1
                continue
            note(f"NAO ENCONTRADO: {label}")
            if not optional:
                note("   na tela: " + " | ".join(t for t in (label_of(n) for n in nodes()) if t)[:900])
                fail(label)
            return False
        time.sleep(1)


def visible(text=None, regex=None):
    return bool(find(text, regex))


def check(description, condition):
    if condition:
        note("ok: " + description)
    else:
        note("FALHOU: " + description)
        fail(description)


def shot(name):
    time.sleep(1.0)
    data = adb("exec-out", "screencap", "-p").stdout
    with open(os.path.join(OUT, name + ".png"), "wb") as f:
        f.write(data)
    note(f"imagem: {name}")


def type_text(value):
    adb("shell", "input", "text", value.replace(" ", "%s"))
    time.sleep(0.8)


def back():
    adb("shell", "input", "keyevent", "4")
    time.sleep(1.2)


def hide_keyboard():
    out = adb("shell", "dumpsys", "input_method").stdout.decode("utf-8", "ignore")
    if "mInputShown=true" in out:
        back()



def permitir_avisos():
    """Fecha o pedido de permissão de notificação da primeira abertura."""
    for rotulo in ("Allow", "ALLOW", "Permitir", "While using the app", "Only this time"):
        if tap(rotulo, optional=True, scrolls=0, wait=2):
            return


def abrir_app():
    adb("shell", "am", "start", "-n", PKG + "/.MainActivity")
    time.sleep(6)
    for _ in range(25):
        permitir_avisos()
        if visible("Agora"):
            return
        press_wait_dialog()
        time.sleep(1)
    fail("abrir o app")


def instalar(apk, rotulo):
    note(f"instalando {rotulo}: {apk}")
    resultado = adb("install", "-r", apk, timeout=300)
    saida = (resultado.stdout + resultado.stderr).decode("utf-8", "ignore").strip()
    note(saida[-300:])
    check(f"instalacao de {rotulo}", "Success" in saida)


def anotar(titulo, tempo, energia):
    """Anota uma tarefa sem data: título, tempo, energia e Guardar."""
    tap("Anotar tarefa")
    tap_campo("O que precisa ser feito?", 0)
    type_text(titulo)
    hide_keyboard()
    tap(tempo)
    tap(energia)
    tap("Guardar")
    tap("Não, guardar assim")
    time.sleep(1.5)


def tap_campo(rotulo, indice):
    """Toca no campo de texto pelo rótulo ou, se não achar, pela posição na tela."""
    campos = [n for n in nodes() if n.get("class") == "android.widget.EditText"]
    if indice < len(campos):
        x, y = center(campos[indice])
        adb("shell", "input", "tap", str(x), str(y))
        note(f"toque: campo {indice} ({x},{y})")
        time.sleep(1.5)
    else:
        tap(rotulo, wait=4)


def anotar_com_data(titulo, obs):
    """Anota uma tarefa marcada para amanhã, às 08:05 — sempre no futuro."""
    amanha = device_today() + datetime.timedelta(days=1)
    tap("Anotar tarefa")
    tap_campo("O que precisa ser feito?", 0)
    type_text(titulo)
    hide_keyboard()
    tap("até 5 min")
    tap("Pouca")
    tap("Guardar")
    tap("Sim, definir data")
    shot("08-passo-dia")
    tap(str(amanha.day))
    shot("09-passo-mes")
    tap(MESES[amanha.month - 1])
    shot("10-passo-ano")
    tap(str(amanha.year))
    shot("11-passo-horario")
    tap("08")
    tap("05")
    tap("Continuar")
    shot("12-passo-observacao")
    tap_campo("Observação (obrigatória)", 0)
    type_text(obs)
    hide_keyboard()
    tap("Guardar tarefa")
    time.sleep(2)


MESES = [
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
]


def esperar_ampulheta():
    """Espera as peças da ampulheta ficarem prontas (é o desenho mais pesado do app)."""
    for _ in range(40):
        if not visible("Preparando a ampulheta…"):
            return True
        time.sleep(2)
    return False


def main():
    novo = sys.argv[1]
    publicado = sys.argv[2] if len(sys.argv) > 2 else None
    try:
        if publicado:
            roteiro_de_atualizacao(publicado, novo)
        else:
            instalar(novo, "versao nova")
            abrir_app()
            shot("01-inicio")
            anotar("Lavar a louca do jantar", "até 15 min", "Média")
            anotar("Regar as plantas", "até 5 min", "Pouca")
        roteiro_de_uso()
    except Abort:
        note("roteiro interrompido: falhas demais")
        shot("99-parou-aqui")
    except Exception as erro:  # noqa: BLE001 - qualquer erro aqui reprova a rodada
        note(f"erro no roteiro: {erro}")
        failures.append(f"erro no roteiro: {erro}")
        shot("99-erro")
    finally:
        steps.close()
    return report()


def roteiro_de_atualizacao(publicado, novo):
    """Cria tarefas na versão publicada e confere que elas sobrevivem à atualização."""
    instalar(publicado, "versao publicada")
    abrir_app()
    shot("01-versao-publicada")
    anotar("Lavar a louca do jantar", "até 15 min", "Média")
    shot("02-tarefa-anotada")
    anotar("Regar as plantas", "até 5 min", "Pouca")
    anotar_com_data("Ligar para a operadora", "Levar o numero do protocolo")
    shot("13-tarefa-com-data")
    check("contagem da tarefa com data", visible("faltam"))

    instalar(novo, "versao nova por cima")
    abrir_app()
    shot("14-depois-de-atualizar")
    tap("Moderado")
    tap("15 min")
    check(
        "tarefas continuam depois de atualizar",
        visible("Lavar a louca do jantar") or visible("Regar as plantas"),
    )
    shot("15-tarefa-escolhida-apos-atualizar")
    tap("Escolher a próxima", optional=True, scrolls=0, wait=2)


def roteiro_de_uso():
    """Percorre escolher, fazer, parar, concluir, a pausa e o Mais 5 min."""
    # ---- escolher
    abrir_app()
    shot("20-escolher-vazio")
    check("convite para escolher", visible("Toque nas duas escolhas acima. Eu mostro uma tarefa só."))
    tap("Foco")
    shot("21-so-energia")
    check("pede o tempo", visible("Agora, quanto tempo você tem?"))
    tap("45 min+")
    shot("22-tarefa-escolhida")
    check("explicacao da escolha", visible(regex=r"Por que esta\?.*"))

    # ---- trocar
    if visible(regex=r"Trocar \(\d\)"):
        tap(regex=r"Trocar \(\d\)")
        shot("23-depois-de-trocar")
        if visible(regex=r"Trocar \(\d\)"):
            tap(regex=r"Trocar \(\d\)")
        check("as trocas acabam", visible("Sem trocas") or visible(regex=r".*trocas acabaram.*"))
        shot("24-sem-trocas")
    else:
        note("aviso: so havia uma tarefa cabendo, o botao de trocar nao apareceu")

    # ---- fazer e parar
    tap("Começar")
    shot("25-ampulheta-comecando")
    check("ampulheta ficou pronta", esperar_ampulheta())
    time.sleep(4)
    shot("26-ampulheta-pronta")
    check("rotulo de fazendo agora", visible("FAZENDO AGORA"))
    tap("Parar")
    shot("27-depois-de-parar")
    check("aviso ao parar", visible("Tudo bem. A tarefa voltou para a lista."))

    # ---- concluir
    tap("Moderado")
    tap("15 min")
    tap("Começar")
    esperar_ampulheta()
    tap("Concluí")
    shot("28-feito")
    check("tela de feito", visible("Feito!"))
    check("resumo no singular", visible("1 tarefa concluída hoje."))
    tap("Escolher a próxima")
    shot("29-de-volta-ao-escolher")
    check("contador do dia no topo", visible("Hoje: 1"))

    # ---- nada cabe e a pausa de 2 minutos
    # O emulador roda com as animações desligadas, e o app respeita isso trocando a virada
    # da ampulheta por um encolhimento. Aqui elas voltam, para o "Mais 5 min" ser testado
    # do jeito que a pessoa vai ver. A ampulheta lê esse ajuste ao abrir a tela de fazer.
    adb("shell", "settings", "put", "global", "animator_duration_scale", "1")
    tap("Zumbi")
    tap("5 min")
    shot("30-nada-cabe-ou-tarefa")
    if visible("Nada cabe agora. E tudo bem."):
        tap("Pausa de 2 min")
    else:
        note("aviso: ainda havia tarefa para pouca energia; a pausa foi aberta pela tela cheia")
        tap("Começar")
    shot("31-pausa-ou-tarefa")
    check("ampulheta da pausa", esperar_ampulheta())

    # ---- Mais 5 min: espera o tempo acabar de verdade (a ampulheta não segue o relógio do sistema)
    note("esperando o tempo da pausa acabar (cerca de 2 min)")
    acabou = False
    for _ in range(80):
        if visible("Mais 5 min"):
            acabou = True
            break
        time.sleep(3)
    check("o botao Mais 5 min aparece quando o tempo acaba", acabou)
    if acabou:
        shot("32-tempo-acabou")
        check("rotulo de tempo esgotado", visible("O TEMPO ACABOU"))
        tap("Mais 5 min")
        time.sleep(2)
        shot("33-virando-a-ampulheta")
        time.sleep(4)
        shot("34-ciclo-novo")
        check("segue na tela de fazer depois de virar", visible("Concluí"))
        tap("Parar")

    # ---- a tarefa com data e a contagem no topo
    shot("35-tela-final")
    if visible("faltam"):
        check("contagem da tarefa com data segue na tela", True)
    else:
        note("aviso: nenhuma tarefa com data na tela final")


def report():
    quedas = adb("logcat", "-d", "-s", "AndroidRuntime:E").stdout.decode("utf-8", "ignore")
    with open(os.path.join(OUT, "crashes.txt"), "w") as f:
        f.write(quedas)
    avisos = adb("logcat", "-d", "-s", "Agora:W").stdout.decode("utf-8", "ignore")
    with open(os.path.join(OUT, "avisos-do-app.txt"), "w") as f:
        f.write(avisos)
    if avisos.strip():
        note("avisos do app: " + avisos[-1500:])
        failures.append("avisos de falha no log do app")
    rodando = adb("shell", "pidof", PKG).stdout.decode().strip()
    caiu = "FATAL EXCEPTION" in quedas
    note(f"app em execucao: {bool(rodando)}; erro fatal: {caiu}; passos com problema: {failures}")
    return 1 if (caiu or failures or not rodando) else 0


if __name__ == "__main__":
    sys.exit(main())
