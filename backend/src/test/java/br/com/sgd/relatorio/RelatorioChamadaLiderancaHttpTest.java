package br.com.sgd.relatorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.sgd.frequencia.SituacaoFrequencia;
import br.com.sgd.lideranca.ChamadaLideranca;
import br.com.sgd.lideranca.ChamadaLiderancaDiscipulado;
import br.com.sgd.lideranca.ChamadaLiderancaRepository;
import br.com.sgd.lideranca.PapelLideranca;
import br.com.sgd.lideranca.PresencaLideranca;
import br.com.sgd.organizacao.Discipulado;
import br.com.sgd.organizacao.DiscipuladoRepository;
import br.com.sgd.organizacao.FaixaEtaria;
import br.com.sgd.organizacao.Gerencia;
import br.com.sgd.organizacao.GerenciaRepository;
import br.com.sgd.organizacao.Sexo;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RelatorioChamadaLiderancaHttpTest {
  private static final String SENHA = "senha-inicial-segura";
  private static final LocalDate DATA = LocalDate.of(2026, 8, 14);
  private static final Instant AGORA = Instant.parse("2026-08-14T22:00:00Z");

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UserRepository usuarios;
  @Autowired GerenciaRepository gerencias;
  @Autowired DiscipuladoRepository discipulados;
  @Autowired ChamadaLiderancaRepository chamadas;
  @Autowired PasswordEncoder passwords;

  private User admin;
  private User lider;
  private User coLider;
  private Discipulado alpha;
  private Discipulado beta;

  @BeforeEach
  void preparar() {
    String s = UUID.randomUUID().toString();
    admin = usuario("Admin", "admin-rcl-" + s, Role.ADMIN);
    lider = usuario("Líder Alpha", "lider-rcl-" + s, Role.DISCIPULADOR);
    coLider = usuario("Co Alpha", "co-rcl-" + s, Role.CO_LIDER);
    User liderBeta = usuario("Líder Beta", "lider-beta-rcl-" + s, Role.DISCIPULADOR);
    User gerente = usuario("Gerente", "gerente-rcl-" + s, Role.GERENTE);
    Gerencia centro =
        gerencias.saveAndFlush(
            new Gerencia("Centro RL", Sexo.MASCULINO, Set.of(FaixaEtaria.DE_15_MAIS), gerente));
    alpha = new Discipulado("Alpha RL", Sexo.MASCULINO, FaixaEtaria.DE_15_MAIS, centro, lider);
    alpha.replaceCoLideres(Set.of(coLider));
    alpha = discipulados.saveAndFlush(alpha);
    beta =
        discipulados.saveAndFlush(
            new Discipulado("Beta RL", Sexo.FEMININO, FaixaEtaria.DE_15_MAIS, centro, liderBeta));

    ChamadaLideranca sexta = new ChamadaLideranca(DATA);
    sexta.atualizarObservacaoGeral("Culto tranquilo", AGORA);
    ChamadaLiderancaDiscipulado itemAlpha =
        new ChamadaLiderancaDiscipulado(alpha, "Líder chegou atrasado");
    itemAlpha.substituirPresencas(
        List.of(
            new PresencaLideranca(lider, PapelLideranca.DISCIPULADOR, SituacaoFrequencia.PRESENTE),
            new PresencaLideranca(coLider, PapelLideranca.CO_LIDER, SituacaoFrequencia.AUSENTE)));
    ChamadaLiderancaDiscipulado itemBeta = new ChamadaLiderancaDiscipulado(beta, null);
    itemBeta.substituirPresencas(
        List.of(
            new PresencaLideranca(
                liderBeta, PapelLideranca.DISCIPULADOR, SituacaoFrequencia.PRESENTE)));
    sexta.mesclarItens(List.of(itemAlpha, itemBeta), AGORA);
    chamadas.saveAndFlush(sexta);

    ChamadaLideranca sextaAnterior = new ChamadaLideranca(DATA.minusWeeks(1));
    sextaAnterior.atualizarObservacaoGeral(null, AGORA);
    ChamadaLiderancaDiscipulado itemAnterior = new ChamadaLiderancaDiscipulado(alpha, null);
    itemAnterior.substituirPresencas(
        List.of(
            new PresencaLideranca(lider, PapelLideranca.DISCIPULADOR, SituacaoFrequencia.AUSENTE),
            new PresencaLideranca(coLider, PapelLideranca.CO_LIDER, SituacaoFrequencia.AUSENTE)));
    sextaAnterior.mesclarItens(List.of(itemAnterior), AGORA);
    chamadas.saveAndFlush(sextaAnterior);
  }

  @Test
  void adminConsultaPeriodoComResumoEFiltroPorDiscipulado() throws Exception {
    String token = token(admin);

    mvc.perform(
            get("/api/v1/relatorios/chamadas-lideranca")
                .param("dataInicio", DATA.minusWeeks(1).toString())
                .param("dataFim", DATA.toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.relatorios.length()").value(2))
        .andExpect(jsonPath("$.relatorios[0].data").value(DATA.minusWeeks(1).toString()))
        .andExpect(jsonPath("$.relatorios[0].resumo.ausentes").value(2))
        .andExpect(jsonPath("$.relatorios[1].data").value(DATA.toString()))
        .andExpect(jsonPath("$.relatorios[1].observacaoGeral").value("Culto tranquilo"))
        .andExpect(jsonPath("$.relatorios[1].discipulados.length()").value(2))
        .andExpect(jsonPath("$.relatorios[1].discipulados[0].discipuladoNome").value("Alpha RL"))
        .andExpect(
            jsonPath("$.relatorios[1].discipulados[0].observacao").value("Líder chegou atrasado"))
        .andExpect(
            jsonPath("$.relatorios[1].discipulados[0].presencas[0].papel").value("DISCIPULADOR"))
        .andExpect(
            jsonPath("$.relatorios[1].discipulados[0].presencas[0].situacao").value("PRESENTE"))
        .andExpect(jsonPath("$.relatorios[1].discipulados[0].presencas[1].papel").value("CO_LIDER"))
        .andExpect(jsonPath("$.relatorios[1].resumo.presentes").value(2))
        .andExpect(jsonPath("$.relatorios[1].resumo.ausentes").value(1))
        .andExpect(jsonPath("$.relatorios[1].resumo.participantes").value(3));

    mvc.perform(
            get("/api/v1/relatorios/chamadas-lideranca")
                .param("dataInicio", DATA.toString())
                .param("dataFim", DATA.toString())
                .param("discipuladoId", String.valueOf(alpha.getId()))
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.relatorios.length()").value(1))
        .andExpect(jsonPath("$.relatorios[0].discipulados.length()").value(1))
        .andExpect(jsonPath("$.relatorios[0].discipulados[0].discipuladoNome").value("Alpha RL"))
        .andExpect(
            jsonPath("$.relatorios[0].discipulados[?(@.discipuladoNome == 'Beta RL')]").isEmpty())
        .andExpect(jsonPath("$.relatorios[0].resumo.participantes").value(2));
  }

  @Test
  void validaLimitesDoPeriodoEDiscipuladoInexistente() throws Exception {
    String token = token(admin);
    mvc.perform(
            get("/api/v1/relatorios/chamadas-lideranca")
                .param("dataInicio", DATA.toString())
                .param("dataFim", DATA.minusDays(1).toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/relatorios/chamadas-lideranca")
                .param("dataInicio", DATA.toString())
                .param("dataFim", DATA.plusMonths(12).plusDays(1).toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/relatorios/chamadas-lideranca")
                .param("dataInicio", DATA.toString())
                .param("dataFim", DATA.toString())
                .param("discipuladoId", "999999")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isNotFound());
  }

  @Test
  void exportaExcelComAnexo() throws Exception {
    String hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString();
    byte[] corpo =
        mvc.perform(
                get("/api/v1/relatorios/chamadas-lideranca/export")
                    .param("dataInicio", DATA.toString())
                    .param("dataFim", DATA.toString())
                    .header(HttpHeaders.AUTHORIZATION, bearer(token(admin))))
            .andExpect(status().isOk())
            .andExpect(
                header()
                    .string(
                        HttpHeaders.CONTENT_TYPE,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(
                header()
                    .string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"chamada-lideranca-" + hoje + ".xlsx\""))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertThat(corpo.length).isGreaterThan(100);
    assertThat(corpo[0]).isEqualTo((byte) 0x50);
    assertThat(corpo[1]).isEqualTo((byte) 0x4b);
  }

  @Test
  void naoAdminRecebeForbidden() throws Exception {
    mvc.perform(
            get("/api/v1/relatorios/chamadas-lideranca")
                .param("dataInicio", DATA.toString())
                .param("dataFim", DATA.toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(token(lider))))
        .andExpect(status().isForbidden());
  }

  private User usuario(String nome, String prefixo, Role... perfis) {
    return usuarios.saveAndFlush(
        new User(nome, prefixo + "@sgd.local", passwords.encode(SENHA), Set.of(perfis)));
  }

  private String token(User usuario) throws Exception {
    String response =
        mvc.perform(
                post("/api/v1/autenticacao/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"" + usuario.getEmail() + "\",\"senha\":\"" + SENHA + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(response).get("accessToken").asText();
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }
}
