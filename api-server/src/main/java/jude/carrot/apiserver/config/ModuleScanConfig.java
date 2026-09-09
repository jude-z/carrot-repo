package jude.carrot.apiserver.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackages = {"jude.carrot.web", "jude.carrot.infra"})
@EntityScan(basePackages = "jude.carrot.infra.entity")
@EnableJpaRepositories(basePackages = "jude.carrot.infra.repository")
public class ModuleScanConfig {
}
