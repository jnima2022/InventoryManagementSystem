package com.example.inventory.javafx.controller;

import com.example.inventory.entity.Product;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

public class MainController {

    @FXML private TableView<Product> productTable;
    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private TextField quantityField;
    @FXML private TextField searchField;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE_URL = "http://localhost:8081/api/products";

    @FXML
public void initialize() {
    System.out.println("Initializing UI...");
    // Delay loading until backend is ready
    new Thread(() -> {
        boolean serverReady = false;
        while (!serverReady) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    serverReady = true;
                }
            } catch (Exception e) {
                System.out.println("Waiting for backend... " + e.getMessage());
                try {
                    Thread.sleep(500); // Wait 0.5 seconds before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        // Once ready, load products on the JavaFX thread
        javafx.application.Platform.runLater(() -> loadProducts(""));
    }).start();

    productTable.getSelectionModel().selectedItemProperty().addListener((_, _, newSelection) -> {
        if (newSelection != null) {
            nameField.setText(newSelection.getName());
            priceField.setText(newSelection.getPrice().toString());
            quantityField.setText(newSelection.getQuantity().toString());
        }
    });
}

    private void loadProducts(String searchTerm) {
        try {
            String url = searchTerm.isEmpty() ? BASE_URL : BASE_URL + "/search?term=" + java.net.URLEncoder.encode(searchTerm, "UTF-8");
            System.out.println("Loading products from: " + url);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response status: " + response.statusCode());
            System.out.println("Response body: " + response.body());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Unexpected status code: " + response.statusCode());
            }
            List<Product> products = objectMapper.readValue(response.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, Product.class));
            productTable.setItems(FXCollections.observableArrayList(products));
            System.out.println("Loaded " + products.size() + " products.");
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
            showAlert("Error", "Failed to load products: " + errorMessage);
            e.printStackTrace(); // Print stack trace to console for debugging
        }
    }

    @FXML
    private void searchProducts() {
        String searchTerm = searchField.getText().trim();
        loadProducts(searchTerm);
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
        loadProducts("");
    }

    @FXML
    private void addProduct() {
        if (!isValidInput()) {
            return;
        }
        try {
            Product product = new Product();
            product.setName(nameField.getText());
            product.setPrice(Double.parseDouble(priceField.getText()));
            product.setQuantity(Integer.parseInt(quantityField.getText()));

            String json = objectMapper.writeValueAsString(product);
            System.out.println("Adding Product: " + json);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Add Response: " + response.statusCode() + " - " + response.body());
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                loadProducts(searchField.getText().trim());
                clearFields();
            } else {
                showAlert("Error", "Failed to add product. Status: " + response.statusCode());
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to add product: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void updateProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select a product to update.");
            return;
        }
        if (!isValidInput()) {
            return;
        }
        try {
            selected.setName(nameField.getText());
            selected.setPrice(Double.parseDouble(priceField.getText()));
            selected.setQuantity(Integer.parseInt(quantityField.getText()));

            String json = objectMapper.writeValueAsString(selected);
            System.out.println("Updating Product: " + json);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/" + selected.getId()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Update Response: " + response.statusCode() + " - " + response.body());
            if (response.statusCode() == 200) {
                loadProducts(searchField.getText().trim());
                clearFields();
            } else {
                showAlert("Error", "Failed to update product. Status: " + response.statusCode());
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to update product: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void deleteProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select a product to delete.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + selected.getName() + "?");
        if (confirmation.showAndWait().get() == ButtonType.OK) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/" + selected.getId()))
                    .DELETE()
                    .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 204) {
                    loadProducts(searchField.getText().trim());
                    clearFields();
                } else {
                    showAlert("Error", "Failed to delete product. Status: " + response.statusCode());
                }
            } catch (Exception e) {
                showAlert("Error", "Failed to delete product: " + e.getMessage());
            }
        }
    }

    private void clearFields() {
        nameField.clear();
        priceField.clear();
        quantityField.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean isValidInput() {
        if (nameField.getText().isEmpty() || priceField.getText().isEmpty() || quantityField.getText().isEmpty()) {
            showAlert("Warning", "All fields are required.");
            return false;
        }
        try {
            double price = Double.parseDouble(priceField.getText());
            int quantity = Integer.parseInt(quantityField.getText());
            if (price < 0 || quantity < 0) {
                showAlert("Warning", "Price and quantity must be positive.");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Warning", "Price and quantity must be numeric.");
            return false;
        }
        return true;
    }
}