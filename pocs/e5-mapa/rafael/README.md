# PoC Simulador de Telemetria - Rafael

PoC isolada da Entrega 5 (Semana 11).

## Objetivo

Provar, antes de integrar ao app, a técnica usada para simular a frota:

1. Cada veículo percorre uma **sequência pré-determinada** de pontos, e **cada ponto tem a sua telemetria** (velocidade, motor, portas), como pede o manual do Projeto #1.
2. **Um único loop de coroutine** em um `ViewModel` avança todos os veículos e publica o estado em um `StateFlow`. A tela apenas observa.
3. Os marcadores do osmdroid são criados **uma vez** e depois só mudam de posição, ícone e texto, em vez de `overlays.clear()` a cada passo.
4. A **cor do marcador indica o status**: verde para em trânsito e âmbar para parado.
5. O balão do marcador mostra velocidade, motor e portas.

## Pesquisa

- osmdroid, `Marker` e `MapView`: https://github.com/osmdroid/osmdroid/wiki
- `StateFlow` e ViewModel: https://developer.android.com/kotlin/flow/stateflow-and-sharedflow
- Views clássicas dentro do Compose (`AndroidView`): https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/views-in-compose

## Como a sequência é montada (`Simulador.kt`)

A classe `Rota` gera os pontos a partir de trechos e paradas:

```kotlin
Rota().inicio(lat, lon)
    .trecho(lat2, lon2, passos = 4, kmh = 42)            // andando: motor ligado, portas fechadas
    .parada(2, motorLigado = true,  portasAbertas = false) // semáforo
    .parada(3, motorLigado = false, portasAbertas = true)  // entrega
```

O mesmo código gera sempre a mesma viagem, porque nada é aleatório. O status é derivado da velocidade: acima de 0 km/h o veículo está em trânsito, com 0 km/h está parado.

## Como rodar

1. Abra **esta pasta** (`pocs/e5-mapa/rafael`) no Android Studio.
2. Aguarde o Gradle Sync e rode no emulador **com internet**, que é necessária para os tiles do OpenStreetMap.
3. O que observar:
   - Os 2 veículos andam a cada 2 segundos.
   - O marcador fica âmbar quando o veículo para e verde quando volta a andar.
   - Tocando no marcador, o balão mostra a telemetria atual.
   - Durante uma entrega, o motor aparece desligado e as portas abertas.
   - O botão **Pausar** congela a simulação e **Retomar** continua do mesmo ponto.

Não precisa de API key.

## O que foi levado para o app

- `PontoTelemetria` e o construtor de rotas: `app/.../simulacao/`.
- O fluxo único de passos: `SimuladorFrota`. No app, o passo é calculado a partir do relógio (`agora / intervalo`), e não com um contador no ViewModel. Assim a simulação não reinicia ao sair do mapa e fica igual em todos os aparelhos.
- O mapa de marcadores por veículo, a atualização sem recriar os marcadores e a cor por status: `MapaFrotaScreen`.
