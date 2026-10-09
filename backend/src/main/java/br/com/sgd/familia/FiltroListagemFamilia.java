package br.com.sgd.familia;

public record FiltroListagemFamilia(
    String busca,
    SituacaoIgrejaFamilia situacaoIgreja,
    SituacaoPaisFamilia situacaoPais,
    String situacaoFicha,
    String tipoDesafio,
    String situacaoIntervencao) {}
