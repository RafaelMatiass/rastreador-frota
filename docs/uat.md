# TrakSync 1.0.0 – Guia para a equipe de testes (UAT)

Projeto #1 – Rastreamento de veículos de entrega. Versão entregue no code freeze (18/11/2026).

## 1. Instalação

1. Baixe o arquivo **`TrakSync-1.0.0-uat.apk`** no link enviado pela equipe (Release do GitHub).
2. No celular Android 8 ou superior, abra o arquivo e permita "instalar apps desconhecidos" quando o Android pedir.
3. **Emulador:** use um AVD **com Google Play Store**, que é exigido pelo login do Firebase e pela localização. Arraste o APK para a janela do emulador.
4. É preciso internet: login, mapa, rotas e endereços dependem da rede.

> Não compile a partir do código-fonte para testar: a chave da API de endereços (Geoapify) não está no repositório, e sem ela os endereços aparecem como "indisponível".

## 2. Contas de teste

| Perfil | Email |
|---|---|
| Gestor (controlador) | `controlador@traksync.test` |
| Motorista | `motorista@traksync.test` |

A senha é enviada à parte pela equipe, e não fica no repositório. Também é possível criar contas novas em **Cadastre-se**.

## 3. O que testar

### Gestor
- **Mapa da frota:** veículos em trânsito (verde), parados (âmbar) e sem motorista (cinza), filtros e trajeto até o cliente.
- **Telemetria:** ao tocar num veículo, ver velocidade, motor, portas e o **endereço atual** (reverse geocoding).
- **Rotas:** "Traçar rota" até um veículo ou, com toque longo no mapa, até um destino. Mostra distância, tempo e passo a passo.
- **Cercas (geofencing):** criar uma cerca (para toda a frota, para um tipo de veículo ou para placas escolhidas) e receber os alertas de **entrada e saída** na aba de alertas e em notificações.
- **Equipe e frota:** aprovar o veículo cadastrado pelo motorista, atribuir um veículo a um motorista e ativar ou inativar motoristas. Veículo sem motorista ativo fica parado no CD.

### Motorista
- **Cadastrar o veículo com foto** (câmera ou galeria). Ele fica pendente até o gestor aprovar.
- Ver **o próprio veículo no mapa** e traçar rota.
- **Cercas pelo GPS real:** permitir a localização "o tempo todo". Ao entrar ou sair de uma cerca, o alerta é registrado e chega ao gestor na sincronização.
  - No emulador: *Extended Controls → Location*, coloque um ponto dentro da cerca e depois fora.

### Offline
- Desligue a internet, cadastre ou edite algo e religue. Os dados ficam salvos no aparelho e sobem sozinhos para a nuvem.

## 4. Como reportar

Abra uma **issue** no GitHub do projeto (ou use a planilha combinada) com:

- **Título:** curto, por exemplo "Mapa: veículo some ao trocar de aba".
- **Perfil e aparelho:** gestor ou motorista; modelo ou emulador; versão do Android.
- **Passos para reproduzir**, **resultado esperado** e **resultado obtido**.
- **Print ou vídeo**, se possível.
- **Tipo:** bug ou melhoria.

Os pedidos são respondidos até 02/12 (Entrega 9). A equipe pode recusar uma melhoria, explicando o motivo.

## 5. Limitações conhecidas (não são bugs)

- **As posições e a telemetria dos veículos são simuladas,** como pede o manual. Os veículos seguem 4 viagens fixas (CD → cliente → CD).
- **As fotos ficam só no aparelho em que foram tiradas** (decisão da Entrega 4, sem Firebase Storage).
- **O endereço de um veículo em movimento é atualizado no máximo a cada 15 s,** para respeitar a cota da API.
- **Os alertas de cercas simuladas aparecem enquanto o app do gestor está aberto.** Os alertas do GPS real do motorista funcionam com o app fechado.
