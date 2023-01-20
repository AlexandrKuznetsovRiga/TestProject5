package my.example;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
public class ApplicationMain {


    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ApplicationMain.class);
        application.setBannerMode(Banner.Mode.OFF);
        application.run(args);
    }


}