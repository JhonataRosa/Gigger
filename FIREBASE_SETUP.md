# Configuração do Firebase para o Instrumentaliza

## Passos para configurar o Firebase

### 1. Criar projeto no Firebase Console

1. Acesse [console.firebase.google.com](https://console.firebase.google.com)
2. Clique em "Criar projeto"
3. Digite o nome: "Instrumentaliza"
4. Siga os passos de configuração

### 2. Adicionar aplicativo Android

1. No console do Firebase, clique no ícone do Android
2. Digite o package name: `com.example.gigger`
3. Digite o nome do app: "Instrumentaliza"
4. Clique em "Registrar app"

### 3. Baixar arquivo de configuração

1. Baixe o arquivo `google-services.json`
2. Coloque-o na pasta `app/` do projeto
3. **IMPORTANTE**: Substitua o arquivo atual pelo arquivo real do seu projeto

### 4. Habilitar serviços no Firebase

#### Authentication
1. No console do Firebase, vá para "Authentication"
2. Clique em "Get started"
3. Vá para a aba "Sign-in method"
4. Habilite "Email/Password"
5. Opcional: Habilite "Google" para login social

#### Firestore Database
1. Vá para "Firestore Database"
2. Clique em "Create database"
3. Escolha "Start in test mode" (para desenvolvimento)
4. Escolha a localização mais próxima

#### Storage
1. Vá para "Storage"
2. Clique em "Get started"
3. Escolha "Start in test mode" (para desenvolvimento)
4. Escolha a localização mais próxima

### 5. Configurar regras de segurança

#### Firestore Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Usuários podem ler/escrever seus próprios dados
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Qualquer pessoa pode ler instrumentos disponíveis
    match /instruments/{instrumentId} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == resource.data.ownerId;
    }
    
    // Usuários podem ler/escrever suas próprias reservas
    match /reservations/{reservationId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    
    // Usuários podem ler/escrever favoritos
    match /favorites/{favoriteId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    
    // REGRAS PARA CHATS - Usuários podem ler/escrever chats onde participam
    match /chats/{chatId} {
      allow read, write: if request.auth != null && 
        (request.auth.uid == resource.data.locatorId || request.auth.uid == resource.data.ownerId);
    }
    
    // REGRAS PARA MENSAGENS - Regras simplificadas para desenvolvimento
    match /messages/{messageId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

#### Storage Rules
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Usuários podem fazer upload de imagens de instrumentos
    match /instruments/{fileName} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    // Usuários podem fazer upload de suas fotos de perfil
    match /profiles/{userId} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### 6. Testar a configuração

1. Execute o projeto no Android Studio
2. Tente fazer login/registro
3. Verifique se os dados aparecem no console do Firebase

## Estrutura do Firestore

### Coleção: users
```json
{
  "name": "Nome do Usuário",
  "email": "usuario@email.com",
  "phone": "(11) 99999-9999",
  "profileImageUri": "https://...",
  "createdAt": "2024-01-01T00:00:00Z"
}
```

### Coleção: instruments
```json
{
  "ownerId": "user_id",
  "name": "Violão Acústico",
  "description": "Violão em excelente estado",
  "category": "Cordas",
  "price": 50.0,
  "imageUri": "https://...",
  "createdAt": "2024-01-01T00:00:00Z",
  "available": true
}
```

### Coleção: reservations
```json
{
  "userId": "user_id",
  "instrumentId": "instrument_id",
  "startDate": "2024-01-01T00:00:00Z",
  "endDate": "2024-01-03T00:00:00Z",
  "totalPrice": 150.0,
  "status": "PENDING",
  "createdAt": "2024-01-01T00:00:00Z"
}
```

## Próximos passos

1. Implementar notificações push com Firebase Cloud Messaging
2. Adicionar autenticação social (Google, Facebook)
3. Implementar chat em tempo real
4. Adicionar sistema de avaliações
5. Implementar pagamentos 