# Entrega 5 (Semana 11, 07/10/2026): veículos parados e em trânsito no mapa

> Cronograma: "Indicação no mapa da localização de veículos parados e em trânsito (projeto #1)."
> Especificação: "Simular a mudança de posição geográfica e geração de telemetria no próprio aplicativo, usando uma sequência de dados pré-determinada. Para cada posição geográfica, deve haver um conjunto de dados de telemetria."

## 1. O que foi feito

### PoCs (feitas antes de integrar)
| PoC | O que prova |
|---|---|
| `pocs/e5-mapa/otavio` | Mapa OpenStreetMap com osmdroid (sem API key) e um marcador trocando de posição. |
| `pocs/e5-mapa/rafael` | Simulador de telemetria: sequência pré-determinada de pontos com velocidade, motor e portas; um único loop com `StateFlow`; marcadores criados uma vez e só atualizados; cor por status; botão Pausar/Retomar. |

### No app
| Arquivo | Papel |
|---|---|
| `simulacao/Telemetria.kt` | `PontoTelemetria` (lat, lon, velocidade, motor, portas) e `StatusVeiculo`. O status é **derivado**: com velocidade > 0 o veículo está em trânsito, com 0 está parado. |
| `simulacao/RotasSimuladas.kt` | 4 viagens circulares em Araraquara, montadas com `trecho(...)`, `semaforo(n)` e `entrega(n)`. No semáforo o veículo fica parado com motor ligado e portas fechadas. Na entrega fica parado com motor desligado e portas abertas. |
| `simulacao/SimuladorFrota.kt` | Calcula a telemetria de cada veículo a cada 3 s (detalhes abaixo). |
| `ui/viewmodel/MapaViewModel.kt` | Junta (`combine`) os veículos do Room com o passo da simulação e gera `VeiculoNoMapa(veiculo, telemetria)`. |
| `ui/screens/MapaFrotaScreen.kt` | Mapa com marcadores verde/âmbar, filtros com contagem, painel de telemetria, botão de centralizar e estados vazios. |
| `navigation/NavGraph.kt`, `HomeMotoristaScreen.kt` | Nova rota `mapa_motorista?veiculo=...`: o motorista vê só o veículo associado a ele. |

## 2. Como a simulação funciona

```
Room (veículos) ──┐
                  ├─ combine ─> MapaViewModel.frota ─> MapaFrotaScreen (marcadores + lista)
SimuladorFrota ───┘
  passo = agora / 3000 ms
  rota  = rotas[hash(placa) % 4]
  ponto = rota[(passo + deslocamento(placa)) % tamanho]
```

- **Sequência pré-determinada:** as rotas são listas fixas de pontos, e cada ponto já traz a própria telemetria. Nada é aleatório.
- **O passo vem do relógio**, e não de um contador. Isso traz duas vantagens:
  1. Sair do mapa e voltar não reinicia a viagem, porque o veículo "continuou andando".
  2. O controlador e o motorista, em aparelhos diferentes, veem o mesmo veículo no mesmo lugar sem trocar mensagens, porque os dois fazem a mesma conta.
- **Cada veículo tem a sua rota e o seu ponto de partida**, calculados a partir da placa. Assim os veículos não ficam sobrepostos.
- **A telemetria não vai para o Room nem para o Firestore.** O manual pede a simulação "no próprio aplicativo", e gravar um ponto a cada 3 segundos na nuvem gastaria a cota gratuita sem ganho. Os dados reais (veículos, usuários, fotos) continuam passando pelo cache local e pela sincronização das entregas anteriores.
- **Desempenho no mapa:** cada veículo tem um `Marker` criado uma única vez, e a cada passo só mudam a posição e o ícone. A versão anterior apagava e recriava todos os marcadores a cada recomposição.

## 3. Como testar

Pré-requisitos: emulador ou celular **com internet** (os tiles do OpenStreetMap vêm da rede), pelo menos 2 veículos cadastrados e 1 motorista.

**PoC**
1. Abrir `pocs/e5-mapa/rafael` no Android Studio e rodar.
2. Conferir o comportamento:
   - Os 2 veículos andam a cada 2 s.
   - O marcador fica âmbar quando o veículo para e o texto mostra "motor desligado, portas abertas" na entrega.
   - **Pausar** congela a simulação e **Retomar** continua do mesmo ponto.

**App como controlador**
1. Entrar e tocar em **Mapa da frota**. O mapa enquadra todos os veículos.
2. Esperar alguns segundos. Os marcadores andam, e cada veículo está em um lugar diferente.
3. Quando um veículo para, o marcador fica âmbar e o card mostra "Parado · 0 km/h".
4. Tocar nos filtros **Em trânsito** e **Parados**. A lista e o mapa mostram só os veículos daquele status, e as contagens batem.
5. Tocar em um veículo, no card ou no marcador. Abre o painel com status, velocidade, motor e portas, e o mapa passa a seguir o veículo. O **X** fecha o painel.
6. O botão de centralizar, no canto superior, enquadra a frota de novo.
7. Voltar para a tela inicial, esperar uns 10 s e abrir o mapa de novo. Os veículos estão mais à frente, e não no início da rota.

**App como motorista**
1. Com um veículo associado pelo controlador, tocar em **Ver meu veículo no mapa**. Aparecem só o veículo dele e o painel de telemetria, e o mapa acompanha o veículo.
2. Sem veículo associado, a tela mostra "Nenhum veículo atribuído".

**Dois aparelhos (opcional, para impressionar)**
Abrir o mapa como controlador em um emulador e como motorista em outro. O veículo aparece na mesma posição nos dois.

## 4. Roteiro de apresentação para o professor (cerca de 5 minutos)

1. **Contexto (30 s):** "A Entrega 5 pede os veículos parados e em trânsito no mapa. Como não há veículos reais, o manual pede que a posição e a telemetria sejam simuladas no app, com uma sequência pré-determinada."
2. **PoC (1 min):** rodar `pocs/e5-mapa/rafael`. Mostrar a cor mudando quando o veículo para e o botão Pausar. Dizer: "Antes de mexer no app, provamos a técnica isolada: sequência de pontos com telemetria, um único loop com StateFlow e marcadores que só são atualizados."
3. **App como controlador (2 min):** seguir os passos 1 a 6 do roteiro de teste. Enfatizar os filtros com contagem, porque são o critério da entrega, e o painel de telemetria.
4. **Código (1 min):** mostrar `RotasSimuladas.kt`, com `trecho`, `semaforo` e `entrega`, e `SimuladorFrota.telemetria(...)`, com a conta do passo pelo relógio.
5. **Motorista (30 s):** entrar como motorista e mostrar que ele vê só o próprio veículo.

### Perguntas prováveis
- **"Como a simulação funciona?"** Cada veículo recebe uma das 4 rotas fixas, escolhida pela placa. A cada 3 s o passo avança, e o passo é o horário atual dividido por 3 s. Cada ponto da rota já traz a velocidade, o motor e as portas.
- **"Onde está a sequência pré-determinada?"** Em `simulacao/RotasSimuladas.kt`. As rotas são montadas com `trecho`, `semaforo` e `entrega` e geram sempre os mesmos pontos.
- **"O que define parado ou em trânsito?"** A velocidade daquele ponto: 0 km/h é parado, acima disso é em trânsito. Parado se divide em dois casos: semáforo (motor ligado) e entrega (motor desligado, portas abertas).
- **"Por que a telemetria não vai para o Firestore?"** Porque o manual pede a simulação no próprio app e gravar um ponto a cada 3 s consumiria a cota gratuita. Como o passo vem do relógio, todos os aparelhos já mostram a mesma posição sem precisar da nuvem.
- **"Por que osmdroid e não Google Maps?"** O manual permite os dois. O OpenStreetMap não exige API key nem cartão de crédito.
- **"Por que a posição não reinicia quando sai e volta?"** Porque não existe contador em memória: a posição é função do horário.
- **"E a Entrega 6 (rotas)?"** O simulador já expõe a posição atual de cada veículo, que será o destino da rota. Falta pegar a localização do aparelho e desenhar a rota com um serviço como o OSRM.

## 5. Limitações conhecidas
- Os veículos andam em linha reta entre os pontos da rota e não seguem as ruas. É suficiente para a entrega; a rota por ruas é o tema da Entrega 6.
- A foto no painel só aparece no aparelho em que ela foi tirada, porque o armazenamento é local por decisão da Entrega 4.
