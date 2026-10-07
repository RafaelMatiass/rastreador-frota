# Entrega 6 (Semana 13, 21/10/2026): geração de rotas

> Cronograma: "Geração de rotas (projeto #1 e projeto #2)."
> Especificação (Motorista e Controlador): "Geração de rota da localização atual até um ponto indicado no mapa (esse ponto pode ser a localização de um veículo da frota ou um destino de entrega, por exemplo)."

A entrega tem duas partes, que se completam:

1. **Viagens da frota pelas ruas.** Cada veículo faz uma viagem com destino: sai do centro de distribuição (CD), vai até um endereço de entrega **pelas ruas**, entrega, volta e descarrega. O mapa mostra a linha do trajeto que falta, e ela encolhe conforme o veículo chega. O controlador vê para onde cada veículo vai e em quanto tempo chega. O motorista vê a rota do próprio veículo, como no Maps.
2. **Rota sob demanda (texto literal da entrega).** Rota da localização atual do aparelho até um veículo da frota ou até um destino de entrega escolhido no mapa.

## 1. O que foi feito

### PoC (feita antes de integrar)
| PoC | O que prova |
|---|---|
| `pocs/e6-rotas/rafael` | (a) Localização atual (`FusedLocationProviderClient`) com permissão em tempo de execução; (b) destino com toque longo (`MapEventsOverlay`); (c) rota por ruas no **OSRM**, desenhada como `Polyline`, com distância e tempo; (d) botão **Simular viagem**: um veículo percorre a rota em passos de distância fixa **sobre a linha**, e o trajeto restante encolhe. |

### No app
| Arquivo | Papel |
|---|---|
| `simulacao/TrajetosRuas.kt` | Os 8 trajetos por ruas (ida e volta de 4 viagens), gerados **uma vez** no OSRM e guardados como polyline codificada. A simulação não usa a rede. |
| `simulacao/PolylineCodec.kt` | Decodifica a polyline (formato Google/OSRM) em `List<GeoPoint>`. |
| `simulacao/Trajeto.kt` | `amostrar()` anda ao longo do trajeto e marca um ponto a cada N metros; `trajetoRestante()` devolve o que falta (veio da PoC). |
| `simulacao/RotasSimuladas.kt` | Monta cada `Viagem`: **ida** (com semáforos) → **entrega** (motor desligado, portas abertas) → **volta** → **descarga no CD**. Contém o `CENTRO_DISTRIBUICAO`. |
| `simulacao/SimuladorFrota.kt` | `situacao(placa, passo)`: telemetria, etapa, destino, trajeto restante, metros e tempo restantes. |
| `rota/RotaService.kt`, `rota/LocalizacaoAtual.kt` | Rota sob demanda: chamada ao OSRM e localização atual do aparelho. |
| `ui/viewmodel/RotaViewModel.kt` | Estado da rota sob demanda (origem GPS ou CD, cálculo, erro). |
| `ui/viewmodel/MapaViewModel.kt` | `VeiculoNoMapa` passa a trazer a `SituacaoViagem` de cada veículo. |
| `ui/screens/MapaFrotaScreen.kt` | Linha do trajeto restante de cada veículo; marcadores do CD e da entrega; textos "Indo para Rua X · faltam 2,3 km · 4 min"; rota sob demanda em outra cor. |

## 2. Viagens da frota pelas ruas

### Por que mudou desde a Entrega 5
Na Entrega 5, as viagens simuladas eram cerca de 20 pontos ligados em **linha reta** e repetidos em círculo. O veículo atravessava quarteirões, saltava centenas de metros e não tinha destino. Parecia andar "sem rumo".

### Como funciona agora
```
OSRM (uma vez, na implementação) ─> polyline codificada em TrajetosRuas.kt
                                            │ PolylineCodec.decodificar
                                            ▼
              trajeto por ruas (centenas de vértices)
                                            │ amostrar(trajeto, metros por passo)
                                            ▼
  passos: IDA ... (semáforo) ... ENTREGA ×20 ... VOLTA ... DESCARGA ×10   ← sequência pré-determinada
                                            │
  SimuladorFrota.situacao(placa, passo = agora / 3 s)
     → telemetria + etapa + destino + trajetoRestante + metros/segundos restantes
```

- **Ainda é uma "sequência pré-determinada", como pede o manual.** Os trajetos estão fixos no código, e cada passo tem a sua telemetria (velocidade, motor, portas). Nada é aleatório e nada depende de internet.
- **Seguir as ruas:** `amostrar()` caminha pela linha do OSRM e marca um ponto a cada `velocidade × 3 s × ACELERACAO` metros. O ponto sempre cai sobre um segmento da rua.
- **Simulação acelerada (`ACELERACAO = 3`):** uma ida real de 8 a 13 minutos aparece em 3 a 4 minutos na tela. A telemetria mostra a velocidade média real do trajeto, que o OSRM estimou.
- **Linha que encolhe:** cada passo guarda em qual segmento do trajeto ele está. O trajeto restante é o ponto atual mais os vértices seguintes.
- **Tempo restante:** número de passos até o fim da etapa × 3 s, no tempo da tela.
- **Mantido da Entrega 5:** o passo vem do relógio, então dois aparelhos veem o mesmo veículo no mesmo lugar. A viagem e o deslocamento de cada veículo são escolhidos pela placa.

### Os 4 destinos
Rua Papa João Paulo I, Avenida Queiroz Filho, Avenida Barroso e Acesso Rodoviário Abdo Najn, em Araraquara. Todas as viagens saem do CD, perto da Rodovia Washington Luís.

## 3. Rota sob demanda

```
Toque longo no mapa ──────────────┐
Botão "Traçar rota até o veículo" ┴─> RotaViewModel.tracar()
   1. localizacaoAtual()  (GPS; sem permissão ou a mais de 300 km → CD)
   2. RotaService.calcular(origem, destino)  ── HTTPS ──> router.project-osrm.org
   3. Polyline (cor terciária) + card com distância, tempo e "Recalcular"
```
- **Por que OSRM:** é o serviço de rotas do ecossistema OpenStreetMap (o mesmo do mapa) e não exige API key nem cartão. A Directions API do Google exige faturamento.
- **Atenção:** a resposta traz as coordenadas como `[longitude, latitude]`.
- **Permissão:** é pedida só na primeira rota. Se o usuário negar, a rota sai do CD e o card explica o motivo.
- **Emulador** (Califórnia por padrão): a rota sai do CD, com aviso.
- **A rota não é salva** no Room nem no Firestore. É calculada na hora, a partir de posições que mudam.

## 4. Como testar

Pré-requisitos: emulador **com Play Store** e **com internet** (os tiles do mapa e a rota sob demanda vêm da rede), pelo menos 2 veículos cadastrados.

**PoC**
1. Abrir `pocs/e6-rotas/rafael` e rodar. Aceitar a permissão, ou tocar em **Centro dist.**
2. Tocar e segurar numa rua. Aparecem a linha azul pelas ruas e "X km • Y min".
3. Tocar em **Simular viagem**. O veículo anda sobre a rua, a linha verde encolhe e "faltam X m • Y s" diminui até "chegou".

**App como controlador**
1. Abrir **Mapa da frota**. Os veículos andam **sobre as ruas**, cada um com uma linha fina até o destino. O marcador cinza é o CD.
2. A lista mostra, em cada veículo, "Indo para Rua X · 4 min", "Entregando em ...", "Voltando ao CD · ..." e assim por diante.
3. Tocar num veículo:
   - a linha dele fica forte;
   - aparece o marcador vermelho da entrega;
   - o mapa enquadra o veículo e o que falta do trajeto;
   - o painel mostra "Indo para Rua X · faltam 2,3 km · 4 min".
4. Esperar e ver a linha encolher. Ao chegar, o marcador fica âmbar, o painel mostra "Entregando em ... · sai em 1 min" e as portas aparecem abertas. Depois o veículo volta ao CD, por outro trajeto.
5. **Traçar rota até este veículo**: a rota sob demanda aparece em outra cor, a partir do GPS ou do CD. As linhas dos outros veículos somem para não poluir o mapa. O **X** do card limpa a rota.
6. Tocar e segurar num ponto do mapa: rota sob demanda até esse destino de entrega.

**App como motorista**
1. **Ver meu veículo no mapa**: o veículo dele, a linha até a entrega, o marcador da entrega e o tempo restante.
2. **Traçar rota até este veículo**, ou toque longo: a rota do motorista até o veículo ou até outro destino.

## 5. Roteiro de apresentação para o professor (cerca de 5 minutos)

1. **Contexto (30 s):** "A Entrega 6 pede a geração de rota da localização atual até um ponto do mapa, para os dois perfis. Também melhoramos a simulação: agora os veículos fazem viagens com destino, pelas ruas."
2. **PoC (1 min):** rodar a PoC, traçar uma rota com toque longo e tocar em **Simular viagem**. "Provamos isoladamente: a localização, a rota do OSRM e a técnica de andar sobre a linha da rota."
3. **App como controlador (2 min):**
   - mostrar os veículos sobre as ruas com as linhas;
   - selecionar um veículo "indo para" e mostrar a linha encolhendo e o tempo;
   - mostrar outro veículo "entregando";
   - **Traçar rota até este veículo**.
4. **Código (1 min):** `TrajetosRuas.kt` (trajetos fixos), `amostrar()` em `Trajeto.kt`, `SimuladorFrota.situacao()` e `RotaService.calcular()`.
5. **Motorista (30 s):** a rota do próprio veículo até a entrega.

### Perguntas prováveis
- **"A simulação usa internet?"** Não. Os trajetos por ruas foram gerados uma vez com o OSRM e estão no código como polyline codificada. A internet só é usada para os tiles do mapa e para a rota sob demanda.
- **"Isso ainda é uma sequência pré-determinada?"** Sim. A mesma viagem gera sempre os mesmos passos, e cada passo tem a sua telemetria.
- **"Como o veículo segue a rua?"** `amostrar()` anda pelos segmentos da linha do OSRM e marca um ponto a cada N metros. Todo ponto cai sobre a rua.
- **"Como vocês sabem quanto falta?"** Cada passo sabe em qual segmento do trajeto está. O que falta é o ponto atual mais os vértices seguintes. A distância é a soma dos segmentos, e o tempo é o número de passos até o fim da etapa × 3 s.
- **"Por que o tempo é menor que o real?"** A simulação é acelerada 3×, para a demonstração caber na aula. A velocidade da telemetria é a média real estimada pelo OSRM.
- **"Que serviço calcula a rota sob demanda?"** O OSRM, sem API key. O Google exige faturamento, e o nosso mapa já é OpenStreetMap.
- **"E se o usuário negar a permissão de localização?"** A rota sai do CD, com aviso. A funcionalidade não trava.
- **"Por que a rota não é salva?"** Porque depende de posições que mudam o tempo todo. Os dados do domínio continuam no cache local e no Firestore.
- **"E a Entrega 7?"** O painel já tem as coordenadas do veículo e o nome do destino. Falta o endereço da posição atual, por reverse geocoding (Nominatim ou Geoapify).

## 6. Limitações conhecidas
- O servidor público do OSRM, usado na rota sob demanda, é de demonstração: sem garantia de disponibilidade e com limite de uso justo.
- Os destinos das viagens são fixos (4 endereços). Atribuir entregas pelo controlador seria a evolução natural: cadastro de entrega no Room e no Firestore, com rota gerada para o destino escolhido.
- Sem navegação curva a curva: o app mostra trajeto, distância e tempo, que é o que a entrega pede.
