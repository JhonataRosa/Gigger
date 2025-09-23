# 🔥 Regras de Segurança do Firestore

## **📋 Como Configurar:**

### **1. Acesse o Firebase Console:**
- Vá para: https://console.firebase.google.com/
- Selecione seu projeto "Instrumentaliza"

### **2. Vá para Firestore Database:**
- No menu lateral, clique em "Firestore Database"
- Clique na aba "Regras"

### **3. Substitua as regras existentes por estas:**

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // ==================== USUÁRIOS ====================
    match /users/{userId} {
      // Usuário pode ler e editar apenas seu próprio perfil
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      // Usuários logados podem ler perfis de outros usuários (para ver informações do dono do instrumento)
      allow read: if request.auth != null;
    }
    
    // ==================== INSTRUMENTOS ====================
    match /instruments/{instrumentId} {
      // Qualquer usuário logado pode ler instrumentos disponíveis
      allow read: if request.auth != null;
      
      // Usuário logado pode criar instrumentos
      allow create: if request.auth != null && 
                      request.auth.uid == resource.data.ownerId;
      
      // Apenas o dono pode editar ou deletar seu instrumento
      allow update, delete: if request.auth != null && 
                              request.auth.uid == resource.data.ownerId;
    }
    
    // ==================== RESERVAS ====================
    match /reservations/{reservationId} {
      // Usuário logado pode ler suas próprias reservas
      allow read: if request.auth != null && 
                    (request.auth.uid == resource.data.userId || 
                     request.auth.uid == resource.data.ownerId);
      
      // Usuário logado pode criar reservas
      allow create: if request.auth != null && 
                      request.auth.uid == request.resource.data.userId;
      
      // Apenas o usuário da reserva ou o dono do instrumento pode editar
      allow update: if request.auth != null && 
                      (request.auth.uid == resource.data.userId || 
                       request.auth.uid == resource.data.ownerId);
    }
  }
}
```

### **4. Clique em "Publicar"**

---

## **🔍 O que essas regras fazem:**

1. **Usuários**: Podem ler perfis de outros usuários, mas editar apenas o próprio
2. **Instrumentos**: Qualquer usuário logado pode ler, mas apenas o dono pode criar/editar/deletar
3. **Reservas**: Usuários podem criar suas próprias reservas e ver reservas relacionadas

---

## **📱 Depois de configurar:**

1. **Execute o app novamente**
2. **Tente criar um instrumento**
3. **Verifique se funciona** agora

---

## **❌ Se ainda der erro:**

Pode ser que você precise:
1. **Verificar se o Firestore está ativado** no projeto
2. **Verificar se as regras foram publicadas** corretamente
3. **Aguardar alguns minutos** para as regras propagarem

---

## **📞 Precisa de ajuda?**

Se não conseguir configurar, me avise e posso te ajudar com screenshots ou instruções mais detalhadas! 