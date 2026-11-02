# Entrega 8 (Semana 17, 18/11/2026): geofencing e versão 1.0

> Cronograma: "Notificações geofencing: veículo entrando e saindo de perímetros (projeto #1)."
> Especificação (Controlador): "Notificação de entrada e saída de perímetro (geofencing) para um veículo específico ou para um grupo."

## 1. O produto: TrakSync

**Gestão de frota para distribuidoras regionais de alimentos e bebidas.** A empresa demo é a fictícia *Distribuidora Morada do Sol*: um centro de distribuição em Araraquara e caminhões, furgões e vans entregando para mercados e restaurantes.

| Perfil | Quem é | O que faz no app |
|---|---|---|
| **Gestor de frota** (Controlador) | Fica no escritório | Dashboard da operação, aprova veículos, atribui motorista ↔ veículo (um para um), cria cercas e recebe alertas |
| **Motorista** | Está na rua | Vê o veículo do turno, faz a vistoria (foto + km), navega até a entrega (rota no app, Waze ou Google Maps) e tem as chegadas/saídas registradas pelo GPS |

## 2. Geofencing (o que a entrega pede)

| Parte | Arquivo | O que faz |
|---|---|---|
| Cercas | `data/local/entity/CercaEntity.kt`, `ui/screens/CercaEditorScreen.kt` | Gestor desenha a cerca no mapa (centro + raio de 50 a 2000 m), escolhe a quem vale: **toda a frota, um tipo (grupo) ou placas específicas**, e se avisa entrada e/ou saída. Room primeiro, Firestore depois (`cercas`). |
| Frota simulada | `geofence/AvaliadorCercas.kt`, `geofence/GeofenceMonitor.kt` | A cada passo da simulação compara a posição de cada veículo com as cercas (haversine). Transição dentro↔fora = alerta + notificação. A 1ª leitura só registra o estado (abrir o app não dispara uma rajada). Roda no escopo do app, não de uma tela. |
| GPS real do motorista | `geofence/GeofenceAparelho.kt`, `geofence/GeofenceBroadcastReceiver.kt` | Registra no `GeofencingClient` do Google as cercas que valem para o veículo do motorista. O receiver recebe as transições mesmo com o app fechado, grava o alerta no Room (pendente) e ele sobe para `alertas` no Firestore. O gestor recebe a notificação na sincronização. |
| Notificações | `geofence/NotificacaoAlertas.kt` | Canal "Alertas de cerca", permissão `POST_NOTIFICATIONS` pedida com explicação. Tocar abre o mapa focado no veículo. |
| Histórico | aba **Alertas** | Eventos agrupados por dia, com origem (telemetria simulada / GPS do motorista) e badge de não lidos na barra de abas. |

**PoC:** `pocs/e8-geofencing/otavio`: cerca real com `GeofencingClient` e cerca simulada (slider move o veículo de 0 a 1000 m do CD), as duas notificando.

### Como testar
1. Gestor: aba **Alertas → Cercas → "Criar cerca do CD (200 m)"**. Com um veículo em operação (aprovado e com motorista ativo), em poucos minutos ele sai do CD e chega a notificação "Saída · CD Morada do Sol".
2. Cerca de cliente: **Nova cerca** → buscar "Avenida Barroso" → raio 150 m → salvar. O veículo que entrega lá gera "Chegada".
3. GPS real: logado como motorista (com veículo atribuído), no card do Início toque **"Permitir localização"** e escolha *Permitir o tempo todo*. No emulador: *Extended Controls → Location*, coloque um ponto dentro da cerca e depois fora. O alerta aparece no Início do motorista e, após sincronizar, no gestor.

Limites do Android: até 100 cercas por app. Cercas reais somem ao reiniciar o aparelho e são registradas de novo ao abrir o app.

## 3. Outras melhorias da versão 1.0
- **Navegação por abas** (gestor: Início · Mapa · Frota · Equipe · Alertas; motorista: Início · Mapa · Veículo · Perfil), splash do sistema, onboarding na 1ª abertura, tema claro/escuro seguindo o aparelho.
- **Dashboard interativo**: donut da frota (toque filtra o mapa), indicadores, "Precisa de atenção" (aprovações, CNH vencendo, porta aberta em movimento, excesso de velocidade), próximas chegadas, etapas das viagens, atividade recente.
- **Regras de negócio**: um veículo, um motorista (transferência com confirmação, saneamento de dados antigos); veículo do motorista entra como *Aguardando aprovação*; só o gestor aprova, desativa, exclui e atribui; placa validada (antiga ou Mercosul) e única; exclusões com "Desfazer".
- **Mapa corrigido**: o zoom com dois dedos virava "toque longo" e traçava uma rota com zoom-out automático. Agora não há toque longo, o mapa ocupa a tela com painel deslizante, a câmera só se move por ação explícita e qualquer gesto desliga o "seguir veículo".
- **Planejador de rotas estilo Waze**: digitar origem e destino (autocomplete da Geoapify), atalhos (minha localização, CD, entrega atual, veículos, recentes), escolher no mapa com endereço ao vivo, passo a passo em português, hora de chegada, abrir no Waze/Google Maps.
- **Desempenho**: uma sincronização só no escopo do app (antes cada tela criava a sua com loop de 60 s), fotos com Coil (antes eram decodificadas a cada recomposição), fluxos parando em segundo plano.
- **Banco v5** com migração sem perda (`MIGRATION_4_5`) e **regras do Firestore** atualizadas (`firestore.rules`, publicar no console).

## 4. Testes
`./gradlew :app:testDebugUnitTest`: `AvaliadorCercasTest`, `RegrasFrotaTest` (placa, transferência, duplicados, CNH), `InstrucoesRotaTest` e `ViagensSimuladasTest`.
