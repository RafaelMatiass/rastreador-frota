# POC E3 - Sincronização SQLite/Room → Firebase Firestore

POC mínima para testar a Entrega 3:

> Sincronização do cache local (SQLite/Room) com o banco de dados na nuvem (Firebase Firestore).

## 1. Pré-requisitos

- Android Studio
- JDK compatível com a versão do Android Studio
- Projeto Firebase
- Firestore habilitado
- Aplicativo Android registrado no Firebase

## 2. Configurar Firebase

No Firebase Console:

1. Crie ou selecione um projeto.
2. Adicione um aplicativo Android.
3. Use o package name:

`com.example.pocesync`

4. Baixe o arquivo `google-services.json`.
5. Coloque o arquivo em:

`app/google-services.json`

O arquivo não está incluído neste ZIP porque é específico do seu projeto Firebase.

## 3. Executar

Abra o projeto no Android Studio e faça Sync Project with Gradle Files.

Depois execute no emulador ou aparelho.

## 4. Teste online

1. Informe "Arroz 5kg".
2. Informe "25.90".
3. Clique em "Salvar localmente".
4. O produto aparecerá como "Pendente".
5. Clique em "Sincronizar com Firestore".
6. Abra o Firebase Console → Firestore Database.
7. Confira a coleção:

`produtos`

O documento deve possuir `id`, `nome` e `preco`.

No aplicativo, o produto deve mudar para:

`✓ Sincronizado`

## 5. Teste offline

1. Desative Wi-Fi/dados móveis.
2. Crie "Feijão 1kg" com preço "8.50".
3. Salve localmente.
4. O produto ficará como "⟳ Pendente".
5. Clique em sincronizar. A operação deve falhar.
6. Ligue novamente a internet.
7. Clique em sincronizar.
8. O produto deverá aparecer no Firestore e mudar para "✓ Sincronizado".

## 6. Regra principal da POC

O registro só é marcado como sincronizado depois que o `.set(...).await()` do Firestore termina com sucesso.

Assim:

SQLite -> Firestore OK -> `sincronizado = true`

Se houver erro:

SQLite -> Firestore falhou -> `sincronizado` continua `false`

## 7. Estrutura

- `ProdutoEntity.kt`: entidade Room.
- `ProdutoDao.kt`: operações SQLite.
- `AppDatabase.kt`: banco Room.
- `SyncRepository.kt`: sincronização com Firestore.
- `MainActivity.kt`: tela simples para teste.

## 8. Observação

Esta POC usa sincronização manual por botão de propósito. Depois que o fluxo estiver validado, a mesma ideia pode ser integrada ao projeto principal e posteriormente automatizada com detecção de conectividade/WorkManager.
