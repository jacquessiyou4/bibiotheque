package com.ibizabroker.bibliotheque.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springdoc.core.SwaggerUiConfigProperties;
import org.springdoc.core.SwaggerUiOAuthProperties;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.StreamUtils;
import org.springframework.web.servlet.resource.ResourceTransformerChain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests unitaires du transformer qui fait afficher l'API bibliothèque (et non
 * la démo Petstore) sur /swagger-ui/index.html. Aucun serveur : la page est
 * un fichier temporaire au même chemin relatif que dans le webjar swagger-ui.
 */
class SwaggerUiRedirectConfigurationTest {

    private static final String PETSTORE = "url: \"https://petstore.swagger.io/v2/swagger.json\"";

    @TempDir
    Path dossier;

    private SwaggerIndexTransformer transformer;
    private final MockHttpServletRequest requete = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
    private final ResourceTransformerChain chaine = mock(ResourceTransformerChain.class);

    @BeforeEach
    void setUp() {
        transformer = new SwaggerUiRedirectConfiguration().indexPageTransformer(
                new SwaggerUiConfigProperties(), new SwaggerUiOAuthProperties(), new ObjectMapper());
    }

    @Test
    void indexHtml_remplaceLaDemoPetstoreParLaConfigurationDeLApplication() throws IOException {
        Resource index = fichier("swagger-ui/4.1.2/index.html",
                "<script>const ui = SwaggerUIBundle({\n  " + PETSTORE + ",\n  dom_id: '#swagger-ui'\n});</script>");

        String html = contenu(transformer.transform(requete, index, chaine));

        assertThat(html).doesNotContain("petstore");
        assertThat(html).contains("configUrl: \"" + SwaggerUiRedirectConfiguration.CONFIG_URL + "\"");
        assertThat(html).contains("dom_id: '#swagger-ui'");
    }

    @Test
    void indexHtml_avecUrlVideeParSpringdoc_utiliseAussiLaConfigurationDeLApplication() throws IOException {
        Resource index = fichier("swagger-ui/4.1.2/index.html", "SwaggerUIBundle({ url: \"\", dom_id: '#swagger-ui' });");

        String html = contenu(transformer.transform(requete, index, chaine));

        assertThat(html).doesNotContain("url: \"\"");
        assertThat(html).contains("configUrl: \"/v3/api-docs/swagger-config\"");
    }

    @Test
    void autreFichierSwaggerUi_estRenvoyeSansModification() throws IOException {
        Resource css = fichier("swagger-ui/4.1.2/swagger-ui.css", "body { " + PETSTORE + " }");

        Resource resultat = transformer.transform(requete, css, chaine);

        assertThat(resultat).isSameAs(css);
    }

    @Test
    void indexHtmlHorsSwaggerUi_estRenvoyeSansModification() throws IOException {
        // L'index.html de l'application Angular ne doit jamais être réécrit.
        Resource indexAngular = fichier("static/index.html", "<app-root></app-root> " + PETSTORE);

        Resource resultat = transformer.transform(requete, indexAngular, chaine);

        assertThat(resultat).isSameAs(indexAngular);
    }

    private Resource fichier(String cheminRelatif, String texte) throws IOException {
        Path chemin = dossier.resolve(cheminRelatif);
        Files.createDirectories(chemin.getParent());
        Files.write(chemin, texte.getBytes(StandardCharsets.UTF_8));
        return new FileSystemResource(chemin.toFile());
    }

    private String contenu(Resource resource) throws IOException {
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }
}
