package br.com.rastreadorfrota.simulacao

import br.com.rastreadorfrota.data.local.entity.EntregaEntity
import br.com.rastreadorfrota.data.local.entity.SituacaoEntrega
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.osmdroid.util.GeoPoint

class ViagensSimuladasTest {

    // Trajetos sintéticos em Araraquara: ida reta ~2,2 km para o leste e a volta pelo mesmo caminho.
    private val cd = CENTRO_DISTRIBUICAO
    private val cliente = GeoPoint(cd.latitude, cd.longitude + 0.02)
    private val idaPlanejada = listOf(cd, GeoPoint(cd.latitude, cd.longitude + 0.01), cliente)
    private val volta = idaPlanejada.reversed()
    // Desvio: no meio da ida o veículo sobe ~550 m para o norte e depois volta para o caminho.
    private val idaComDesvio = listOf(
        cd,
        GeoPoint(cd.latitude, cd.longitude + 0.008),
        GeoPoint(cd.latitude + 0.005, cd.longitude + 0.009),
        GeoPoint(cd.latitude + 0.005, cd.longitude + 0.012),
        GeoPoint(cd.latitude, cd.longitude + 0.013),
        cliente
    )

    @Test
    fun `decodifica o exemplo da documentacao do Google`() {
        val pontos = PolylineCodec.decodificar("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        assertEquals(3, pontos.size)
        assertEquals(38.5, pontos[0].latitude, 1e-5)
        assertEquals(-120.2, pontos[0].longitude, 1e-5)
        assertEquals(43.252, pontos[2].latitude, 1e-5)
    }

    @Test
    fun `codificar e decodificar devolvem a mesma rota`() {
        val volta = PolylineCodec.decodificar(PolylineCodec.codificar(idaComDesvio))
        assertEquals(idaComDesvio.size, volta.size)
        idaComDesvio.zip(volta).forEach { (a, b) -> assertTrue(a.distanceToAsDouble(b) < 2) }
    }

    @Test
    fun `distancia ate a rota e zero sobre a linha e cresce ao se afastar`() {
        val sobreALinha = GeoPoint(cd.latitude, cd.longitude + 0.005)
        assertTrue(distanciaAoTrajeto(sobreALinha, idaPlanejada) < 1)
        val aoNorte = GeoPoint(cd.latitude + 0.001, cd.longitude + 0.005) // ~110 m acima
        assertEquals(110.5, distanciaAoTrajeto(aoNorte, idaPlanejada), 5.0)
    }

    @Test
    fun `ponto de desvio fica afastado da rota pelo valor pedido`() {
        val via = pontoDeDesvio(idaPlanejada, 0.5, 400.0)
        assertEquals(400.0, distanciaAoTrajeto(via, idaPlanejada), 10.0)
    }

    @Test
    fun `viagem sem desvio nunca fica fora da rota e segue a ordem das etapas`() {
        val v = montarViagem("Cliente", "Rua X", idaPlanejada, idaPlanejada, volta, 40, 40)
        assertFalse(v.passos.any { it.foraDaRota })
        assertEquals(
            listOf(EtapaViagem.IDA, EtapaViagem.ENTREGA, EtapaViagem.VOLTA, EtapaViagem.DESCARGA),
            v.passos.map { it.etapa }.distinct()
        )
        v.passos.map { GeoPoint(it.telemetria.latitude, it.telemetria.longitude) }.zipWithNext()
            .forEach { (a, b) -> assertTrue(a.distanceToAsDouble(b) <= 250) }
    }

    @Test
    fun `viagem com desvio marca o trecho fora da rota so no meio da ida`() {
        val v = montarViagem("Cliente", "Rua X", idaPlanejada, idaComDesvio, volta, 40, 40)
        val fora = v.passos.filter { it.foraDaRota }
        assertTrue("deveria detectar o desvio", fora.isNotEmpty())
        assertTrue(fora.all { it.etapa == EtapaViagem.IDA })
        assertFalse(v.passos.first().foraDaRota)
        assertEquals(1, v.trechosForaDaRota(v.passos.lastIndex).size)
    }

    @Test
    fun `entrega so movimenta o veiculo entre o inicio e o fim`() {
        val inicio = 1_000_000L * SimuladorFrota.INTERVALO_MS
        val viagem = montarViagem("Cliente", "Rua X", idaPlanejada, idaComDesvio, volta, 40, 40)
        val entrega = EntregaEntity(
            id = 7,
            placa = "TST1A23",
            clienteNome = "Cliente",
            endereco = "Rua X",
            latitude = cliente.latitude,
            longitude = cliente.longitude,
            rotaIda = PolylineCodec.codificar(idaPlanejada),
            rotaVolta = PolylineCodec.codificar(volta),
            rotaIdaExecutada = PolylineCodec.codificar(idaComDesvio),
            kmhIda = 40,
            kmhVolta = 40,
            comDesvio = true,
            inicioEm = inicio,
            fimPrevistoEm = inicio + viagem.duracaoMs
        )
        val passoInicial = inicio / SimuladorFrota.INTERVALO_MS
        assertNull(SimuladorFrota.situacao(entrega, passoInicial - 1))
        val saindo = SimuladorFrota.situacao(entrega, passoInicial)
        assertNotNull(saindo)
        assertEquals(EtapaViagem.IDA, saindo!!.etapa)
        assertEquals(7L, saindo.entregaId)
        assertNull(SimuladorFrota.situacao(entrega, passoInicial + viagem.passos.size))

        assertEquals(SituacaoEntrega.AGENDADA, entrega.situacao(inicio - 1))
        assertEquals(SituacaoEntrega.EM_ANDAMENTO, entrega.situacao(inicio))
        assertEquals(SituacaoEntrega.CONCLUIDA, entrega.situacao(entrega.fimPrevistoEm))
        assertEquals(SituacaoEntrega.CANCELADA, entrega.copy(canceladaEm = inicio).situacao(inicio + 1))
        // Em algum momento da ida o veículo aparece fora da rota.
        assertTrue((0 until viagem.passos.size).any { SimuladorFrota.situacao(entrega, passoInicial + it)?.foraDaRota == true })
    }

    private fun entrega(id: Long, inicio: Long, ida: List<GeoPoint> = idaPlanejada): EntregaEntity {
        val v = montarViagem("Cliente $id", "Rua $id", ida, ida, volta, 40, 40)
        return EntregaEntity(
            id = id, placa = "TST1A23", clienteNome = "Cliente $id", endereco = "Rua $id",
            latitude = ida.last().latitude, longitude = ida.last().longitude,
            rotaIda = PolylineCodec.codificar(ida), rotaVolta = PolylineCodec.codificar(volta),
            rotaIdaExecutada = PolylineCodec.codificar(ida), kmhIda = 40, kmhVolta = 40,
            inicioEm = inicio, fimPrevistoEm = inicio + v.duracaoMs
        )
    }

    @Test
    fun `sem entrega na fila a nova sai do CD no proximo passo`() {
        val agora = 5_000_000L * SimuladorFrota.INTERVALO_MS + 1000
        val p = encadearEntrega(emptyList(), agora)
        assertEquals((agora / SimuladorFrota.INTERVALO_MS + 1) * SimuladorFrota.INTERVALO_MS, p.inicio)
        assertEquals(NOME_CD, p.origemNome)
        assertNull(p.fimDaAnterior)
    }

    @Test
    fun `nova entrega sai do cliente da anterior quando ela termina a parada`() {
        val inicio = 5_000_000L * SimuladorFrota.INTERVALO_MS
        val anterior = entrega(1, inicio)
        val v = viagemDaEntrega(anterior)
        val fimDaParada = inicio + v.inicioDaEtapa(EtapaViagem.VOLTA)!! * SimuladorFrota.INTERVALO_MS
        // Pedido enquanto a anterior ainda vai para o cliente.
        val p = encadearEntrega(listOf(anterior), inicio + SimuladorFrota.INTERVALO_MS * 2)
        assertEquals(fimDaParada, p.inicio)
        assertEquals(fimDaParada, p.fimDaAnterior)
        assertEquals("Cliente 1", p.origemNome)
        assertTrue(p.origem.distanceToAsDouble(cliente) < 5)
        // A anterior, cortada em fimDaParada, não aparece mais na rua depois disso (não volta ao CD).
        val cortada = anterior.copy(fimPrevistoEm = p.fimDaAnterior!!)
        assertNull(SimuladorFrota.situacao(cortada, fimDaParada / SimuladorFrota.INTERVALO_MS))
        assertNotNull(SimuladorFrota.situacao(cortada, fimDaParada / SimuladorFrota.INTERVALO_MS - 1))
    }

    @Test
    fun `se a anterior ja esta voltando a nova sai de onde o veiculo esta`() {
        val inicio = 5_000_000L * SimuladorFrota.INTERVALO_MS
        val anterior = entrega(1, inicio)
        val v = viagemDaEntrega(anterior)
        val noMeioDaVolta = inicio + (v.inicioDaEtapa(EtapaViagem.VOLTA)!! + 5) * SimuladorFrota.INTERVALO_MS + 500
        val p = encadearEntrega(listOf(anterior), noMeioDaVolta)
        assertEquals((noMeioDaVolta / SimuladorFrota.INTERVALO_MS + 1) * SimuladorFrota.INTERVALO_MS, p.inicio)
        assertTrue(p.origem.distanceToAsDouble(cliente) > 50) // já saiu do cliente
        assertTrue(p.origemNome.startsWith("posição de"))
    }

    @Test
    fun `cancelada no meio da ida volta ao CD a partir de onde estava`() {
        val inicio = 5_000_000L * SimuladorFrota.INTERVALO_MS
        val original = entrega(1, inicio)
        val v = viagemDaEntrega(original)
        val meioDaIda = v.inicioDaEtapa(EtapaViagem.ENTREGA)!! / 2
        val canceladaEm = inicio + meioDaIda * SimuladorFrota.INTERVALO_MS + 500
        val onde = v.passos[meioDaIda].telemetria
        val aqui = GeoPoint(onde.latitude, onde.longitude)
        // Retorno sintético: da posição do cancelamento direto para o CD.
        val retorno = listOf(aqui, cd)
        val cancelada = original.copy(canceladaEm = canceladaEm, rotaRetorno = PolylineCodec.codificar(retorno))
        val vc = viagemDaEntrega(cancelada)
        val comFim = cancelada.copy(fimPrevistoEm = inicio + vc.duracaoMs)

        assertEquals(meioDaIda + 1, passoDoCancelamento(cancelada))
        // Até o cancelamento, o mesmo caminho; nunca chega ao cliente.
        assertNull(vc.inicioDaEtapa(EtapaViagem.ENTREGA))
        val primeiroDaVolta = vc.passos[meioDaIda + 1]
        assertEquals(EtapaViagem.VOLTA, primeiroDaVolta.etapa)
        // Sai de perto de onde parou (não "teleporta" para o CD).
        assertTrue(GeoPoint(primeiroDaVolta.telemetria.latitude, primeiroDaVolta.telemetria.longitude).distanceToAsDouble(aqui) < 200)
        assertEquals(EtapaViagem.DESCARGA, vc.passos.last().etapa)
        assertTrue(GeoPoint(vc.passos.last().telemetria.latitude, vc.passos.last().telemetria.longitude).distanceToAsDouble(cd) < 5)

        // Depois de cancelar, ainda está na rua (voltando) e segue ocupando o veículo.
        val depois = canceladaEm + 10 * SimuladorFrota.INTERVALO_MS
        assertEquals(SituacaoEntrega.CANCELADA, comFim.situacao(depois))
        assertTrue(comFim.voltandoAposCancelar(depois))
        assertTrue(comFim.ativa(depois))
        assertEquals(EtapaViagem.VOLTA, SimuladorFrota.situacao(comFim, depois / SimuladorFrota.INTERVALO_MS)!!.etapa)
        assertFalse(comFim.naRua(comFim.fimPrevistoEm))
    }

    @Test
    fun `cancelada ja voltando ao CD segue a volta normal`() {
        val inicio = 5_000_000L * SimuladorFrota.INTERVALO_MS
        val original = entrega(1, inicio)
        val v = viagemDaEntrega(original)
        val naVolta = inicio + (v.inicioDaEtapa(EtapaViagem.VOLTA)!! + 3) * SimuladorFrota.INTERVALO_MS
        val vc = viagemDaEntrega(original.copy(canceladaEm = naVolta, rotaRetorno = original.rotaVolta))
        assertEquals(v.passos.size, vc.passos.size)
    }

    @Test
    fun `cancelada antes de sair nao volta para a rua`() {
        val inicio = 5_000_000L * SimuladorFrota.INTERVALO_MS
        val naFila = entrega(1, inicio).copy(canceladaEm = inicio - 1000)
        assertFalse(naFila.ativa(inicio + 1000))
        assertFalse(naFila.naRua(inicio + 1000))
    }
}
