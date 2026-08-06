package br.com.sgd.adolescente;

import java.time.LocalDate;

/** Dados cadastrais do adolescente, sem o vínculo com o discipulado. */
public record DadosCadastroAdolescente(
    String nome,
    LocalDate dataNascimento,
    String telefone,
    String instagram,
    LocalDate consentimentoEm,
    CategoriaAdolescente categoria,
    String estrutura,
    String motivoAfastamento,
    ContatosAdolescente contatos,
    boolean naoPossuiTelefone,
    boolean naoPossuiContatoFamiliar) {}
