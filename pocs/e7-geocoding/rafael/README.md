# PoC Reverse Geocoding - Rafael

PoC isolada da Entrega 7 (Semana 15).

## Objetivo

Provar, antes de integrar ao app, a "recuperação de endereços a partir das coordenadas (reverse geocoding API)":

1. Tocar num ponto do mapa e obter a coordenada (`MapEventsOverlay` do osmdroid).
2. Enviar a coordenada à **Reverse Geocoding API da Geoapify**, a API citada no manual da disciplina, que usa os dados do OpenStreetMap.
3. Mostrar o endereço formatado e as partes: rua, número, bairro, cidade, estado e CEP.
4. Usar **cache por coordenada arredondada**, para não gastar requisição repetida no mesmo lugar.

## Pesquisa

- Geoapify Reverse Geocoding API: https://apidocs.geoapify.com/docs/geocoding/reverse-geocoding/
- Página citada no manual: https://www.geoapify.com/openstreetmap-geocoding/
- Planos (gratuito: 3.000 requisições/dia): https://www.geoapify.com/pricing/

## Chave da API (obrigatória)

1. Crie uma conta gratuita em https://myprojects.geoapify.com, crie um projeto e copie a **API key**.
2. Em `pocs/e7-geocoding/rafael/local.properties`, acrescente:
   ```
   GEOAPIFY_API_KEY=sua_chave_aqui
   ```
3. O `local.properties` está no `.gitignore`, então a chave **não vai para o GitHub**. O Gradle copia a chave para `BuildConfig.GEOAPIFY_API_KEY` na compilação.

Sem a chave, a PoC mostra "Chave da Geoapify ausente" em vez de travar.

## Como funciona

```
toque no mapa ─> GeoPoint(lat, lon)
                    │
                    ├─ está no cache (lat/lon com 4 casas ≈ 11 m)? ─> mostra na hora ("Fonte: cache")
                    │
                    └─ não ─> GET https://api.geoapify.com/v1/geocode/reverse
                                   ?lat=-21.78829&lon=-48.17792&lang=pt&format=json&apiKey=...
                              ─> results[0]: formatted, address_line1, address_line2,
                                 street, housenumber, suburb, city, state_code, postcode, distance
                              ─> guarda no cache e mostra ("Fonte: API Geoapify (230 ms)")
```

- `GeoapifyClient.kt`: monta a URL e chama a API com `HttpURLConnection` em `Dispatchers.IO`. Lê o JSON com `org.json` e trata os erros (401 chave inválida, 429 limite atingido, sem resultado).
- `GeocodingViewModel.kt`: guarda o estado da tela, o cache e os contadores de requisições e de respostas do cache. Um toque novo cancela a busca anterior.
- `MainActivity.kt`: mapa, marcador no ponto tocado e painel com o endereço.

## Como rodar

1. Configure a chave (acima).
2. Abra **esta pasta** (`pocs/e7-geocoding/rafael`) no Android Studio e rode num emulador **com internet**.
3. O que observar:
   - Toque numa rua de Araraquara. Aparecem "Avenida ..., 1234", o bairro, a cidade, o CEP e "Fonte: API Geoapify (xxx ms)".
   - Toque **de novo no mesmo lugar**. Aparece "Fonte: cache (sem requisição)" e o contador de requisições não aumenta.
   - Toque numa área sem ruas (mato, lago). A API devolve o endereço mais próximo e informa a distância.
   - Sem internet ou sem chave, aparece a mensagem de erro e o app não trava.

## O que vai para o app

- `GeoapifyClient` e o cache, num `GeocodingService` no app.
- O endereço aparece no painel do veículo selecionado, com a posição atual dele, e na origem e no destino das rotas.
- Só o veículo selecionado é consultado, e com cache. Os veículos andam a cada 3 s, e consultar todos a cada passo esgotaria a cota diária.
