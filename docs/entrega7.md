# Entrega 7 (Semana 15, 04/11/2026): endereços a partir das coordenadas

> Cronograma: "Recuperação de endereços a partir das coordenadas (reverse geocoding API)."
> Especificação (Controlador): "Localização de veículos no mapa, com indicação do endereço fornecido por API de localização (reverse geocoding API)."

## 1. O que foi feito

### PoC (feita antes de integrar)
| PoC | O que prova |
|---|---|
| `pocs/e7-geocoding/rafael` | Tocar no mapa -> coordenada -> **Reverse Geocoding API da Geoapify** -> endereço (rua, número, bairro, cidade, CEP). Mostra se a resposta veio da API (com o tempo em ms) ou do cache, e conta as requisições. |

### No app
| Arquivo | Papel |
|---|---|
| `geocoding/GeocodingService.kt` | Chama a Geoapify, lê o JSON e guarda as respostas em **cache por coordenada arredondada** (4 casas ≈ 11 m). Trata chave ausente ou inválida, limite diário e falta de internet. |
| `ui/viewmodel/EnderecoViewModel.kt` | Acompanha o veículo selecionado e decide **quando** consultar de novo (veja a seção 2). Busca também o endereço da entrega da viagem. |
| `ui/viewmodel/RotaViewModel.kt` | Na rota sob demanda, busca em paralelo o endereço da origem e do destino. |
| `ui/screens/MapaFrotaScreen.kt` | Endereço atual no painel do veículo, endereço completo da entrega e endereços no card da rota. |
| `app/build.gradle.kts` | Lê `GEOAPIFY_API_KEY` do `local.properties` e gera `BuildConfig.GEOAPIFY_API_KEY`. |

### Onde o endereço aparece
- **Painel do veículo** (controlador e motorista): "📍 Avenida Barroso 627 / Araraquara - Sudeste, 14801-160, Brasil".
- **Endereço da entrega** no mesmo painel: "Endereço da entrega: Rua Papa Joao Paulo I 947 – Araraquara · CEP 14807-253".
- **Card da rota sob demanda**: "Saindo de: Sua localização · Rua X 123 – Bairro" e "Destino: ..." quando o destino é um ponto escolhido no mapa.

## 2. Como funciona

```
coordenada (lat, lon)
   │
   ├─ cache (lat/lon com 4 casas)? ── sim ─> endereço na hora, sem requisição
   │
   └─ não ─> GET https://api.geoapify.com/v1/geocode/reverse
                  ?lat=-21.78829&lon=-48.17792&lang=pt&format=json&apiKey=...
             ─> results[0]: address_line1, address_line2, suburb, city, postcode
             ─> guarda no cache ─> tela
```

- **API escolhida:** a Geoapify, citada no manual. Usa dados do OpenStreetMap, como o mapa (osmdroid) e as rotas (OSRM). O plano grátis dá 3.000 requisições por dia.
- **Chave fora do código:** fica em `local.properties`, que está no `.gitignore`. O Gradle a copia para o `BuildConfig`, e ela não vai para o GitHub.
- **Controle de cota.** O veículo anda a cada 3 s, e consultar a API a cada passo gastaria a cota diária em pouco tempo. Por isso:
  1. só o **veículo selecionado** é consultado;
  2. uma nova consulta só acontece se ele **andou 100 m e passaram 15 s** desde a última;
  3. **veículo parado não gera consulta**, porque o endereço já está no cache;
  4. os **destinos de entrega** são fixos, então cada um é consultado uma vez e depois vem do cache.
  
  O pior caso é 4 consultas por minuto com o painel aberto.
- **Falhas:** se a atualização falha, o painel mantém o último endereço conhecido e mostra o motivo. Sem endereço anterior, mostra "Endereço indisponível: ...". O app nunca trava.
- **O endereço não vai para o Room nem para o Firestore.** É derivado da posição, que muda o tempo todo. O cache em memória já evita requisições repetidas.

## 3. Como testar

Pré-requisitos: `GEOAPIFY_API_KEY=...` no `local.properties` da raiz (e no da PoC), emulador com internet e pelo menos 1 veículo cadastrado.

**PoC**
1. Abrir `pocs/e7-geocoding/rafael` e rodar.
2. Tocar numa rua. Aparecem o endereço, o CEP e "Fonte: API Geoapify (xxx ms)".
3. Tocar de novo no mesmo lugar. Aparece "Fonte: cache (sem requisição)", e o contador de requisições não aumenta.

**App**
1. **Mapa da frota** > tocar num veículo. O painel mostra "Buscando endereço..." e, logo depois, a rua e o número.
2. Ver o "Endereço da entrega" com o CEP.
3. Acompanhar por cerca de 30 s um veículo em movimento. O endereço muda conforme ele anda (a cada 15 s, no máximo).
4. Escolher um veículo **entregando** (parado). O endereço é o da entrega e não muda.
5. Tocar e segurar num ponto do mapa. O card da rota mostra o endereço da origem e o do destino.
6. Sem internet, o painel mostra "Sem conexão para buscar o endereço" e o app continua funcionando.
7. Como motorista, **Ver meu veículo no mapa** mostra o endereço atual do próprio veículo.

## 4. Roteiro de apresentação para o professor (cerca de 5 minutos)

1. **Contexto (30 s):** "A Entrega 7 pede o endereço dos veículos a partir das coordenadas, usando uma reverse geocoding API. Usamos a Geoapify, que o manual indica."
2. **PoC (1 min):** tocar em duas ruas e depois repetir o toque no mesmo ponto para mostrar o cache e o contador. "Provamos a chamada à API e a leitura do JSON isoladamente."
3. **App (2 min):** selecionar um veículo em movimento e mostrar o endereço mudando. Depois um veículo entregando (endereço da entrega com CEP). Por fim, a rota sob demanda com os endereços de origem e destino.
4. **Código (1 min):** `GeocodingService` (URL, cache, tratamento de erro) e `EnderecoViewModel.acompanhar` (regra de 100 m e 15 s).
5. **Chave (30 s):** mostrar que a chave está no `local.properties` e não no código nem no GitHub.

### Perguntas prováveis
- **"Que API vocês usam?"** A Reverse Geocoding da Geoapify, citada no manual. Usa dados do OpenStreetMap, e o plano grátis tem 3.000 requisições por dia.
- **"Onde fica a chave?"** No `local.properties`, fora do Git. O Gradle injeta no `BuildConfig` na compilação.
- **"Vocês consultam a API a cada 3 segundos?"** Não. Só o veículo selecionado, só se ele andou 100 m e passaram 15 s, e com cache. Veículo parado não gera consulta.
- **"Por que não mostrar o endereço de todos os veículos na lista?"** Seriam várias consultas a cada passo, e a cota acabaria. O endereço aparece ao selecionar o veículo, que é quando o controlador precisa dele.
- **"E sem internet?"** Fica o último endereço conhecido com um aviso, ou a mensagem "Endereço indisponível". O app não trava.
- **"Por que o endereço não vai para o Firestore?"** Porque é derivado da posição simulada, que muda o tempo todo. Os dados do domínio continuam no Room e no Firestore.
- **"E a Entrega 8?"** Geofencing: avisar quando um veículo entra ou sai de um perímetro. O simulador já fornece a posição a cada passo, então basta comparar com os perímetros e disparar a notificação. O endereço desta entrega pode ir no texto da notificação.

## 5. Ajustes no mapa feitos nesta iteração
- **Ícone por tipo de veículo:** caminhão, furgão, van ou carro, num círculo com a cor do status. É o mesmo ícone no mapa e na lista (`ui/components/IconesVeiculo.kt`).
- **Só viaja quem tem motorista ativo.** No mapa do controlador, um veículo sem motorista associado, ou com motorista **inativo**, fica **parado no pátio do CD, em cinza**, sem viagem e sem linha de rota, com "Sem motorista ativo". Ele aparece no filtro **Sem motorista (n)**, e não em "Em trânsito". Ao reativar o motorista, o veículo volta a viajar no próximo passo da simulação (`MapaViewModel` combina veículos, motoristas e o passo do simulador).
  - No mapa do **motorista**, o veículo dele sempre viaja: quem está logado é ativo, porque o inativo não consegue entrar, e o aparelho do motorista não baixa a tabela de motoristas.

## 6. Limitações conhecidas
- Em algumas ruas a Geoapify devolve a cidade no lugar do bairro (por exemplo, "Araraquara"). É o dado disponível no OpenStreetMap.
- O endereço de um veículo em movimento pode estar até 15 s atrasado, por causa do controle de cota.
- Quem clonar o repositório precisa criar a própria chave (gratuita) e colocá-la no `local.properties`.
