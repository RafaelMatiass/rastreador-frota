package br.com.rastreadorfrota.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.rastreadorfrota.auth.Perfil
import br.com.rastreadorfrota.data.repository.VeiculoNoMapa
import br.com.rastreadorfrota.simulacao.StatusVeiculo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.osmdroid.util.GeoPoint

object Rotas {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val CADASTRO_USUARIO = "cadastro_usuario"

    // Abas
    const val INICIO_GESTOR = "inicio_gestor"
    const val INICIO_MOTORISTA = "inicio_motorista"
    const val MAPA = "mapa"
    const val FROTA = "frota"
    const val EQUIPE = "equipe"
    const val ALERTAS = "alertas"
    const val MEU_VEICULO = "meu_veiculo"
    const val PERFIL = "perfil"

    // Telas cheias (sem a barra de abas)
    const val VEICULO_FORM = "veiculo_form?id={id}"
    const val VEICULO_DETALHE = "veiculo/{id}"
    const val CERCA_EDITOR = "cerca?id={id}"

    fun veiculoForm(id: Long? = null) = "veiculo_form" + (id?.let { "?id=$it" } ?: "")
    fun veiculoDetalhe(id: Long) = "veiculo/$id"
    fun cercaEditor(id: Long? = null) = "cerca" + (id?.let { "?id=$it" } ?: "")

    fun inicioDo(perfil: Perfil) = when (perfil) {
        Perfil.CONTROLADOR -> INICIO_GESTOR
        Perfil.MOTORISTA -> INICIO_MOTORISTA
    }
}

/** Uma aba da barra inferior. */
data class Aba(
    val rota: String,
    val rotulo: String,
    val icone: ImageVector,
    val iconeSelecionado: ImageVector
)

private val ABAS_GESTOR = listOf(
    Aba(Rotas.INICIO_GESTOR, "Início", Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
    Aba(Rotas.MAPA, "Mapa", Icons.Outlined.Map, Icons.Filled.Map),
    Aba(Rotas.FROTA, "Frota", Icons.Outlined.LocalShipping, Icons.Filled.LocalShipping),
    Aba(Rotas.EQUIPE, "Equipe", Icons.Outlined.Groups, Icons.Filled.Groups),
    Aba(Rotas.ALERTAS, "Alertas", Icons.Outlined.NotificationsActive, Icons.Filled.NotificationsActive)
)

private val ABAS_MOTORISTA = listOf(
    Aba(Rotas.INICIO_MOTORISTA, "Início", Icons.Outlined.Home, Icons.Filled.Home),
    Aba(Rotas.MAPA, "Mapa", Icons.Outlined.Map, Icons.Filled.Map),
    Aba(Rotas.MEU_VEICULO, "Veículo", Icons.Outlined.LocalShipping, Icons.Filled.LocalShipping),
    Aba(Rotas.PERFIL, "Perfil", Icons.Outlined.Person, Icons.Filled.Person)
)

fun abasDo(perfil: Perfil): List<Aba> = when (perfil) {
    Perfil.CONTROLADOR -> ABAS_GESTOR
    Perfil.MOTORISTA -> ABAS_MOTORISTA
}

/** O que a aba Mapa deve fazer ao abrir: filtrar, focar um veículo ou traçar uma rota. */
data class PedidoMapa(
    val filtro: StatusVeiculo? = null,
    val placa: String? = null,
    val rotaAte: GeoPoint? = null,
    val rotaDescricao: String? = null,
    val instante: Long = System.nanoTime()
)

/**
 * Recados entre abas. A aba mantém o próprio estado (restoreState), então em vez de
 * argumentos de rota o pedido fica aqui até o Mapa consumir.
 */
object PedidosNavegacao {
    private val _mapa = MutableStateFlow<PedidoMapa?>(null)
    val mapa: StateFlow<PedidoMapa?> = _mapa.asStateFlow()

    fun pedirMapa(pedido: PedidoMapa) {
        _mapa.value = pedido
    }

    fun focar(item: VeiculoNoMapa) = pedirMapa(PedidoMapa(placa = item.veiculo.placa))

    fun consumirMapa() {
        _mapa.value = null
    }
}
