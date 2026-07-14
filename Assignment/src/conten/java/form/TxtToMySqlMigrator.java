package conten.java.form;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * One-time helper: reads whatever is still in the old bencafe_orders.txt / bencafe_products.txt
 * files (from the previous txt-file version of the app) and copies it into MySQL via DBManager,
 * so nothing you already entered gets lost when switching over.
 *
 * How to run it once:
 *   1. Make sure bencafe_orders.txt / bencafe_products.txt are in the same folder you run the app from.
 *   2. Run this class's main() method directly (or call TxtToMySqlMigrator.run() once from anywhere).
 *   3. Check the console output, then you can delete the old .txt files.
 */
public class TxtToMySqlMigrator {

    private static final String ORDERS_FILE = "bencafe_orders.txt";
    private static final String PRODUCTS_FILE = "bencafe_products.txt";

    public static void main(String[] args) {
        run();
    }

    public static void run() {
    	Dabmager db = new Dabmager();
        if (!db.testConnection()) {
            System.out.println("Could not connect to MySQL - check DBManager's URL/username/password. Nothing was migrated.");
            return;
        }
        int products = migrateProducts(db);
        int orders = migrateOrders(db);
        System.out.println("Migration complete: " + products + " product(s) and " + orders + " order(s) copied into MySQL.");
    }

    private static int migrateProducts(Dabmager db) {
        File f = new File(PRODUCTS_FILE);
        if (!f.exists()) {
            System.out.println("No " + PRODUCTS_FILE + " found - skipping products.");
            return 0;
        }
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split("\\|", -1);
                if (p.length < 2) continue;
                try {
                    String name = p[0];
                    double price = Double.parseDouble(p[1]);
                    if (db.saveProduct(name, price)) count++;
                } catch (NumberFormatException ignored) {
                    // skip a malformed line rather than aborting the whole migration
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }

    private static int migrateOrders(Dabmager db) {
        File f = new File(ORDERS_FILE);
        if (!f.exists()) {
            System.out.println("No " + ORDERS_FILE + " found - skipping orders.");
            return 0;
        }
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (firstLine) {
                    firstLine = false;
                    continue; // that line was just the old txt-file order counter - MySQL auto-increments its own
                }
                // format: orderId|customer|product|qty|status|total|date|createdAt
                String[] p = line.split("\\|", -1);
                if (p.length < 8) continue;
                try {
                    String customer = p[1];
                    String product = p[2];
                    int qty = Integer.parseInt(p[3]);
                    String status = p[4];
                    double total = Double.parseDouble(p[5]);
                    LocalDateTime createdAt = LocalDateTime.parse(p[7]);
                    double pricePerUnit = qty > 0 ? total / qty : total;

                    int newId = db.insertOrder(customer, product, qty, status, pricePerUnit, total, createdAt);
                    if (newId > 0) count++;
                } catch (Exception ignored) {
                    // skip a malformed/corrupted line rather than aborting the whole migration
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }
}