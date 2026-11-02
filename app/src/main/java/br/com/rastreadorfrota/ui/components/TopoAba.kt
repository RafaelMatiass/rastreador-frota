package br.com.rastreadorfrota.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import br.com.rastreadorfrota.ui.theme.PreferenciaTema
import br.com.rastreadorfrota.ui.theme.TemaApp
import br.com.rastreadorfrota.ui.theme.temaEscuroAtivo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.rastreadorfrota.auth.Usuario
import br.com.rastreadorfrota.ui.theme.Espaco
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme

/** Sol/lua: alterna entre claro e escuro com um toque (a opção "Sistema" fica no Perfil). */
@Composable
fun BotaoTema() {
    val context = LocalContext.current
    val escuro = temaEscuroAtivo()
    IconButton(onClick = { PreferenciaTema.definir(context, if (escuro) TemaApp.CLARO else TemaApp.ESCURO) }) {
        Icon(
            if (escuro) Icons.Filled.LightMode else Icons.Filled.DarkMode,
            contentDescription = if (escuro) "Usar tema claro" else "Usar tema escuro",
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}

/**
 * Cabeçalho das abas: título grande, subtítulo e o avatar que abre o perfil.
 * Já respeita a barra de status (o app desenha de ponta a ponta).
 */
@Composable
fun TopoAba(
    titulo: String,
    usuario: Usuario?,
    onAbrirPerfil: () -> Unit,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    acoes: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = Espaco.tela, end = Espaco.sm, top = Espaco.md, bottom = Espaco.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            if (subtitulo != null) {
                Text(
                    subtitulo,
                    style = MaterialTheme.typography.labelLarge,
                    color = TrakSyncTheme.colors.cyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                titulo,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
        }
        acoes()
        BotaoTema()
        if (usuario != null) {
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onAbrirPerfil) {
                AvatarUsuario(uid = usuario.uid, nome = usuario.nome, tamanho = 38.dp)
            }
        }
    }
}
