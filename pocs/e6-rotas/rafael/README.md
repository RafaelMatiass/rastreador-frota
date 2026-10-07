# PoC Geração de Rotas - Rafael

PoC isolada da Entrega 6 (Semana 13).

## Objetivo

Provar, antes de integrar ao app, as três peças da "geração de rota da localização atual até um ponto indicado no mapa":

1. **Localização atual** do aparelho com o `FusedLocationProviderClient` (Google Play Services), pedindo a permissão em tempo de execução.
2. **Escolha do destino** com um toque longo no mapa (`MapEventsOverlay` do osmdroid).
3. **Cálculo da rota por ruas** no **OSRM** (Open Source Routing Machine), que usa os dados do OpenStreetMap. O app recebe o desenho da rota, a distância e o tempo estimado e desenha uma `Polyline`.

## Pesquisa

- Localização atual: https://developer.android.com/develop/sensors-and-location/location/retrieve-current
- Permissões em tempo de execução: https://developer.android.com/training/permissions/requesting
- API do OSRM (`/route/v1`): https://project-osrm.org/docs/v5.24.0/api/#route-service
- Política de uso do servidor público do OSRM: https://github.com/Project-OSRM/osrm-backend/wiki/Api-usage-policy
- `Polyline` e `MapEventsOverlay` no osmdroid: https://github.com/osmdroid/osmdroid/wiki

## Por que OSRM

O manual permite Google Maps ou OpenStreetMap. Já usamos OpenStreetMap (osmdroid) desde a Entrega 5. O OSRM é o serviço de rotas do mesmo ecossistema: não exige API key nem cartão de crédito e responde em JSON simples. A Directions API do Google exige faturamento ativo.

## Como funciona (`OsrmClient.kt`)

```
GET https://router.project-osrm.org/route/v1/driving/{lonA},{latA};{lonB},{latB}?overview=full&geometries=geojson
```

Resposta (resumida):

```json
{ "code": "Ok",
  "routes": [ { "distance": 3120.4, "duration": 412.7,
                "geometry": { "coordinates": [[-48.1756, -21.7946], ...] } } ] }
```

- As coordenadas vêm como **[longitude, latitude]**, ao contrário do `GeoPoint(lat, lon)`. Trocar a ordem é o erro mais comum.
- A chamada roda em `Dispatchers.IO` com `HttpURLConnection` + `org.json`, sem biblioteca extra.
- Um toque novo no mapa cancela o pedido anterior (`Job.cancel()`), e o pedido cancelado não sobrescreve o estado.

## Como rodar

1. Abra **esta pasta** (`pocs/e6-rotas/rafael`) no Android Studio.
2. Rode no emulador **com Play Store** e **com internet**.
3. **Localização do emulador:** por padrão o emulador fica na Califórnia, e não há rota por ruas até o Brasil. Antes de testar, abra os *Extended Controls* (`...`) > *Location*, procure "Araraquara" e clique em *Set location*. Outra opção é usar o botão **Centro dist.**, que põe a origem num ponto fixo em Araraquara.
4. O que observar:
   - Ao abrir, o app pede a permissão de localização. Concedida, o marcador de origem vai para a sua posição.
   - Toque e segure em qualquer rua: aparece o destino, a linha azul seguindo as ruas e o texto "X km • Y min".
   - Outro toque longo troca o destino e recalcula.
   - **Limpar** apaga a rota. Sem internet, aparece a mensagem de erro em vez de travar.

## O que foi levado para o app

- `OsrmClient` virou `app/.../rota/RotaService.kt`.
- A obtenção da localização virou `app/.../rota/LocalizacaoAtual.kt`.
- O estado da rota (origem, destino, cálculo, erro) virou `RotaViewModel` no app, e o mapa da frota ganhou a `Polyline`, o toque longo e o botão "Traçar rota até este veículo".
