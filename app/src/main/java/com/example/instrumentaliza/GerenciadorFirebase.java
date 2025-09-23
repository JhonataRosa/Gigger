package com.example.instrumentaliza;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
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
        dadosReserva.put("status", "PENDING");
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
                .whereNotIn("status", List.of("CANCELLED"))
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
                                
                                // Filtrar apenas chats que têm mensagens
                                filtrarChatsComMensagens(todosChats, futuro);
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
} 