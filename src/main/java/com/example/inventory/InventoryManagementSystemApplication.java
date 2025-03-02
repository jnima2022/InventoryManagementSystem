package com.example.inventory;

import com.example.inventory.javafx.MainApp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class InventoryManagementSystemApplication {
    public static void main(String[] args) {
        // Start Spring Boot in a separate thread
        new Thread(() -> SpringApplication.run(InventoryManagementSystemApplication.class, args)).start();
        // Launch JavaFX
        MainApp.main(args);
    }
}