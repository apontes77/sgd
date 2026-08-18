package br.com.sgd.familia;

final class TelefoneFamiliaValidator {
  private TelefoneFamiliaValidator() {}

  static String validar(String telefone, String rotulo) {
    if (telefone == null || telefone.isBlank()) {
      throw new IllegalArgumentException("O " + rotulo + " é obrigatório.");
    }
    String digits = telefone.replaceAll("\\D", "");
    if (digits.startsWith("55") && (digits.length() == 12 || digits.length() == 13)) {
      digits = digits.substring(2);
    }
    if (digits.length() != 10 && digits.length() != 11) {
      throw new IllegalArgumentException(
          "O " + rotulo + " deve ser um telefone válido com DDD (10 ou 11 dígitos).");
    }
    return telefone.trim();
  }
}
