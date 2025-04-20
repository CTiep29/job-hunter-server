package vn.ctiep.jobhunter.config;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ConfigCloudinary {

    @Bean
    public Cloudinary ConfigKey(){
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dke0iesix");
        config.put("api_key", "263363882537523");
        config.put("api_secret", "6aj0Vrf7oJod04zzX5G5BeE4mhs");
        return new Cloudinary(config);
    }
}
