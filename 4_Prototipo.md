# 4. PROTÓTIPO

Este capítulo apresenta o protótipo desenvolvido do aplicativo Instrumentaliza, detalhando suas características técnicas, funcionalidades implementadas e interfaces de usuário. O protótipo constitui uma plataforma móvel de aluguel de instrumentos musicais peer-to-peer, desenvolvida para o sistema operacional Android.

## 4.1 Visão Geral do Sistema

O Instrumentaliza é um aplicativo móvel nativo Android que conecta proprietários de instrumentos musicais a potenciais locatários, facilitando o processo de aluguel de forma direta e segura. O sistema adota a arquitetura cliente-servidor, utilizando o Firebase como backend-as-a-service (BaaS) para gerenciamento de dados, autenticação e armazenamento de mídia.

A aplicação foi desenvolvida em linguagem Java, seguindo os padrões de desenvolvimento Android recomendados pelo Google, com interface adaptativa que se ajusta a diferentes tamanhos de tela e resoluções. O protótipo implementa todas as funcionalidades essenciais para o ecossistema de compartilhamento de instrumentos musicais, desde o cadastro de usuários até a conclusão e avaliação de aluguéis.

**[Inserir captura de tela: Tela inicial do aplicativo]**

## 4.2 Arquitetura e Tecnologias Utilizadas

### 4.2.1 Plataforma e Linguagem de Desenvolvimento

O aplicativo foi desenvolvido utilizando:

- **Plataforma**: Android nativo
- **Linguagem de programação**: Java
- **IDE**: Android Studio
- **SDK mínimo**: Android 6.0 (API level 23)
- **SDK alvo**: Android 13 (API level 33)

### 4.2.2 Backend e Serviços em Nuvem

Para o backend, optou-se pelo Firebase, plataforma do Google que oferece diversos serviços integrados:

- **Firebase Authentication**: Gerenciamento de autenticação de usuários via email e senha
- **Cloud Firestore**: Banco de dados NoSQL em tempo real para armazenamento de dados estruturados
- **Firebase Storage**: Armazenamento de imagens de instrumentos e fotos de perfil dos usuários
- **Firebase Cloud Messaging**: Sistema de notificações push (preparado para implementação futura)

### 4.2.3 Persistência Local de Dados

Além do armazenamento em nuvem, o aplicativo utiliza:

- **Room Database**: Biblioteca de persistência local que fornece uma camada de abstração sobre SQLite
- **SharedPreferences**: Armazenamento de preferências e dados de sessão do usuário

## 4.3 Modelo de Dados

O sistema utiliza um modelo de dados estruturado em coleções do Firestore, organizadas da seguinte forma:

### 4.3.1 Coleção de Usuários

Armazena informações dos usuários cadastrados na plataforma, incluindo:
- Identificador único do usuário
- Nome completo
- Email
- Telefone para contato
- URL da foto de perfil
- Data de criação da conta
- Nota média recebida
- Total de avaliações recebidas

### 4.3.2 Coleção de Instrumentos

Contém os dados dos instrumentos cadastrados para aluguel:
- Identificador único do instrumento
- Identificador do proprietário
- Nome e descrição do instrumento
- Categoria (Cordas, Sopro, Teclas, Percussão, Acessórios)
- Preço diário de aluguel
- URL da imagem do instrumento
- Status de disponibilidade
- Lista de períodos indisponíveis
- Nota média recebida
- Total de avaliações recebidas

### 4.3.3 Coleção de Solicitações

Gerencia as solicitações de reserva realizadas pelos usuários:
- Identificador único da solicitação
- Identificadores do solicitante, proprietário e instrumento
- Período solicitado (data de início e fim)
- Preço total calculado
- Status (Pendente, Aceita, Recusada)
- Observações do solicitante
- Motivo de recusa (quando aplicável)
- Timestamps de criação e atualização

### 4.3.4 Coleção de Reservas

Registra as reservas confirmadas:
- Identificador único da reserva
- Identificadores do usuário e instrumento
- Período da reserva
- Preço total
- Status da reserva
- Data de criação

### 4.3.5 Coleção de Mensagens e Chats

Implementa o sistema de comunicação entre usuários:
- Identificador único da conversa
- Identificadores dos participantes (locatário e proprietário)
- Identificador do instrumento em discussão
- Mensagens trocadas com timestamps
- Status da conversa
- Data da última mensagem

### 4.3.6 Coleções de Avaliações

O sistema mantém dois tipos de avaliações:

**Avaliações de Instrumentos**:
- Nota de 1 a 5 estrelas
- Comentário opcional
- Identificadores do avaliador, instrumento e reserva
- Data da avaliação

**Avaliações de Usuários**:
- Nota de 1 a 5 estrelas
- Comentário opcional
- Identificadores do avaliador (proprietário), avaliado (locatário) e reserva
- Data da avaliação

## 4.4 Funcionalidades Implementadas

### 4.4.1 Autenticação e Gerenciamento de Conta

O sistema de autenticação permite que usuários criem contas e acessem a plataforma de forma segura.

**Cadastro de Usuário**:
O processo de cadastro coleta informações essenciais do usuário, incluindo nome completo, email, telefone e senha. O sistema valida os campos obrigatórios e verifica a conformidade da senha antes de criar a conta no Firebase Authentication. Simultaneamente, um documento é criado na coleção de usuários do Firestore para armazenar os dados complementares do perfil.

**[Inserir captura de tela: Tela de cadastro]**

**Login**:
A tela de login permite que usuários registrados acessem suas contas através de email e senha. O sistema mantém a sessão do usuário ativa até que ele realize o logout manualmente, proporcionando uma experiência de uso contínua.

**[Inserir captura de tela: Tela de login]**

**Recuperação de Senha**:
Embora não detalhado em tela específica, o sistema utiliza a funcionalidade de recuperação de senha do Firebase Authentication, enviando emails de redefinição para usuários que esqueceram suas credenciais.

### 4.4.2 Navegação e Interface Principal

Após o login, o usuário é direcionado à tela principal de instrumentos, que serve como hub de navegação do aplicativo.

**Catálogo de Instrumentos**:
A interface principal apresenta uma lista rolável de todos os instrumentos disponíveis para aluguel na plataforma. Cada item exibe informações resumidas do instrumento, incluindo foto, nome, categoria, preço diário e avaliação média. A lista é carregada dinamicamente do Firestore, atualizando-se em tempo real quando novos instrumentos são adicionados ou quando existentes são modificados.

**[Inserir captura de tela: Tela principal com lista de instrumentos]**

**Filtragem por Categoria**:
O sistema oferece filtragem rápida através de botões de categoria dispostos horizontalmente no topo da tela. As categorias implementadas incluem:
- Cordas (violões, guitarras, baixos, violinos, etc.)
- Sopro (flautas, saxofones, trompetes, etc.)
- Teclas (pianos, teclados, sintetizadores, etc.)
- Percussão (baterias, cajons, pandeiros, etc.)
- Acessórios (pedais, amplificadores, cases, etc.)

Ao selecionar uma categoria, a lista é instantaneamente filtrada para exibir apenas os instrumentos correspondentes.

**[Inserir captura de tela: Filtros de categoria]**

**Busca de Instrumentos**:
Uma barra de busca permite que usuários localizem instrumentos específicos através de palavras-chave, realizando busca nos campos de nome e descrição dos instrumentos cadastrados.

**Menu de Navegação Lateral**:
O aplicativo implementa um menu drawer lateral (Navigation Drawer) que proporciona acesso rápido às principais seções:
- Perfil do usuário
- Meus instrumentos
- Minhas reservas
- Solicitações recebidas
- Favoritos
- Conversas/Chats
- Ajuda
- Logout

**[Inserir captura de tela: Menu de navegação lateral aberto]**

### 4.4.3 Visualização Detalhada de Instrumentos

Ao tocar em um instrumento da lista, o usuário é direcionado para a tela de detalhes, que apresenta informações completas sobre o item.

**Informações Exibidas**:
- Imagem em tamanho ampliado do instrumento
- Nome e descrição detalhada
- Categoria e preço por dia
- Avaliação média com visualização de estrelas e número de avaliações
- Informações do proprietário (nome e avaliação)
- Botão para visualizar disponibilidade
- Botão para solicitar reserva
- Botão para adicionar/remover dos favoritos
- Botão para iniciar conversa com o proprietário

**[Inserir captura de tela: Tela de detalhes do instrumento]**

**Visualização de Disponibilidade**:
Uma funcionalidade crucial permite que potenciais locatários visualizem a disponibilidade do instrumento através de um calendário interativo. O calendário destaca visualmente os períodos em que o instrumento já está reservado, permitindo que o usuário planeje adequadamente sua solicitação de aluguel.

**[Inserir captura de tela: Calendário de disponibilidade]**

**Lista de Avaliações**:
Na parte inferior da tela de detalhes, são exibidas as avaliações recebidas pelo instrumento, incluindo a nota em estrelas, comentário e nome do avaliador. Esta seção proporciona transparência e auxilia na tomada de decisão do potencial locatário.

**[Inserir captura de tela: Lista de avaliações do instrumento]**

### 4.4.4 Sistema de Solicitação de Reserva

O processo de solicitação de reserva é estruturado para garantir clareza nas condições do aluguel.

**Formulário de Solicitação**:
A tela de solicitação apresenta:
- Informações resumidas do instrumento selecionado
- Seletor de data de início através de calendário
- Seletor de data de término
- Cálculo automático do número de dias e preço total
- Campo de observações para comunicação adicional com o proprietário
- Confirmação dos dados de contato do solicitante (email e telefone)

O sistema valida automaticamente que:
- As datas selecionadas não conflitam com períodos já reservados
- A data de início é posterior à data atual
- A data de término é posterior à data de início

**[Inserir captura de tela: Formulário de solicitação de reserva]**

**Confirmação de Solicitação**:
Após o envio da solicitação, o usuário recebe confirmação visual e a solicitação é registrada no Firestore com status "Pendente". O proprietário do instrumento receberá a solicitação em sua lista de solicitações pendentes.

### 4.4.5 Gerenciamento de Instrumentos Próprios

Usuários que possuem instrumentos podem cadastrá-los na plataforma e gerenciá-los de forma centralizada.

**Cadastro de Novo Instrumento**:
A interface de cadastro permite:
- Upload de foto do instrumento através da câmera ou galeria
- Inserção do nome do instrumento
- Seleção de categoria através de menu dropdown
- Descrição detalhada
- Definição do preço diário de aluguel
- Confirmação e salvamento no Firestore

As imagens são automaticamente redimensionadas e enviadas ao Firebase Storage, com a URL resultante sendo armazenada no documento do instrumento.

**[Inserir captura de tela: Formulário de cadastro de instrumento]**

**Lista de Meus Instrumentos**:
A seção "Meus Instrumentos" apresenta todos os instrumentos cadastrados pelo usuário atual. Para cada instrumento, são exibidos:
- Foto miniatura
- Nome e categoria
- Preço diário
- Avaliação média
- Opções de edição e exclusão

**[Inserir captura de tela: Lista de meus instrumentos]**

**Edição de Instrumentos**:
Proprietários podem modificar informações de seus instrumentos a qualquer momento, incluindo alteração de foto, descrição, preço e status de disponibilidade.

**Gerenciamento de Disponibilidade**:
Uma funcionalidade importante permite que proprietários definam manualmente períodos de indisponibilidade para seus instrumentos, útil para situações de manutenção, uso pessoal ou outros compromissos. Estes períodos são visualizados no calendário de disponibilidade do instrumento.

**[Inserir captura de tela: Gerenciamento de disponibilidade]**

### 4.4.6 Gerenciamento de Solicitações

Proprietários de instrumentos recebem e gerenciam solicitações de reserva através de uma interface dedicada.

**Lista de Solicitações**:
A interface apresenta três abas:
- **Pendentes**: Solicitações aguardando análise
- **Aceitas**: Solicitações aprovadas
- **Recusadas**: Solicitações negadas

Cada solicitação exibe:
- Nome e foto do solicitante
- Instrumento solicitado
- Período desejado
- Preço total
- Data da solicitação

**[Inserir captura de tela: Lista de solicitações com abas]**

**Detalhes da Solicitação**:
Ao selecionar uma solicitação, o proprietário visualiza:
- Informações completas do solicitante (nome, email, telefone)
- Detalhes do instrumento
- Período solicitado com número de dias
- Preço total calculado
- Observações do solicitante
- Opções de aceitar ou recusar (para solicitações pendentes)

**[Inserir captura de tela: Detalhes da solicitação]**

**Aceitar Solicitação**:
Ao aceitar uma solicitação, o sistema:
- Atualiza o status da solicitação para "Aceita"
- Cria uma reserva confirmada
- Adiciona o período à lista de indisponibilidade do instrumento
- Permite que o proprietário posteriormente avalie o locatário

**Recusar Solicitação**:
Ao recusar, o proprietário pode informar o motivo da recusa, que será registrado e poderá ser visualizado pelo solicitante.

### 4.4.7 Visualização de Reservas

Usuários podem acompanhar suas reservas ativas e históricas.

**Minhas Reservas**:
A interface de reservas organiza-se em duas abas:
- **Como Locatário**: Reservas de instrumentos que o usuário alugou
- **Como Proprietário**: Reservas de instrumentos do usuário que foram alugados por terceiros

Para cada reserva, são exibidos:
- Foto e nome do instrumento
- Nome da outra parte envolvida (proprietário ou locatário)
- Período da reserva
- Preço total
- Status da reserva
- Opções de avaliação (após conclusão da reserva)

**[Inserir captura de tela: Lista de minhas reservas]**

### 4.4.8 Sistema de Comunicação (Chat)

O aplicativo implementa um sistema de mensagens em tempo real para comunicação entre usuários.

**Lista de Conversas**:
A interface de chats apresenta todas as conversas ativas do usuário, organizadas em duas abas:
- **Como Locatário**: Conversas sobre instrumentos que o usuário deseja alugar
- **Como Proprietário**: Conversas sobre instrumentos do usuário

Cada conversa exibe:
- Foto e nome do outro participante
- Nome do instrumento em discussão
- Última mensagem enviada
- Timestamp da última mensagem
- Indicador de mensagens não lidas

**[Inserir captura de tela: Lista de conversas]**

**Tela de Conversa**:
Ao abrir uma conversa, o usuário acessa:
- Histórico completo de mensagens com timestamps
- Diferenciação visual entre mensagens próprias e do outro usuário
- Campo de digitação de nova mensagem
- Botão de envio
- Informações do instrumento no cabeçalho

As mensagens são sincronizadas em tempo real através do Firestore, proporcionando uma experiência de chat fluida. O sistema implementa notificação visual para mensagens não lidas através de badges numéricos.

**[Inserir captura de tela: Tela de conversa/chat]**

### 4.4.9 Sistema de Avaliações

O sistema de avaliações é bidirecional, permitindo tanto a avaliação de instrumentos quanto de usuários.

**Avaliação de Instrumento (pelo Locatário)**:
Após a conclusão de uma reserva, o locatário pode avaliar o instrumento alugado através de:
- Seleção de nota de 1 a 5 estrelas
- Campo de comentário opcional descrevendo a experiência
- Botão de confirmação de envio

A avaliação é registrada no Firestore e automaticamente agregada à nota média do instrumento, que é recalculada e atualizada.

**[Inserir captura de tela: Tela de avaliação de instrumento]**

**Avaliação de Usuário (pelo Proprietário)**:
Similarmente, proprietários podem avaliar locatários após a devolução do instrumento, fornecendo feedback sobre a experiência de aluguel. Este sistema promove comportamento responsável por parte dos locatários.

**[Inserir captura de tela: Tela de avaliação de usuário]**

**Visualização de Minhas Avaliações**:
Usuários podem acessar uma seção dedicada para visualizar todas as avaliações recebidas, tanto como proprietários (avaliações de seus instrumentos) quanto como locatários (avaliações de seu comportamento). As avaliações são organizadas em abas correspondentes.

**[Inserir captura de tela: Visualização de minhas avaliações]**

### 4.4.10 Gerenciamento de Perfil

O sistema oferece funcionalidades completas de gerenciamento de perfil de usuário.

**Visualização de Perfil**:
A tela de perfil apresenta:
- Foto de perfil do usuário
- Nome completo
- Email e telefone
- Avaliação média recebida como locatário
- Total de avaliações recebidas
- Botão para editar perfil
- Abas para visualizar diferentes informações:
  - **Dados**: Informações pessoais
  - **Avaliações**: Avaliações recebidas como locatário

**[Inserir captura de tela: Tela de perfil]**

**Edição de Perfil**:
A interface de edição permite:
- Alteração da foto de perfil através de upload de nova imagem
- Modificação do nome
- Atualização do número de telefone
- Salvamento das alterações no Firestore

O email não é editável através desta interface, seguindo boas práticas de segurança.

**[Inserir captura de tela: Tela de edição de perfil]**

### 4.4.11 Sistema de Favoritos

Usuários podem marcar instrumentos como favoritos para fácil acesso posterior.

**Adicionar aos Favoritos**:
Na tela de detalhes de qualquer instrumento, um ícone de coração permite adicionar/remover o item dos favoritos. A ação é sincronizada imediatamente com o Firestore.

**Lista de Favoritos**:
A seção "Favoritos" do menu apresenta todos os instrumentos marcados pelo usuário, com layout similar ao catálogo principal. Tocar em um item direciona para sua tela de detalhes.

**[Inserir captura de tela: Lista de favoritos]**

## 4.5 Elementos de Interface e Design

### 4.5.1 Paleta de Cores

O aplicativo utiliza uma paleta de cores coesa baseada em tons terrosos e quentes:
- **Cor primária**: Terracota (#E07856) - utilizada em elementos principais como botões de ação e destaques
- **Cor secundária**: Laranja (#FF8C42) - aplicada em elementos secundários e indicadores
- **Fundo**: Tons claros de bege e branco para máxima legibilidade
- **Texto**: Cinza escuro (#333333) para corpo de texto e preto para títulos

Esta paleta foi selecionada para transmitir sensações de calor, criatividade e conexão, valores alinhados ao propósito do compartilhamento de instrumentos musicais.

### 4.5.2 Tipografia

O sistema tipográfico utiliza fontes sans-serif padrão do Android (Roboto), com variações de peso para estabelecer hierarquia:
- **Títulos principais**: Roboto Bold, 20-24sp
- **Subtítulos**: Roboto Medium, 16-18sp
- **Corpo de texto**: Roboto Regular, 14sp
- **Textos auxiliares**: Roboto Light, 12sp

### 4.5.3 Iconografia

O aplicativo emprega um conjunto consistente de ícones vetoriais (Vector Drawables) para navegação e ações:
- Ícones de categoria para cada tipo de instrumento (cordas, sopro, teclas, percussão)
- Ícones de ação (adicionar, editar, excluir, favoritar, compartilhar)
- Ícones de navegação (menu, voltar, pesquisar)
- Ícones de status (notificações, mensagens não lidas)

**[Inserir captura de tela: Conjunto de ícones utilizados]**

### 4.5.4 Componentes de Interface

O protótipo utiliza componentes do Material Design, incluindo:
- **CardView**: Para exibição de itens de instrumento, reservas e solicitações
- **RecyclerView**: Para listas roláveis eficientes
- **TabLayout**: Para navegação entre diferentes categorias de informação
- **FloatingActionButton**: Para ação principal de adicionar instrumento
- **NavigationDrawer**: Para menu lateral de navegação
- **BottomNavigationView**: Preparado para navegação inferior (implementação futura)
- **Dialogs**: Para confirmações e alertas importantes

### 4.5.5 Feedback Visual

O sistema implementa diversos mecanismos de feedback ao usuário:
- **Indicadores de carregamento**: Exibidos durante operações de rede
- **Mensagens toast**: Para feedback de ações rápidas
- **Snackbars**: Para confirmações com opção de desfazer
- **Animações de transição**: Entre telas e estados
- **Estados visuais de botões**: Normal, pressionado, desabilitado
- **Badges numéricos**: Para notificações e mensagens não lidas

## 4.6 Fluxos de Usuário Principais

### 4.6.1 Fluxo de Aluguel (Perspectiva do Locatário)

1. Usuário acessa o aplicativo e visualiza o catálogo de instrumentos
2. Filtra por categoria ou busca instrumento específico
3. Seleciona instrumento de interesse para ver detalhes
4. Visualiza avaliações e disponibilidade
5. Clica em "Solicitar Reserva"
6. Seleciona período desejado e adiciona observações
7. Confirma solicitação
8. Aguarda resposta do proprietário
9. Recebe notificação de aceite ou recusa
10. Pode iniciar conversa com proprietário para combinar detalhes
11. Após devolução do instrumento, avalia a experiência

### 4.6.2 Fluxo de Aluguel (Perspectiva do Proprietário)

1. Proprietário cadastra instrumento com foto e informações
2. Recebe notificação de nova solicitação
3. Acessa lista de solicitações pendentes
4. Visualiza detalhes da solicitação e informações do solicitante
5. Decide aceitar ou recusar
6. Pode comunicar-se via chat com o locatário
7. Após devolução do instrumento, avalia o locatário

### 4.6.3 Fluxo de Comunicação

1. Usuário acessa detalhes de um instrumento
2. Clica no botão de mensagem/chat
3. Sistema verifica se já existe conversa sobre este instrumento entre os usuários
4. Abre conversa existente ou cria nova
5. Usuário digita e envia mensagem
6. Mensagem é sincronizada em tempo real
7. Destinatário recebe notificação visual de nova mensagem
8. Ambos usuários podem continuar a conversa de forma assíncrona

## 4.7 Aspectos de Segurança e Privacidade

### 4.7.1 Autenticação

O sistema de autenticação do Firebase garante:
- Senhas criptografadas e nunca armazenadas em texto plano
- Validação de email antes do primeiro acesso
- Tokens de sessão seguros e renováveis
- Proteção contra ataques de força bruta

### 4.7.2 Regras de Segurança do Firestore

Foram implementadas regras de segurança no Firestore que garantem:
- Usuários só podem ler e modificar seus próprios dados de perfil
- Usuários autenticados podem ler informações públicas de outros usuários
- Apenas proprietários podem criar, editar ou excluir seus instrumentos
- Solicitações só podem ser criadas pelo próprio solicitante
- Apenas solicitante e proprietário podem visualizar e editar solicitações
- Mensagens só são acessíveis aos participantes da conversa
- Avaliações só podem ser criadas pelo usuário autorizado (locatário ou proprietário, conforme o caso)

### 4.7.3 Validação de Dados

O aplicativo implementa validação em múltiplas camadas:
- Validação no lado do cliente antes de enviar dados
- Validação através de regras do Firestore
- Sanitização de entradas de usuário para prevenir injeção de código
- Verificação de permissões antes de operações críticas

### 4.7.4 Privacidade de Dados

O sistema respeita a privacidade dos usuários:
- Informações de contato (email e telefone) só são visíveis em contextos relevantes (solicitações, conversas)
- Usuários controlam quais informações compartilhar em seu perfil
- Histórico de conversas é privado entre os participantes
- Sistema preparado para implementação de funcionalidades de exclusão de dados conforme LGPD

## 4.8 Desempenho e Otimizações

### 4.8.1 Carregamento de Imagens

O sistema implementa otimizações para carregamento eficiente de imagens:
- Compressão automática de imagens antes do upload
- Redimensionamento de imagens para dimensões apropriadas
- Cache de imagens já carregadas para evitar downloads repetidos
- Carregamento progressivo (placeholders enquanto imagens carregam)

### 4.8.2 Paginação e Carregamento Incremental

Listas extensas implementam:
- Carregamento incremental de dados conforme usuário rola a lista
- Limite inicial de itens carregados para reduzir tempo de carregamento inicial
- Listeners do Firestore que atualizam listas apenas quando necessário

### 4.8.3 Sincronização de Dados

O sistema utiliza estratégias de sincronização eficientes:
- Listeners de tempo real apenas em telas ativas
- Desconexão automática de listeners quando telas são fechadas
- Cache local através do Room Database para operação offline parcial
- Sincronização em background de dados essenciais

## 4.9 Tratamento de Erros e Situações Excepcionais

O aplicativo implementa tratamento robusto de erros:

### 4.9.1 Erros de Rede

- Detecção de ausência de conexão com internet
- Mensagens informativas ao usuário sobre problemas de conectividade
- Tentativas automáticas de reconexão em operações críticas
- Modo de visualização limitada com dados em cache

### 4.9.2 Erros de Autenticação

- Mensagens claras sobre credenciais inválidas
- Orientação para recuperação de senha
- Feedback sobre problemas de criação de conta (email já cadastrado, senha fraca)
- Tratamento de expiração de sessão

### 4.9.3 Erros de Validação

- Validação em tempo real de campos de formulário
- Mensagens de erro específicas e orientativas
- Prevenção de envio de formulários com dados inválidos
- Destaque visual de campos com problemas

### 4.9.4 Erros de Operações do Firebase

- Tratamento de falhas em upload de imagens
- Gerenciamento de conflitos de gravação no Firestore
- Feedback ao usuário sobre operações que falharam
- Mecanismos de retry para operações importantes

## 4.10 Acessibilidade

O protótipo incorpora considerações básicas de acessibilidade:
- Contraste adequado entre texto e fundo para legibilidade
- Tamanhos de fonte ajustáveis conforme configurações do sistema
- Áreas de toque adequadas para botões e elementos interativos (mínimo 48dp)
- Descrições de conteúdo (content descriptions) para leitores de tela em elementos visuais
- Navegação por teclado funcional em campos de formulário

## 4.11 Compatibilidade e Testes

### 4.11.1 Dispositivos Suportados

O aplicativo foi testado em:
- Smartphones com telas de 5 a 6,7 polegadas
- Resoluções de hdpi a xxhdpi
- Diferentes versões do Android (6.0 a 13.0)

### 4.11.2 Orientação de Tela

O protótipo foi desenvolvido primariamente para orientação vertical (portrait), com suporte básico para rotação de tela mantendo estado de dados.

### 4.11.3 Testes Realizados

Durante o desenvolvimento, foram conduzidos:
- Testes funcionais de todas as features principais
- Testes de interface em diferentes tamanhos de tela
- Testes de conectividade e sincronização com Firebase
- Testes de fluxos completos de usuário
- Testes de validação de dados e tratamento de erros

## 4.12 Limitações do Protótipo

O protótipo atual apresenta algumas limitações que podem ser endereçadas em versões futuras:

1. **Sistema de Pagamento**: Não implementado - o sistema atual apenas gerencia solicitações e confirmações, mas não processa pagamentos. A integração com gateways de pagamento seria necessária para um sistema completo.

2. **Notificações Push**: Estrutura preparada, mas não completamente implementada. Usuários não recebem notificações em tempo real fora do aplicativo.

3. **Geolocalização**: Não implementado - seria útil para filtrar instrumentos por proximidade geográfica.

4. **Sistema de Denúncia**: Ausente - importante para moderação de conteúdo e comportamento de usuários.

5. **Verificação de Identidade**: Não implementado - sistema de verificação de identidade aumentaria confiança entre usuários.

6. **Suporte Offline**: Limitado - muitas funcionalidades requerem conexão ativa com internet.

7. **Histórico de Transações**: Não há relatórios ou visualizações de histórico completo para usuários.

8. **Sistema de Disputas**: Ausente - mecanismo para resolução de conflitos entre usuários.

9. **Múltiplas Fotos por Instrumento**: Limitado a uma foto principal por instrumento.

10. **Calendário de Proprietário**: Não há sincronização com calendários externos do dispositivo.

## 4.13 Considerações Finais sobre o Protótipo

O protótipo desenvolvido demonstra a viabilidade técnica de uma plataforma de compartilhamento de instrumentos musicais. As funcionalidades implementadas cobrem os casos de uso essenciais identificados na fase de análise de requisitos, proporcionando uma experiência completa de aluguel peer-to-peer.

A arquitetura escolhida, baseada em Firebase, mostrou-se adequada para o escopo do projeto, oferecendo escalabilidade, sincronização em tempo real e redução da complexidade de infraestrutura de backend. A implementação em Android nativo garantiu desempenho fluido e acesso às APIs mais recentes da plataforma.

O sistema de avaliações bidirecionais implementado é fundamental para estabelecer confiança na comunidade de usuários, enquanto o chat integrado facilita a comunicação direta entre as partes envolvidas no aluguel.

As interfaces desenvolvidas seguem princípios de Material Design, resultando em uma experiência visual consistente e intuitiva para usuários familiarizados com o ecossistema Android.

O protótipo constitui uma base sólida para evolução futura do sistema, com arquitetura preparada para incorporação das funcionalidades atualmente limitadas, como sistema de pagamentos, notificações push e geolocalização, que podem ser desenvolvidas em fases subsequentes do projeto.
