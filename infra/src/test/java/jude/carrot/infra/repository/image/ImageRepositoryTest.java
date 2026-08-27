package jude.carrot.infra.repository.image;

import jude.carrot.infra.InfraTestConfig;
import jude.carrot.infra.entity.image.MultipleImage;
import jude.carrot.infra.entity.image.SingleImage;
import jude.carrot.infra.repository.image.jpa.MultipleImageJpaRepository;
import jude.carrot.infra.repository.image.jpa.SingleImageJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static jude.carrot.infra.repository.image.ImageRepositoryTest.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create")
@ContextConfiguration(classes = InfraTestConfig.class)
@Testcontainers
@Transactional
@Import(ImageRepositoryConfig.class)
class ImageRepositoryTest {

    @TestConfiguration
    static class ImageRepositoryConfig{
        @Bean
        ImageRepository imageRepository(SingleImageJpaRepository singleImageJpaRepository,
                                        MultipleImageJpaRepository multipleImageJpaRepository){
            return new ImageRepositoryImpl(singleImageJpaRepository, multipleImageJpaRepository);
        }
    }

    static String DATABASE_NAME = "test";
    static String DATABASE_USERNAME = "test";
    static String DATABASE_PASSWORD = UUID.randomUUID().toString();
    static String TEST_URL = "test.org.com";

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName(DATABASE_NAME)
            .withUsername(DATABASE_USERNAME)
            .withPassword(DATABASE_PASSWORD);
    @Autowired
    ImageRepository imageRepository;

    @Test
    void saveSingleImage() {
        SingleImage singleImage = SingleImage.builder()
                .url(TEST_URL)
                .build();

        imageRepository.save(singleImage);
    }

    @Test
    void saveAllAssignsIdToEachMultipleImage() {
        List<MultipleImage> multipleImages = List.of(
                MultipleImage.from(TEST_URL + "/1"),
                MultipleImage.from(TEST_URL + "/2")
        );

        imageRepository.saveAll(multipleImages);

        assertThat(multipleImages).allSatisfy(image -> assertThat(image.getId()).isNotNull());
    }
}