package com.example.inventory.service;

import com.example.inventory.entity.Product;
import com.example.inventory.repository.ProductRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.Reader;

@Component
public class CsvLoader implements CommandLineRunner {

    @Autowired
    private ProductRepository productRepository;

    @Override
public void run(String... args) throws Exception {
    if (productRepository.count() == 0) { // Only load if DB is empty
        Reader in = new FileReader("src/main/resources/data/products.csv");
        
        CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(in);
                
            for (CSVRecord record : parser) {
                Product product = new Product();
                product.setId(Long.parseLong(record.get("id")));
                product.setName(record.get("name"));
                product.setPrice(Double.parseDouble(record.get("price")));
                product.setQuantity(Integer.parseInt(record.get("quantity")));
                productRepository.save(product);
            }
            System.out.println("Loaded " + productRepository.count() + " products from CSV.");
            } else {
                System.out.println("Database already has " + productRepository.count() + " products. Skipping CSV load.");
        }
    }
}