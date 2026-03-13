package movieapp;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

//disable security
//@SpringBootApplication(exclude = {
//        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
//        org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration.class
//})
@SpringBootApplication
@EnableScheduling
@EnableRetry
public class MovieApplication {
    @PostConstruct
    void setTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }
    
    public static void main(String[] args) {
        SpringApplication.run(MovieApplication.class, args);

        System.out.println("\n🎬 ========================================");
        System.out.println("🎬 Movie Backend Started Successfully!");
        System.out.println("🎬 ========================================");

        // Thêm endpoint mới
        System.out.println("📍 Homepage API (NEW):");
        System.out.println("   - Grouped: GET http://localhost:8080/api/v1/homepage/{initial|group1|group2}");

        System.out.println("");
        System.out.println("⏰ Cache Strategy: GROUPED REFRESH");
        System.out.println("   - Warm-up: After 10 seconds");
        System.out.println("   - Minute 54: Initial group (sections 1-4)");
        System.out.println("   - Minute 55: Group 1 (sections 5-8)");
        System.out.println("   - Minute 56: Group 2 (sections 9-12)");
        System.out.println("   - Cache TTL: 60 minutes");
        System.out.println("========================================\n");
    }
}
