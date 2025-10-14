package com.example.instrumentaliza;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;

// Imports para modelos de chat e mensagens
import com.example.instrumentaliza.models.FirebaseChat;
import com.example.instrumentaliza.models.FirebaseMessage;
import com.example.instrumentaliza.models.FirebaseAvaliacao;

/**
 * GerenciadorFirebase - Classe central de gerenciamento do Firebase
 * 
 * Esta é a classe mais importante do sistema, responsável por toda a integração
 * com os serviços do Firebase (Authentication, Firestore, Storage). Centraliza
 * todas as operações de dados, autenticação e armazenamento do aplicativo.
 * 
 * Serviços integrados:
 * - Firebase Authentication: login, registro, logout, verificação de sessão
 * - Firebase Firestore: CRUD de usuários, instrumentos, reservas, favoritos, chats
 * - Firebase Storage: upload e download de imagens de instrumentos e perfis
 * 
 * Funcionalidades principais:
 * 
 * AUTENTICAÇÃO:
 * - Login com email e senha
 * - Registro de novos usuários
 * - Logout e gerenciamento de sessão
 * - Verificação de status de autenticação
 * 
 * USUÁRIOS:
 * - Criação e atualização de perfis
 * - Upload de fotos de perfil
 * - Busca e recuperação de dados do usuário
 * 
 * INSTRUMENTOS:
 * - CRUD completo de instrumentos
 * - Upload de imagens de instrumentos
 * - Busca e filtros por categoria
 * - Gerenciamento de disponibilidade
 * 
 * RESERVAS:
 * - Criação e gerenciamento de reservas
 * - Verificação de conflitos de datas
 * - Busca de reservas por usuário
 * 
 * FAVORITOS:
 * - Adicionar/remover instrumentos dos favoritos
 * - Listar instrumentos favoritos do usuário
 * 
 * CHAT E MENSAGENS:
 * - Criação e gerenciamento de chats
 * - Envio e recebimento de mensagens
 * - Busca de conversas do usuário
 * 
 * Características técnicas:
 * - Padrão Singleton para instâncias Firebase
 * - Uso de CompletableFuture para operações assíncronas
 * - Tratamento robusto de erros
 * - Logs detalhados para debugging
 * - Validação de dados antes das operações
 * 
 * @author Jhonata
 * @version 1.0
 */
public class GerenciadorFirebase {
    
    // Constantes
    private static final String TAG = "GerenciadorFirebase";
    
    // Instâncias singleton do Firebase
    private static FirebaseAuth autenticacao;
    private static FirebaseFirestore firestore;
    private static FirebaseStorage armazenamento;
    
    // Constantes para coleções do Firestore
    private static final String COLECAO_USUARIOS = "users";
    private static final String COLECAO_INSTRUMENTOS = "instruments";
    private static final String COLECAO_RESERVAS = "reservations";
    private static final String COLECAO_FAVORITOS = "favorites";
    private static final String COLECAO_CHATS = "chats";
    private static final String COLECAO_MENSAGENS = "messages";
    
    // Constantes para diretórios do Firebase Storage
    private static final String ARMAZENAMENTO_INSTRUMENTOS = "instruments";
    private static final String ARMAZENAMENTO_PERFIS = "profiles";
    
    /**
     * Inicializa todas as instâncias do Firebase
     * 
     * Este método deve ser chamado antes de qualquer operação com o Firebase.
     * Implementa o padrão Singleton para garantir que apenas uma instância
     * de cada serviço seja criada durante o ciclo de vida do aplicativo.
     * 
     * Serviços inicializados:
     * - Firebase Authentication: para autenticação de usuários
     * - Firebase Firestore: para operações de banco de dados
     * - Firebase Storage: para upload/download de arquivos
     * 
     * @param contexto Contexto da aplicação (geralmente Activity)
     */
    public static void inicializar(Context contexto) {
        if (autenticacao == null) {
            autenticacao = FirebaseAuth.getInstance();
        }
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        if (armazenamento == null) {
            armazenamento = FirebaseStorage.getInstance();
            Log.d(TAG, "FirebaseStorage inicializado");
        }
        Log.d(TAG, "Firebase inicializado com sucesso - Auth: " + (autenticacao != null) + ", Firestore: " + (firestore != null) + ", Storage: " + (armazenamento != null));
    }
    
    // ==================== AUTENTICAÇÃO ====================
    
    /**
     * Realiza login do usuário com email e senha
     * 
     * Autentica o usuário no Firebase Authentication usando as credenciais
     * fornecidas. Retorna um CompletableFuture para operação assíncrona.
     * 
     * @param email Email do usuário
     * @param senha Senha do usuário
     * @return CompletableFuture<FirebaseUser> Usuário autenticado em caso de sucesso
     */
    public static CompletableFuture<FirebaseUser> entrarComEmailESenha(String email, String senha) {
        CompletableFuture<FirebaseUser> futuro = new CompletableFuture<>();
        
        // Garantir que autenticacao está inicializado
        if (autenticacao == null) {
            autenticacao = FirebaseAuth.getInstance();
        }
        
        autenticacao.signInWithEmailAndPassword(email, senha)
                .addOnCompleteListener(tarefa -> {
                    if (tarefa.isSuccessful()) {
                        FirebaseUser usuario = tarefa.getResult().getUser();
                        Log.d(TAG, "Login bem-sucedido: " + usuario.getEmail());
                        futuro.complete(usuario);
                    } else {
                        Log.e(TAG, "Erro no login: " + tarefa.getException().getMessage());
                        futuro.completeExceptionally(tarefa.getException());
                    }
                });
        
        return futuro;
    }
    
    /**
     * Cria um novo usuário no Firebase Authentication
     * 
     * Registra um novo usuário no sistema com email, senha e nome.
     * Após a criação, atualiza o perfil do usuário com o nome fornecido.
     * 
     * @param email Email do novo usuário
     * @param senha Senha do novo usuário
     * @param nome Nome completo do usuário
     * @return CompletableFuture<FirebaseUser> Usuário criado em caso de sucesso
     */
    public static CompletableFuture<FirebaseUser> criarUsuarioComEmailESenha(String email, String senha, String nome) {
        CompletableFuture<FirebaseUser> futuro = new CompletableFuture<>();
        
        // Garantir que autenticacao está inicializado
        if (autenticacao == null) {
            autenticacao = FirebaseAuth.getInstance();
        }
        
        autenticacao.createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(tarefa -> {
                    if (tarefa.isSuccessful()) {
                        FirebaseUser usuario = tarefa.getResult().getUser();
                        
                        // Atualizar nome do perfil
                        UserProfileChangeRequest atualizacoesPerfil = new UserProfileChangeRequest.Builder()
                                .setDisplayName(nome)
                                .build();
                        
                        usuario.updateProfile(atualizacoesPerfil)
                                .addOnCompleteListener(tarefaPerfil -> {
                                    if (tarefaPerfil.isSuccessful()) {
                                        Log.d(TAG, "Usuário criado com sucesso: " + usuario.getEmail());
                                        futuro.complete(usuario);
                                    } else {
                                        Log.e(TAG, "Erro ao atualizar perfil: " + tarefaPerfil.getException().getMessage());
                                        futuro.completeExceptionally(tarefaPerfil.getException());
                                    }
                                });
                    } else {
                        Log.e(TAG, "Erro ao criar usuário: " + tarefa.getException().getMessage());
                        futuro.completeExceptionally(tarefa.getException());
                    }
                });
        
        return futuro;
    }
    
    /**
     * Realiza logout do usuário atual
     * 
     * Desconecta o usuário do Firebase Authentication, encerrando a sessão atual.
     * Após o logout, o usuário precisará fazer login novamente para acessar
     * funcionalidades que requerem autenticação.
     */
    public static void sair() {
        // Garantir que autenticacao está inicializado
        if (autenticacao == null) {
            autenticacao = FirebaseAuth.getInstance();
        }
        autenticacao.signOut();
        Log.d(TAG, "Usuário deslogado");
    }
    
    /**
     * Obtém o usuário atualmente autenticado
     * 
     * @return FirebaseUser Usuário atual ou null se não estiver logado
     */
    public static FirebaseUser obterUsuarioAtual() {
        // Garantir que autenticacao está inicializado
        if (autenticacao == null) {
            autenticacao = FirebaseAuth.getInstance();
        }
        return autenticacao.getCurrentUser();
    }
    
    /**
     * Verifica se há um usuário logado no sistema
     * 
     * @return boolean true se há usuário logado, false caso contrário
     */
    public static boolean usuarioEstaLogado() {
        // Garantir que autenticacao está inicializado
        if (autenticacao == null) {
            autenticacao = FirebaseAuth.getInstance();
            Log.d(TAG, "FirebaseAuth inicializado em usuarioEstaLogado");
        }
        
        FirebaseUser usuarioAtual = autenticacao.getCurrentUser();
        boolean estaLogado = usuarioAtual != null;
        
        Log.d(TAG, "usuarioEstaLogado chamado - auth: " + (autenticacao != null) + ", currentUser: " + (usuarioAtual != null));
        if (usuarioAtual != null) {
            Log.d(TAG, "Usuário atual: " + usuarioAtual.getEmail() + " (UID: " + usuarioAtual.getUid() + ")");
        }
        
        return estaLogado;
    }
    
    // ==================== USUÁRIOS ====================
    // Seção responsável por operações relacionadas aos usuários no Firestore
    
    /**
     * Cria documento do usuário no Firestore
     * 
     * Após a criação do usuário no Firebase Auth, este método cria
     * um documento correspondente no Firestore com dados adicionais.
     * 
     * @param usuarioFirebase Usuário criado no Firebase Auth
     * @param nome Nome completo do usuário
     * @param telefone Telefone de contato do usuário
     * @return CompletableFuture<Void> Completa quando documento é criado
     */
    public static CompletableFuture<Void> criarDocumentoUsuario(FirebaseUser usuarioFirebase, String nome, String telefone) {
        CompletableFuture<Void> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Criando documento do usuário para: " + usuarioFirebase.getUid());
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        Map<String, Object> dadosUsuario = new HashMap<>();
        dadosUsuario.put("name", nome);
        dadosUsuario.put("email", usuarioFirebase.getEmail());
        dadosUsuario.put("phone", telefone);
        dadosUsuario.put("createdAt", Timestamp.now()); // Usar Timestamp em vez de Date
        dadosUsuario.put("profileImageUri", null);
        
        Log.d(TAG, "Dados do usuário preparados: " + dadosUsuario);
        
        firestore.collection(COLECAO_USUARIOS)
                .document(usuarioFirebase.getUid())
                .set(dadosUsuario)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Documento do usuário criado com sucesso no Firestore");
                    futuro.complete(null);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao criar documento do usuário no Firestore: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    public static CompletableFuture<Map<String, Object>> obterDadosUsuario(String idUsuario) {
        CompletableFuture<Map<String, Object>> futuro = new CompletableFuture<>();
        
        firestore.collection(COLECAO_USUARIOS)
                .document(idUsuario)
                .get()
                .addOnSuccessListener(documentoSnapshot -> {
                    if (documentoSnapshot.exists()) {
                        Map<String, Object> dadosUsuario = documentoSnapshot.getData();
                        Log.d(TAG, "Dados do usuário carregados: " + dadosUsuario.get("name"));
                        futuro.complete(dadosUsuario);
                    } else {
                        Log.w(TAG, "Documento do usuário não encontrado");
                        futuro.completeExceptionally(new Exception("Usuário não encontrado"));
                    }
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao carregar dados do usuário: " + erro.getMessage());
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    public static CompletableFuture<Void> atualizarDadosUsuario(String idUsuario, Map<String, Object> atualizacoes) {
        CompletableFuture<Void> futuro = new CompletableFuture<>();
        
        firestore.collection(COLECAO_USUARIOS)
                .document(idUsuario)
                .update(atualizacoes)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Dados do usuário atualizados com sucesso");
                    futuro.complete(null);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao atualizar dados do usuário: " + erro.getMessage());
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    // ==================== INSTRUMENTOS ====================
    // Seção responsável por operações CRUD de instrumentos musicais
    
    public static CompletableFuture<String> criarInstrumento(String idProprietario, String nome, String descricao, 
                                                           String categoria, double preco, String uriImagem) {
        CompletableFuture<String> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "criarInstrumento chamado - idProprietario: " + idProprietario + ", nome: " + nome + ", categoria: " + categoria);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore inicializado em criarInstrumento");
        }
        
        Map<String, Object> dadosInstrumento = new HashMap<>();
        dadosInstrumento.put("ownerId", idProprietario);
        dadosInstrumento.put("name", nome);
        dadosInstrumento.put("description", descricao);
        dadosInstrumento.put("category", categoria);
        dadosInstrumento.put("price", preco);
        dadosInstrumento.put("imageUri", uriImagem);
        dadosInstrumento.put("createdAt", Timestamp.now()); // Usar Timestamp em vez de Date
        dadosInstrumento.put("available", true);
        dadosInstrumento.put("unavailableRanges", new ArrayList<>()); // Lista vazia de faixas indisponíveis
        
        Log.d(TAG, "Dados do instrumento preparados: " + dadosInstrumento);
        Log.d(TAG, "Tentando salvar na coleção: " + COLECAO_INSTRUMENTOS);
        
        firestore.collection(COLECAO_INSTRUMENTOS)
                .add(dadosInstrumento)
                .addOnSuccessListener(documentReference -> {
                    String idInstrumento = documentReference.getId();
                    Log.d(TAG, "Instrumento criado com sucesso: " + idInstrumento);
                    futuro.complete(idInstrumento);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao criar instrumento: " + e.getMessage(), e);
                    futuro.completeExceptionally(e);
                });
        
        return futuro;
    }
    
    public static CompletableFuture<List<DocumentSnapshot>> obterTodosInstrumentos() {
        CompletableFuture<List<DocumentSnapshot>> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "obterTodosInstrumentos chamado");
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore inicializado em obterTodosInstrumentos");
        }
        
        Log.d(TAG, "Consultando coleção: " + COLECAO_INSTRUMENTOS);
        Log.d(TAG, "Filtros: available = true (sem ordenação temporariamente)");
        
        // Consulta simplificada temporariamente (sem ordenação) até o índice ser criado
        firestore.collection(COLECAO_INSTRUMENTOS)
                .whereEqualTo("available", true)
                .get()
                .addOnSuccessListener(snapshotConsulta -> {
                    List<DocumentSnapshot> instrumentos = snapshotConsulta.getDocuments();
                    Log.d(TAG, "Instrumentos carregados com sucesso: " + instrumentos.size());
                    
                    // Ordenar localmente por enquanto
                    instrumentos.sort((a, b) -> {
                        Object dataA = a.get("createdAt");
                        Object dataB = b.get("createdAt");
                        
                        if (dataA instanceof Timestamp && dataB instanceof Timestamp) {
                            return ((Timestamp) dataB).compareTo((Timestamp) dataA); // Mais recente primeiro
                        } else if (dataA instanceof Date && dataB instanceof Date) {
                            return ((Date) dataB).compareTo((Date) dataA); // Mais recente primeiro
                        }
                        return 0;
                    });
                    
                    // Log detalhado de cada instrumento
                    for (DocumentSnapshot documento : instrumentos) {
                        Log.d(TAG, "Instrumento: " + documento.get("name") + " - Categoria: " + documento.get("category") + " - Preço: " + documento.get("price"));
                    }
                    
                    futuro.complete(instrumentos);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao carregar instrumentos: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    public static CompletableFuture<List<DocumentSnapshot>> obterInstrumentosPorCategoria(String categoria) {
        CompletableFuture<List<DocumentSnapshot>> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "obterInstrumentosPorCategoria chamado para categoria: " + categoria);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        // Consulta simplificada temporariamente (sem ordenação) até o índice ser criado
        firestore.collection(COLECAO_INSTRUMENTOS)
                .whereEqualTo("category", categoria)
                .whereEqualTo("available", true)
                .get()
                .addOnSuccessListener(snapshotConsulta -> {
                    List<DocumentSnapshot> instrumentos = snapshotConsulta.getDocuments();
                    Log.d(TAG, "Instrumentos da categoria " + categoria + ": " + instrumentos.size());
                    
                    // Ordenar localmente por enquanto
                    instrumentos.sort((a, b) -> {
                        Object dataA = a.get("createdAt");
                        Object dataB = b.get("createdAt");
                        
                        if (dataA instanceof Timestamp && dataB instanceof Timestamp) {
                            return ((Timestamp) dataB).compareTo((Timestamp) dataA); // Mais recente primeiro
                        } else if (dataA instanceof Date && dataB instanceof Date) {
                            return ((Date) dataB).compareTo((Date) dataA); // Mais recente primeiro
                        }
                        return 0;
                    });
                    
                    futuro.complete(instrumentos);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao carregar instrumentos por categoria: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    public static CompletableFuture<List<DocumentSnapshot>> obterInstrumentosPorProprietario(String idProprietario) {
        CompletableFuture<List<DocumentSnapshot>> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "obterInstrumentosPorProprietario chamado para proprietário: " + idProprietario);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        // Buscar instrumentos do proprietário específico
        firestore.collection(COLECAO_INSTRUMENTOS)
                .whereEqualTo("ownerId", idProprietario)
                .get()
                .addOnSuccessListener(snapshotConsulta -> {
                    List<DocumentSnapshot> instrumentos = snapshotConsulta.getDocuments();
                    Log.d(TAG, "Instrumentos do proprietário " + idProprietario + ": " + instrumentos.size());
                    
                    // Ordenar localmente por data de criação (mais recente primeiro)
                    instrumentos.sort((a, b) -> {
                        Object dataA = a.get("createdAt");
                        Object dataB = b.get("createdAt");
                        
                        if (dataA instanceof Timestamp && dataB instanceof Timestamp) {
                            return ((Timestamp) dataB).compareTo((Timestamp) dataA);
                        } else if (dataA instanceof Date && dataB instanceof Date) {
                            return ((Date) dataB).compareTo((Date) dataA);
                        }
                        return 0;
                    });
                    
                    futuro.complete(instrumentos);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao carregar instrumentos do proprietário: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    public static CompletableFuture<List<DocumentSnapshot>> buscarInstrumentos(String consulta) {
        CompletableFuture<List<DocumentSnapshot>> futuro = new CompletableFuture<>();
        
        // Busca por nome ou descrição
        firestore.collection(COLECAO_INSTRUMENTOS)
                .whereEqualTo("available", true)
                .get()
                .addOnSuccessListener(snapshotConsulta -> {
                    List<DocumentSnapshot> instrumentos = snapshotConsulta.getDocuments();
                    // Filtrar localmente por nome ou descrição
                    instrumentos.removeIf(documento -> {
                        String nome = (String) documento.get("name");
                        String descricao = (String) documento.get("description");
                        return !nome.toLowerCase().contains(consulta.toLowerCase()) &&
                               !descricao.toLowerCase().contains(consulta.toLowerCase());
                    });
                    Log.d(TAG, "Instrumentos encontrados na busca: " + instrumentos.size());
                    futuro.complete(instrumentos);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro na busca de instrumentos: " + erro.getMessage());
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    public static CompletableFuture<DocumentSnapshot> obterInstrumentoPorId(String idInstrumento) {
        CompletableFuture<DocumentSnapshot> futuro = new CompletableFuture<>();
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        firestore.collection(COLECAO_INSTRUMENTOS)
                .document(idInstrumento)
                .get()
                .addOnSuccessListener(snapshotDocumento -> {
                    if (snapshotDocumento.exists()) {
                        Log.d(TAG, "Instrumento carregado: " + snapshotDocumento.get("name"));
                        futuro.complete(snapshotDocumento);
                    } else {
                        Log.w(TAG, "Instrumento não encontrado");
                        futuro.completeExceptionally(new Exception("Instrumento não encontrado"));
                    }
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao carregar instrumento: " + erro.getMessage());
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    public static CompletableFuture<Void> atualizarInstrumento(String idInstrumento, Map<String, Object> atualizacoes) {
        CompletableFuture<Void> futuro = new CompletableFuture<>();
        
        firestore.collection(COLECAO_INSTRUMENTOS)
                .document(idInstrumento)
                .update(atualizacoes)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Instrumento atualizado com sucesso");
                    futuro.complete(null);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao atualizar instrumento: " + erro.getMessage());
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Atualiza um instrumento com os dados específicos fornecidos
     * 
     * @param idInstrumento ID do instrumento a ser atualizado
     * @param nome Novo nome do instrumento
     * @param categoria Nova categoria do instrumento
     * @param preco Novo preço do instrumento
     * @param descricao Nova descrição do instrumento
     * @param urlImagem Nova URL da imagem do instrumento
     * @return CompletableFuture<Boolean> indicando sucesso da operação
     */
    public static CompletableFuture<Boolean> atualizarInstrumento(String idInstrumento, String nome, String categoria, 
                                                                 double preco, String descricao, String urlImagem) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "=== ATUALIZANDO INSTRUMENTO ===");
        Log.d(TAG, "ID: " + idInstrumento);
        Log.d(TAG, "Nome: " + nome);
        Log.d(TAG, "Categoria: " + categoria);
        Log.d(TAG, "Preço: " + preco);
        Log.d(TAG, "Descrição: " + descricao);
        Log.d(TAG, "URL Imagem: " + urlImagem);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore inicializado em atualizarInstrumento");
        }
        
        Map<String, Object> atualizacoes = new HashMap<>();
        atualizacoes.put("name", nome);
        atualizacoes.put("category", categoria);
        atualizacoes.put("price", preco);
        atualizacoes.put("description", descricao);
        atualizacoes.put("imageUri", urlImagem);
        atualizacoes.put("updatedAt", Timestamp.now());
        
        firestore.collection(COLECAO_INSTRUMENTOS)
                .document(idInstrumento)
                .update(atualizacoes)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Instrumento atualizado com sucesso");
                    futuro.complete(true);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao atualizar instrumento: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    public static CompletableFuture<Boolean> deletarInstrumento(String idInstrumento) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore inicializado em deletarInstrumento");
        }
        
        firestore.collection(COLECAO_INSTRUMENTOS)
                .document(idInstrumento)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Instrumento deletado com sucesso");
                    futuro.complete(true);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao deletar instrumento: " + erro.getMessage(), erro);
                    futuro.complete(false);
                });
        
        return futuro;
    }
    
    // ==================== RESERVAS ====================
    // Seção responsável por operações de reservas de instrumentos
    
    public static CompletableFuture<String> criarReserva(String idUsuario, String idInstrumento, 
                                                         Date dataInicio, Date dataFim, double precoTotal) {
        CompletableFuture<String> futuro = new CompletableFuture<>();
        
        Map<String, Object> dadosReserva = new HashMap<>();
        dadosReserva.put("userId", idUsuario);
        dadosReserva.put("idInstrumento", idInstrumento);
        dadosReserva.put("startDate", dataInicio);
        dadosReserva.put("endDate", dataFim);
        dadosReserva.put("totalPrice", precoTotal);
        dadosReserva.put("status", "PENDENTE");
        dadosReserva.put("createdAt", new Date());
        
        firestore.collection(COLECAO_RESERVAS)
                .add(dadosReserva)
                .addOnSuccessListener(referenciaDocumento -> {
                    String idReserva = referenciaDocumento.getId();
                    Log.d(TAG, "Reserva criada com sucesso: " + idReserva);
                    futuro.complete(idReserva);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao criar reserva: " + erro.getMessage());
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    public static CompletableFuture<List<DocumentSnapshot>> obterReservasUsuario(String idUsuario) {
        CompletableFuture<List<DocumentSnapshot>> futuro = new CompletableFuture<>();
        
        firestore.collection(COLECAO_RESERVAS)
                .whereEqualTo("userId", idUsuario)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshotConsulta -> {
                    List<DocumentSnapshot> reservas = snapshotConsulta.getDocuments();
                    Log.d(TAG, "Reservas do usuário carregadas: " + reservas.size());
                    futuro.complete(reservas);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao carregar reservas do usuário: " + erro.getMessage());
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    public static CompletableFuture<List<DocumentSnapshot>> obterReservasSobrepostas(String idInstrumento, 
                                                                                     Date dataInicio, Date dataFim) {
        CompletableFuture<List<DocumentSnapshot>> futuro = new CompletableFuture<>();
        
        firestore.collection(COLECAO_RESERVAS)
                .whereEqualTo("idInstrumento", idInstrumento)
                .whereNotIn("status", List.of("CANCELADA"))
                .get()
                .addOnSuccessListener(snapshotConsulta -> {
                    List<DocumentSnapshot> reservas = snapshotConsulta.getDocuments();
                    // Filtrar reservas sobrepostas localmente
                    reservas.removeIf(documento -> {
                        Date dataInicioDoc = (Date) documento.get("startDate");
                        Date dataFimDoc = (Date) documento.get("endDate");
                        return !(dataInicio.before(dataFimDoc) && dataFim.after(dataInicioDoc));
                    });
                    Log.d(TAG, "Reservas sobrepostas encontradas: " + reservas.size());
                    futuro.complete(reservas);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao verificar reservas sobrepostas: " + erro.getMessage());
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    // ==================== STORAGE ====================
    // Seção responsável por upload e download de arquivos no Firebase Storage
    
    public static CompletableFuture<String> enviarImagemInstrumento(Uri uriImagem, String nomeArquivo) {
        CompletableFuture<String> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "enviarImagemInstrumento chamado - URI: " + uriImagem + ", nomeArquivo: " + nomeArquivo);
        
        // Garantir que armazenamento está inicializado
        if (armazenamento == null) {
            armazenamento = FirebaseStorage.getInstance();
            Log.d(TAG, "FirebaseStorage inicializado em enviarImagemInstrumento");
        }
        
        Log.d(TAG, "Criando referência para: " + ARMAZENAMENTO_INSTRUMENTOS + "/" + nomeArquivo);
        StorageReference referenciaArmazenamento = armazenamento.getReference().child(ARMAZENAMENTO_INSTRUMENTOS + "/" + nomeArquivo);
        
        Log.d(TAG, "Iniciando upload do arquivo...");
        referenciaArmazenamento.putFile(uriImagem)
                .addOnSuccessListener(snapshotTarefa -> {
                    Log.d(TAG, "Upload concluído com sucesso, obtendo URL de download...");
                    referenciaArmazenamento.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String urlDownload = uri.toString();
                                Log.d(TAG, "Imagem do instrumento enviada: " + urlDownload);
                                futuro.complete(urlDownload);
                            })
                            .addOnFailureListener(erro -> {
                                Log.e(TAG, "Erro ao obter URL da imagem: " + erro.getMessage(), erro);
                                futuro.completeExceptionally(erro);
                            });
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao enviar imagem do instrumento: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Faz upload de uma nova imagem para um instrumento existente
     * 
     * @param uriImagem URI da nova imagem selecionada
     * @param idInstrumento ID do instrumento que está sendo editado
     * @return CompletableFuture<String> com a URL da nova imagem
     */
    public static CompletableFuture<String> fazerUploadImagemInstrumento(Uri uriImagem, String idInstrumento) {
        CompletableFuture<String> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "fazerUploadImagemInstrumento chamado - URI: " + uriImagem + ", ID Instrumento: " + idInstrumento);
        
        // Garantir que armazenamento está inicializado
        if (armazenamento == null) {
            armazenamento = FirebaseStorage.getInstance();
            Log.d(TAG, "FirebaseStorage inicializado em fazerUploadImagemInstrumento");
        }
        
        // Gerar nome único para a nova imagem
        String nomeArquivo = "instrumento_" + idInstrumento + "_" + System.currentTimeMillis() + ".jpg";
        Log.d(TAG, "Nome do arquivo gerado: " + nomeArquivo);
        
        Log.d(TAG, "Criando referência para: " + ARMAZENAMENTO_INSTRUMENTOS + "/" + nomeArquivo);
        StorageReference referenciaArmazenamento = armazenamento.getReference().child(ARMAZENAMENTO_INSTRUMENTOS + "/" + nomeArquivo);
        
        Log.d(TAG, "Iniciando upload da nova imagem...");
        referenciaArmazenamento.putFile(uriImagem)
                .addOnSuccessListener(snapshotTarefa -> {
                    Log.d(TAG, "Upload da nova imagem concluído com sucesso, obtendo URL de download...");
                    referenciaArmazenamento.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String urlDownload = uri.toString();
                                Log.d(TAG, "Nova imagem do instrumento enviada: " + urlDownload);
                                futuro.complete(urlDownload);
                            })
                            .addOnFailureListener(erro -> {
                                Log.e(TAG, "Erro ao obter URL da nova imagem: " + erro.getMessage(), erro);
                                futuro.completeExceptionally(erro);
                            });
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao enviar nova imagem do instrumento: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    // Métodos para gerenciar perfil do usuário
    public static CompletableFuture<Boolean> atualizarPerfilUsuario(String idUsuario, String nome, String email, String telefone) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Atualizando perfil do usuário: " + idUsuario + " - Nome: " + nome + ", Email: " + email + ", Telefone: " + telefone);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore inicializado em atualizarPerfilUsuario");
        }
        
        Map<String, Object> atualizacoes = new HashMap<>();
        atualizacoes.put("name", nome);
        atualizacoes.put("email", email);
        atualizacoes.put("phone", telefone);
        atualizacoes.put("updatedAt", Timestamp.now());
        
        firestore.collection(COLECAO_USUARIOS).document(idUsuario)
                .update(atualizacoes)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Perfil do usuário atualizado com sucesso");
                    futuro.complete(true);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao atualizar perfil do usuário: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    public static CompletableFuture<String> enviarImagemPerfil(Uri imageUri, String fileName) {
        CompletableFuture<String> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "enviarImagemPerfil chamado - URI: " + imageUri + ", fileName: " + fileName);
        
        // Garantir que armazenamento está inicializado
        if (armazenamento == null) {
            armazenamento = FirebaseStorage.getInstance();
            Log.d(TAG, "FirebaseStorage inicializado em enviarImagemPerfil");
        }
        
        Log.d(TAG, "Criando referência para: " + ARMAZENAMENTO_PERFIS + "/" + fileName);
        StorageReference armazenamentoRef = armazenamento.getReference().child(ARMAZENAMENTO_PERFIS + "/" + fileName);
        
        Log.d(TAG, "Iniciando upload da imagem de perfil...");
        armazenamentoRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Upload da imagem de perfil concluído, obtendo URL de download...");
                    armazenamentoRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String downloadUrl = uri.toString();
                                Log.d(TAG, "Imagem de perfil enviada: " + downloadUrl);
                                futuro.complete(downloadUrl);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Erro ao obter URL da imagem de perfil: " + e.getMessage(), e);
                                futuro.completeExceptionally(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao enviar imagem de perfil: " + e.getMessage(), e);
                    futuro.completeExceptionally(e);
                });
        
        return futuro;
    }
    
    public static CompletableFuture<Boolean> atualizarImagemPerfilUsuario(String idUsuario, String urlImagem) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Atualizando imagem de perfil do usuário: " + idUsuario);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore inicializado em atualizarImagemPerfilUsuario");
        }
        
        firestore.collection(COLECAO_USUARIOS).document(idUsuario)
                .update("profileImageUrl", urlImagem)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Imagem de perfil do usuário atualizada com sucesso");
                    futuro.complete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao atualizar imagem de perfil do usuário: " + e.getMessage(), e);
                    futuro.completeExceptionally(e);
                });
        
        return futuro;
    }

    // ==================== FAVORITOS ====================
    // Seção responsável por gerenciamento de instrumentos favoritos
    
    public static CompletableFuture<Boolean> adicionarAosFavoritos(String idUsuario, String idInstrumento) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Adicionando instrumento aos favoritos - userId: " + idUsuario + ", idInstrumento: " + idInstrumento);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore inicializado em addToFavorites");
        }
        
        Map<String, Object> favoriteData = new HashMap<>();
        favoriteData.put("userId", idUsuario);
        favoriteData.put("idInstrumento", idInstrumento);
        favoriteData.put("createdAt", Timestamp.now());
        
        firestore.collection(COLECAO_FAVORITOS)
                .add(favoriteData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Instrumento adicionado aos favoritos com sucesso");
                    futuro.complete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao adicionar aos favoritos: " + e.getMessage(), e);
                    futuro.completeExceptionally(e);
                });
        
        return futuro;
    }
    
    public static CompletableFuture<Boolean> removerDosFavoritos(String idUsuario, String idInstrumento) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Removendo instrumento dos favoritos - userId: " + idUsuario + ", idInstrumento: " + idInstrumento);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore inicializado em removeFromFavorites");
        }
        
        firestore.collection(COLECAO_FAVORITOS)
                .whereEqualTo("userId", idUsuario)
                .whereEqualTo("idInstrumento", idInstrumento)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Deletar o primeiro documento encontrado
                        querySnapshot.getDocuments().get(0).getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Instrumento removido dos favoritos com sucesso");
                                    futuro.complete(true);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Erro ao remover dos favoritos: " + e.getMessage(), e);
                                    futuro.completeExceptionally(e);
                                });
                    } else {
                        Log.w(TAG, "Favorito não encontrado");
                        futuro.complete(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao buscar favorito: " + e.getMessage(), e);
                    futuro.completeExceptionally(e);
                });
        
        return futuro;
    }
    
    public static CompletableFuture<Boolean> ehFavorito(String idUsuario, String idInstrumento) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Verificando se instrumento é favorito - userId: " + idUsuario + ", idInstrumento: " + idInstrumento);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore inicializado em isFavorite");
        }
        
        firestore.collection(COLECAO_FAVORITOS)
                .whereEqualTo("userId", idUsuario)
                .whereEqualTo("idInstrumento", idInstrumento)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean isFavorite = !querySnapshot.isEmpty();
                    Log.d(TAG, "Instrumento é favorito: " + isFavorite);
                    futuro.complete(isFavorite);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao verificar favorito: " + e.getMessage(), e);
                    futuro.completeExceptionally(e);
                });
        
        return futuro;
    }
    
    public static CompletableFuture<List<DocumentSnapshot>> obterFavoritosUsuario(String idUsuario) {
        CompletableFuture<List<DocumentSnapshot>> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Buscando favoritos do usuário: " + idUsuario);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore inicializado em obterFavoritosUsuario");
        }
        
        firestore.collection(COLECAO_FAVORITOS)
                .whereEqualTo("userId", idUsuario)
                // Removido temporariamente: .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentSnapshot> favorites = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        favorites.add(doc);
                    }
                    Log.d(TAG, "Favoritos encontrados: " + favorites.size());
                    futuro.complete(favorites);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao buscar favoritos: " + e.getMessage(), e);
                    futuro.completeExceptionally(e);
                });
        
        return futuro;
    }
    
    public static CompletableFuture<List<DocumentSnapshot>> obterInstrumentosFavoritos(String idUsuario) {
        CompletableFuture<List<DocumentSnapshot>> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Buscando instrumentos favoritos do usuário: " + idUsuario);
        
        // Primeiro buscar os IDs dos favoritos
        obterFavoritosUsuario(idUsuario)
                .thenCompose(favorites -> {
                    if (favorites.isEmpty()) {
                        futuro.complete(new ArrayList<>());
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    // Extrair IDs dos instrumentos
                    List<String> idInstrumentos = new ArrayList<>();
                    for (DocumentSnapshot favorite : favorites) {
                        String idInstrumento = favorite.getString("idInstrumento");
                        if (idInstrumento != null) {
                            idInstrumentos.add(idInstrumento);
                        }
                    }
                    
                    if (idInstrumentos.isEmpty()) {
                        futuro.complete(new ArrayList<>());
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    // Buscar os instrumentos
                    firestore.collection(COLECAO_INSTRUMENTOS)
                            .whereIn(FieldPath.documentId(), idInstrumentos)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                List<DocumentSnapshot> instruments = new ArrayList<>();
                                for (DocumentSnapshot doc : querySnapshot) {
                                    instruments.add(doc);
                                }
                                Log.d(TAG, "Instrumentos favoritos encontrados: " + instruments.size());
                                futuro.complete(instruments);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Erro ao buscar instrumentos favoritos: " + e.getMessage(), e);
                                futuro.completeExceptionally(e);
                            });
                    
                    return CompletableFuture.completedFuture(null);
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Erro ao buscar favoritos: " + throwable.getMessage(), throwable);
                    futuro.completeExceptionally(throwable);
                    return null;
                });
        
        return futuro;
    }
    
    // ==================== DISPONIBILIDADE ====================
    // Seção responsável por gerenciamento de disponibilidade de instrumentos
    
    public static CompletableFuture<Boolean> atualizarDisponibilidadeInstrumento(String idInstrumento, List<Map<String, Object>> faixasIndisponiveis) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Atualizando disponibilidade do instrumento: " + idInstrumento);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        Map<String, Object> atualizacoes = new HashMap<>();
        atualizacoes.put("unavailableRanges", faixasIndisponiveis);
        atualizacoes.put("updatedAt", Timestamp.now());
        
        firestore.collection(COLECAO_INSTRUMENTOS).document(idInstrumento)
                .update(atualizacoes)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Disponibilidade do instrumento atualizada com sucesso");
                    futuro.complete(true);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao atualizar disponibilidade do instrumento: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    public static CompletableFuture<List<Map<String, Object>>> obterFaixasIndisponiveisInstrumento(String idInstrumento) {
        CompletableFuture<List<Map<String, Object>>> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Obtendo faixas indisponíveis do instrumento: " + idInstrumento);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        firestore.collection(COLECAO_INSTRUMENTOS).document(idInstrumento).get()
                .addOnSuccessListener(snapshotDocumento -> {
                    if (snapshotDocumento.exists()) {
                        List<Object> faixas = (List<Object>) snapshotDocumento.get("unavailableRanges");
                        List<Map<String, Object>> resultado = new ArrayList<>();
                        
                        if (faixas != null) {
                            for (Object faixa : faixas) {
                                if (faixa instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> mapaFaixa = (Map<String, Object>) faixa;
                                    resultado.add(mapaFaixa);
                                }
                            }
                        }
                        
                        Log.d(TAG, "Faixas indisponíveis carregadas: " + resultado.size());
                        futuro.complete(resultado);
                    } else {
                        Log.w(TAG, "Instrumento não encontrado");
                        futuro.completeExceptionally(new Exception("Instrumento não encontrado"));
                    }
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao carregar faixas indisponíveis: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    // ==================== MÉTODOS DE CHAT E MENSAGENS ====================
    // Seção responsável por sistema de chat e mensagens entre usuários
    
    /**
     * Verificar se já existe um chat entre locator/owner para um instrumento (não cria)
     */
    public static CompletableFuture<String> encontrarChatExistente(String idInstrumento, String idLocatario, String idProprietario) {
        CompletableFuture<String> futuro = new CompletableFuture<>();
        
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        firestore.collection(COLECAO_CHATS)
                .whereEqualTo("idInstrumento", idInstrumento)
                .whereEqualTo("locatorId", idLocatario)
                .whereEqualTo("ownerId", idProprietario)
                .whereEqualTo("status", "active")
                .limit(1)
                .get()
                .addOnSuccessListener(consulta -> {
                    if (!consulta.isEmpty()) {
                        futuro.complete(consulta.getDocuments().get(0).getId());
                    } else {
                        futuro.complete(null);
                    }
                })
                .addOnFailureListener(futuro::completeExceptionally);
        return futuro;
    }

    /**
     * Criar um chat explicitamente (usado ao enviar a primeira mensagem)
     */
    public static CompletableFuture<String> criarChat(String idInstrumento, String idLocatario, String idProprietario, String nomeInstrumento) {
        CompletableFuture<String> futuro = new CompletableFuture<>();
        
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        Map<String, Object> dadosChat = new HashMap<>();
        dadosChat.put("idInstrumento", idInstrumento);
        dadosChat.put("locatorId", idLocatario);
        dadosChat.put("ownerId", idProprietario);
        dadosChat.put("createdAt", Timestamp.now());
        dadosChat.put("lastMessageAt", Timestamp.now());
        dadosChat.put("status", "active");
        if (nomeInstrumento != null) {
            dadosChat.put("instrumentName", nomeInstrumento);
        }
        
        firestore.collection(COLECAO_CHATS)
                .add(dadosChat)
                .addOnSuccessListener(referencia -> futuro.complete(referencia.getId()))
                .addOnFailureListener(futuro::completeExceptionally);
        return futuro;
    }

    /**
     * Criar ou obter chat existente para um instrumento
     */
    public static CompletableFuture<String> criarOuObterChat(String idInstrumento, String idLocatario, String idProprietario) {
        CompletableFuture<String> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Criando ou obtendo chat para instrumento: " + idInstrumento + ", locator: " + idLocatario + ", owner: " + idProprietario);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        // Primeiro, verificar se já existe um chat
        firestore.collection(COLECAO_CHATS)
                .whereEqualTo("idInstrumento", idInstrumento)
                .whereEqualTo("locatorId", idLocatario)
                .whereEqualTo("ownerId", idProprietario)
                .whereEqualTo("status", "active")
                .limit(1)
                .get()
                .addOnSuccessListener(snapshotConsulta -> {
                    if (!snapshotConsulta.isEmpty()) {
                        // Chat já existe, retornar o ID
                        String idChatExistente = snapshotConsulta.getDocuments().get(0).getId();
                        Log.d(TAG, "Chat existente encontrado: " + idChatExistente);
                        futuro.complete(idChatExistente);
                    } else {
                        // Criar novo chat - usar add() com dados específicos
                        Map<String, Object> dadosChat = new HashMap<>();
                        dadosChat.put("idInstrumento", idInstrumento);
                        dadosChat.put("locatorId", idLocatario);
                        dadosChat.put("ownerId", idProprietario);
                        dadosChat.put("createdAt", Timestamp.now());
                        dadosChat.put("lastMessageAt", Timestamp.now());
                        dadosChat.put("status", "active");
                        
                        firestore.collection(COLECAO_CHATS)
                                .add(dadosChat)
                                .addOnSuccessListener(referenciaDocumento -> {
                                    String novoIdChat = referenciaDocumento.getId();
                                    Log.d(TAG, "Novo chat criado: " + novoIdChat);
                                    futuro.complete(novoIdChat);
                                })
                                .addOnFailureListener(erro -> {
                                    Log.e(TAG, "Erro ao criar chat: " + erro.getMessage(), erro);
                                    futuro.completeExceptionally(erro);
                                });
                    }
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao verificar chat existente: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Enviar mensagem para um chat
     */
    public static CompletableFuture<Boolean> enviarMensagem(String idChat, String idRemetente, String conteudo) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Enviando mensagem para chat: " + idChat + ", sender: " + idRemetente);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        FirebaseMessage mensagem = new FirebaseMessage(idChat, idRemetente, conteudo);
        
        firestore.collection(COLECAO_MENSAGENS)
                .add(mensagem)
                .addOnSuccessListener(referenciaDocumento -> {
                    Log.d(TAG, "Mensagem enviada com sucesso: " + referenciaDocumento.getId());
                    
                    // Atualizar lastMessageAt do chat
                    firestore.collection(COLECAO_CHATS).document(idChat)
                            .update("lastMessageAt", Timestamp.now())
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Chat atualizado com sucesso");
                                futuro.complete(true);
                            })
                            .addOnFailureListener(erro -> {
                                Log.e(TAG, "Erro ao atualizar chat: " + erro.getMessage(), erro);
                                futuro.complete(true); // Mensagem foi enviada, mesmo com erro na atualização
                            });
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao enviar mensagem: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Obter mensagens de um chat
     */
    public static CompletableFuture<List<DocumentSnapshot>> obterMensagensChat(String idChat) {
        CompletableFuture<List<DocumentSnapshot>> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Obtendo mensagens do chat: " + idChat);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        firestore.collection(COLECAO_MENSAGENS)
                .whereEqualTo("chatId", idChat)
                // Temporariamente removido orderBy para evitar necessidade de índice
                // .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshotConsulta -> {
                    List<DocumentSnapshot> mensagens = snapshotConsulta.getDocuments();
                    Log.d(TAG, "Mensagens carregadas: " + mensagens.size());
                    
                    // Ordenar localmente por timestamp (mais antiga primeiro)
                    mensagens.sort((a, b) -> {
                        Object timestampA = a.get("timestamp");
                        Object timestampB = b.get("timestamp");
                        
                        if (timestampA instanceof com.google.firebase.Timestamp && timestampB instanceof com.google.firebase.Timestamp) {
                            return ((com.google.firebase.Timestamp) timestampA).compareTo((com.google.firebase.Timestamp) timestampB);
                        } else if (timestampA instanceof Date && timestampB instanceof Date) {
                            return ((Date) timestampA).compareTo((Date) timestampB);
                        }
                        return 0;
                    });
                    
                    futuro.complete(mensagens);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao carregar mensagens: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Obter chats de um usuário (apenas chats com mensagens)
     */
    public static CompletableFuture<List<DocumentSnapshot>> obterChatsUsuario(String idUsuario) {
        CompletableFuture<List<DocumentSnapshot>> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Obtendo chats do usuário: " + idUsuario);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        // Buscar chats onde o usuário é locator OU owner
        firestore.collection(COLECAO_CHATS)
                .whereEqualTo("status", "active")
                .whereEqualTo("locatorId", idUsuario)
                // Temporariamente removido orderBy para evitar necessidade de índice
                // .orderBy("lastMessageAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshotConsulta1 -> {
                    List<DocumentSnapshot> chatsComoLocatario = snapshotConsulta1.getDocuments();
                    
                    // Buscar chats onde o usuário é owner
                    firestore.collection(COLECAO_CHATS)
                            .whereEqualTo("status", "active")
                            .whereEqualTo("ownerId", idUsuario)
                            // Temporariamente removido orderBy para evitar necessidade de índice
                            // .orderBy("lastMessageAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener(snapshotConsulta2 -> {
                                List<DocumentSnapshot> chatsComoProprietario = snapshotConsulta2.getDocuments();
                                
                                // Combinar todos os chats
                                List<DocumentSnapshot> todosChats = new ArrayList<>();
                                todosChats.addAll(chatsComoLocatario);
                                todosChats.addAll(chatsComoProprietario);
                                
                                // Filtrar chats excluídos pelo usuário atual
                                List<DocumentSnapshot> chatsNaoExcluidos = filtrarChatsExcluidos(todosChats, idUsuario);
                                
                                // Filtrar apenas chats que têm mensagens
                                filtrarChatsComMensagens(chatsNaoExcluidos, futuro);
                            })
                            .addOnFailureListener(erro -> {
                                Log.e(TAG, "Erro ao carregar chats como owner: " + erro.getMessage(), erro);
                                futuro.completeExceptionally(erro);
                            });
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao carregar chats como locator: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Filtra chats que não foram excluídos pelo usuário atual
     */
    private static List<DocumentSnapshot> filtrarChatsExcluidos(List<DocumentSnapshot> todosChats, String idUsuario) {
        List<DocumentSnapshot> chatsNaoExcluidos = new ArrayList<>();
        
        for (DocumentSnapshot chat : todosChats) {
            List<String> usuariosExcluidos = (List<String>) chat.get("usuariosExcluidos");
            
            // Se não há lista de usuários excluídos ou o usuário atual não está na lista
            if (usuariosExcluidos == null || !usuariosExcluidos.contains(idUsuario)) {
                chatsNaoExcluidos.add(chat);
                Log.d(TAG, "Chat incluído (não excluído): " + chat.getId());
            } else {
                Log.d(TAG, "Chat excluído pelo usuário, removendo da lista: " + chat.getId());
            }
        }
        
        Log.d(TAG, "Chats filtrados: " + chatsNaoExcluidos.size() + " de " + todosChats.size() + " (excluídos: " + (todosChats.size() - chatsNaoExcluidos.size()) + ")");
        return chatsNaoExcluidos;
    }
    
    /**
     * Filtrar chats que realmente têm mensagens
     */
    private static void filtrarChatsComMensagens(List<DocumentSnapshot> todosChats, CompletableFuture<List<DocumentSnapshot>> futuro) {
        if (todosChats.isEmpty()) {
            futuro.complete(new ArrayList<>());
            return;
        }
        
        List<DocumentSnapshot> chatsComMensagens = new ArrayList<>();
        int[] contadorProcessados = {0};
        
        for (DocumentSnapshot chat : todosChats) {
            String idChat = chat.getId();
            
            // Verificar se o chat tem mensagens
            firestore.collection(COLECAO_MENSAGENS)
                    .whereEqualTo("chatId", idChat)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshotConsulta -> {
                        if (!snapshotConsulta.isEmpty()) {
                            // Chat tem mensagens, adicionar à lista
                            chatsComMensagens.add(chat);
                        }
                        
                        contadorProcessados[0]++;
                        if (contadorProcessados[0] == todosChats.size()) {
                            // Todos os chats foram verificados
                            // Ordenar por lastMessageAt (mais recente primeiro) - ordenação local
                            chatsComMensagens.sort((a, b) -> {
                                Object dataA = a.get("lastMessageAt");
                                Object dataB = b.get("lastMessageAt");
                                
                                if (dataA instanceof com.google.firebase.Timestamp && dataB instanceof com.google.firebase.Timestamp) {
                                    return ((com.google.firebase.Timestamp) dataB).compareTo((com.google.firebase.Timestamp) dataA);
                                } else if (dataA instanceof Date && dataB instanceof Date) {
                                    return ((Date) dataB).compareTo((Date) dataA);
                                }
                                return 0;
                            });
                            
                            Log.d(TAG, "Chats com mensagens: " + chatsComMensagens.size() + " de " + todosChats.size());
                            futuro.complete(chatsComMensagens);
                        }
                    })
                    .addOnFailureListener(erro -> {
                        Log.e(TAG, "Erro ao verificar mensagens do chat " + idChat + ": " + erro.getMessage());
                        contadorProcessados[0]++;
                        if (contadorProcessados[0] == todosChats.size()) {
                            // Mesmo com erro, retornar o que foi possível verificar
                            futuro.complete(chatsComMensagens);
                        }
                    });
        }
    }
    
    /**
     * Obter chat por ID
     */
    public static CompletableFuture<DocumentSnapshot> obterChatPorId(String idChat) {
        CompletableFuture<DocumentSnapshot> futuro = new CompletableFuture<>();
        
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        firestore.collection(COLECAO_CHATS).document(idChat).get()
                .addOnSuccessListener(futuro::complete)
                .addOnFailureListener(futuro::completeExceptionally);
        
        return futuro;
    }

    /**
     * Obter dados do chat com informações do instrumento
     */
    public static CompletableFuture<Map<String, Object>> obterChatComInfoInstrumento(String idChat) {
        CompletableFuture<Map<String, Object>> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Obtendo dados do chat com instrumento: " + idChat);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        firestore.collection(COLECAO_CHATS).document(idChat).get()
                .addOnSuccessListener(documentoChat -> {
                    if (documentoChat.exists()) {
                        String idInstrumento = documentoChat.getString("idInstrumento");
                        
                        // Buscar informações do instrumento
                        firestore.collection(COLECAO_INSTRUMENTOS).document(idInstrumento).get()
                                .addOnSuccessListener(documentoInstrumento -> {
                                    Map<String, Object> resultado = new HashMap<>();
                                    resultado.put("chat", documentoChat.getData());
                                    resultado.put("instrument", documentoInstrumento.getData());
                                    futuro.complete(resultado);
                                })
                                .addOnFailureListener(erro -> {
                                    Log.e(TAG, "Erro ao carregar instrumento: " + erro.getMessage(), erro);
                                    futuro.completeExceptionally(erro);
                                });
                    } else {
                        futuro.completeExceptionally(new Exception("Chat não encontrado"));
                    }
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao carregar chat: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }

    /**
     * Limpar chats vazios (sem mensagens) - opcional para limpeza
     */
    public static CompletableFuture<Integer> limparChatsVazios() {
        CompletableFuture<Integer> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Iniciando limpeza de chats vazios...");
        
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        // Buscar todos os chats ativos
        firestore.collection(COLECAO_CHATS)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentSnapshot> allChats = querySnapshot.getDocuments();
                    int[] processedCount = {0};
                    int[] deletedCount = {0};
                    
                    if (allChats.isEmpty()) {
                        futuro.complete(0);
                        return;
                    }
                    
                    for (DocumentSnapshot chat : allChats) {
                        String chatId = chat.getId();
                        
                        // Verificar se o chat tem mensagens
                        firestore.collection(COLECAO_MENSAGENS)
                                .whereEqualTo("chatId", chatId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(messagesQuery -> {
                                    if (messagesQuery.isEmpty()) {
                                        // Chat vazio, deletar
                                        firestore.collection(COLECAO_CHATS).document(chatId).delete()
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d(TAG, "Chat vazio deletado: " + chatId);
                                                    deletedCount[0]++;
                                                })
                                                .addOnFailureListener(e -> 
                                                    Log.e(TAG, "Erro ao deletar chat vazio: " + chatId + " - " + e.getMessage())
                                                );
                                    }
                                    
                                    processedCount[0]++;
                                    if (processedCount[0] == allChats.size()) {
                                        Log.d(TAG, "Limpeza concluída. Chats deletados: " + deletedCount[0]);
                                        futuro.complete(deletedCount[0]);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Erro ao verificar mensagens do chat " + chatId + ": " + e.getMessage());
                                    processedCount[0]++;
                                    if (processedCount[0] == allChats.size()) {
                                        futuro.complete(deletedCount[0]);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao buscar chats para limpeza: " + e.getMessage(), e);
                    futuro.completeExceptionally(e);
                });
        
        return futuro;
    }
    
    // ==================== SOLICITAÇÕES DE RESERVA ====================
    // Seção responsável por gerenciamento de solicitações de reserva
    
    /**
     * Criar uma nova solicitação de reserva
     */
    public static CompletableFuture<String> criarSolicitacaoReserva(com.example.instrumentaliza.models.FirebaseSolicitacao solicitacao) {
        CompletableFuture<String> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Criando solicitação de reserva para instrumento: " + solicitacao.getInstrumentoId());
        Log.d(TAG, "Dados da solicitação: " + solicitacao.toString());
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore inicializado em criarSolicitacaoReserva");
        } else {
            Log.d(TAG, "Firestore já estava inicializado");
        }
        
        Map<String, Object> dadosSolicitacao = new HashMap<>();
        dadosSolicitacao.put("solicitanteId", solicitacao.getSolicitanteId());
        dadosSolicitacao.put("proprietarioId", solicitacao.getProprietarioId());
        dadosSolicitacao.put("instrumentoId", solicitacao.getInstrumentoId());
        dadosSolicitacao.put("instrumentoNome", solicitacao.getInstrumentoNome());
        dadosSolicitacao.put("solicitanteNome", solicitacao.getSolicitanteNome());
        dadosSolicitacao.put("solicitanteEmail", solicitacao.getSolicitanteEmail());
        dadosSolicitacao.put("solicitanteTelefone", solicitacao.getSolicitanteTelefone());
        dadosSolicitacao.put("dataInicio", new Timestamp(solicitacao.getDataInicio()));
        dadosSolicitacao.put("dataFim", new Timestamp(solicitacao.getDataFim()));
        dadosSolicitacao.put("precoTotal", solicitacao.getPrecoTotal());
        dadosSolicitacao.put("status", solicitacao.getStatus());
        Log.d(TAG, "Status sendo salvo: '" + solicitacao.getStatus() + "'");
        dadosSolicitacao.put("observacoes", solicitacao.getObservacoes());
        dadosSolicitacao.put("lida", false); // Campo para controlar notificações
        dadosSolicitacao.put("dataCriacao", Timestamp.now());
        dadosSolicitacao.put("dataAtualizacao", Timestamp.now());
        
        Log.d(TAG, "Tentando adicionar documento à coleção 'solicitacoes'");
        Log.d(TAG, "Dados que serão salvos:");
        for (Map.Entry<String, Object> entry : dadosSolicitacao.entrySet()) {
            Log.d(TAG, "  " + entry.getKey() + ": " + entry.getValue());
        }
        
        firestore.collection("solicitacoes")
                .add(dadosSolicitacao)
                .addOnSuccessListener(referenciaDocumento -> {
                    String idSolicitacao = referenciaDocumento.getId();
                    Log.d(TAG, "✓ Solicitação criada com sucesso: " + idSolicitacao);
                    Log.d(TAG, "Referência do documento: " + referenciaDocumento.getPath());
                    Log.d(TAG, "Coleção: solicitacoes");
                    futuro.complete(idSolicitacao);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao criar solicitação: " + erro.getMessage(), erro);
                    Log.e(TAG, "Stack trace do erro: ", erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Verifica e atualiza solicitações pendentes expiradas para RECUSADA
     * 
     * @param usuarioId ID do usuário para buscar apenas suas solicitações
     * @return CompletableFuture com número de solicitações atualizadas
     */
    public static CompletableFuture<Integer> verificarEAtualizarSolicitacoesExpiradas(String usuarioId) {
        CompletableFuture<Integer> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "=== VERIFICANDO SOLICITAÇÕES EXPIRADAS PARA USUÁRIO: " + usuarioId + " ===");
        
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        Date hoje = new Date();
        Log.d(TAG, "Data atual: " + hoje);
        
        // Buscar solicitações onde o usuário é proprietário (recebidas)
        firestore.collection("solicitacoes")
                .whereEqualTo("status", "PENDENTE")
                .whereEqualTo("proprietarioId", usuarioId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    final int[] atualizadas = {0}; // Array para ser final
                    List<Task<Void>> tarefas = new ArrayList<>();
                    
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Timestamp dataFimTimestamp = doc.getTimestamp("dataFim");
                        if (dataFimTimestamp != null) {
                            Date dataFim = dataFimTimestamp.toDate();
                            Log.d(TAG, "Verificando solicitação " + doc.getId() + " - Data fim: " + dataFim);
                            
                            if (dataFim.before(hoje)) {
                                Log.d(TAG, "Solicitação expirada, atualizando para RECUSADA: " + doc.getId());
                                
                                Task<Void> updateTask = doc.getReference().update(
                                    "status", "RECUSADA",
                                    "motivoRecusa", "Período solicitado expirado",
                                    "dataAtualizacao", Timestamp.now()
                                );
                                tarefas.add(updateTask);
                                atualizadas[0]++;
                            }
                        }
                    }
                    
                    if (tarefas.isEmpty()) {
                        Log.d(TAG, "Nenhuma solicitação expirada encontrada");
                        futuro.complete(0);
                    } else {
                        Tasks.whenAll(tarefas)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Solicitações expiradas atualizadas: " + atualizadas[0]);
                                    futuro.complete(atualizadas[0]);
                                })
                                .addOnFailureListener(erro -> {
                                    Log.e(TAG, "Erro ao atualizar solicitações expiradas: " + erro.getMessage(), erro);
                                    futuro.completeExceptionally(erro);
                                });
                    }
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao buscar solicitações pendentes: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }

    /**
     * Atualiza reservas existentes que não possuem o campo ownerId
     * 
     * @param usuarioId ID do usuário para buscar apenas suas reservas
     * @return CompletableFuture com número de reservas atualizadas
     */
    public static CompletableFuture<Integer> atualizarReservasSemOwnerId(String usuarioId) {
        CompletableFuture<Integer> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "=== ATUALIZANDO RESERVAS SEM OWNERID PARA USUÁRIO: " + usuarioId + " ===");
        
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        // Buscar reservas onde ownerId é null E o usuário está envolvido (como locatário)
        firestore.collection("reservations")
                .whereEqualTo("userId", usuarioId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    final int[] atualizadas = {0};
                    List<Task<Void>> tarefas = new ArrayList<>();
                    
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String ownerId = doc.getString("ownerId");
                        
                        // Só atualizar se ownerId for null
                        if (ownerId == null) {
                            String instrumentId = doc.getString("instrumentId");
                            if (instrumentId != null) {
                                Log.d(TAG, "Atualizando reserva " + doc.getId() + " - instrumentId: " + instrumentId);
                            
                            // Buscar o ownerId do instrumento
                            firestore.collection("instruments").document(instrumentId)
                                    .get()
                                    .addOnSuccessListener(instrumentDoc -> {
                                        if (instrumentDoc.exists()) {
                                            String instrumentOwnerId = instrumentDoc.getString("ownerId");
                                            if (instrumentOwnerId != null) {
                                                Log.d(TAG, "Encontrado ownerId: " + instrumentOwnerId + " para instrumento: " + instrumentId);
                                                
                                                Task<Void> updateTask = doc.getReference().update("ownerId", instrumentOwnerId);
                                                tarefas.add(updateTask);
                                                atualizadas[0]++;
                                            }
                                        }
                                    });
                            }
                        }
                    }
                    
                    // Processar as tarefas se houver alguma
                    if (tarefas.isEmpty()) {
                        Log.d(TAG, "Nenhuma reserva precisa ser atualizada");
                        futuro.complete(0);
                    } else {
                        Tasks.whenAll(tarefas)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Reservas atualizadas: " + atualizadas[0]);
                                    futuro.complete(atualizadas[0]);
                                })
                                .addOnFailureListener(erro -> {
                                    Log.e(TAG, "Erro ao atualizar reservas: " + erro.getMessage(), erro);
                                    futuro.completeExceptionally(erro);
                                });
                    }
                    
                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "Nenhuma reserva sem ownerId encontrada");
                        futuro.complete(0);
                    }
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao buscar reservas sem ownerId: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }

    /**
     * Marca uma solicitação como lida
     */
    public static CompletableFuture<Boolean> marcarSolicitacaoComoLida(String solicitacaoId) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Marcando solicitação como lida: " + solicitacaoId);
        
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        Map<String, Object> atualizacoes = new HashMap<>();
        atualizacoes.put("lida", true);
        atualizacoes.put("dataLeitura", Timestamp.now());
        
        firestore.collection("solicitacoes").document(solicitacaoId)
                .update(atualizacoes)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Solicitação marcada como lida com sucesso");
                    futuro.complete(true);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao marcar solicitação como lida: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Marca TODAS as solicitações de um proprietário como lidas
     * Útil para limpar o badge de notificação
     */
    public static CompletableFuture<Integer> marcarTodasSolicitacoesComoLidas(String proprietarioId) {
        CompletableFuture<Integer> futuro = new CompletableFuture<>();
        
        
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        // Buscar todas as solicitações não lidas do proprietário
        firestore.collection("solicitacoes")
                .whereEqualTo("proprietarioId", proprietarioId)
                .whereEqualTo("lida", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentSnapshot> solicitacoesNaoLidas = querySnapshot.getDocuments();
                    
                    if (solicitacoesNaoLidas.isEmpty()) {
                        futuro.complete(0);
                        return;
                    }
                    
                    // Usar lote para atualizar todas de uma vez
                    WriteBatch batch = firestore.batch();
                    
                    for (DocumentSnapshot solicitacao : solicitacoesNaoLidas) {
                        Map<String, Object> atualizacoes = new HashMap<>();
                        atualizacoes.put("lida", true);
                        atualizacoes.put("dataLeitura", Timestamp.now());
                        
                        batch.update(solicitacao.getReference(), atualizacoes);
                    }
                    
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                futuro.complete(solicitacoesNaoLidas.size());
                            })
                            .addOnFailureListener(erro -> {
                                Log.e(TAG, "Erro ao marcar todas as solicitações como lidas: " + erro.getMessage(), erro);
                                futuro.completeExceptionally(erro);
                            });
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao buscar solicitações não lidas: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Buscar solicitações de um proprietário
     */
    public static CompletableFuture<List<DocumentSnapshot>> buscarSolicitacoesProprietario(String idProprietario) {
        CompletableFuture<List<DocumentSnapshot>> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "=== BUSCANDO SOLICITAÇÕES DO PROPRIETÁRIO ===");
        Log.d(TAG, "ID do proprietário: " + idProprietario);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore inicializado em buscarSolicitacoesProprietario");
        }
        
        Log.d(TAG, "Executando consulta: collection('solicitacoes').whereEqualTo('proprietarioId', '" + idProprietario + "')");
        
        firestore.collection("solicitacoes")
                .whereEqualTo("proprietarioId", idProprietario)
                .get()
                .addOnSuccessListener(snapshotConsulta -> {
                    List<DocumentSnapshot> solicitacoes = snapshotConsulta.getDocuments();
                    Log.d(TAG, "✓ Consulta executada com sucesso!");
                    Log.d(TAG, "Total de solicitações encontradas: " + solicitacoes.size());
                    
                    // Ordenar por data de criação (mais recente primeiro)
                    solicitacoes.sort((doc1, doc2) -> {
                        Timestamp timestamp1 = (Timestamp) doc1.get("dataCriacao");
                        Timestamp timestamp2 = (Timestamp) doc2.get("dataCriacao");
                        
                        if (timestamp1 == null || timestamp2 == null) return 0;
                        
                        return timestamp2.compareTo(timestamp1); // Descendente
                    });
                    
                    // Log de cada documento encontrado
                    for (int i = 0; i < solicitacoes.size(); i++) {
                        DocumentSnapshot doc = solicitacoes.get(i);
                        Log.d(TAG, "Documento " + (i+1) + ": " + doc.getId());
                        Log.d(TAG, "  - proprietarioId: " + doc.getString("proprietarioId"));
                        Log.d(TAG, "  - instrumentoId: " + doc.getString("instrumentoId"));
                        Log.d(TAG, "  - status: " + doc.getString("status"));
                        Log.d(TAG, "  - dataCriacao: " + doc.get("dataCriacao"));
                    }
                    
                    futuro.complete(solicitacoes);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao buscar solicitações: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Buscar solicitações de um solicitante
     */
    public static CompletableFuture<List<DocumentSnapshot>> buscarSolicitacoesSolicitante(String idSolicitante) {
        CompletableFuture<List<DocumentSnapshot>> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Buscando solicitações do solicitante: " + idSolicitante);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore inicializado em buscarSolicitacoesSolicitante");
        }
        
        firestore.collection("solicitacoes")
                .whereEqualTo("solicitanteId", idSolicitante)
                .get()
                .addOnSuccessListener(snapshotConsulta -> {
                    List<DocumentSnapshot> solicitacoes = snapshotConsulta.getDocuments();
                    Log.d(TAG, "Solicitações encontradas: " + solicitacoes.size());
                    
                    // Ordenar por data de criação (mais recente primeiro)
                    solicitacoes.sort((doc1, doc2) -> {
                        Timestamp timestamp1 = (Timestamp) doc1.get("dataCriacao");
                        Timestamp timestamp2 = (Timestamp) doc2.get("dataCriacao");
                        
                        if (timestamp1 == null || timestamp2 == null) return 0;
                        
                        return timestamp2.compareTo(timestamp1); // Descendente
                    });
                    
                    futuro.complete(solicitacoes);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao buscar solicitações: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Aceitar uma solicitação de reserva
     */
    public static CompletableFuture<Boolean> aceitarSolicitacao(String idSolicitacao) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Aceitando solicitação: " + idSolicitacao);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore inicializado em aceitarSolicitacao");
        }
        
        Map<String, Object> atualizacoes = new HashMap<>();
        atualizacoes.put("status", "ACEITA");
        atualizacoes.put("dataAtualizacao", Timestamp.now());
        
        firestore.collection("solicitacoes").document(idSolicitacao)
                .update(atualizacoes)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Solicitação aceita com sucesso");
                    futuro.complete(true);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao aceitar solicitação: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Recusar uma solicitação de reserva
     */
    public static CompletableFuture<Boolean> recusarSolicitacao(String idSolicitacao, String motivoRecusa) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Recusando solicitação: " + idSolicitacao + " - Motivo: " + motivoRecusa);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore inicializado em recusarSolicitacao");
        }
        
        Map<String, Object> atualizacoes = new HashMap<>();
        atualizacoes.put("status", "RECUSADA");
        atualizacoes.put("motivoRecusa", motivoRecusa);
        atualizacoes.put("dataAtualizacao", Timestamp.now());
        
        firestore.collection("solicitacoes").document(idSolicitacao)
                .update(atualizacoes)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Solicitação recusada com sucesso");
                    futuro.complete(true);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao recusar solicitação: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Obter solicitação por ID
     */
    public static CompletableFuture<DocumentSnapshot> obterSolicitacaoPorId(String idSolicitacao) {
        CompletableFuture<DocumentSnapshot> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Obtendo solicitação por ID: " + idSolicitacao);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore inicializado em obterSolicitacaoPorId");
        }
        
        firestore.collection("solicitacoes").document(idSolicitacao)
                .get()
                .addOnSuccessListener(documento -> {
                    if (documento.exists()) {
                        Log.d(TAG, "Solicitação encontrada");
                        futuro.complete(documento);
                    } else {
                        Log.w(TAG, "Solicitação não encontrada");
                        futuro.completeExceptionally(new Exception("Solicitação não encontrada"));
                    }
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao obter solicitação: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Verificar se já existe solicitação pendente para o mesmo período
     */
    public static CompletableFuture<Boolean> verificarSolicitacaoExistente(String idInstrumento, String idSolicitante, Date dataInicio, Date dataFim) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Verificando solicitação existente para instrumento: " + idInstrumento);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore inicializado em verificarSolicitacaoExistente");
        }
        
        firestore.collection("solicitacoes")
                .whereEqualTo("instrumentoId", idInstrumento)
                .whereEqualTo("solicitanteId", idSolicitante)
                .whereEqualTo("status", "PENDENTE")
                .get()
                .addOnSuccessListener(snapshotConsulta -> {
                    boolean existeSolicitacao = false;
                    
                    for (DocumentSnapshot documento : snapshotConsulta.getDocuments()) {
                        Timestamp timestampInicio = (Timestamp) documento.get("dataInicio");
                        Timestamp timestampFim = (Timestamp) documento.get("dataFim");
                        
                        if (timestampInicio != null && timestampFim != null) {
                            Date docInicio = timestampInicio.toDate();
                            Date docFim = timestampFim.toDate();
                            
                            // Verificar sobreposição de datas
                            if ((dataInicio.before(docFim) || dataInicio.equals(docFim)) &&
                                (dataFim.after(docInicio) || dataFim.equals(docInicio))) {
                                existeSolicitacao = true;
                                break;
                            }
                        }
                    }
                    
                    Log.d(TAG, "Solicitação existente encontrada: " + existeSolicitacao);
                    futuro.complete(existeSolicitacao);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao verificar solicitação existente: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Converter solicitação aceita em reserva ativa
     */
    public static CompletableFuture<Boolean> converterSolicitacaoEmReserva(String idSolicitacao) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "=== CONVERTENDO SOLICITAÇÃO EM RESERVA ===");
        Log.d(TAG, "ID da solicitação: " + idSolicitacao);
        
        // Primeiro obter os dados da solicitação
        obterSolicitacaoPorId(idSolicitacao)
                .thenAccept(solicitacaoDoc -> {
                    if (!solicitacaoDoc.exists()) {
                        futuro.completeExceptionally(new RuntimeException("Solicitação não encontrada"));
                        return;
                    }
                    
                    // Criar reserva ativa
                    Map<String, Object> dadosReserva = new HashMap<>();
                    String solicitanteId = solicitacaoDoc.getString("solicitanteId");
                    String proprietarioId = solicitacaoDoc.getString("proprietarioId");
                    String instrumentoId = solicitacaoDoc.getString("instrumentoId");
                    
                    Log.d(TAG, "Dados da solicitação:");
                    Log.d(TAG, "  - solicitanteId: " + solicitanteId);
                    Log.d(TAG, "  - proprietarioId: " + proprietarioId);
                    Log.d(TAG, "  - instrumentoId: " + instrumentoId);
                    
                    dadosReserva.put("userId", solicitanteId);
                    dadosReserva.put("ownerId", proprietarioId);
                    dadosReserva.put("instrumentId", instrumentoId);
                    dadosReserva.put("startDate", solicitacaoDoc.get("dataInicio"));
                    dadosReserva.put("endDate", solicitacaoDoc.get("dataFim"));
                    dadosReserva.put("totalPrice", solicitacaoDoc.getDouble("precoTotal"));
                    dadosReserva.put("status", "CONFIRMADA");
                    dadosReserva.put("createdAt", Timestamp.now());
                    
                    // Garantir que firestore está inicializado
                    if (firestore == null) {
                        firestore = FirebaseFirestore.getInstance();
                    }
                    
                    // Criar reserva e atualizar disponibilidade do instrumento
                    Timestamp dataInicio = solicitacaoDoc.getTimestamp("dataInicio");
                    Timestamp dataFim = solicitacaoDoc.getTimestamp("dataFim");
                    
                    Log.d(TAG, "Criando reserva e atualizando disponibilidade do instrumento: " + instrumentoId);
                    Log.d(TAG, "Período: " + dataInicio.toDate() + " a " + dataFim.toDate());
                    
                    // Primeiro criar a reserva
                    Log.d(TAG, "Criando reserva na coleção 'reservations'...");
                    firestore.collection("reservations").add(dadosReserva)
                            .addOnSuccessListener(referenciaReserva -> {
                                Log.d(TAG, "=== RESERVA CRIADA COM SUCESSO ===");
                                Log.d(TAG, "ID da reserva: " + referenciaReserva.getId());
                                Log.d(TAG, "userId: " + solicitanteId);
                                Log.d(TAG, "ownerId: " + proprietarioId);
                                Log.d(TAG, "instrumentId: " + instrumentoId);
                                
                                // Depois atualizar a disponibilidade do instrumento
                                atualizarDisponibilidadeInstrumento(instrumentoId, dataInicio, dataFim)
                                        .thenAccept(sucesso -> {
                                            if (sucesso) {
                                                Log.d(TAG, "Disponibilidade do instrumento atualizada com sucesso");
                                                futuro.complete(true);
                                            } else {
                                                Log.e(TAG, "Falha ao atualizar disponibilidade do instrumento");
                                                futuro.completeExceptionally(new RuntimeException("Falha ao atualizar disponibilidade"));
                                            }
                                        })
                                        .exceptionally(erro -> {
                                            Log.e(TAG, "Erro ao atualizar disponibilidade: " + erro.getMessage(), erro);
                                            futuro.completeExceptionally(erro);
                                            return null;
                                        });
                            })
                            .addOnFailureListener(erro -> {
                                Log.e(TAG, "Erro ao criar reserva: " + erro.getMessage(), erro);
                                futuro.completeExceptionally(erro);
                            });
                })
                .exceptionally(erro -> {
                    Log.e(TAG, "Erro ao obter solicitação: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                    return null;
                });
        
        return futuro;
    }
    
    /**
     * Buscar reservas de um usuário
     */
    public static CompletableFuture<List<DocumentSnapshot>> buscarReservasUsuario(String idUsuario) {
        CompletableFuture<List<DocumentSnapshot>> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "=== BUSCANDO RESERVAS DO USUÁRIO ===");
        Log.d(TAG, "ID do usuário: " + idUsuario);
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore inicializado em buscarReservasUsuario");
        }
        
        Log.d(TAG, "Executando consulta: collection('reservations').whereEqualTo('userId', '" + idUsuario + "')");
        
        firestore.collection("reservations")
                .whereEqualTo("userId", idUsuario)
                .get()
                .addOnSuccessListener(snapshotConsulta -> {
                    List<DocumentSnapshot> reservas = snapshotConsulta.getDocuments();
                    Log.d(TAG, "✓ Consulta executada com sucesso!");
                    Log.d(TAG, "Total de reservas encontradas: " + reservas.size());
                    
                    // Ordenar por data de criação (mais recente primeiro)
                    reservas.sort((doc1, doc2) -> {
                        Timestamp timestamp1 = (Timestamp) doc1.get("createdAt");
                        Timestamp timestamp2 = (Timestamp) doc2.get("createdAt");
                        
                        if (timestamp1 == null || timestamp2 == null) return 0;
                        
                        return timestamp2.compareTo(timestamp1); // Descendente
                    });
                    
                    // Log de cada documento encontrado
                    for (int i = 0; i < reservas.size(); i++) {
                        DocumentSnapshot doc = reservas.get(i);
                        Log.d(TAG, "Reserva " + (i+1) + ": " + doc.getId());
                        Log.d(TAG, "  - userId: " + doc.getString("userId"));
                        Log.d(TAG, "  - instrumentId: " + doc.getString("instrumentId"));
                        Log.d(TAG, "  - status: " + doc.getString("status"));
                        Log.d(TAG, "  - totalPrice: " + doc.getDouble("totalPrice"));
                    }
                    
                    futuro.complete(reservas);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao buscar reservas: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Atualiza a disponibilidade do instrumento ao aceitar uma solicitação
     * 
     * Adiciona o período da reserva à lista de períodos indisponíveis do instrumento,
     * garantindo que as datas não possam ser solicitadas novamente.
     * 
     * @param instrumentoId ID do instrumento
     * @param dataInicio Data de início da reserva
     * @param dataFim Data de fim da reserva
     * @return CompletableFuture com resultado da operação
     */
    public static CompletableFuture<Boolean> atualizarDisponibilidadeInstrumento(String instrumentoId, 
            Timestamp dataInicio, Timestamp dataFim) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "=== ATUALIZANDO DISPONIBILIDADE DO INSTRUMENTO ===");
        Log.d(TAG, "Instrumento: " + instrumentoId);
        Log.d(TAG, "Período: " + dataInicio.toDate() + " a " + dataFim.toDate());
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore inicializado em atualizarDisponibilidadeInstrumento");
        }
        
        // Criar o período indisponível
        Map<String, Object> periodoIndisponivel = new HashMap<>();
        periodoIndisponivel.put("startDate", dataInicio);
        periodoIndisponivel.put("endDate", dataFim);
        periodoIndisponivel.put("type", "reservation");
        periodoIndisponivel.put("createdAt", Timestamp.now());
        
        // Primeiro obter o documento do instrumento
        firestore.collection("instruments").document(instrumentoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Log.e(TAG, "Instrumento não encontrado: " + instrumentoId);
                        futuro.completeExceptionally(new RuntimeException("Instrumento não encontrado"));
                        return;
                    }
                    
                    // Obter lista atual de períodos indisponíveis
                    List<Map<String, Object>> periodosIndisponiveis = 
                            (List<Map<String, Object>>) documentSnapshot.get("unavailableRanges");
                    
                    if (periodosIndisponiveis == null) {
                        periodosIndisponiveis = new ArrayList<>();
                    }
                    
                    // Adicionar o novo período indisponível
                    periodosIndisponiveis.add(periodoIndisponivel);
                    
                    Log.d(TAG, "Adicionando período indisponível. Total de períodos: " + periodosIndisponiveis.size());
                    
                    // Atualizar o documento do instrumento
                    Map<String, Object> atualizacoes = new HashMap<>();
                    atualizacoes.put("unavailableRanges", periodosIndisponiveis);
                    
                    firestore.collection("instruments").document(instrumentoId)
                            .update(atualizacoes)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Disponibilidade do instrumento atualizada com sucesso");
                                futuro.complete(true);
                            })
                            .addOnFailureListener(erro -> {
                                Log.e(TAG, "Erro ao atualizar disponibilidade: " + erro.getMessage(), erro);
                                futuro.completeExceptionally(erro);
                            });
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao obter instrumento: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Verifica se um período está disponível para reserva
     * 
     * Checa se o período solicitado não conflita com períodos já
     * indisponíveis ou reservas existentes do instrumento.
     * 
     * @param instrumentoId ID do instrumento
     * @param dataInicio Data de início solicitada
     * @param dataFim Data de fim solicitada
     * @return CompletableFuture com resultado da verificação
     */
    public static CompletableFuture<Boolean> verificarDisponibilidadePeriodo(String instrumentoId, 
            Timestamp dataInicio, Timestamp dataFim) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "=== VERIFICANDO DISPONIBILIDADE DO PERÍODO ===");
        Log.d(TAG, "Instrumento: " + instrumentoId);
        Log.d(TAG, "Período solicitado: " + dataInicio.toDate() + " a " + dataFim.toDate());
        
        // Garantir que firestore está inicializado
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        // Primeiro verificar períodos indisponíveis do instrumento
        firestore.collection("instruments").document(instrumentoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Log.e(TAG, "Instrumento não encontrado: " + instrumentoId);
                        futuro.completeExceptionally(new RuntimeException("Instrumento não encontrado"));
                        return;
                    }
                    
                    // Verificar períodos indisponíveis
                    List<Map<String, Object>> periodosIndisponiveis = 
                            (List<Map<String, Object>>) documentSnapshot.get("unavailableRanges");
                    
                    if (periodosIndisponiveis != null && !periodosIndisponiveis.isEmpty()) {
                        for (Map<String, Object> periodo : periodosIndisponiveis) {
                            Timestamp inicioExistente = (Timestamp) periodo.get("startDate");
                            Timestamp fimExistente = (Timestamp) periodo.get("endDate");
                            
                            if (inicioExistente != null && fimExistente != null) {
                                // Verificar sobreposição de datas
                                boolean sobrepoe = (dataInicio.toDate().before(fimExistente.toDate()) && 
                                                   dataFim.toDate().after(inicioExistente.toDate()));
                                
                                if (sobrepoe) {
                                    Log.d(TAG, "Período indisponível encontrado: " + inicioExistente.toDate() + " a " + fimExistente.toDate());
                                    futuro.complete(false);
                                    return;
                                }
                            }
                        }
                    }
                    
                    // Se chegou até aqui, verificar também reservas ativas
                    verificarReservasAtivas(instrumentoId, dataInicio, dataFim)
                            .thenAccept(disponivel -> {
                                Log.d(TAG, "Verificação de disponibilidade concluída: " + disponivel);
                                futuro.complete(disponivel);
                            })
                            .exceptionally(erro -> {
                                Log.e(TAG, "Erro ao verificar reservas: " + erro.getMessage(), erro);
                                futuro.completeExceptionally(erro);
                                return null;
                            });
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao verificar instrumento: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Verifica se há reservas ativas conflitantes
     */
    private static CompletableFuture<Boolean> verificarReservasAtivas(String instrumentoId, 
            Timestamp dataInicio, Timestamp dataFim) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        firestore.collection("reservations")
                .whereEqualTo("instrumentId", instrumentoId)
                .whereEqualTo("status", "CONFIRMADA")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean disponivel = true;
                    
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Timestamp inicioReserva = doc.getTimestamp("startDate");
                        Timestamp fimReserva = doc.getTimestamp("endDate");
                        
                        if (inicioReserva != null && fimReserva != null) {
                            boolean sobrepoe = (dataInicio.toDate().before(fimReserva.toDate()) && 
                                               dataFim.toDate().after(inicioReserva.toDate()));
                            
                            if (sobrepoe) {
                                Log.d(TAG, "Reserva ativa conflitante encontrada: " + inicioReserva.toDate() + " a " + fimReserva.toDate());
                                disponivel = false;
                                break;
                            }
                        }
                    }
                    
                    futuro.complete(disponivel);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao verificar reservas ativas: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    // ==========================================
    // MÉTODOS DE GERENCIAMENTO DE AVALIAÇÕES
    // ==========================================
    
    /**
     * Enviar uma avaliação de usuário (locatário)
     * 
     * @param avaliacao Avaliação de usuário a ser enviada
     * @return CompletableFuture<Boolean> indicando sucesso
     */
    public static CompletableFuture<Boolean> enviarAvaliacaoUsuario(com.example.instrumentaliza.models.FirebaseAvaliacaoUsuario avaliacao) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "=== ENVIANDO AVALIAÇÃO DE USUÁRIO ===");
        Log.d(TAG, "Locatário: " + avaliacao.getLocatarioNome());
        Log.d(TAG, "Nota: " + avaliacao.getNota());
        Log.d(TAG, "Comentário: " + avaliacao.getComentario());
        
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        // Verificar se já existe avaliação para esta reserva
        firestore.collection("avaliacoes_usuarios")
                .whereEqualTo("reservaId", avaliacao.getReservaId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Log.w(TAG, "Já existe avaliação de usuário para esta reserva");
                        futuro.complete(false);
                        return;
                    }
                    
                    // Adicionar avaliação de usuário
                    Log.d(TAG, "Dados a serem enviados: " + avaliacao.toMap());
                    
                    firestore.collection("avaliacoes_usuarios")
                            .add(avaliacao.toMap())
                            .addOnSuccessListener(documentReference -> {
                                Log.d(TAG, "Avaliação de usuário enviada com sucesso: " + documentReference.getId());
                                futuro.complete(true);
                            })
                            .addOnFailureListener(erro -> {
                                Log.e(TAG, "Erro ao enviar avaliação de usuário: " + erro.getMessage(), erro);
                                futuro.completeExceptionally(erro);
                            });
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao verificar avaliação de usuário existente: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Envia uma avaliação para o Firebase
     * 
     * @param avaliacao Objeto FirebaseAvaliacao com os dados da avaliação
     * @return CompletableFuture<Boolean> indicando sucesso
     */
    public static CompletableFuture<Boolean> enviarAvaliacao(FirebaseAvaliacao avaliacao) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "=== ENVIANDO AVALIAÇÃO ===");
        Log.d(TAG, "Instrumento: " + avaliacao.getInstrumentoNome());
        Log.d(TAG, "Nota: " + avaliacao.getNota());
        Log.d(TAG, "Comentário: " + avaliacao.getComentario());
        
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore inicializado em enviarAvaliacao");
        }
        
        // Validar avaliação
        if (!avaliacao.isValid()) {
            Log.e(TAG, "Avaliação inválida");
            futuro.complete(false);
            return futuro;
        }
        
        // Verificar se já existe avaliação para esta reserva
        firestore.collection("avaliacoes")
                .whereEqualTo("reservaId", avaliacao.getReservaId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Log.w(TAG, "Já existe avaliação para esta reserva");
                        futuro.complete(false);
                        return;
                    }
                    
                    // Adicionar avaliação
                    firestore.collection("avaliacoes")
                            .add(avaliacao.toMap())
                            .addOnSuccessListener(documentReference -> {
                                Log.d(TAG, "Avaliação enviada com sucesso: " + documentReference.getId());
                                
                                // Atualizar nota média do instrumento
                                atualizarNotaMediaInstrumento(avaliacao.getInstrumentoId())
                                        .thenAccept(sucesso -> {
                                            Log.d(TAG, "Nota média do instrumento atualizada: " + sucesso);
                                            futuro.complete(true);
                                        })
                                        .exceptionally(erro -> {
                                            Log.e(TAG, "Erro ao atualizar nota média: " + erro.getMessage(), erro);
                                            futuro.complete(true); // Ainda consideramos sucesso
                                            return null;
                                        });
                            })
                            .addOnFailureListener(erro -> {
                                Log.e(TAG, "Erro ao enviar avaliação: " + erro.getMessage(), erro);
                                futuro.completeExceptionally(erro);
                            });
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao verificar avaliações existentes: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Obtém todas as avaliações de um instrumento
     * 
     * @param instrumentoId ID do instrumento
     * @return CompletableFuture<List<DocumentSnapshot>> com as avaliações
     */
    public static CompletableFuture<List<DocumentSnapshot>> obterAvaliacoesInstrumento(String instrumentoId) {
        CompletableFuture<List<DocumentSnapshot>> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "=== OBTENDO AVALIAÇÕES DO INSTRUMENTO ===");
        Log.d(TAG, "Instrumento: " + instrumentoId);
        
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        firestore.collection("avaliacoes")
                .whereEqualTo("instrumentoId", instrumentoId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentSnapshot> avaliacoes = querySnapshot.getDocuments();
                    Log.d(TAG, "Avaliações encontradas: " + avaliacoes.size());
                    
                    // Ordenar por data de avaliação (mais recentes primeiro)
                    avaliacoes.sort((a, b) -> {
                        Timestamp dataA = a.getTimestamp("dataAvaliacao");
                        Timestamp dataB = b.getTimestamp("dataAvaliacao");
                        
                        if (dataA == null && dataB == null) return 0;
                        if (dataA == null) return 1;
                        if (dataB == null) return -1;
                        
                        return dataB.compareTo(dataA); // Descending order
                    });
                    
                    futuro.complete(avaliacoes);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao obter avaliações: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Obtém todas as avaliações recebidas por um proprietário
     * 
     * @param proprietarioId ID do proprietário
     * @return CompletableFuture<List<DocumentSnapshot>> com as avaliações
     */
    public static CompletableFuture<List<DocumentSnapshot>> obterAvaliacoesProprietario(String proprietarioId) {
        CompletableFuture<List<DocumentSnapshot>> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "=== OBTENDO AVALIAÇÕES DO PROPRIETÁRIO ===");
        Log.d(TAG, "Proprietário: " + proprietarioId);
        
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        firestore.collection("avaliacoes")
                .whereEqualTo("proprietarioId", proprietarioId)
                .orderBy("dataAvaliacao", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentSnapshot> avaliacoes = querySnapshot.getDocuments();
                    Log.d(TAG, "Avaliações encontradas: " + avaliacoes.size());
                    futuro.complete(avaliacoes);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao obter avaliações do proprietário: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Calcula e atualiza a nota média de um instrumento
     * 
     * @param instrumentoId ID do instrumento
     * @return CompletableFuture<Boolean> indicando sucesso
     */
    public static CompletableFuture<Boolean> atualizarNotaMediaInstrumento(String instrumentoId) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "=== ATUALIZANDO NOTA MÉDIA DO INSTRUMENTO ===");
        Log.d(TAG, "Instrumento: " + instrumentoId);
        
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        firestore.collection("avaliacoes")
                .whereEqualTo("instrumentoId", instrumentoId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentSnapshot> avaliacoes = querySnapshot.getDocuments();
                    
                    if (avaliacoes.isEmpty()) {
                        Log.d(TAG, "Nenhuma avaliação encontrada");
                        futuro.complete(true);
                        return;
                    }
                    
                    // Calcular nota média
                    double somaNotas = 0;
                    for (DocumentSnapshot avaliacao : avaliacoes) {
                        Double nota = avaliacao.getDouble("nota");
                        if (nota != null) {
                            somaNotas += nota;
                        }
                    }
                    
                    double notaMedia = somaNotas / avaliacoes.size();
                    long totalAvaliacoes = avaliacoes.size();
                    
                    Log.d(TAG, "Nota média calculada: " + notaMedia + " (" + totalAvaliacoes + " avaliações)");
                    
                    // Atualizar no documento do instrumento
                    Map<String, Object> atualizacoes = new HashMap<>();
                    atualizacoes.put("notaMedia", notaMedia);
                    atualizacoes.put("totalAvaliacoes", totalAvaliacoes);
                    
                    Log.d(TAG, "Tentando atualizar instrumento " + instrumentoId + " com nota média: " + notaMedia);
                    
                    firestore.collection("instruments").document(instrumentoId)
                            .update(atualizacoes)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Nota média atualizada com sucesso no instrumento: " + instrumentoId);
                                futuro.complete(true);
                            })
                            .addOnFailureListener(erro -> {
                                Log.e(TAG, "Erro ao atualizar nota média no instrumento " + instrumentoId + ": " + erro.getMessage(), erro);
                                
                                // Tentar usar set() como fallback se update() falhar
                                Log.d(TAG, "Tentando fallback com set()...");
                                firestore.collection("instruments").document(instrumentoId)
                                        .get()
                                        .addOnSuccessListener(documentSnapshot -> {
                                            if (documentSnapshot.exists()) {
                                                Map<String, Object> dados = documentSnapshot.getData();
                                                dados.put("notaMedia", notaMedia);
                                                dados.put("totalAvaliacoes", totalAvaliacoes);
                                                
                                                firestore.collection("instruments").document(instrumentoId)
                                                        .set(dados)
                                                        .addOnSuccessListener(aVoid2 -> {
                                                            Log.d(TAG, "Nota média atualizada com set() como fallback");
                                                            futuro.complete(true);
                                                        })
                                                        .addOnFailureListener(erro2 -> {
                                                            Log.e(TAG, "Erro no fallback set(): " + erro2.getMessage(), erro2);
                                                            futuro.completeExceptionally(erro2);
                                                        });
                                            } else {
                                                Log.e(TAG, "Documento do instrumento não encontrado");
                                                futuro.completeExceptionally(new Exception("Instrumento não encontrado"));
                                            }
                                        })
                                        .addOnFailureListener(erro2 -> {
                                            Log.e(TAG, "Erro ao buscar instrumento para fallback: " + erro2.getMessage(), erro2);
                                            futuro.completeExceptionally(erro2);
                                        });
                            });
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao calcular nota média: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Obtém uma reserva por ID
     * 
     * @param reservaId ID da reserva
     * @return CompletableFuture<DocumentSnapshot> com os dados da reserva
     */
    public static CompletableFuture<DocumentSnapshot> obterReservaPorId(String reservaId) {
        CompletableFuture<DocumentSnapshot> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "=== OBTENDO RESERVA POR ID ===");
        Log.d(TAG, "Reserva: " + reservaId);
        
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        firestore.collection("reservations").document(reservaId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Reserva encontrada");
                        futuro.complete(documentSnapshot);
                    } else {
                        Log.w(TAG, "Reserva não encontrada");
                        futuro.complete(null);
                    }
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao obter reserva: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Marca uma reserva como avaliada
     * 
     * @param reservaId ID da reserva
     * @return CompletableFuture<Boolean> indicando sucesso
     */
    public static CompletableFuture<Boolean> marcarReservaComoAvaliada(String reservaId) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "=== MARCANDO RESERVA COMO AVALIADA ===");
        Log.d(TAG, "Reserva: " + reservaId);
        
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        Map<String, Object> atualizacoes = new HashMap<>();
        atualizacoes.put("avaliada", true);
        atualizacoes.put("dataAvaliacao", Timestamp.now());
        
        firestore.collection("reservations").document(reservaId)
                .update(atualizacoes)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Reserva marcada como avaliada com sucesso");
                    futuro.complete(true);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao marcar reserva como avaliada: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Obtém um usuário por ID do Firestore
     * 
     * @param usuarioId ID do usuário
     * @return CompletableFuture<DocumentSnapshot> com os dados do usuário
     */
    public static CompletableFuture<DocumentSnapshot> obterUsuarioPorId(String usuarioId) {
        CompletableFuture<DocumentSnapshot> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "=== OBTENDO USUÁRIO POR ID ===");
        Log.d(TAG, "Usuário: " + usuarioId);
        
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        firestore.collection("users").document(usuarioId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Usuário encontrado");
                        futuro.complete(documentSnapshot);
                    } else {
                        Log.w(TAG, "Usuário não encontrado");
                        futuro.complete(null);
                    }
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao obter usuário: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Força a atualização da nota média de um instrumento específico
     * Método de debug para testar atualizações manuais
     * 
     * @param instrumentoId ID do instrumento
     */
    public static CompletableFuture<Boolean> forcarAtualizacaoNotaMedia(String instrumentoId) {
        Log.d(TAG, "=== FORÇANDO ATUALIZAÇÃO DA NOTA MÉDIA ===");
        Log.d(TAG, "Instrumento: " + instrumentoId);
        
        return atualizarNotaMediaInstrumento(instrumentoId);
    }
    
    
    /**
     * Marca uma conversa como excluída para um usuário específico
     * As mensagens só são apagadas definitivamente se ambos os usuários excluírem
     */
    public static CompletableFuture<Boolean> marcarConversaComoExcluida(String chatId, String userId) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Marcando conversa como excluída - Chat: " + chatId + ", Usuário: " + userId);
        
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        // Adicionar o usuário à lista de usuários que excluíram a conversa
        firestore.collection("chats").document(chatId)
                .update("usuariosExcluidos", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Conversa marcada como excluída com sucesso");
                    
                    // Verificar se ambos os usuários excluíram - se sim, apagar mensagens
                    verificarEExcluirMensagensSeNecessario(chatId)
                            .thenAccept(sucesso -> {
                                futuro.complete(true);
                            })
                            .exceptionally(erro -> {
                                Log.e(TAG, "Erro ao verificar exclusão de mensagens: " + erro.getMessage(), erro);
                                futuro.complete(true); // Ainda assim marcar como sucesso para a exclusão da conversa
                                return null;
                            });
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao marcar conversa como excluída: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Verifica se ambos os usuários excluíram a conversa e, se sim, apaga as mensagens
     */
    private static CompletableFuture<Boolean> verificarEExcluirMensagensSeNecessario(String chatId) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        firestore.collection("chats").document(chatId)
                .get()
                .addOnSuccessListener(chatDoc -> {
                    if (chatDoc.exists()) {
                        List<String> usuariosExcluidos = (List<String>) chatDoc.get("usuariosExcluidos");
                        String userId1 = chatDoc.getString("userId1");
                        String userId2 = chatDoc.getString("userId2");
                        
                        // Se ambos os usuários excluíram, apagar mensagens
                        if (usuariosExcluidos != null && usuariosExcluidos.contains(userId1) && usuariosExcluidos.contains(userId2)) {
                            Log.d(TAG, "Ambos os usuários excluíram a conversa, apagando mensagens...");
                            apagarMensagensConversa(chatId)
                                    .thenAccept(sucesso -> {
                                        Log.d(TAG, "Mensagens apagadas com sucesso: " + sucesso);
                                        futuro.complete(sucesso);
                                    })
                                    .exceptionally(erro -> {
                                        Log.e(TAG, "Erro ao apagar mensagens: " + erro.getMessage(), erro);
                                        futuro.complete(false);
                                        return null;
                                    });
                        } else {
                            Log.d(TAG, "Apenas um usuário excluiu, mantendo mensagens");
                            futuro.complete(true);
                        }
                    } else {
                        Log.w(TAG, "Documento do chat não encontrado");
                        futuro.complete(false);
                    }
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao verificar exclusão: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Apaga todas as mensagens de uma conversa
     */
    private static CompletableFuture<Boolean> apagarMensagensConversa(String chatId) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        Log.d(TAG, "Apagando mensagens da conversa: " + chatId);
        
        firestore.collection("chats").document(chatId).collection("messages")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<com.google.android.gms.tasks.Task<Void>> deleteTasks = new ArrayList<>();
                    
                    for (DocumentSnapshot messageDoc : querySnapshot.getDocuments()) {
                        deleteTasks.add(messageDoc.getReference().delete());
                    }
                    
                    com.google.android.gms.tasks.Tasks.whenAll(deleteTasks)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Todas as mensagens apagadas com sucesso");
                                futuro.complete(true);
                            })
                            .addOnFailureListener(erro -> {
                                Log.e(TAG, "Erro ao apagar mensagens: " + erro.getMessage(), erro);
                                futuro.complete(false);
                            });
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao buscar mensagens para apagar: " + erro.getMessage(), erro);
                    futuro.complete(false);
                });
        
        return futuro;
    }
    
    /**
     * Verifica se uma reserva já foi avaliada pelo usuário
     * 
     * @param reservaId ID da reserva
     * @param tipoAvaliacao "instrumento" ou "usuario"
     * @return CompletableFuture<Boolean> true se já foi avaliada
     */
    public static CompletableFuture<Boolean> verificarSeReservaFoiAvaliada(String reservaId, String tipoAvaliacao) {
        CompletableFuture<Boolean> futuro = new CompletableFuture<>();
        
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        String collection = tipoAvaliacao.equals("usuario") ? "avaliacoes_usuarios" : "avaliacoes";
        
        firestore.collection(collection)
                .whereEqualTo("reservaId", reservaId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean jaAvaliada = !querySnapshot.isEmpty();
                    Log.d(TAG, "Reserva " + reservaId + " já avaliada (" + tipoAvaliacao + "): " + jaAvaliada);
                    futuro.complete(jaAvaliada);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao verificar se reserva foi avaliada: " + erro.getMessage(), erro);
                    futuro.complete(false); // Em caso de erro, permitir avaliação
                });
        
        return futuro;
    }
    
    /**
     * Busca avaliações recebidas por um usuário (quando ele foi locatário)
     * 
     * @param usuarioId ID do usuário (como locatário)
     * @return CompletableFuture<List<DocumentSnapshot>> Lista de avaliações recebidas como locatário
     */
    public static CompletableFuture<List<DocumentSnapshot>> obterAvaliacoesRecebidas(String usuarioId) {
        CompletableFuture<List<DocumentSnapshot>> futuro = new CompletableFuture<>();
        
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        Log.d(TAG, "Buscando avaliações recebidas para usuário: " + usuarioId);
        
        firestore.collection("avaliacoes_usuarios")
                .whereEqualTo("avaliadoId", usuarioId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentSnapshot> avaliacoes = querySnapshot.getDocuments();
                    
                    // Ordenar manualmente por data de avaliação (mais recente primeiro)
                    avaliacoes.sort((doc1, doc2) -> {
                        Timestamp data1 = doc1.getTimestamp("dataAvaliacao");
                        Timestamp data2 = doc2.getTimestamp("dataAvaliacao");
                        
                        if (data1 == null && data2 == null) return 0;
                        if (data1 == null) return 1;
                        if (data2 == null) return -1;
                        
                        return data2.compareTo(data1); // DESCENDING
                    });
                    
                    Log.d(TAG, "Encontradas " + avaliacoes.size() + " avaliações recebidas");
                    futuro.complete(avaliacoes);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao buscar avaliações recebidas: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
    
    /**
     * Busca avaliações de instrumentos feitas por um usuário
     * 
     * @param usuarioId ID do usuário
     * @return CompletableFuture<List<DocumentSnapshot>> Lista de avaliações feitas
     */
    public static CompletableFuture<List<DocumentSnapshot>> obterAvaliacoesFeitas(String usuarioId) {
        CompletableFuture<List<DocumentSnapshot>> futuro = new CompletableFuture<>();
        
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        
        Log.d(TAG, "Buscando avaliações feitas pelo usuário: " + usuarioId);
        
        firestore.collection("avaliacoes")
                .whereEqualTo("locatarioId", usuarioId)
                .orderBy("dataAvaliacao", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentSnapshot> avaliacoes = querySnapshot.getDocuments();
                    Log.d(TAG, "Encontradas " + avaliacoes.size() + " avaliações feitas");
                    futuro.complete(avaliacoes);
                })
                .addOnFailureListener(erro -> {
                    Log.e(TAG, "Erro ao buscar avaliações feitas: " + erro.getMessage(), erro);
                    futuro.completeExceptionally(erro);
                });
        
        return futuro;
    }
} 