package com.example.instrumentaliza;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.text.Normalizer;

/**
 * AdicionarInstrumentoActivity - Tela de adição/edição de instrumentos
 * 
 * Esta tela permite que usuários adicionem novos instrumentos ao sistema
 * ou editem instrumentos existentes. Oferece funcionalidades para
 * preenchimento de dados, seleção de categoria e upload de imagem.
 * 
 * Funcionalidades principais:
 * - Adição de novos instrumentos
 * - Edição de instrumentos existentes
 * - Seleção de categoria via dropdown
 * - Upload de imagem do instrumento
 * - Validação de campos obrigatórios
 * - Salvamento no Firebase Firestore
 * 
 * Características técnicas:
 * - ActivityResultLauncher para seleção de imagem
 * - AutoCompleteTextView para categorias
 * - Validação de dados antes do salvamento
 * - Upload de imagem para Firebase Storage
 * - Modo de edição baseado no Intent
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AdicionarInstrumentoActivity extends AppCompatActivity {
    
    // Constantes
    private static final String TAG = "AdicionarInstrumentoActivity";
    
    // Componentes da interface
    private TextInputEditText campoNome, campoDescricao, campoPreco;
    private AutoCompleteTextView campoCategoria;
    private ImageView imagemInstrumento;
    
    // Dados da imagem
    private Uri uriImagemSelecionada;
    
    // Autenticação e controle de estado
    private FirebaseAuth autenticacao;
    private boolean estaSalvando = false;

    /**
     * Launcher para seleção de imagem da galeria
     * 
     * Configurado para capturar o resultado da seleção de imagem
     * e atualizar a interface com a imagem selecionada.
     */
    private final ActivityResultLauncher<Intent> selecionarImagem = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            resultado -> {
                if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null) {
                    uriImagemSelecionada = resultado.getData().getData();
                    imagemInstrumento.setImageURI(uriImagemSelecionada);
                }
            });

    /**
     * Normaliza uma string de categoria removendo acentos e formatando
     * 
     * @param entrada String de entrada a ser normalizada
     * @return String normalizada com primeira letra maiúscula
     */
    private static String normalizarCategoria(String entrada) {
        if (entrada == null) return "";
        String normalizada = Normalizer.normalize(entrada.trim(), Normalizer.Form.NFD)
            .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        if (!normalizada.isEmpty()) {
            normalizada = normalizada.substring(0, 1).toUpperCase() + normalizada.substring(1).toLowerCase();
        }
        return normalizada;
    }

    /**
     * Método chamado quando a atividade é criada
     * 
     * Configura a interface de adição/edição de instrumentos, incluindo:
     * - Configuração da toolbar com botão de voltar
     * - Inicialização do Firebase e autenticação
     * - Configuração dos campos de entrada
     * - Configuração do dropdown de categorias
     * - Carregamento de dados se estiver em modo de edição
     * - Configuração dos listeners dos botões
     * 
     * @param savedInstanceState Estado anterior da atividade (se houver)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Log.d(TAG, "Iniciando AdicionarInstrumentoActivity...");
            setContentView(R.layout.activity_add_instrument);

            // Inicializar Firebase ANTES de qualquer uso
            GerenciadorFirebase.inicializar(this);

            autenticacao = FirebaseAuth.getInstance();
            Log.d(TAG, "FirebaseAuth inicializado");

            // Verificar se o usuário está logado
            if (autenticacao.getCurrentUser() == null) {
                Log.d(TAG, "Usuário não está logado, redirecionando para LoginActivity");
                Toast.makeText(this, getString(R.string.login_required), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, AtividadeLogin.class));
                finish();
                return;
            }

            Log.d(TAG, "Usuário logado, ID: " + autenticacao.getCurrentUser().getUid());

            // Inicializar views
            campoNome = findViewById(R.id.nameEditText);
            campoDescricao = findViewById(R.id.descriptionEditText);
            campoPreco = findViewById(R.id.priceEditText);
            campoCategoria = findViewById(R.id.categoryAutoComplete);
            imagemInstrumento = findViewById(R.id.instrumentImageView);
            Button botaoAdicionarFoto = findViewById(R.id.addPhotoButton);
            Button botaoSalvar = findViewById(R.id.saveButton);

            // Configurar categorias
            String[] categorias = {getString(R.string.category_strings), getString(R.string.category_percussao), getString(R.string.category_sopro), getString(R.string.category_teclas), getString(R.string.category_acessorios)};
            ArrayAdapter<String> adaptador = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, categorias);
            campoCategoria.setAdapter(adaptador);

            // Configurar listeners
            botaoAdicionarFoto.setOnClickListener(v -> {
                Log.d(TAG, "Botão de adicionar foto clicado");
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                selecionarImagem.launch(intent);
            });

            botaoSalvar.setOnClickListener(v -> salvarInstrumento());

            Log.d(TAG, "AdicionarInstrumentoActivity inicializada com sucesso");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao inicializar AdicionarInstrumentoActivity: " + e.getMessage(), e);
            Toast.makeText(this, getString(R.string.error_generic) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void salvarInstrumento() {
        if (estaSalvando) {
            Log.d(TAG, "Já existe um salvamento em andamento");
            return;
        }

        try {
            Log.d(TAG, "Iniciando processo de salvamento do instrumento...");
            String nome = campoNome.getText().toString().trim();
            String descricao = campoDescricao.getText().toString().trim();
            String textoPreco = campoPreco.getText().toString().trim();
            String categoria = campoCategoria.getText().toString();
            categoria = normalizarCategoria(categoria);
            String idProprietario = autenticacao.getCurrentUser().getUid();

            // Validação básica
            if (nome.isEmpty() || descricao.isEmpty() || textoPreco.isEmpty() || categoria.isEmpty()) {
                Log.d(TAG, "Campos vazios detectados");
                Toast.makeText(this, getString(R.string.validation_required), Toast.LENGTH_SHORT).show();
                return;
            }

            double preco;
            try {
                preco = Double.parseDouble(textoPreco);
            } catch (NumberFormatException e) {
                Log.d(TAG, "Preço inválido: " + textoPreco);
                Toast.makeText(this, getString(R.string.validation_price_positive), Toast.LENGTH_SHORT).show();
                return;
            }

            // Marcar que está salvando
            estaSalvando = true;

            // Se há uma imagem selecionada, fazer upload primeiro
            if (uriImagemSelecionada != null) {
                Log.d(TAG, "Fazendo upload da imagem...");
                Log.d(TAG, "URI da imagem: " + uriImagemSelecionada.toString());
                String nomeArquivo = "instrumento_" + System.currentTimeMillis() + ".jpg";
                Log.d(TAG, "Nome do arquivo: " + nomeArquivo);
                
                // Capturar as variáveis finais para usar no lambda
                final String idProprietarioFinal = idProprietario;
                final String nomeFinal = nome;
                final String descricaoFinal = descricao;
                final String categoriaFinal = categoria;
                final double precoFinal = preco;
                
                Log.d(TAG, "Chamando GerenciadorFirebase.uploadInstrumentImage...");
                GerenciadorFirebase.enviarImagemInstrumento(uriImagemSelecionada, nomeArquivo)
                        .thenCompose(urlImagem -> {
                            Log.d(TAG, "Imagem enviada com sucesso: " + urlImagem);
                            // Agora salvar o instrumento com a URL da imagem
                            Log.d(TAG, "Salvando instrumento com URL da imagem: " + urlImagem);
                            return GerenciadorFirebase.criarInstrumento(idProprietarioFinal, nomeFinal, descricaoFinal, categoriaFinal, precoFinal, urlImagem);
                        })
                        .thenAccept(idInstrumento -> {
                            Log.d(TAG, "Instrumento salvo com sucesso no Firebase, ID: " + idInstrumento);
                            runOnUiThread(() -> {
                                Toast.makeText(this, getString(R.string.instrument_saved), Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            });
                        })
                        .exceptionally(erro -> {
                            Log.e(TAG, "Erro ao salvar instrumento no Firebase: " + erro.getMessage(), erro);
                            runOnUiThread(() -> {
                                String mensagemErro = "Erro ao salvar instrumento";
                                if (erro.getMessage() != null) {
                                    if (erro.getMessage().contains("network")) {
                                        mensagemErro = "Erro de conexão. Verifique sua internet.";
                                    } else if (erro.getMessage().contains("permission")) {
                                        mensagemErro = "Erro de permissão. Verifique as regras do Firebase.";
                                    } else {
                                        mensagemErro = "Erro: " + erro.getMessage();
                                    }
                                }
                                Toast.makeText(this, mensagemErro, Toast.LENGTH_LONG).show();
                                estaSalvando = false;
                            });
                            return null;
                        });
            } else {
                // Sem imagem, salvar diretamente
                Log.d(TAG, "Salvando instrumento sem imagem...");
                
                // Capturar as variáveis finais para usar no lambda
                final String idProprietarioFinal = idProprietario;
                final String nomeFinal = nome;
                final String descricaoFinal = descricao;
                final String categoriaFinal = categoria;
                final double precoFinal = preco;
                
                GerenciadorFirebase.criarInstrumento(idProprietarioFinal, nomeFinal, descricaoFinal, categoriaFinal, precoFinal, null)
                        .thenAccept(idInstrumento -> {
                            Log.d(TAG, "Instrumento salvo com sucesso no Firebase, ID: " + idInstrumento);
                            runOnUiThread(() -> {
                                Toast.makeText(this, getString(R.string.instrument_saved), Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            });
                        })
                        .exceptionally(erro -> {
                            Log.e(TAG, "Erro ao salvar instrumento no Firebase: " + erro.getMessage(), erro);
                            runOnUiThread(() -> {
                                String mensagemErro = "Erro ao salvar instrumento";
                                if (erro.getMessage() != null) {
                                    if (erro.getMessage().contains("network")) {
                                        mensagemErro = "Erro de conexão. Verifique sua internet.";
                                    } else if (erro.getMessage().contains("permission")) {
                                        mensagemErro = "Erro de permissão. Verifique as regras do Firebase.";
                                    } else {
                                        mensagemErro = "Erro: " + erro.getMessage();
                                    }
                                }
                                Toast.makeText(this, mensagemErro, Toast.LENGTH_LONG).show();
                                estaSalvando = false;
                            });
                            return null;
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro no método salvarInstrumento: " + e.getMessage(), e);
            Toast.makeText(this, getString(R.string.error_generic) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
            estaSalvando = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Firebase gerencia automaticamente as conexões
    }
} 