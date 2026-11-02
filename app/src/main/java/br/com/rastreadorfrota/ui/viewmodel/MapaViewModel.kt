package br.com.rastreadorfrota.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.rastreadorfrota.data.local.AppDatabase
import br.com.rastreadorfrota.data.local.entity.CercaEntity
import br.com.rastreadorfrota.data.repository.FrotaAoVivoRepository
import br.com.rastreadorfrota.data.repository.VeiculoNoMapa
import br.com.rastreadorfrota.simulacao.StatusVeiculo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import org.osmdroid.util.GeoPoint

/** Centro e zoom do mapa, guardados para a aba voltar onde estava. */
data class CameraMapa(val centro: GeoPoint, val zoom: Double)

/**
 * Estado da aba Mapa. Fica no ViewModel (e não na tela) para sobreviver à troca de abas:
 * a câmera, o veículo selecionado, o filtro e o "seguir veículo" continuam iguais na volta.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapaViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = FrotaAoVivoRepository.getInstance(application)

    /** null até a tela dizer quem está olhando o mapa (evita mostrar a regra errada no 1º quadro). */
    private val modoMotorista = MutableStateFlow<Boolean?>(null)

    fun definirModo(motorista: Boolean) {
        modoMotorista.value = motorista
    }

    /** null enquanto o Room ainda não respondeu (pra não piscar o "nenhum veículo"). */
    val frota: StateFlow<List<VeiculoNoMapa>?> = modoMotorista.filterNotNull()
        .flatMapLatest { repositorio.frota(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = null)

    val cercas: StateFlow<List<CercaEntity>> = AppDatabase.getInstance(application).cercaDao().observarTodas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    var camera by mutableStateOf<CameraMapa?>(null)
    var selecionadoId by mutableStateOf<Long?>(null)
    var filtro by mutableStateOf<StatusVeiculo?>(null)
    var mostrarCercas by mutableStateOf(true)

    /** Seguindo o veículo selecionado: a câmera acompanha cada passo até o usuário mexer no mapa. */
    var seguindo by mutableStateOf(false)

    /** A frota já foi enquadrada uma vez (na 1ª abertura). */
    var enquadrouFrota = false
}
