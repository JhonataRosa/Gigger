package com.example.instrumentaliza;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * AtividadeEditarInstrumento - Tela para editar instrumento existente
 * 
 * Esta tela permite editar as informações de um instrumento já existente,
 * incluindo nome, categoria, preço, descrição e foto. Integra com o
 * sistema Firebase para atualizar os dados.
 * 
 * Funcionalidades:
 * - Carregamento dos dados atuais do instrumento
 * - Edição de nome, categoria, preço e descrição
 * - Alteração da foto do instrumento
 * - Validação de dados antes de salvar
 * - Atualização no Firebase
 * - Navegação de volta após sucesso
 * 
 * @author Jhonata
 * @version 1.0
 */
public class AtividadeEditarInstrumento extends AppCompatActivity {
    
    private static final String TAG = "EditarInstrumento";
    private static final int PICK_IMAGE_REQUEST = 1;
    
    // Componentes da interface
    private TextInputEditText campoNome;
    private AutoCompleteTextView campoCategoria;
    private TextInputEditText campoPreco;
    private TextInputEditText campoDescricao;
    private ImageView imagemInstrumento;
    private MaterialButton botaoAlterarFoto;
    private MaterialButton botaoSalvar;
    
    // Dados do instrumento
    private String instrumentoId;
    private String proprietarioId;
    private Uri imagemUriAtual;
    private String imagemUrlAtual;
    private boolean imagemAlterada = false;
    
    // Firebase
    private FirebaseUser usuarioAtual;
    
    // Lista de categorias
    private final String[] categorias = {
        "Cordas", "Percussao", "Sopro", "Teclas", "Outros"
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_instrument);
        
        Log.d(TAG, "=== ATIVIDADE EDITAR INSTRUMENTO INICIADA ===");
        
        // Configurar toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        // Inicializar Firebase
        GerenciadorFirebase.inicializar(this);
        usuarioAtual = FirebaseAuth.getInstance().getCurrentUser();
        
        // Verificar se o usuário está logado
        if (usuarioAtual == null) {
            Log.e(TAG, "Usuário não está logado");
            finish();
            return;
        }
        
        // Obter dados do Intent
        instrumentoId = getIntent().getStringExtra("instrument_id");
        if (instrumentoId == null) {
            Log.e(TAG, "ID do instrumento não fornecido");
            Toast.makeText(this, "Erro: ID do instrumento não encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        Log.d(TAG, "Editando instrumento: " + instrumentoId);
        
        // Inicializar componentes
        inicializarComponentes();
        
        // Configurar listeners
        configurarListeners();
        
        // Carregar dados do instrumento
        carregarDadosInstrumento();
    }
    
    /**
     * Inicializa todos os componentes da interface
     */
    private void inicializarComponentes() {
        Log.d(TAG, "Inicializando componentes");
        
        // Campos de texto
        campoNome = findViewById(R.id.nameEditText);
        campoCategoria = findViewById(R.id.categorySpinner);
        campoPreco = findViewById(R.id.priceEditText);
        campoDescricao = findViewById(R.id.descriptionEditText);
        
        // Imagem e botões
        imagemInstrumento = findViewById(R.id.instrumentImageView);
        botaoAlterarFoto = findViewById(R.id.changePhotoButton);
        botaoSalvar = findViewById(R.id.saveButton);
        
        // Configurar spinner de categorias
        ArrayAdapter<String> adapterCategorias = new ArrayAdapter<>(this, 
            android.R.layout.simple_dropdown_item_1line, categorias);
        campoCategoria.setAdapter(adapterCategorias);
    }
    
    /**
     * Configura os listeners dos componentes
     */
    private void configurarListeners() {
        // Botão alterar foto
        botaoAlterarFoto.setOnClickListener(v -> selecionarImagem());
        
        // Botão salvar
        botaoSalvar.setOnClickListener(v -> salvarAlteracoes());
    }
    
    /**
     * Carrega os dados atuais do instrumento do Firebase
     */
    private void carregarDadosInstrumento() {
        Log.d(TAG, "Carregando dados do instrumento: " + instrumentoId);
        
        GerenciadorFirebase.obterInstrumentoPorId(instrumentoId)
                .thenAccept(documento -> {
                    if (documento != null && documento.exists()) {
                        runOnUiThread(() -> {
                            preencherCampos(documento);
                        });
                    } else {
                        runOnUiThread(() -> {
                            Log.e(TAG, "Instrumento não encontrado");
                            Toast.makeText(this, "Instrumento não encontrado", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                })
                .exceptionally(erro -> {
                    Log.e(TAG, "Erro ao carregar instrumento: " + erro.getMessage(), erro);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Erro ao carregar dados do instrumento", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return null;
                });
    }
    
    /**
     * Preenche os campos com os dados do instrumento
     */
    private void preencherCampos(DocumentSnapshot documento) {
        Log.d(TAG, "Preenchendo campos com dados do instrumento");
        
        // Preencher campos básicos
        campoNome.setText(documento.getString("name"));
        campoCategoria.setText(documento.getString("category"));
        campoPreco.setText(String.valueOf(documento.getDouble("price")));
        campoDescricao.setText(documento.getString("description"));
        
        // Salvar dados para validação
        proprietarioId = documento.getString("ownerId");
        imagemUrlAtual = documento.getString("imageUri");
        
        // Carregar imagem atual
        if (imagemUrlAtual != null && !imagemUrlAtual.isEmpty()) {
            Glide.with(this)
                    .load(Uri.parse(imagemUrlAtual))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(imagemInstrumento);
        }
        
        Log.d(TAG, "Campos preenchidos com sucesso");
    }
    
    /**
     * Abre o seletor de imagens
     */
    private void selecionarImagem() {
        Log.d(TAG, "Abrindo seletor de imagens");
        
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Selecionar Foto"), PICK_IMAGE_REQUEST);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imagemUriAtual = data.getData();
            imagemAlterada = true;
            
            // Atualizar a imagem na interface
            Glide.with(this)
                    .load(imagemUriAtual)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(imagemInstrumento);
            
            Log.d(TAG, "Imagem selecionada: " + imagemUriAtual.toString());
        }
    }
    
    /**
     * Valida os dados inseridos pelo usuário
     */
    private boolean validarDados() {
        String nome = campoNome.getText().toString().trim();
        String categoria = campoCategoria.getText().toString().trim();
        String precoStr = campoPreco.getText().toString().trim();
        String descricao = campoDescricao.getText().toString().trim();
        
        // Validar nome
        if (TextUtils.isEmpty(nome)) {
            campoNome.setError("Nome é obrigatório");
            campoNome.requestFocus();
            return false;
        }
        
        // Validar categoria
        if (TextUtils.isEmpty(categoria)) {
            Toast.makeText(this, "Selecione uma categoria", Toast.LENGTH_SHORT).show();
            campoCategoria.requestFocus();
            return false;
        }
        
        // Validar preço
        if (TextUtils.isEmpty(precoStr)) {
            campoPreco.setError("Preço é obrigatório");
            campoPreco.requestFocus();
            return false;
        }
        
        try {
            double preco = Double.parseDouble(precoStr);
            if (preco <= 0) {
                campoPreco.setError("Preço deve ser maior que zero");
                campoPreco.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            campoPreco.setError("Preço inválido");
            campoPreco.requestFocus();
            return false;
        }
        
        return true;
    }
    
    /**
     * Salva as alterações do instrumento
     */
    private void salvarAlteracoes() {
        Log.d(TAG, "Salvando alterações do instrumento");
        
        if (!validarDados()) {
            return;
        }
        
        // Desabilitar botão para evitar múltiplos cliques
        botaoSalvar.setEnabled(false);
        botaoSalvar.setText("Salvando...");
        
        // Obter dados dos campos
        String nome = campoNome.getText().toString().trim();
        String categoria = campoCategoria.getText().toString().trim();
        double preco = Double.parseDouble(campoPreco.getText().toString().trim());
        String descricao = campoDescricao.getText().toString().trim();
        
        Log.d(TAG, "Dados a serem salvos - Nome: " + nome + ", Categoria: " + categoria + 
              ", Preço: " + preco + ", Descrição: " + descricao);
        
        // Se a imagem foi alterada, fazer upload primeiro
        if (imagemAlterada && imagemUriAtual != null) {
            fazerUploadImagemEAtualizar(nome, categoria, preco, descricao);
        } else {
            // Atualizar apenas os dados, sem alterar a imagem
            atualizarDadosInstrumento(nome, categoria, preco, descricao, imagemUrlAtual);
        }
    }
    
    /**
     * Faz upload da nova imagem e atualiza o instrumento
     */
    private void fazerUploadImagemEAtualizar(String nome, String categoria, double preco, String descricao) {
        Log.d(TAG, "Fazendo upload da nova imagem");
        
        GerenciadorFirebase.fazerUploadImagemInstrumento(imagemUriAtual, instrumentoId)
                .thenAccept(novaUrlImagem -> {
                    Log.d(TAG, "Upload da imagem concluído: " + novaUrlImagem);
                    atualizarDadosInstrumento(nome, categoria, preco, descricao, novaUrlImagem);
                })
                .exceptionally(erro -> {
                    Log.e(TAG, "Erro no upload da imagem: " + erro.getMessage(), erro);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Erro ao fazer upload da imagem", Toast.LENGTH_SHORT).show();
                        reativarBotao();
                    });
                    return null;
                });
    }
    
    /**
     * Atualiza os dados do instrumento no Firebase
     */
    private void atualizarDadosInstrumento(String nome, String categoria, double preco, String descricao, String urlImagem) {
        Log.d(TAG, "Atualizando dados do instrumento no Firebase");
        
        GerenciadorFirebase.atualizarInstrumento(instrumentoId, nome, categoria, preco, descricao, urlImagem)
                .thenAccept(sucesso -> {
                    runOnUiThread(() -> {
                        if (sucesso) {
                            Log.d(TAG, "Instrumento atualizado com sucesso");
                            Toast.makeText(this, "Instrumento atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                            
                            // Voltar para a tela anterior com indicação de sucesso
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("instrument_updated", true);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        } else {
                            Log.e(TAG, "Falha ao atualizar instrumento");
                            Toast.makeText(this, "Erro ao atualizar instrumento", Toast.LENGTH_SHORT).show();
                            reativarBotao();
                        }
                    });
                })
                .exceptionally(erro -> {
                    Log.e(TAG, "Erro ao atualizar instrumento: " + erro.getMessage(), erro);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Erro ao atualizar instrumento: " + erro.getMessage(), Toast.LENGTH_LONG).show();
                        reativarBotao();
                    });
                    return null;
                });
    }
    
    /**
     * Reativa o botão de salvar
     */
    private void reativarBotao() {
        botaoSalvar.setEnabled(true);
        botaoSalvar.setText("Salvar Alterações");
    }
    
    /**
     * Manipula a seleção de itens do menu (navegação)
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        // Verificar se há alterações não salvas
        // Por simplicidade, sempre permite voltar
        super.onBackPressed();
    }
}
