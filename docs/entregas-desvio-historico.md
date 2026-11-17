# Entregas atribuídas, desvio de rota e histórico de trajeto

Três funcionalidades que completam o propósito do rastreador. Entraram antes do code freeze de 18/11/2026.

1. **O gestor atribui entregas, inclusive várias em fila.** O veículo só sai do CD quando recebe uma entrega e vai de um cliente direto ao próximo. As 4 viagens fixas das Entregas 5 e 6 foram removidas.
2. **Detecção de desvio de rota.** O app compara a posição do veículo com a rota planejada e avisa quando ele sai e quando volta.
3. **Histórico de trajeto.** Cada entrega guarda a rota planejada e permite ver por onde o veículo passou, com os trechos fora da rota.

## 1. Como funciona

### Atribuir uma entrega (gestor)
**Frota** → veículo → **Atribuir entrega**. Então:
- o gestor escolhe o destino pela busca de endereço ou arrastando o mapa;
- informa o cliente;
- pode marcar **Simular desvio de rota**.

Ao confirmar:
1. o app pede ao **OSRM**, uma única vez, as rotas CD → cliente e cliente → CD;
2. tudo é salvo na entrega: as rotas codificadas como polyline, a velocidade média e o **horário de saída**;
3. a entrega grava no Room e sobe para a coleção `entregas` do Firestore (mesmo padrão das cercas);
4. o veículo sai do CD no próximo passo da simulação.

Regras:
- **Só o veículo aprovado e com motorista ativo** pode receber uma entrega.
- **Sem entrega, o veículo fica no pátio do CD** ("Aguardando entrega"). Sem motorista, fica "Sem motorista".

### Fila de entregas
O gestor pode atribuir várias entregas ao mesmo veículo. A partir da segunda, o botão vira **"Adicionar à fila"**. A regra fica em `simulacao/Encadeamento.kt`, `encadearEntrega`:
- **A nova entrega continua de onde a última da fila para.** Ela sai **do cliente anterior**, assim que a parada lá termina, e só a última da fila volta ao CD. A volta ao CD da anterior é cortada: o fim dela passa a ser o início da próxima.
- **Se a última já está voltando ao CD,** a nova sai de onde o veículo estiver no próximo passo.
- **Enquanto espera,** a entrega aparece como **"Na fila"**. O editor mostra "Entra na fila: sai de Mercado A às 07:35".
- **No histórico,** cada entrega mostra de onde saiu e, se foi encadeada, "Seguiu para a próxima" no lugar da volta ao CD.

### Cancelar
O cancelamento vale **só para aquela entrega**; as próximas da fila continuam, e a fila é replanejada:
- **Cancelou a que estava em andamento:** a próxima sai **de onde o veículo está agora**, com uma rota nova gerada no OSRM.
- **Cancelou uma do meio da fila:** a seguinte passa a sair do cliente anterior a ela, com rota nova, e os horários são recalculados.
- **Cancelou a última:** a anterior recupera a volta ao CD.
- **Sem outra entrega na fila,** o veículo **volta ao CD a partir de onde estava** no cancelamento:
  - indo para o cliente: o app pede ao OSRM uma rota nova, da posição atual até o CD;
  - parado no cliente: usa a volta planejada;
  - já voltando ao CD: segue a volta normal.
  A rota de retorno fica na entrega (`rotaRetorno`, banco v8) e é sincronizada, então todos os aparelhos veem o mesmo caminho. No stepper, a etapa "Entrega" aparece com um X vermelho ("Cancelada").
- **Sem internet,** quando o replanejamento precisa de uma rota nova, nada é alterado e o gestor é avisado. A fila nunca fica pela metade.

### Por que todos os aparelhos veem o mesmo
A posição é calculada a partir do relógio: `passo da viagem = (agora − saída da entrega) / 3 s`. Como as rotas e o horário de saída estão na entrega sincronizada, o gestor e o motorista calculam o mesmo ponto sem trocar mensagens. A simulação continua sem internet; a rede só é necessária na hora de atribuir a entrega, para gerar a rota.

### Desvio de rota
- **Detecção** (`simulacao/Trajeto.kt`, `distanciaAoTrajeto`): a cada passo, a menor distância entre o veículo e a linha da rota **planejada**. **Acima de 60 m, o veículo está fora da rota.**
  - O cálculo projeta os pontos num plano em metros e mede a distância a cada segmento da linha.
  - A detecção só compara geometria: não sabe se o desvio foi simulado, então funcionaria igual com um GPS real.
- **Alerta** (`geofence/GeofenceMonitor.kt`): na mudança de estado, gera "UAT1A23 **saiu da** rota para Cliente X" e "**voltou à** rota para Cliente X". Eles aparecem na aba Alertas, na notificação e no item **"fora da rota planejada"** de "Precisa de atenção", no Início do gestor.
- **No mapa:** a linha do veículo fica **vermelha** quando ele está fora da rota, e a rota planejada aparece em **cinza tracejado**.
- **Simulação do desvio (demonstração):** com "Simular desvio" marcado, o app escolhe um ponto a 45% da ida, afastado 400 m da rua, e pede ao OSRM a rota CD → ponto → cliente. O veículo percorre essa rota, que sai do caminho planejado. Se o ponto não gerar um desvio de verdade, o app tenta o outro lado e 700 m.

### Histórico
**Frota** → veículo → seção **Entregas** → toque numa entrega. A tela mostra:
- **mapa:** a rota planejada (cinza tracejado), o caminho **percorrido** (azul), os **trechos fora da rota** (vermelho) e o veículo, se a entrega estiver em andamento;
- **desvios:** quantidade e distância percorrida fora da rota;
- **linha do tempo:** atribuída, saída do CD, chegada ao cliente e volta ao CD (ou cancelamento);
- **detalhes:** veículo, motorista, distância planejada e distância percorrida.

O histórico é reconstruído a partir da entrega guardada (rotas + horário de saída). Não é preciso gravar um ponto a cada 3 s, o que gastaria a cota do Firestore.

## 2. Arquivos

| Arquivo | Papel |
|---|---|
| `data/local/entity/EntregaEntity.kt`, `dao/EntregaDao.kt` | A entrega e a situação dela (agendada, em andamento, concluída, cancelada). Banco local v8 (v6 cria a tabela, v7 adiciona a origem da fila, v8 a rota de retorno após cancelar), com migração sem perda de dados. |
| `simulacao/RotasSimuladas.kt` | `montarViagem` (ida → entrega → volta → descarga) e o cálculo de "fora da rota" em cada passo. As viagens fixas e o `TrajetosRuas.kt` foram removidos. |
| `simulacao/SimuladorFrota.kt` | `situacao(entrega, passo)`, `aguardandoEntrega`, `foraDeOperacao`. |
| `simulacao/Trajeto.kt`, `PolylineCodec.kt` | `distanciaAoTrajeto`, `pontoDeDesvio` e `codificar` a polyline. |
| `rota/RotaService.kt` | `calcularPor(pontos)`: rota com ponto intermediário (desvio). |
| `data/repository/FrotaAoVivoRepository.kt` | Junta veículos, motoristas, **entregas** e o relógio. |
| `geofence/GeofenceMonitor.kt` | Alertas de desvio e de volta à rota. |
| `data/sync/FirestoreSyncManager.kt` | Sincroniza a coleção `entregas`. O gestor envia e recebe; o motorista só recebe. |
| `ui/screens/EntregaEditorScreen.kt`, `HistoricoEntregaScreen.kt`, `ui/viewmodel/EntregaViewModel.kt` | Telas de atribuir e de histórico. |
| `firestore.rules` | `entregas`: todos os usuários logados leem; só o gestor escreve. |

## 3. Como testar

**Antes de testar:** publicar o `firestore.rules` atualizado (veja o README). Sem isso, a entrega funciona no aparelho, mas a sincronização falha com "Falha ao sincronizar".

1. **Como gestor:** abrir **Frota** → um veículo com motorista → **Atribuir entrega**.
2. Buscar um endereço (ex.: "Avenida Barroso 627"), informar o cliente, ligar **Simular desvio de rota** e tocar em **Atribuir entrega**.
3. A tela de histórico abre com "Em andamento" e os horários previstos.
   - **Fila:** volte à ficha e toque em **Adicionar à fila** com outro endereço. A nova fica "Na fila", saindo do primeiro cliente.
   - **Cancelar a primeira:** a segunda passa na hora a "Em andamento", saindo de onde o veículo estava.
4. Em cerca de 2 minutos, no meio da ida, chega a notificação **"Desvio de rota · placa"** e, logo depois, **"De volta à rota · placa"**.
5. No histórico aparecem "1 desvio(s) da rota planejada" e o trecho vermelho no mapa. No mapa da frota, a linha do veículo fica vermelha durante o desvio.
6. **Como motorista:** o Início mostra a viagem atual. Sem entrega, mostra "Aguardando entrega".
7. **Cancelar:** no histórico, **Cancelar entrega**. A entrega fica como "Cancelada"; se houver outra na fila, o veículo segue para ela, e se não houver, volta ao CD saindo de onde estava.

Testado no emulador em 17/11/2026:
- **Desvio:** entrega CD → Av. Barroso (12 km ida e volta), desvio detectado às 07:15:56, volta à rota às 07:16:39, 1,1 km fora da rota.
- **Cancelar no meio da ida (sem fila):** entrega CD → Av. Barroso cancelada às 16:51:12, com 2,3 km percorridos. O caminhão fez a volta dali até o CD (chegada prevista às 16:53), e o histórico mostra "Não chegou" e "Volta prevista ao CD".
- **Fila e cancelamento:** "Mercado A" (Av. Barroso) e "Mercado B" (Av. Queiroz Filho) na fila. A B ficou "Na fila", saindo do Mercado A às 07:35. A A foi cancelada às 07:32:18, e a B passou a "Em andamento" às 07:32, saindo da posição do veículo com rota nova.

## 4. Perguntas prováveis
- **"Quem decide para onde o veículo vai?"** O gestor, ao atribuir a entrega. Sem entrega, o veículo fica no CD.
- **"Como o app sabe que o veículo desviou?"** Mede a distância entre a posição e a linha da rota planejada. Acima de 60 m é desvio. É geometria pura, então funcionaria com um GPS real.
- **"O desvio é de mentira?"** O desvio do trajeto é simulado, porque não há veículo real. Mas a detecção é real: ela não sabe que é simulado, só compara as linhas.
- **"Onde fica o histórico?"** Na própria entrega, no Room e no Firestore. A rota planejada e o horário de saída bastam para reconstruir o percurso a qualquer momento, sem gravar um ponto a cada 3 s.
- **"E sem internet?"** A rota é gerada na hora de atribuir, o que exige internet. Depois disso, a simulação, o desvio e o histórico funcionam offline.

## 5. Limitações
- **A ordem da fila é a ordem de atribuição.** Não dá para reordenar as entregas.
- **O desvio é detectado nos veículos simulados.** Para o GPS real do motorista, seria preciso comparar a localização do aparelho com a rota no próprio celular.
