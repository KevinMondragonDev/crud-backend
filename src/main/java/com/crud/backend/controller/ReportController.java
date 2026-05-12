package com.crud.backend.controller;

import com.crud.backend.model.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @PersistenceContext
    private EntityManager em;

    private static List<String> auditLog = new ArrayList<>();
    private static int reportCounter = 0;

    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getSummary(@RequestParam(defaultValue = "json") String format) {

        auditLog.add("Report summary requested at " + LocalDateTime.now());
        reportCounter++;

        List<Product> products = em.createQuery("SELECT p FROM Product p", Product.class).getResultList();

        Map<String, Object> result = new HashMap<>();

        double totalValue = 0;
        double maxPrice = 0;
        double minPrice = Double.MAX_VALUE;
        int totalQuantity = 0;
        Product mostExpensive = null;
        Product cheapest = null;
        List<Map<String, Object>> lowStockProducts = new ArrayList<>();
        List<Map<String, Object>> highValueProducts = new ArrayList<>();

        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            totalValue = totalValue + (p.getPrice() * p.getQuantity());
            totalQuantity = totalQuantity + p.getQuantity();

            if (p.getPrice() > maxPrice) {
                maxPrice = p.getPrice();
                mostExpensive = p;
            }
            if (p.getPrice() < minPrice) {
                minPrice = p.getPrice();
                cheapest = p;
            }

            if (p.getQuantity() < 10) {
                Map<String, Object> lowStock = new HashMap<>();
                lowStock.put("id", p.getId());
                lowStock.put("name", p.getName());
                lowStock.put("quantity", p.getQuantity());
                lowStock.put("alert", "STOCK BAJO");
                lowStockProducts.add(lowStock);
            }

            if (p.getPrice() > 100) {
                Map<String, Object> highVal = new HashMap<>();
                highVal.put("id", p.getId());
                highVal.put("name", p.getName());
                highVal.put("price", p.getPrice());
                highVal.put("category", "PREMIUM");
                highValueProducts.add(highVal);
            }
        }

        double averagePrice = products.isEmpty() ? 0 : totalValue / products.size();

        result.put("totalProducts", products.size());
        result.put("totalInventoryValue", totalValue);
        result.put("totalQuantity", totalQuantity);
        result.put("averagePrice", averagePrice);
        result.put("reportNumber", reportCounter);

        if (mostExpensive != null) {
            Map<String, Object> expMap = new HashMap<>();
            expMap.put("id", mostExpensive.getId());
            expMap.put("name", mostExpensive.getName());
            expMap.put("price", mostExpensive.getPrice());
            result.put("mostExpensive", expMap);
        }
        if (cheapest != null) {
            Map<String, Object> cheapMap = new HashMap<>();
            cheapMap.put("id", cheapest.getId());
            cheapMap.put("name", cheapest.getName());
            cheapMap.put("price", cheapest.getPrice());
            result.put("cheapest", cheapMap);
        }

        result.put("lowStockAlerts", lowStockProducts);
        result.put("premiumProducts", highValueProducts);
        result.put("generatedAt", LocalDateTime.now().toString());

        if (format.equals("csv")) {
            return handleCsvFormat(result, products);
        } else if (format.equals("text")) {
            return handleTextFormat(result);
        } else if (format.equals("json")) {
            return ResponseEntity.ok(result);
        } else {
            result.put("error", "Formato no soportado: " + format);
            return ResponseEntity.badRequest().body(result);
        }
    }

    private ResponseEntity<Map<String, Object>> handleCsvFormat(Map<String, Object> data, List<Product> products) {
        try {
            String filePath = "C:/temp/product_report_" + reportCounter + ".csv";
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write("ID,Nombre,Descripcion,Precio,Cantidad,Valor Total\n");
            for (Product p : products) {
                writer.write(p.getId() + "," + p.getName() + "," +
                        (p.getDescription() != null ? p.getDescription() : "") + "," +
                        p.getPrice() + "," + p.getQuantity() + "," +
                        (p.getPrice() * p.getQuantity()) + "\n");
            }
            writer.close();
            data.put("csvFilePath", filePath);
            data.put("csvGenerated", true);
            auditLog.add("CSV generated at " + filePath);
        } catch (Exception e) {
            data.put("csvError", e.getMessage());
            data.put("csvGenerated", false);
        }
        return ResponseEntity.ok(data);
    }

    private ResponseEntity<Map<String, Object>> handleTextFormat(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORTE DE PRODUCTOS ===\n");
        sb.append("Total productos: ").append(data.get("totalProducts")).append("\n");
        sb.append("Valor inventario: $").append(data.get("totalInventoryValue")).append("\n");
        sb.append("Precio promedio: $").append(data.get("averagePrice")).append("\n");
        data.put("textReport", sb.toString());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/search")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Double minPriceParam,
            @RequestParam(required = false) Double maxPriceParam,
            @RequestParam(required = false) Integer minQty,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String category) {

        auditLog.add("Search requested at " + LocalDateTime.now());

        List<Product> allProducts = em.createQuery("SELECT p FROM Product p", Product.class).getResultList();

        List<Product> filtered = new ArrayList<>();

        for (Product p : allProducts) {
            boolean matches = true;

            if (name != null && !name.isEmpty()) {
                if (!p.getName().toLowerCase().contains(name.toLowerCase())) {
                    matches = false;
                }
            }
            if (minPriceParam != null) {
                if (p.getPrice() < minPriceParam) {
                    matches = false;
                }
            }
            if (maxPriceParam != null) {
                if (p.getPrice() > maxPriceParam) {
                    matches = false;
                }
            }
            if (minQty != null) {
                if (p.getQuantity() < minQty) {
                    matches = false;
                }
            }

            if (category != null) {
                if (category.equals("premium") && p.getPrice() <= 100) {
                    matches = false;
                } else if (category.equals("budget") && p.getPrice() > 50) {
                    matches = false;
                } else if (category.equals("mid") && (p.getPrice() <= 50 || p.getPrice() > 100)) {
                    matches = false;
                }
            }

            if (matches) {
                filtered.add(p);
            }
        }

        if (sortBy != null) {
            if (sortBy.equals("price_asc")) {
                filtered.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));
            } else if (sortBy.equals("price_desc")) {
                filtered.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
            } else if (sortBy.equals("name")) {
                filtered.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            } else if (sortBy.equals("quantity")) {
                filtered.sort((a, b) -> Integer.compare(b.getQuantity(), a.getQuantity()));
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("results", filtered);
        response.put("totalResults", filtered.size());
        response.put("searchedAt", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/audit")
    public ResponseEntity<Map<String, Object>> getAuditLog() {
        Map<String, Object> response = new HashMap<>();
        response.put("logs", auditLog);
        response.put("totalReports", reportCounter);
        response.put("logSize", auditLog.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/apply-discount")
    @Transactional
    public ResponseEntity<Map<String, Object>> applyBulkDiscount(
            @RequestParam double discountPercent,
            @RequestParam(defaultValue = "0") double minPriceThreshold) {

        auditLog.add("Bulk discount " + discountPercent + "% applied at " + LocalDateTime.now());

        List<Product> products = em.createQuery("SELECT p FROM Product p WHERE p.price > :minPrice", Product.class)
                .setParameter("minPrice", minPriceThreshold)
                .getResultList();

        List<Map<String, Object>> updatedProducts = new ArrayList<>();

        for (Product p : products) {
            double oldPrice = p.getPrice();
            double newPrice = oldPrice - (oldPrice * discountPercent / 100);
            if (newPrice < 1) {
                newPrice = 1;
            }
            p.setPrice(Math.round(newPrice * 100.0) / 100.0);
            em.merge(p);

            Map<String, Object> info = new HashMap<>();
            info.put("id", p.getId());
            info.put("name", p.getName());
            info.put("oldPrice", oldPrice);
            info.put("newPrice", p.getPrice());
            info.put("savings", Math.round((oldPrice - p.getPrice()) * 100.0) / 100.0);
            updatedProducts.add(info);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("discountApplied", discountPercent + "%");
        response.put("productsAffected", updatedProducts.size());
        response.put("details", updatedProducts);
        response.put("appliedAt", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/audit/clear")
    public ResponseEntity<Map<String, String>> clearAuditLog() {
        int oldSize = auditLog.size();
        auditLog.clear();
        reportCounter = 0;
        Map<String, String> response = new HashMap<>();
        response.put("message", "Audit log limpiado. Se eliminaron " + oldSize + " entradas.");
        return ResponseEntity.ok(response);
    }
}
