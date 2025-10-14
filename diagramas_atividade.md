# Diagramas de Atividade - Sistema Instrumentaliza

## 1. Fluxo Principal de Autenticação

```mermaid
graph TD
    A[Iniciar App] --> B{Usuário já logado?}
    B -->|Sim| C[Ir para Tela Principal]
    B -->|Não| D[Tela de Login]
    
    D --> E[Inserir Email e Senha]
    E --> F[Validar Campos]
    F -->|Vazios| G[Exibir Erro]
    G --> E
    F -->|Válidos| H[Autenticar no Firebase]
    
    H -->|Sucesso| I[Reiniciar Notificações]
    I --> J[Ir para Tela de Instrumentos]
    H -->|Erro| K[Exibir Erro de Login]
    K --> E
    
    D --> L[Criar Nova Conta]
    L --> M[Tela de Registro]
    M --> N[Inserir Dados]
    N --> O[Validar Dados]
    O -->|Inválidos| P[Exibir Erro]
    P --> N
    O -->|Válidos| Q[Criar Usuário no Firebase]
    Q -->|Sucesso| R[Atualizar Perfil]
    R --> S[Ir para Tela de Instrumentos]
    Q -->|Erro| T[Exibir Erro de Registro]
    T --> N
```

## 2. Fluxo de Solicitação de Reserva

```mermaid
graph TD
    A[Visualizar Instrumento] --> B[Botão Solicitar Reserva]
    B --> C{Usuário logado?}
    C -->|Não| D[Redirecionar para Login]
    C -->|Sim| E[Verificar se é próprio instrumento]
    
    E -->|Próprio| F[Exibir Erro: Não pode solicitar próprio instrumento]
    E -->|Não é próprio| G[Tela de Solicitação]
    
    G --> H[Selecionar Data Início]
    H --> I[Selecionar Data Fim]
    I --> J[Calcular Preço Total]
    J --> K[Inserir Observações Opcionais]
    K --> L[Validar Período]
    
    L -->|Inválido| M[Exibir Erro de Data]
    M --> H
    L -->|Válido| N[Verificar Disponibilidade]
    
    N -->|Indisponível| O[Exibir Erro: Período indisponível]
    O --> H
    N -->|Disponível| P[Criar Solicitação no Firebase]
    
    P -->|Sucesso| Q[Exibir Sucesso]
    Q --> R[Voltar para Tela Anterior]
    P -->|Erro| S[Exibir Erro de Criação]
    S --> G
```

## 3. Fluxo de Gerenciamento de Solicitações

```mermaid
graph TD
    A[Proprietário acessa Solicitações] --> B[Carregar Solicitações Recebidas]
    B --> C[Exibir Lista de Solicitações]
    
    C --> D[Selecionar Solicitação]
    D --> E[Visualizar Detalhes]
    E --> F{Escolher Ação}
    
    F -->|Aceitar| G[Marcar como Aceita]
    F -->|Recusar| H[Marcar como Recusada]
    F -->|Voltar| C
    
    G --> I[Atualizar Status no Firebase]
    H --> I
    I -->|Sucesso| J[Enviar Notificação]
    J --> K[Atualizar Lista]
    I -->|Erro| L[Exibir Erro]
    L --> E
    
    K --> C
```

## 4. Fluxo de Avaliação

```mermaid
graph TD
    A[Reserva Concluída] --> B{Solicitar Avaliação}
    B -->|Avaliar Locatário| C[Tela Avaliar Usuário]
    B -->|Avaliar Instrumento| D[Tela Avaliar Instrumento]
    
    C --> E[Selecionar Nota 1-5]
    E --> F[Inserir Comentário Opcional]
    F --> G[Validar Dados]
    G -->|Inválidos| H[Exibir Erro]
    H --> E
    G -->|Válidos| I[Salvar Avaliação no Firebase]
    
    D --> J[Selecionar Nota 1-5]
    J --> K[Inserir Comentário Opcional]
    K --> L[Validar Dados]
    L -->|Inválidos| M[Exibir Erro]
    M --> J
    L -->|Válidos| N[Salvar Avaliação no Firebase]
    
    I -->|Sucesso| O[Atualizar Nota Média do Usuário]
    N -->|Sucesso| P[Atualizar Nota Média do Instrumento]
    
    O --> Q[Voltar para Tela Anterior]
    P --> Q
    I -->|Erro| R[Exibir Erro de Salvamento]
    N -->|Erro| R
    R --> C
```

## 5. Fluxo de Chat/Mensagens

```mermaid
graph TD
    A[Reserva Aceita] --> B[Criar Chat no Firebase]
    B --> C[Proprietário e Locatário podem conversar]
    
    C --> D[Acessar Lista de Chats]
    D --> E[Selecionar Chat]
    E --> F[Tela de Conversa]
    
    F --> G[Enviar Mensagem]
    G --> H[Validar Mensagem]
    H -->|Vazia| I[Exibir Erro]
    I --> G
    H -->|Válida| J[Salvar no Firebase]
    
    J -->|Sucesso| K[Atualizar Interface]
    J -->|Erro| L[Exibir Erro de Envio]
    L --> G
    
    K --> F
```

## 6. Fluxo de Gerenciamento de Badge de Notificações

```mermaid
graph TD
    A[App Inicia] --> B[Verificar Solicitações Não Lidas]
    B --> C{Existem solicitações não lidas?}
    
    C -->|Sim| D[Mostrar Badge Vermelho]
    C -->|Não| E[Mostrar Ícone Normal]
    
    D --> F[Usuário toca no ícone]
    F --> G[Marcar todas como lidas]
    G --> H[Atualizar no Firebase]
    H --> I[Remover Badge]
    I --> E
    
    J[Nova Solicitação Chega] --> K[Badge Aparece Automaticamente]
    K --> F
```

## 7. Fluxo de Adicionar Instrumento

```mermaid
graph TD
    A[Usuário acessa Meus Instrumentos] --> B[Botão Adicionar Instrumento]
    B --> C[Tela de Adicionar]
    
    C --> D[Inserir Nome do Instrumento]
    D --> E[Selecionar Categoria]
    E --> F[Inserir Preço Diário]
    F --> G[Adicionar Foto]
    G --> H[Inserir Descrição]
    H --> I[Validar Campos Obrigatórios]
    
    I -->|Inválidos| J[Exibir Erro de Validação]
    J --> D
    I -->|Válidos| K[Upload da Foto para Firebase Storage]
    
    K -->|Sucesso| L[Salvar Instrumento no Firebase]
    K -->|Erro| M[Exibir Erro de Upload]
    M --> G
    
    L -->|Sucesso| N[Exibir Sucesso]
    N --> O[Voltar para Lista]
    L -->|Erro| P[Exibir Erro de Salvamento]
    P --> C
```

## 8. Fluxo de Busca e Filtros

```mermaid
graph TD
    A[Tela de Instrumentos] --> B[Usuário insere busca]
    B --> C[Filtrar por Nome]
    
    C --> D[Aplicar Filtro de Categoria]
    D --> E[Aplicar Filtro de Preço]
    E --> F[Ordenar Resultados]
    
    F --> G[Exibir Instrumentos Filtrados]
    G --> H[Usuário seleciona instrumento]
    H --> I[Ir para Detalhes]
    
    J[Limpar Filtros] --> K[Voltar para Lista Completa]
    K --> G
```

## 9. Fluxo de Logout

```mermaid
graph TD
    A[Usuário acessa Menu] --> B[Selecionar Logout]
    B --> C[Confirmar Logout]
    C -->|Cancelar| D[Voltar para App]
    C -->|Confirmar| E[Limpar Sessão Local]
    E --> F[Logout do Firebase]
    F --> G[Parar Notificações]
    G --> H[Ir para Tela de Login]
    H --> I[Limpar Pilha de Atividades]
```

## 10. Fluxo de Recuperação de Erros

```mermaid
graph TD
    A[Operação no Firebase] --> B{Sucesso?}
    B -->|Sim| C[Continuar Fluxo Normal]
    B -->|Não| D[Identificar Tipo de Erro]
    
    D --> E{Erro de Rede?}
    E -->|Sim| F[Exibir Mensagem: Verifique conexão]
    E -->|Não| G{Erro de Autenticação?}
    
    G -->|Sim| H[Redirecionar para Login]
    G -->|Não| I{Erro de Permissão?}
    
    I -->|Sim| J[Exibir Mensagem: Sem permissão]
    I -->|Não| K[Exibir Mensagem Genérica]
    
    F --> L[Tentar Novamente]
    J --> M[Verificar Permissões]
    K --> N[Log do Erro]
    L --> A
    M --> A
```

---

## Resumo dos Principais Fluxos

### **Fluxos de Usuário:**
1. **Autenticação** - Login/Registro
2. **Busca** - Encontrar instrumentos
3. **Solicitação** - Reservar instrumento
4. **Chat** - Comunicação
5. **Avaliação** - Feedback mútuo

### **Fluxos de Proprietário:**
1. **Gerenciamento** - Adicionar/Editar instrumentos
2. **Solicitações** - Aceitar/Recusar reservas
3. **Notificações** - Badge de alertas
4. **Avaliação** - Avaliar locatários

### **Fluxos do Sistema:**
1. **Notificações** - Push notifications
2. **Sincronização** - Firebase realtime
3. **Validação** - Dados e permissões
4. **Recuperação** - Tratamento de erros
