package br.com.rastreadorfrota.data.local.entity

/**
 * Ciclo de vida do veículo na frota:
 * o motorista cadastra -> PENDENTE; o gestor aprova -> ATIVO; o gestor pode desativar -> INATIVO.
 * Veículo cadastrado pelo gestor já nasce ATIVO.
 */
enum class SituacaoVeiculo(val label: String) {
    PENDENTE("Aguardando aprovação"),
    ATIVO("Ativo"),
    INATIVO("Inativo");

    companion object {
        fun de(nome: String?): SituacaoVeiculo = entries.find { it.name == nome } ?: ATIVO
    }
}
