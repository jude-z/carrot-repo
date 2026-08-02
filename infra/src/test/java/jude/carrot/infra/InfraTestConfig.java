package jude.carrot.infra;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackages = {
        "jude.carrot.infra.entity.user",
        "jude.carrot.infra.entity.post",
        "jude.carrot.infra.entity.image",
        "jude.carrot.infra.entity.chat"
})
@EnableJpaRepositories(basePackages = {
        "jude.carrot.infra.repository.user",
        "jude.carrot.infra.repository.post",
        "jude.carrot.infra.repository.image",
        "jude.carrot.infra.repository.chat"
})
public class InfraTestConfig {
}
