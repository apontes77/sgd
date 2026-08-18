package br.com.sgd.relatorio;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.sgd.adolescente.Adolescente;
import br.com.sgd.adolescente.VinculoAdolescenteDiscipulado;
import br.com.sgd.adolescente.VinculoAdolescenteRepository;
import br.com.sgd.familia.FamiliaConstantes;
import br.com.sgd.familia.FichaFamilia;
import br.com.sgd.familia.FichaFamiliaRepository;
import br.com.sgd.familia.ResponsavelFamilia;
import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.organizacao.DiscipuladoRepository;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;

@Service
public class RelatorioAdolescentesService {
  private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Sao_Paulo");
  private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
  private static final String[] CABECALHO = {
    "Nome",
    "Data de nascimento",
    "Idade",
    "Telefone",
    "Instagram",
    "Categoria",
    "Ativo",
    "Estrutura",
    "Motivo afastamento",
    "Responsável 1 nome",
    "Responsável 1 parentesco",
    "Responsável 1 telefone",
    "Responsável 1 e-mail",
    "Responsável 2 nome",
    "Responsável 2 parentesco",
    "Responsável 2 telefone",
    "Responsável 2 e-mail",
    "Endereço",
    "Situação na igreja",
    "Situação dos pais",
    "Intervenção",
    "Irmão no Dokmos",
    "Discipulado",
    "Discipulador",
    "Gerência"
  };

  private final VinculoAdolescenteRepository vinculos;
  private final DiscipuladoRepository discipulados;
  private final FichaFamiliaRepository fichas;
  private final Clock clock;

  public RelatorioAdolescentesService(
      VinculoAdolescenteRepository vinculos,
      DiscipuladoRepository discipulados,
      FichaFamiliaRepository fichas,
      Clock clock) {
    this.vinculos = vinculos;
    this.discipulados = discipulados;
    this.fichas = fichas;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public void exportarCsv(User usuario, Long discipuladoId, Boolean ativo, OutputStream out)
      throws IOException {
    autorizarExportacao(usuario, discipuladoId);
    List<VinculoAdolescenteDiscipulado> linhas =
        vinculos.findAtivosParaExport(discipuladoId, ativo);
    Map<Long, FichaFamilia> fichasPorAdolescente = fichasPorAdolescente(linhas);
    LocalDate hoje = LocalDate.now(clock.withZone(ZONA_NEGOCIO));

    out.write(UTF8_BOM);
    Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
    escreverLinha(writer, CABECALHO);
    for (VinculoAdolescenteDiscipulado vinculo : linhas) {
      escreverLinha(
          writer,
          camposLinha(
              vinculo.getAdolescente(),
              vinculo.getDiscipulado(),
              fichasPorAdolescente.get(vinculo.getAdolescente().getId()),
              hoje));
    }
    writer.flush();
  }

  private void autorizarExportacao(User usuario, Long discipuladoId) {
    if (!usuario.getPerfis().contains(Role.ADMIN)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
    }
    if (discipuladoId != null && !discipulados.existsById(discipuladoId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Discipulado não encontrado.");
    }
  }

  private Map<Long, FichaFamilia> fichasPorAdolescente(List<VinculoAdolescenteDiscipulado> linhas) {
    List<Long> ids = linhas.stream().map(v -> v.getAdolescente().getId()).toList();
    return fichas.findByAdolescenteIdIn(ids).stream()
        .collect(Collectors.toMap(f -> f.getAdolescente().getId(), Function.identity()));
  }

  private static String[] camposLinha(
      Adolescente a, Discipulado d, FichaFamilia ficha, LocalDate hoje) {
    ResponsavelFamilia r1 = ficha == null ? null : ficha.getResponsavel1();
    ResponsavelFamilia r2 = ficha == null ? null : ficha.getResponsavel2();
    return new String[] {
      texto(a.getNome()),
      texto(a.getDataNascimento()),
      String.valueOf(idade(a.getDataNascimento(), hoje)),
      texto(a.getTelefone()),
      texto(a.getInstagram()),
      a.getCategoria() == null ? "" : a.getCategoria().name(),
      simNao(a.isAtivo()),
      texto(a.getEstrutura()),
      texto(a.getMotivoAfastamento()),
      celulaResponsavel(r1, ResponsavelFamilia::getNome),
      celulaResponsavel(r1, ResponsavelFamilia::getParentesco),
      celulaResponsavel(r1, ResponsavelFamilia::getTelefone),
      celulaResponsavel(r1, ResponsavelFamilia::getEmail),
      celulaResponsavel(r2, ResponsavelFamilia::getNome),
      celulaResponsavel(r2, ResponsavelFamilia::getParentesco),
      celulaResponsavel(r2, ResponsavelFamilia::getTelefone),
      celulaResponsavel(r2, ResponsavelFamilia::getEmail),
      celula(ficha == null ? null : ficha.enderecoResumo()),
      celula(ficha == null ? null : nomeEnum(ficha.getSituacaoIgreja())),
      celula(ficha == null ? null : nomeEnum(ficha.getSituacaoPais())),
      celula(ficha == null ? null : ficha.getIntervencao()),
      celula(ficha == null ? null : ficha.getIrmaoDokmos()),
      texto(d.getNome()),
      texto(d.getDiscipulador().getNome()),
      texto(d.getGerencia().getNome())
    };
  }

  private static String celulaResponsavel(
      ResponsavelFamilia responsavel, Function<ResponsavelFamilia, String> getter) {
    return celula(responsavel == null ? null : getter.apply(responsavel));
  }

  private static String nomeEnum(Enum<?> valor) {
    return valor == null ? null : valor.name();
  }

  private static int idade(LocalDate nascimento, LocalDate hoje) {
    if (nascimento == null) return 0;
    return Period.between(nascimento, hoje).getYears();
  }

  private static String simNao(boolean valor) {
    return valor ? "Sim" : "Não";
  }

  private static String texto(Object valor) {
    return valor == null ? "" : String.valueOf(valor);
  }

  private static String celula(String valor) {
    if (valor == null || valor.isBlank()) return FamiliaConstantes.NAO_CONSTA;
    return valor;
  }

  private static void escreverLinha(Writer writer, String... campos) throws IOException {
    for (int i = 0; i < campos.length; i++) {
      if (i > 0) writer.write(',');
      writer.write(escaparCsv(campos[i]));
    }
    writer.write('\n');
  }

  private static String escaparCsv(String valor) {
    if (valor.indexOf(',') < 0
        && valor.indexOf('"') < 0
        && valor.indexOf('\n') < 0
        && valor.indexOf('\r') < 0) {
      return valor;
    }
    return '"' + valor.replace("\"", "\"\"") + '"';
  }
}
