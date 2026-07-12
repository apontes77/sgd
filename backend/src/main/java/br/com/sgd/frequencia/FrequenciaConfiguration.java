package br.com.sgd.frequencia;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
@Configuration @EnableScheduling
public class FrequenciaConfiguration { @Bean @ConditionalOnMissingBean Clock clock(){return Clock.systemUTC();} }
