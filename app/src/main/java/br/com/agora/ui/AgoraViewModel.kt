package br.com.agora.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.agora.data.Repositorio
import br.com.agora.domain.Tarefa
import br.com.agora.domain.cabem
import br.com.agora.domain.instanteDe
import br.com.agora.domain.trocasRestantes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Em que parte do app a pessoa está. */
enum class Modo { ESCOLHER, FAZENDO, FEITO }

/** Os passos da folha de anotar uma tarefa. */
enum class Passo { FECHADA, FORMULARIO, PERGUNTA, DIA, MES, ANO, HORARIO, OBSERVACAO }

/** A tarefa que está em andamento agora. */
data class EmAndamento(
    val id: Long?,
    val titulo: String,
    val minutos: Int,
    val ehPausa: Boolean,
    val inicioUptime: Long,
    val totalSegundos: Int
)

/** O rascunho da tarefa sendo anotada. */
data class Rascunho(
    val titulo: String = "",
    val minutos: Int = 15,
    val energia: Int = 2,
    val dia: Int? = null,
    val mes: Int? = null,
    val ano: Int? = null,
    val hora: Int? = null,
    val minuto: Int? = null,
    val obs: String = "",
    val erro: String = ""
)

data class EstadoTela(
    val tarefas: List<Tarefa> = emptyList(),
    val energia: Int? = null,
    val tempo: Int? = null,
    val indice: Int = 0,
    val trocasFeitas: Int = 0,
    val modo: Modo = Modo.ESCOLHER,
    val fazendo: EmAndamento? = null,
    val concluidasHoje: Int = 0,
    val ultimaFeita: String = "",
    val aviso: String = "",
    val passo: Passo = Passo.FECHADA,
    val rascunho: Rascunho = Rascunho(),
    val ampulhetaPronta: Boolean = false,
    val relogio: Long = System.currentTimeMillis()
) {
    /** Quantas tarefas cabem no momento escolhido. */
    fun candidatas(): List<Tarefa> {
        val e = energia
        val t = tempo
        if (e == null || t == null) return emptyList()
        return cabem(tarefas, e, t, relogio)
    }

    val escolhida: Tarefa? get() = candidatas().getOrNull(indice)

    fun trocasSobrando(limite: Int): Int =
        trocasRestantes(limite, trocasFeitas, candidatas().size, indice)

    /** O tempo da tarefa em andamento já acabou? */
    val tempoAcabou: Boolean
        get() {
            val f = fazendo ?: return false
            return SystemClock.uptimeMillis() - f.inicioUptime >= f.totalSegundos * 1000L
        }
}

/** O limite de trocas antes de a pessoa precisar encarar a tarefa sugerida. */
const val LIMITE_DE_TROCAS = 2

class AgoraViewModel(app: Application) : AndroidViewModel(app) {

    private val repositorio = Repositorio(app)
    private val _estado = MutableStateFlow(EstadoTela())
    val estado: StateFlow<EstadoTela> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            repositorio.tarefas.collect { lista -> _estado.update { it.copy(tarefas = lista) } }
        }
        viewModelScope.launch {
            repositorio.concluidasHoje.collect { n -> _estado.update { it.copy(concluidasHoje = n) } }
        }
        // Um pulso por segundo move a contagem da tarefa com data e o aviso de tempo esgotado.
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _estado.update { it.copy(relogio = System.currentTimeMillis()) }
            }
        }
    }

    // ---------------------------------------------------------------- escolher
    fun escolherEnergia(valor: Int) = _estado.update {
        it.copy(energia = valor, indice = 0, trocasFeitas = 0)
    }

    fun escolherTempo(valor: Int) = _estado.update {
        it.copy(tempo = valor, indice = 0, trocasFeitas = 0)
    }

    fun trocar() = _estado.update { atual ->
        if (atual.trocasSobrando(LIMITE_DE_TROCAS) <= 0) atual
        else atual.copy(indice = atual.indice + 1, trocasFeitas = atual.trocasFeitas + 1)
    }

    // ---------------------------------------------------------------- fazer
    fun comecar() {
        val atual = _estado.value
        val tarefa = atual.escolhida ?: return
        _estado.update {
            it.copy(
                modo = Modo.FAZENDO,
                ampulhetaPronta = false,
                fazendo = EmAndamento(
                    id = tarefa.id,
                    titulo = tarefa.titulo,
                    minutos = tarefa.minutos,
                    ehPausa = false,
                    inicioUptime = SystemClock.uptimeMillis(),
                    totalSegundos = tarefa.minutos * 60
                )
            )
        }
    }

    fun comecarPausa() = _estado.update {
        it.copy(
            modo = Modo.FAZENDO,
            ampulhetaPronta = false,
            fazendo = EmAndamento(
                id = null,
                titulo = "Respirar e alongar",
                minutos = 2,
                ehPausa = true,
                inicioUptime = SystemClock.uptimeMillis(),
                totalSegundos = 120
            )
        )
    }

    /** Depois da virada da ampulheta, a areia recomeça com cinco minutos. */
    fun maisCincoMinutos(esperaMs: Long) = _estado.update { atual ->
        val f = atual.fazendo ?: return@update atual
        atual.copy(
            fazendo = f.copy(
                inicioUptime = SystemClock.uptimeMillis() + esperaMs,
                totalSegundos = 300
            )
        )
    }

    fun concluir() {
        val atual = _estado.value
        val f = atual.fazendo ?: return
        if (f.ehPausa) {
            _estado.update {
                it.copy(
                    modo = Modo.ESCOLHER, fazendo = null, energia = null, tempo = null,
                    indice = 0, trocasFeitas = 0, aviso = "Pausa feita. Como você está agora?"
                )
            }
            return
        }
        val tarefa = atual.tarefas.firstOrNull { it.id == f.id }
        viewModelScope.launch {
            if (tarefa != null) repositorio.concluir(tarefa, System.currentTimeMillis())
        }
        _estado.update {
            it.copy(modo = Modo.FEITO, fazendo = null, ultimaFeita = f.titulo)
        }
    }

    fun parar() {
        val eraPausa = _estado.value.fazendo?.ehPausa == true
        _estado.update {
            it.copy(
                modo = Modo.ESCOLHER, fazendo = null, indice = 0, trocasFeitas = 0,
                aviso = if (eraPausa) "Pausa encerrada." else "Tudo bem. A tarefa voltou para a lista."
            )
        }
    }

    fun proxima() = _estado.update {
        it.copy(modo = Modo.ESCOLHER, energia = null, tempo = null, indice = 0, trocasFeitas = 0)
    }

    fun ampulhetaPronta(pronta: Boolean) = _estado.update { it.copy(ampulhetaPronta = pronta) }

    fun limparAviso() = _estado.update { it.copy(aviso = "") }

    // ---------------------------------------------------------------- anotar
    fun abrirFolha() = _estado.update { it.copy(passo = Passo.FORMULARIO, rascunho = Rascunho()) }

    fun fecharFolha() = _estado.update { it.copy(passo = Passo.FECHADA) }

    fun mudarTitulo(valor: String) = _estado.update { it.copy(rascunho = it.rascunho.copy(titulo = valor)) }

    fun mudarMinutos(valor: Int) = _estado.update { it.copy(rascunho = it.rascunho.copy(minutos = valor)) }

    fun mudarEnergia(valor: Int) = _estado.update { it.copy(rascunho = it.rascunho.copy(energia = valor)) }

    fun mudarObs(valor: String) = _estado.update { it.copy(rascunho = it.rascunho.copy(obs = valor)) }

    /** Do formulário para a pergunta "definir data?". */
    fun pedirData() = _estado.update {
        if (it.rascunho.titulo.isBlank()) it
        else it.copy(passo = Passo.PERGUNTA, rascunho = it.rascunho.copy(erro = ""))
    }

    fun responderSim() = _estado.update { it.copy(passo = Passo.DIA, rascunho = it.rascunho.copy(erro = "")) }

    fun responderNao() = guardar(null, "")

    fun voltarPasso() = _estado.update { atual ->
        val ordem = listOf(Passo.FORMULARIO, Passo.PERGUNTA, Passo.DIA, Passo.MES, Passo.ANO, Passo.HORARIO, Passo.OBSERVACAO)
        val i = ordem.indexOf(atual.passo)
        val anterior = if (i <= 0) Passo.FORMULARIO else ordem[i - 1]
        atual.copy(passo = anterior, rascunho = atual.rascunho.copy(erro = ""))
    }

    /**
     * Escolhe um valor da data e limpa o que ficou inválido. Dia, mês e ano já levam
     * para o passo seguinte; hora e minuto esperam o botão Continuar.
     */
    fun escolherData(campo: Passo, valor: Int) = _estado.update { atual ->
        val agora = System.currentTimeMillis()
        var r = atual.rascunho
        var proximo = atual.passo
        when (campo) {
            Passo.DIA -> { r = r.copy(dia = valor); proximo = Passo.MES }
            Passo.MES -> { r = r.copy(mes = valor); proximo = Passo.ANO }
            Passo.ANO -> { r = r.copy(ano = valor); proximo = Passo.HORARIO }
            Passo.HORARIO -> r = r.copy(hora = valor)
            else -> r = r.copy(minuto = valor)
        }
        if (r.mes != null && !br.com.agora.domain.mesServe(r.dia, r.mes!!)) r = r.copy(mes = null)
        if (r.ano != null && !br.com.agora.domain.anoServe(r.dia, r.mes, r.ano!!, agora)) r = r.copy(ano = null)
        if (r.hora != null && !br.com.agora.domain.horaServe(r.dia, r.mes, r.ano, r.hora!!, agora)) r = r.copy(hora = null)
        if (r.minuto != null && !br.com.agora.domain.minutoServe(r.dia, r.mes, r.ano, r.hora, r.minuto!!, agora)) r = r.copy(minuto = null)
        atual.copy(passo = proximo, rascunho = r.copy(erro = ""))
    }

    fun escolherMinuto(valor: Int) = escolherData(Passo.OBSERVACAO, valor)

    fun horarioPronto() = _estado.update { it.copy(passo = Passo.OBSERVACAO, rascunho = it.rascunho.copy(erro = "")) }

    /** Guarda a tarefa com data, depois de conferir que o horário ainda está no futuro. */
    fun guardarComData() {
        val atual = _estado.value
        val r = atual.rascunho
        if (r.obs.isBlank()) return
        val dia = r.dia; val mes = r.mes; val ano = r.ano; val hora = r.hora; val minuto = r.minuto
        if (dia == null || mes == null || ano == null || hora == null || minuto == null) return
        val quando = instanteDe(dia, mes, ano, hora, minuto)
        if (quando <= System.currentTimeMillis()) {
            _estado.update {
                it.copy(
                    passo = Passo.HORARIO,
                    rascunho = r.copy(hora = null, minuto = null, erro = "Esse horário já passou. Escolha outro.")
                )
            }
            return
        }
        guardar(quando, r.obs.trim())
    }

    private fun guardar(quando: Long?, obs: String) {
        val atual = _estado.value
        val titulo = atual.rascunho.titulo.trim()
        if (titulo.isEmpty()) return
        val tarefa = Tarefa(
            titulo = titulo,
            minutos = atual.rascunho.minutos,
            energia = atual.rascunho.energia,
            criadaEm = System.currentTimeMillis(),
            em = quando,
            obs = obs
        )
        viewModelScope.launch { repositorio.guardar(tarefa) }
        val recado = if (quando == null) "Guardada. Ela já entra nas sugestões."
        else "Guardada com data: " + br.com.agora.domain.formatarQuando(quando) + "."
        _estado.update { it.copy(passo = Passo.FECHADA, rascunho = Rascunho(), aviso = recado) }
    }
}
