# PoC — Firebase Authentication (Rafael)

## O que pesquisei
- Documentação oficial: https://firebase.google.com/docs/auth/android/start
- Como funciona o signInWithEmailAndPassword
- Como cria e ativa um projeto no Console do Firebase
- Como o Android Studio vincula o projeto ao Firebase (Tools > Firebase)

## O que testei
- Criei um usuário de teste no console do Firebase (email/senha)
- Chamei firebaseAuth.signInWithEmailAndPassword(email, senha) e tratei
  o resultado com coroutines (.await())
- Testei também com senha errada, pra ver o app tratar o erro

## O que aprendi / dificuldades
- Precisa habilitar "Email/senha" em Authentication > Sign-in method,
  senão o login falha mesmo com o app configurado certo


