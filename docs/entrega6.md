# Entrega 6 (Semana 13, 21/10/2026): geração de rotas

> Cronograma: "Geração de rotas (projeto #1 e projeto #2)."
> Especificação (Motorista e Controlador): "Geração de rota da localização atual até um ponto indicado no mapa (esse ponto pode ser a localização de um veículo da frota ou um destino de entrega, por exemplo)."

## 1. O que foi feito

### PoC (feita antes de integrar)
| PoC | O que prova |
|---|---|
| `pocs/e6-rotas/rafael` | Localização atual com `FusedLocationProviderClient` e permissão em tempo de execução; destino com toque longo no mapa (`MapEventsOverlay`); rota por ruas no **OSRM**, desenhada como `Polyline`, com distância e tempo estimado. |

### No app
| Arquivo | Papel |
|---|---|
| `rota/RotaService.kt` | Chama o OSRM (`/route/v1/driving`), lê o JSON e devolve `Rota(pontos, distância, duração)`. Textos prontos: "2,9 km", "6 min". |
| `rota/LocalizacaoAtual.kt` | Permissões de localização, `localizacaoAtual(context)` pelo FusedLocationProvider e o `CENTRO_DISTRIBUICAO` (origem alternativa). |
| `ui/viewmodel/RotaViewModel.kt` | Estado da rota: destino, origem (GPS ou centro de distribuição), cálculo, erro e aviso. Um pedido novo cancela o anterior. |
| `ui/screens/MapaFrotaScreen.kt` | Linha azul da rota, marcador de origem e de destino, toque longo para escolher o destino, botão **Traçar rota até este veículo** no painel e card com distância, tempo e **Recalcular**. |
| `app/build.gradle.kts`, `AndroidManifest.xml` | `play-services-location` e as permissões `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`. |

Vale para os dois perfis: o controlador traça rota até qualquer veículo da frota, e o motorista até o próprio veículo. Os dois podem escolher um destino de entrega no mapa.

## 2. Como funciona

```
Toque longo no mapa ──────────────┐
Botão "Traçar rota até o veículo" ┴─> DestinoRota ─> RotaViewModel.tracar()
                                                       │
                       1. localizacaoAtual()  (GPS; sem permissão ou longe demais → centro de distribuição)
                       2. RotaService.calcular(origem, destino)   ── HTTPS ──> router.project-osrm.org
                       3. EstadoRota(rota = pontos + distância + duração)
                                                       │
                                     MapaFrotaScreen: Polyline + marcadores + CardRota
```

- **Por que OSRM:** o mapa já é OpenStreetMap (osmdroid), e o OSRM é o serviço de rotas desse ecossistema. Não precisa de API key nem de cartão. A Directions API do Google exige faturamento.
- **Formato da resposta:** as coordenadas vêm como `[longitude, latitude]`. O código inverte a ordem para `GeoPoint(lat, lon)`.
- **Rede fora da main thread:** `withContext(Dispatchers.IO)` com `HttpURLConnection` e `org.json`, sem biblioteca extra.
- **Permissão:** pedida só quando o usuário pede a primeira rota. Se ele negar, a rota sai do centro de distribuição e o card explica o motivo.
- **Emulador:** o emulador fica na Califórnia por padrão. Se a localização estiver a mais de 300 km do destino, a rota sai do centro de distribuição e o card mostra a distância real. Assim a demonstração não quebra.
- **Destino em movimento:** o veículo anda a cada 3 s. A rota é calculada até a posição dele no momento do pedido, e **Recalcular** pega a posição atual.
- **A rota não vai para o Room nem para o Firestore.** É um cálculo na hora, a partir de duas posições que mudam. Os dados persistentes continuam com o cache local e a sincronização das entregas anteriores.

## 3. Como testar

Pré-requisitos: emulador **com Play Store** e **com internet**, pelo menos 1 veículo cadastrado.

**Antes:** nos *Extended Controls* do emulador (`...`) > *Location*, procure "Araraquara" e clique em *Set location*. Sem isso o app usa o centro de distribuição como origem, o que também funciona.

**PoC**
1. Abrir `pocs/e6-rotas/rafael` no Android Studio e rodar.
2. Aceitar a permissão de localização. O marcador de origem aparece.
3. Tocar e segurar numa rua. Aparecem o destino, a linha azul pelas ruas e "X km • Y min".
4. Outro toque longo troca o destino. **Centro dist.** troca a origem. **Limpar** apaga a rota.

**App como controlador**
1. Abrir **Mapa da frota** e tocar num veículo. No painel, tocar em **Traçar rota até este veículo**.
2. Na primeira vez, o app pede a permissão de localização.
3. O mapa enquadra a rota, e o card mostra a distância, o tempo e de onde a rota sai.
4. Esperar o veículo andar e tocar em **Recalcular**. A rota vai até a posição nova.
5. Tocar e segurar num ponto do mapa. A rota passa a ir até esse destino de entrega (marcador vermelho).
6. O **X** do card limpa a rota.
7. Desligar a internet e pedir uma rota. Aparece "Sem conexão com o servidor de rotas" e o app não trava.

**App como motorista**
1. **Ver meu veículo no mapa** > **Traçar rota até este veículo**.
2. Tocar e segurar no mapa para traçar a rota até um destino de entrega.

## 4. Roteiro de apresentação para o professor (cerca de 5 minutos)

1. **Contexto (30 s):** "A Entrega 6 pede a geração de rota da localização atual até um ponto do mapa, que pode ser um veículo ou um destino de entrega, para os dois perfis."
2. **PoC (1 min):** rodar `pocs/e6-rotas/rafael`, aceitar a permissão, fazer um toque longo e mostrar a rota por ruas com km e minutos. Dizer: "Antes de mexer no app, provamos isoladamente as três peças: localização, escolha do destino e a chamada ao OSRM."
3. **App como controlador (2 min):** traçar rota até um veículo, mostrar o card, **Recalcular** depois que o veículo andou e depois a rota até um ponto escolhido com toque longo.
4. **Código (1 min):** mostrar `RotaService.calcular` (URL, inversão lon/lat) e `RotaViewModel.tracar` (GPS ou centro de distribuição, cancelamento do pedido anterior).
5. **Motorista (30 s):** entrar como motorista e traçar a rota até o próprio veículo.

### Perguntas prováveis
- **"Que serviço calcula a rota?"** O OSRM, servidor público do projeto, que usa os dados do OpenStreetMap. Não precisa de API key.
- **"Por que não Google Directions?"** O manual permite os dois. O Google exige faturamento ativo, e o nosso mapa já é OpenStreetMap.
- **"Como vocês pegam a localização atual?"** Com o `FusedLocationProviderClient.getCurrentLocation`. Se não vier leitura nova, usamos a `lastLocation`. A permissão é pedida em tempo de execução, só quando o usuário pede a rota.
- **"E se o usuário negar a permissão?"** A rota sai do centro de distribuição e o card avisa. A funcionalidade não trava.
- **"A rota acompanha o veículo?"** Ela é calculada até a posição do momento. O botão **Recalcular** usa a posição atual. Recalcular sozinho a cada 3 s faria muitas chamadas ao servidor público, que tem política de uso justo.
- **"Por que a rota não é salva?"** Porque ela depende de duas posições que mudam o tempo todo. Salvar não traria ganho, e o manual pede cache local só para os dados do domínio.
- **"Como o app lida com falta de internet?"** O pedido tem timeout, e o erro vira uma mensagem no card ("Sem conexão com o servidor de rotas").
- **"E a Entrega 7 (reverse geocoding)?"** O card de rota e o painel do veículo já têm as coordenadas. Falta trocar as coordenadas pelo endereço, com o Nominatim (OpenStreetMap) ou o Geoapify.

## 5. Limitações conhecidas
- O servidor público do OSRM é para demonstração: sem garantia de disponibilidade e com limite de uso justo (cerca de 1 pedido por segundo). Para produção, seria preciso hospedar o próprio OSRM.
- O veículo simulado anda em linha reta entre os pontos da rota pré-determinada, então às vezes está fora de uma rua. O OSRM liga o ponto à rua mais próxima e a rota funciona igual.
- Sem navegação curva a curva: o app mostra o trajeto, a distância e o tempo, que é o que a entrega pede.
