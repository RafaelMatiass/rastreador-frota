package br.com.rastreadorfrota.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RoundaboutRight
import androidx.compose.material.icons.filled.SensorDoor
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.TurnSlightLeft
import androidx.compose.material.icons.filled.TurnSlightRight
import androidx.compose.material.icons.filled.UTurnLeft
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.rastreadorfrota.data.local.entity.TipoVeiculo
import br.com.rastreadorfrota.data.repository.VeiculoNoMapa
import br.com.rastreadorfrota.rota.InstrucaoRota
import br.com.rastreadorfrota.rota.Manobra
import br.com.rastreadorfrota.rota.NavegacaoExterna
import br.com.rastreadorfrota.rota.formatarDistancia
import br.com.rastreadorfrota.simulacao.StatusVeiculo
import br.com.rastreadorfrota.ui.components.BotaoPrincipal
import br.com.rastreadorfrota.ui.components.BotaoSecundario
import br.com.rastreadorfrota.ui.components.FotoVeiculo
import br.com.rastreadorfrota.ui.components.InfoChip
import br.com.rastreadorfrota.ui.components.PlacaBadge
import br.com.rastreadorfrota.ui.components.StatusPill
import br.com.rastreadorfrota.ui.components.StepperViagem
import br.com.rastreadorfrota.ui.components.corDoStatus
import br.com.rastreadorfrota.ui.components.descricaoAcessivel
import br.com.rastreadorfrota.ui.components.icone
import br.com.rastreadorfrota.ui.components.textoViagem
import br.com.rastreadorfrota.ui.components.tipoVeiculo
import br.com.rastreadorfrota.ui.theme.Espaco
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.viewmodel.EnderecosVeiculo
import br.com.rastreadorfrota.ui.viewmodel.EstadoRota

/** Detalhes do veículo selecionado no mapa: telemetria, endereço, viagem e ações. */
@Composable
fun PainelVeiculo(
    item: VeiculoNoMapa,
    enderecos: EnderecosVeiculo?,
    seguindo: Boolean,
    podeFechar: Boolean,
    onFechar: () -> Unit,
    onTracarRota: () -> Unit,
    onNavegarEntrega: (() -> Unit)?,
    onSeguir: () -> Unit,
    onDetalhes: (() -> Unit)?
) {
    val t = item.telemetria
    val v = item.veiculo
    val tipo = tipoVeiculo(v.tipo)
    Column(Modifier.padding(horizontal = Espaco.tela)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = descricaoAcessivel(item) }
        ) {
            FotoVeiculo(v.fotoLocalPath, tipo.icone, Modifier.size(56.dp))
            Spacer(Modifier.width(Espaco.md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlacaBadge(v.placa, compacta = true)
                    Spacer(Modifier.width(Espaco.sm))
                    StatusPill(item.status)
                }
                Text(
                    "${v.modelo} · ${tipo.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TrakSyncTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (podeFechar) {
                IconButton(onClick = onFechar) { Icon(Icons.Filled.Close, contentDescription = "Fechar detalhes") }
            }
        }

        EnderecoAtual(enderecos)

        if (item.emOperacao) {
            Spacer(Modifier.height(Espaco.md))
            StepperViagem(item.viagem.etapa)
        }
        Text(
            textoViagem(item.viagem),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = Espaco.md)
        )
        enderecos?.entrega?.let {
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "${it.linha1}${it.bairro?.let { b -> " – $b" }.orEmpty()}${it.cep?.let { c -> " · CEP $c" }.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TrakSyncTheme.colors.textSecondary
                )
            }
        }

        Row(
            Modifier
                .padding(top = Espaco.md)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Espaco.sm)
        ) {
            InfoChip(Icons.Filled.Speed, "${t.velocidadeKmh} km/h", cor = if (item.acimaDaVelocidade) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            InfoChip(Icons.Filled.PowerSettingsNew, if (t.motorLigado) "Motor ligado" else "Motor desligado")
            InfoChip(
                Icons.Filled.SensorDoor,
                if (t.portasAbertas) "Portas abertas" else "Portas fechadas",
                cor = if (item.portaAbertaEmMovimento) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            item.motorista?.let { InfoChip(Icons.Filled.Person, it.nome.substringBefore(" ")) }
        }

        Spacer(Modifier.height(Espaco.lg))
        if (onNavegarEntrega != null) {
            BotaoPrincipal("Navegar até a entrega", onNavegarEntrega, Modifier.fillMaxWidth(), icone = Icons.Filled.Navigation)
            Spacer(Modifier.height(Espaco.sm))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Espaco.sm)) {
            BotaoSecundario("Rota até aqui", Icons.Filled.Directions, onTracarRota, Modifier.weight(1f))
            BotaoSecundario(if (seguindo) "Seguindo" else "Seguir", Icons.Filled.GpsFixed, onSeguir, Modifier.weight(1f), enabled = !seguindo)
        }
        if (onDetalhes != null) {
            TextButton(onClick = onDetalhes, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Ver ficha do veículo")
            }
        }
        Text(
            "Telemetria simulada · %.5f, %.5f".format(t.latitude, t.longitude),
            style = MaterialTheme.typography.labelSmall,
            color = TrakSyncTheme.colors.textSecondary,
            modifier = Modifier.padding(top = Espaco.xs)
        )
    }
}

/** Endereço da posição atual do veículo, pela reverse geocoding API (Entrega 7). */
@Composable
private fun EnderecoAtual(enderecos: EnderecosVeiculo?) {
    val atual = enderecos?.atual
    val erro = enderecos?.erro
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .padding(top = Espaco.md)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
    ) {
        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Column {
            when {
                atual != null -> {
                    Text(atual.linha1, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(atual.linha2, style = MaterialTheme.typography.bodySmall, color = TrakSyncTheme.colors.textSecondary)
                    if (erro != null) {
                        Text("Último endereço conhecido · $erro", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                erro != null -> Text("Endereço indisponível: $erro", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                else -> Text("Buscando endereço...", style = MaterialTheme.typography.bodySmall, color = TrakSyncTheme.colors.textSecondary)
            }
        }
    }
}

/** Card da rota: de onde, para onde, tempo, chegada, passo a passo e atalhos para Waze/Maps. */
@Composable
fun CardRota(
    estado: EstadoRota,
    onRecalcular: () -> Unit,
    onInverter: () -> Unit,
    onEditar: () -> Unit,
    onLimpar: () -> Unit
) {
    val context = LocalContext.current
    var passoAPasso by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = Espaco.tela)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rota", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onInverter) { Icon(Icons.Filled.SwapVert, contentDescription = "Inverter origem e destino") }
            IconButton(onClick = onLimpar) { Icon(Icons.Filled.Close, contentDescription = "Fechar rota") }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = "Editar origem e destino", onClick = onEditar)
                .padding(vertical = 4.dp)
        ) {
            PontaDaRota(Icons.Filled.TripOrigin, MaterialTheme.colorScheme.primary, estado.origemEscolhida.descricao, estado.enderecoOrigem?.resumo)
            Box(Modifier.padding(start = 9.dp).width(2.dp).height(12.dp).background(TrakSyncTheme.colors.surface3))
            PontaDaRota(Icons.Filled.LocationOn, MaterialTheme.colorScheme.error, estado.destinoEscolhido?.descricao.orEmpty(), estado.enderecoDestino?.resumo)
        }
        Spacer(Modifier.height(Espaco.md))
        val rota = estado.rota
        when {
            estado.calculando -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(Espaco.sm))
                Text("Calculando a melhor rota...", style = MaterialTheme.typography.bodyMedium)
            }
            rota != null -> Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "${rota.duracaoTexto}, ${rota.distanciaTexto}, chegada às ${rota.chegadaTexto()}"
                }
            ) {
                Text(rota.duracaoTexto, style = MaterialTheme.typography.headlineSmall, color = TrakSyncTheme.colors.success)
                Spacer(Modifier.width(Espaco.sm))
                Text(
                    "${rota.distanciaTexto} · chegada ${rota.chegadaTexto()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TrakSyncTheme.colors.textSecondary,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            estado.erro != null -> Text(estado.erro, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        estado.aviso?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = TrakSyncTheme.colors.warning, modifier = Modifier.padding(top = Espaco.xs))
        }

        val destino = estado.destino
        if (!estado.calculando && destino != null) {
            Row(
                Modifier
                    .padding(top = Espaco.md)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Espaco.sm)
            ) {
                BotaoSecundario("Waze", Icons.AutoMirrored.Filled.OpenInNew, { NavegacaoExterna.abrirWaze(context, destino) })
                BotaoSecundario("Google Maps", Icons.Filled.Map, { NavegacaoExterna.abrirGoogleMaps(context, destino) })
                BotaoSecundario("Recalcular", Icons.Filled.Refresh, onRecalcular)
            }
        }
        if (rota != null && rota.instrucoes.isNotEmpty()) {
            TextButton(onClick = { passoAPasso = !passoAPasso }) {
                Icon(if (passoAPasso) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (passoAPasso) "Ocultar passo a passo" else "Ver passo a passo (${rota.instrucoes.size})")
            }
            AnimatedVisibility(passoAPasso) {
                Column {
                    rota.instrucoes.forEachIndexed { i, instrucao ->
                        LinhaInstrucao(instrucao)
                        if (i < rota.instrucoes.lastIndex) HorizontalDivider(color = TrakSyncTheme.colors.surface2)
                    }
                }
            }
        }
    }
}

@Composable
private fun PontaDaRota(icone: ImageVector, cor: Color, titulo: String, endereco: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Espaco.md))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (endereco != null && endereco != titulo) {
                Text(endereco, style = MaterialTheme.typography.bodySmall, color = TrakSyncTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun LinhaInstrucao(instrucao: InstrucaoRota) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(34.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(instrucao.manobra.icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(Espaco.md))
        Text(instrucao.texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (instrucao.distanciaMetros > 0) {
            Text(formatarDistancia(instrucao.distanciaMetros), style = MaterialTheme.typography.labelMedium, color = TrakSyncTheme.colors.textSecondary)
        }
    }
}

private val Manobra.icone: ImageVector
    get() = when (this) {
        Manobra.SAIDA -> Icons.Filled.MyLocation
        Manobra.CHEGADA -> Icons.Filled.Flag
        Manobra.RETO -> Icons.Filled.Straight
        Manobra.DIREITA -> Icons.Filled.TurnRight
        Manobra.ESQUERDA -> Icons.Filled.TurnLeft
        Manobra.LEVE_DIREITA -> Icons.Filled.TurnSlightRight
        Manobra.LEVE_ESQUERDA -> Icons.Filled.TurnSlightLeft
        Manobra.RETORNO -> Icons.Filled.UTurnLeft
        Manobra.ROTATORIA -> Icons.Filled.RoundaboutRight
    }

/** Resumo da frota no painel do mapa (gestor): lista tocável de veículos, com status. */
@Composable
fun ListaFrotaMapa(
    frota: List<VeiculoNoMapa>,
    selecionadoId: Long?,
    onSelecionar: (VeiculoNoMapa) -> Unit
) {
    Column(Modifier.padding(horizontal = Espaco.tela)) {
        frota.forEachIndexed { i, item ->
            LinhaVeiculoMapa(item, item.veiculo.id == selecionadoId) { onSelecionar(item) }
            if (i < frota.lastIndex) HorizontalDivider(color = TrakSyncTheme.colors.surface2)
        }
    }
}

@Composable
private fun LinhaVeiculoMapa(item: VeiculoNoMapa, selecionado: Boolean, onClick: () -> Unit) {
    val status = item.status
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClickLabel = "Mostrar no mapa", onClick = onClick)
            .background(if (selecionado) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .padding(vertical = 10.dp)
            .semantics(mergeDescendants = true) { contentDescription = descricaoAcessivel(item) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(corDoStatus(status), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(tipoVeiculo(item.veiculo.tipo).icone, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(Espaco.md))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.veiculo.placa, style = MaterialTheme.typography.titleSmall)
                Text(
                    " · ${item.motorista?.nome?.substringBefore(" ") ?: item.veiculo.modelo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TrakSyncTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                textoViagem(item.viagem, curto = true),
                style = MaterialTheme.typography.bodySmall,
                color = TrakSyncTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(status.label, style = MaterialTheme.typography.labelMedium, color = corDoStatus(status))
            Text("${item.telemetria.velocidadeKmh} km/h", style = MaterialTheme.typography.bodySmall, color = TrakSyncTheme.colors.textSecondary)
        }
    }
}

/** Chips de filtro por status, com a contagem de cada um. */
@Composable
fun FiltrosStatus(
    frota: List<VeiculoNoMapa>,
    filtro: StatusVeiculo?,
    mostrarCercas: Boolean,
    temCercas: Boolean,
    onFiltro: (StatusVeiculo?) -> Unit,
    onAlternarCercas: () -> Unit
) {
    val cores = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surface,
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
    )
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Espaco.sm)
    ) {
        FilterChip(selected = filtro == null, onClick = { onFiltro(null) }, label = { Text("Todos (${frota.size})") }, colors = cores)
        StatusVeiculo.entries.forEach { status ->
            val n = frota.count { it.status == status }
            if (n > 0 || filtro == status) {
                FilterChip(
                    selected = filtro == status,
                    onClick = { onFiltro(if (filtro == status) null else status) },
                    label = { Text("${status.label} ($n)") },
                    leadingIcon = { Icon(status.icone, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = cores
                )
            }
        }
        if (temCercas) {
            FilterChip(
                selected = mostrarCercas,
                onClick = onAlternarCercas,
                label = { Text("Cercas") },
                colors = cores
            )
        }
    }
}

/** Ícone do tipo de veículo também em telas sem o mapa. */
val TipoVeiculo.iconeTipo: ImageVector get() = this.icone
