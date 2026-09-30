package br.com.rastreadorfrota.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import br.com.rastreadorfrota.auth.CampoUsuario
import br.com.rastreadorfrota.auth.CategoriaCnh
import br.com.rastreadorfrota.auth.FormularioUsuario
import br.com.rastreadorfrota.auth.Perfil
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.theme.trakSyncTextFieldColors

/**
 * Campos do usuário. [modoCadastro] = true mostra email, senha e a escolha de
 * perfil (Cadastrar-se); false deixa email/perfil fixos (Meu perfil).
 */
@Composable
fun CamposUsuario(
    formulario: FormularioUsuario,
    erros: Map<CampoUsuario, String>,
    onChange: (FormularioUsuario) -> Unit,
    modoCadastro: Boolean,
    habilitado: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (modoCadastro) {
            Text(
                "Tipo de conta",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Perfil.entries.forEachIndexed { index, perfil ->
                    SegmentedButton(
                        selected = formulario.perfil == perfil,
                        onClick = { onChange(formulario.copy(perfil = perfil)) },
                        shape = SegmentedButtonDefaults.itemShape(index, Perfil.entries.size),
                        enabled = habilitado,
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            activeBorderColor = MaterialTheme.colorScheme.primary,
                            inactiveContainerColor = MaterialTheme.colorScheme.background,
                            inactiveContentColor = TrakSyncTheme.colors.textSecondary,
                            inactiveBorderColor = TrakSyncTheme.colors.surface2
                        )
                    ) {
                        Text(perfil.label)
                    }
                }
            }
        }

        SecaoTitulo("Dados pessoais")
        Campo("Nome completo", formulario.nome, erros[CampoUsuario.NOME], habilitado) {
            onChange(formulario.copy(nome = it))
        }
        Campo(
            "CPF", formulario.cpf, erros[CampoUsuario.CPF], habilitado,
            keyboardType = KeyboardType.Number
        ) { onChange(formulario.copy(cpf = it)) }
        Campo(
            "Telefone (com DDD)", formulario.telefone, erros[CampoUsuario.TELEFONE], habilitado,
            keyboardType = KeyboardType.Phone
        ) { onChange(formulario.copy(telefone = it)) }

        if (modoCadastro) {
            SecaoTitulo("Acesso")
            Campo(
                "Email", formulario.email, erros[CampoUsuario.EMAIL], habilitado,
                keyboardType = KeyboardType.Email
            ) { onChange(formulario.copy(email = it)) }
            Campo(
                "Senha", formulario.senha, erros[CampoUsuario.SENHA], habilitado,
                keyboardType = KeyboardType.Password, senha = true
            ) { onChange(formulario.copy(senha = it)) }
            Campo(
                "Confirmar senha", formulario.confirmacaoSenha, erros[CampoUsuario.CONFIRMACAO_SENHA], habilitado,
                keyboardType = KeyboardType.Password, senha = true
            ) { onChange(formulario.copy(confirmacaoSenha = it)) }
        }

        when (formulario.perfil) {
            Perfil.MOTORISTA -> {
                SecaoTitulo("Habilitação")
                Campo(
                    "Número da CNH", formulario.cnh, erros[CampoUsuario.CNH], habilitado,
                    keyboardType = KeyboardType.Number
                ) { onChange(formulario.copy(cnh = it)) }
                SeletorCategoriaCnh(
                    selecionada = formulario.categoriaCnh,
                    erro = erros[CampoUsuario.CATEGORIA_CNH],
                    habilitado = habilitado,
                    onSelecionar = { onChange(formulario.copy(categoriaCnh = it)) }
                )
                Campo(
                    "Validade da CNH (dd/mm/aaaa)", formulario.validadeCnh, erros[CampoUsuario.VALIDADE_CNH], habilitado,
                    keyboardType = KeyboardType.Number
                ) { onChange(formulario.copy(validadeCnh = it)) }
            }
            Perfil.CONTROLADOR -> {
                SecaoTitulo("Empresa")
                Campo("Empresa / transportadora", formulario.empresa, erros[CampoUsuario.EMPRESA], habilitado) {
                    onChange(formulario.copy(empresa = it))
                }
                Campo("Cargo (opcional)", formulario.cargo, null, habilitado) {
                    onChange(formulario.copy(cargo = it))
                }
            }
        }
    }
}

@Composable
private fun SecaoTitulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleSmall, color = TrakSyncTheme.colors.textSecondary)
}

@Composable
private fun Campo(
    label: String,
    valor: String,
    erro: String?,
    habilitado: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    senha: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = habilitado,
        isError = erro != null,
        supportingText = erro?.let { { Text(it) } },
        visualTransformation = if (senha) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = MaterialTheme.shapes.medium,
        colors = trakSyncTextFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorCategoriaCnh(
    selecionada: String,
    erro: String?,
    habilitado: Boolean,
    onSelecionar: (String) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { if (habilitado) expandido = it }
    ) {
        OutlinedTextField(
            value = selecionada.ifBlank { "Selecione" },
            onValueChange = {},
            readOnly = true,
            enabled = habilitado,
            label = { Text("Categoria da CNH") },
            isError = erro != null,
            supportingText = erro?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            shape = MaterialTheme.shapes.medium,
            colors = trakSyncTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = habilitado)
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            CategoriaCnh.entries.forEach { categoria ->
                DropdownMenuItem(
                    text = { Text(categoria.name) },
                    onClick = {
                        onSelecionar(categoria.name)
                        expandido = false
                    }
                )
            }
        }
    }
}
