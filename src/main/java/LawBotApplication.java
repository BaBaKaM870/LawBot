import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"controller", "service", "model", "game"})
public class LawBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(LawBotApplication.class, args);
    }
}
