package br.com.sgd.familia;

public final class FamiliaConstantes {
  public static final String NAO_CONSTA = "Não consta";

  private FamiliaConstantes() {}

  public static boolean isNaoConsta(String valor) {
    return valor == null || valor.isBlank() || NAO_CONSTA.equalsIgnoreCase(valor.trim());
  }

  public static String exigirTexto(String valor, String rotulo, int max) {
    String normalizado = valor == null || valor.isBlank() ? NAO_CONSTA : valor.trim();
    if (normalizado.length() > max) {
      throw new IllegalArgumentException(rotulo + " deve ter no máximo " + max + " caracteres.");
    }
    return normalizado;
  }
}
