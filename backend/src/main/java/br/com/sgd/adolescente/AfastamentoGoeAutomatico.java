package br.com.sgd.adolescente;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Converte, na subida, discípulos que já cumpririam a regra de afastamento para GOE. */
@Component
public class AfastamentoGoeAutomatico implements ApplicationRunner {
  private final AdolescenteService adolescentes;

  public AfastamentoGoeAutomatico(AdolescenteService adolescentes) {
    this.adolescentes = adolescentes;
  }

  @Override
  public void run(ApplicationArguments args) {
    adolescentes.afastarElegiveisGoe();
  }
}
