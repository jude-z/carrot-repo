package jude.carrot.infra.repository.image;

import jude.carrot.infra.InfraTestConfig;
import jude.carrot.infra.entity.image.ThumbnailImage;
import jude.carrot.infra.fixture.image.ImageFactory;
import jude.carrot.infra.repository.image.jpa.ThumbnailImageJpaRepository;
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

import java.util.UUID;

import static jude.carrot.infra.repository.image.ImageRepositoryTest.*;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create")
@ContextConfiguration(classes = InfraTestConfig.class)
@Testcontainers
@Transactional
@Import(ImageRepositoryConfig.class)
class ImageRepositoryTest {

    @TestConfiguration
    static class ImageRepositoryConfig{
        @Bean
        ImageRepository imageRepository(ThumbnailImageJpaRepository thumbnailImageJpaRepository){
            return new ImageRepositoryImpl(thumbnailImageJpaRepository);
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
    void saveThumbnailImage() {
        ThumbnailImage thumbnailImage = ImageFactory.create(TEST_URL);
        imageRepository.save(thumbnailImage);
    }
}