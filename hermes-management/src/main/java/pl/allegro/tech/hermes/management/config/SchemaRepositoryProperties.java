package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.config.Configs;

@ConfigurationProperties(prefix = "schema.repository")
public class SchemaRepositoryProperties {

    private String type = Configs.SCHEMA_REPOSITORY_TYPE.getDefaultValue();

    private String serverUrl = Configs.SCHEMA_REPOSITORY_SERVER_URL.getDefaultValue();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
}
