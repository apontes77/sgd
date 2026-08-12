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

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.sgd.adolescente.Adolescente;
import br.com.sgd.adolescente.VinculoAdolescenteDiscipulado;
import br.com.sgd.adolescente.VinculoAdolescenteRepository;
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
    "Nome mãe",
    "Telefone mãe",
    "Nome pai",
    "Telefone pai",
    "Responsável",
    "Telefone responsável",
    "Discipulado",
    "Discipulador",
    "Gerência"
  };

  private final VinculoAdolescenteRepository vinculos;
  private final DiscipuladoRepository discipulados;
  private final Clock clock;

  public RelatorioAdolescentesService(
      VinculoAdolescenteRepository vinculos, DiscipuladoRepository discipulados, Clock clock) {
    this.vinculos = vinculos;
    this.discipulados = discipulados;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public void exportarCsv(User usuario, Long discipuladoId, Boolean ativo, OutputStream out)
      throws IOException {
    if (!usuario.getPerfis().contains(Role.ADMIN)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
    }
    if (discipuladoId != null && !discipulados.existsById(discipuladoId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Discipulado não encontrado.");
    }

    List<VinculoAdolescenteDiscipulado> linhas =
        vinculos.findAtivosParaExport(discipuladoId, ativo);
    LocalDate hoje = LocalDate.now(clock.withZone(ZONA_NEGOCIO));

    out.write(UTF8_BOM);
    Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
    escreverLinha(writer, CABECALHO);
    for (VinculoAdolescenteDiscipulado vinculo : linhas) {
      Adolescente a = vinculo.getAdolescente();
      Discipulado d = vinculo.getDiscipulado();
      escreverLinha(
          writer,
          new String[] {
            texto(a.getNome()),
            texto(a.getDataNascimento()),
            String.valueOf(idade(a.getDataNascimento(), hoje)),
            texto(a.getTelefone()),
            texto(a.getInstagram()),
            a.getCategoria() == null ? "" : a.getCategoria().name(),
            simNao(a.isAtivo()),
            texto(a.getEstrutura()),
            texto(a.getMotivoAfastamento()),
            texto(a.getNomeMae()),
            texto(a.getTelefoneMae()),
            texto(a.getNomePai()),
            texto(a.getTelefonePai()),
            texto(a.getResponsavelNome()),
            texto(a.getResponsavelTelefone()),
            texto(d.getNome()),
            texto(d.getDiscipulador().getNome()),
            texto(d.getGerencia().getNome())
          });
    }
    writer.flush();
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

  private static void escreverLinha(Writer writer, String[] campos) throws IOException {
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
