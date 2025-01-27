package CafeteriaManagementSystem;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class CafeteriaManagementSystem extends JFrame {

    public CafeteriaManagementSystem() {
        setTitle("KCAU Cafeteria Management System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize Firebase
        initializeFirebase();

        // Set the initial screen to the login screen
        setContentPane(new LoginScreen(this));
    }

    private void initializeFirebase() {
        try {
            FileInputStream serviceAccount = new FileInputStream("D:\\morning1\\wuzu\\src\\main\\resources\\mautamu-bd205-firebase-adminsdk-u182e-c59475afd1.json");

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://mautamu-bd205-default-rtdb.firebaseio.com/")
                    .build();

            FirebaseApp.initializeApp(options);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CafeteriaManagementSystem app = new CafeteriaManagementSystem();
            app.setVisible(true);
        });
    }
}

class LoginScreen extends JPanel {
    public LoginScreen(CafeteriaManagementSystem mainFrame) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel usernameLabel = new JLabel("Username:");
        JTextField usernameField = new JTextField(20);
        JLabel passwordLabel = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField(20);
        JButton loginButton = new JButton("Log In");
        JButton signupButton = new JButton("Sign Up");
        

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(usernameLabel, gbc);
        gbc.gridx = 1;
        add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        add(passwordLabel, gbc);
        gbc.gridx = 1;
        add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        add(loginButton, gbc);
        gbc.gridx = 1;
        add(signupButton, gbc);

        

        loginButton.addActionListener(e -> {
            try {
                String email = usernameField.getText();
                String password = new String(passwordField.getPassword());
                // Attempt to sign in
                UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(email);
                if (userRecord != null) {
                    // Handle successful login logic here
                    mainFrame.setContentPane(new MainDashboard(mainFrame));
                    mainFrame.revalidate();
                    mainFrame.repaint();
                }
            } catch (FirebaseAuthException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Login failed: " + ex.getMessage());
            }
        });

        signupButton.addActionListener(e -> {
            String email = usernameField.getText();
            String password = new String(passwordField.getPassword());
            // Handle signup logic
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password);
            try {
                UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);
                JOptionPane.showMessageDialog(this, "User created successfully: " + userRecord.getUid());
            } catch (FirebaseAuthException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Sign Up failed: " + ex.getMessage());
            }
        });
    }
}


class MainDashboard extends JPanel {
    public MainDashboard(CafeteriaManagementSystem mainFrame) {
        setLayout(new GridLayout(6, 1));

        JButton orderManagementButton = new JButton("Order Management");
        JButton inventoryManagementButton = new JButton("Inventory Management");
        JButton billingButton = new JButton("Billing");
        JButton reportButton = new JButton("Reports");
        JButton backButton = new JButton("Back");


        OrderManagementScreen orderScreen = new OrderManagementScreen(mainFrame);

        orderManagementButton.addActionListener(e -> {
            mainFrame.setContentPane(new OrderManagementScreen(mainFrame));
            mainFrame.revalidate();
            mainFrame.repaint();
        });

        inventoryManagementButton.addActionListener(e -> {
            mainFrame.setContentPane(new InventoryManagementScreen(mainFrame));
            mainFrame.revalidate();
            mainFrame.repaint();
        });
        billingButton.addActionListener(e -> {
            int totalCost = orderScreen.getTotalCost();
            mainFrame.setContentPane(new BillingScreen(mainFrame, totalCost));
            mainFrame.revalidate();
            mainFrame.repaint();
        });
        

       

        reportButton.addActionListener(e -> {
            mainFrame.setContentPane(new ReportManagementScreen(mainFrame));
            mainFrame.revalidate();
            mainFrame.repaint();
        });

        backButton.addActionListener(e -> {
            mainFrame.setContentPane(new LoginScreen(mainFrame));
            mainFrame.revalidate();
            mainFrame.repaint();
        });

        add(orderManagementButton);
        add(inventoryManagementButton);
        add(billingButton);
        add(reportButton);
        add(backButton);
    }
}


class OrderManagementScreen extends JPanel {
    private int totalCost; // Store the total cost
    private CafeteriaManagementSystem mainFrame;
    private DefaultListModel<String> menuListModel;
    private javax.swing.JList<String> menuList; // Explicit use of javax.swing.JList
    private DefaultListModel<String> orderListModel;
    private javax.swing.JList<String> orderList; // Explicit use of javax.swing.JList
    private JTextField quantityField;
    private JButton addToOrderButton;
    private JButton viewOrderButton;
    private JButton finalizeOrderButton;
    private JButton backButton;
    private JLabel totalCostLabel;
    private FirebaseDatabase database;
    private DatabaseReference ordersRef;

    public OrderManagementScreen(CafeteriaManagementSystem mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Order Management", SwingConstants.CENTER);

         // Initialize total cost label
    totalCostLabel = new JLabel("Total Cost: $0", SwingConstants.RIGHT);

        // Create menu list model and list
        menuListModel = new DefaultListModel<>();
        menuListModel.addElement("Coffee - $2");
        menuListModel.addElement("Sandwich - $5");
        menuListModel.addElement("Salad - $4");
        menuList = new javax.swing.JList<>(menuListModel);

        // Create order list model and list
        orderListModel = new DefaultListModel<>();
        orderList = new javax.swing.JList<>(orderListModel);

        // Create quantity field and buttons
        quantityField = new JTextField(5);
        addToOrderButton = new JButton("Add to Order");
        viewOrderButton = new JButton("View Order Summary");
        finalizeOrderButton = new JButton("Finalize Order");
        backButton = new JButton("Back");

        // Add components to the panel
        add(title, BorderLayout.NORTH);
        add(new JScrollPane(menuList), BorderLayout.WEST);
        add(new JScrollPane(orderList), BorderLayout.EAST);
        
            // Panel for buttons and total cost
    JPanel buttonPanel = new JPanel(new BorderLayout());
    JPanel controlsPanel = new JPanel(); // For buttons
    controlsPanel.add(quantityField);
    controlsPanel.add(addToOrderButton);
    controlsPanel.add(viewOrderButton);
    controlsPanel.add(finalizeOrderButton);
    controlsPanel.add(backButton);

    // Add total cost label to the bottom-right of the button panel
    buttonPanel.add(controlsPanel, BorderLayout.CENTER);
    buttonPanel.add(totalCostLabel, BorderLayout.SOUTH);

    add(buttonPanel, BorderLayout.SOUTH);

    // Initialize Firebase database connection
        database = FirebaseDatabase.getInstance();
        ordersRef = database.getReference("orders");

    // Add action listeners
        addToOrderButton.addActionListener(e -> {
            int selectedIndex = menuList.getSelectedIndex();
            if (selectedIndex != -1) {
                String selectedItem = menuListModel.get(selectedIndex);
                int quantity;
                try {
                    quantity = Integer.parseInt(quantityField.getText());
                    String orderItem = selectedItem + " x " + quantity;
                    orderListModel.addElement(orderItem);
                    updateOrderSummary();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid quantity.");
                }
            }
        });

        viewOrderButton.addActionListener(e -> {
            if (orderListModel.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Your order is empty. Please add items to view the summary.");
                return;
            }
        
            StringBuilder orderSummary = new StringBuilder("Order Summary:\n");
            for (int i = 0; i < orderListModel.size(); i++) {
                orderSummary.append(orderListModel.get(i)).append("\n");
            }
            orderSummary.append("\nTotal Cost: $").append(totalCost);
        
            JOptionPane.showMessageDialog(this,
                    orderSummary.toString(),
                    "Order Summary",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        finalizeOrderButton.addActionListener(e -> {
            if (orderListModel.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Your order is empty. Please add items to finalize.");
                return;
            }

            StringBuilder orderDetails = new StringBuilder("Order Summary:\n");
            for (int i = 0; i < orderListModel.size(); i++) {
                orderDetails.append(orderListModel.get(i)).append("\n");
            }
            

            int choice = JOptionPane.showConfirmDialog(this,
                 "Order Summary:\n" + orderDetails + "\nTotal Cost: $" + totalCost + "\n\nDo you want to finalize this order?",
            "Confirm Order",
            JOptionPane.YES_NO_OPTION);   

            if (choice == JOptionPane.YES_OPTION) {
                storeOrderInFirebase();
                JOptionPane.showMessageDialog(this, "Order finalized successfully!");
             
                 // Transition to the BillingScreen with order details
                BillingScreen billingScreen = new BillingScreen(mainFrame, totalCost, orderDetails.toString());
                mainFrame.setContentPane(billingScreen);
                mainFrame.revalidate();
                mainFrame.repaint();
            }
        });

        backButton.addActionListener(e -> {
            mainFrame.setContentPane(new MainDashboard(mainFrame));
            mainFrame.revalidate();
            mainFrame.repaint();
        });
    }

    private void updateOrderSummary() {
        int oldTotal = totalCost; // Store the previous total for comparison
        totalCost = 0;

        for (int i = 0; i < orderListModel.size(); i++) {
            try {
                OrderItem orderItem = parseOrderItem(orderListModel.get(i));
                totalCost += orderItem.getPrice() * orderItem.getQuantity();
            } catch (IllegalArgumentException ex) {
                System.err.println("Error parsing order item: " + ex.getMessage());
            }
        }

        if (totalCost != oldTotal) {
            highlightTotalCostChange();
        }

        totalCostLabel.setText("Total Cost: $" + totalCost); // Update the GUI
        System.out.println("Total cost: $" + totalCost);
    }

    private void highlightTotalCostChange() {
        totalCostLabel.setForeground(Color.RED); // Temporarily change to red

        // Use a Timer to revert back to default color after a delay
        Timer timer = new Timer(500, e -> totalCostLabel.setForeground(Color.BLACK));
        timer.setRepeats(false); // Ensure the timer runs only once
        timer.start();
    }

    private void resetTotalCost() {
        Timer timer = new Timer(50, null);
        final int[] animatedCost = {totalCost};
        timer.addActionListener(e -> {
            if (animatedCost[0] > 0) {
                animatedCost[0] -= Math.min(10, animatedCost[0]);
                totalCostLabel.setText("Total Cost: $" + animatedCost[0]);
            } else {
                timer.stop();
                totalCostLabel.setForeground(Color.BLACK);
            }
        });

        totalCostLabel.setForeground(Color.BLUE);
        timer.start();
        totalCost = 0;
    }

    
    // Utility to parse order items from string representation
    private OrderItem parseOrderItem(String orderItemStr) {
        String[] parts = orderItemStr.split(" x ");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid order item format: " + orderItemStr);
        }
        String[] nameAndPrice = parts[0].split(" - ");
        if (nameAndPrice.length != 2) {
            throw new IllegalArgumentException("Invalid menu item format: " + parts[0]);
        }
        try {
            int price = Integer.parseInt(nameAndPrice[1].substring(1)); // Exclude the '$'
            int quantity = Integer.parseInt(parts[1]);
            return new OrderItem(nameAndPrice[0], price, quantity);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Price or quantity is not a valid number.");
        }
    }
    
    // New OrderItem class for structured data representation
    private class OrderItem {
        private final String name;
        private final int price;
        private final int quantity;
    
        public OrderItem(String name, int price, int quantity) {
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }
    
        public String getName() {
            return name;
        }
    
        public int getPrice() {
            return price;
        }
    
        public int getQuantity() {
            return quantity;
        }
    }
    

    private void storeOrderInFirebase() {
        String orderId = ordersRef.push().getKey();
        Order order = new Order(orderId, orderListModel);
        ordersRef.child(orderId).setValue(order, (databaseError, databaseReference) -> {
            if (databaseError != null) {
                System.out.println("Order could not be saved: " + databaseError.getMessage());
            } else {
                System.out.println("Order stored in Firebase database with ID: " + orderId);
            }
        });
    }

    public int getTotalCost() {
        return totalCost;
    }
}

    class Order {
        private String orderId;
        private java.util.List<String> orderItems; // Explicit use of java.util.List

        public Order(String orderId, DefaultListModel<String> orderListModel) {
            this.orderId = orderId;
            this.orderItems = new java.util.ArrayList<>(); // Explicit use of java.util.ArrayList
            for (int i = 0; i < orderListModel.size(); i++) {
                orderItems.add(orderListModel.get(i));
            }
        }

        public String getOrderId() {
            return orderId;
        }

        public java.util.List<String> getOrderItems() {
            return orderItems;
        }
    }

    

class InventoryManagementScreen extends JPanel {
    private DatabaseReference inventoryRef;
    private DefaultListModel<String> inventoryListModel;
    private JList<String> inventoryList;
    private JTextField itemField;
    private JTextField quantityField;
    private JButton updateStockButton;
    private JButton reorderButton;
    private JButton backButton;
    private JButton deleteButton;

    public InventoryManagementScreen(CafeteriaManagementSystem mainFrame) {
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Inventory Management", SwingConstants.CENTER);
        inventoryListModel = new DefaultListModel<>();
        inventoryList = new JList<>(inventoryListModel);

        itemField = new JTextField(15);
        quantityField = new JTextField(5);
        updateStockButton = new JButton("Update Stock");
        reorderButton = new JButton("Reorder Item");
        deleteButton = new JButton("Delete Item");
        backButton = new JButton("Back");

         // Attach a DocumentFilter to restrict input to integers
        ((AbstractDocument) quantityField.getDocument()).setDocumentFilter(new IntegerOnlyFilter());

        // Initialize Firebase database reference
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        inventoryRef = database.getReference("inventory");

        // Load initial inventory data from Firebase
        loadInventoryData();

        // Add components to the panel
        add(title, BorderLayout.NORTH);
        add(new JScrollPane(inventoryList), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(new JLabel("Item:"));
        buttonPanel.add(itemField);
        buttonPanel.add(new JLabel("Quantity:"));
        buttonPanel.add(quantityField);
        buttonPanel.add(updateStockButton);
        buttonPanel.add(reorderButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(backButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Add action listeners
        updateStockButton.addActionListener(e -> updateStock());
        reorderButton.addActionListener(e -> reorderItem());
        deleteButton.addActionListener(e -> deleteItem());
        backButton.addActionListener(e -> {
            mainFrame.setContentPane(new MainDashboard(mainFrame));
            mainFrame.revalidate();
            mainFrame.repaint();
        });
    }
    // Custom DocumentFilter for integer-only input
class IntegerOnlyFilter extends DocumentFilter {
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        if (string != null && string.matches("\\d*")) { // Allow only digits
            super.insertString(fb, offset, string, attr);
        } else{
            showErrorMessage();
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        if (text != null && text.matches("\\d*")) { // Allow only digits
            super.replace(fb, offset, length, text, attrs);
        }else{
            showErrorMessage();
        }
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        super.remove(fb, offset, length); // Allow normal deletion
    }
    private void showErrorMessage() {
        JOptionPane.showMessageDialog(null, 
            "Please enter numeric values only for the quantity.", 
            "Invalid Input", 
            JOptionPane.ERROR_MESSAGE);
    }
}
    
    private void deleteItem() {
        int selectedIndex = inventoryList.getSelectedIndex(); // Get selected index
        if (selectedIndex == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to delete.");
            return;
        }
    
        // Get the selected item and parse its name
        String selectedItem = inventoryListModel.get(selectedIndex);
        String itemName = selectedItem.split(" - ")[0].trim();
    
        // Remove from Firebase database
        inventoryRef.child(itemName).removeValue((error, ref) -> {
            if (error != null) {
                JOptionPane.showMessageDialog(this, "Failed to delete item: " + error.getMessage());
            } else {
                // Remove from the list model
                inventoryListModel.remove(selectedIndex);
                JOptionPane.showMessageDialog(this, "Item deleted successfully!");
            }
        });
    }

    private void loadInventoryData() {
        inventoryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                inventoryListModel.clear();//clear existing data
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    String itemName = itemSnapshot.getKey();
                    Long quantity = itemSnapshot.getValue(Long.class);
                    if (quantity != null) {
                    inventoryListModel.addElement(itemName + " - " + quantity + " units");
                    }
                }


            }

            @Override
            public void onCancelled(DatabaseError error) {
                JOptionPane.showMessageDialog(InventoryManagementScreen.this,
                        "Failed to load inventory data: " + error.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void updateStock() {
        String itemName = itemField.getText().trim();
        String quantityStr = quantityField.getText().trim();
        if (itemName.isEmpty() || quantityStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in both fields.");
            return;
        }
        try {
            int quantity = Integer.parseInt(quantityStr);
            inventoryRef.child(itemName).setValue(quantity, (error, ref) -> {
                if (error != null) {
                    JOptionPane.showMessageDialog(this, "Failed to update stock: " + error.getMessage());
                } else {
                    JOptionPane.showMessageDialog(this, "Stock updated successfully!");
                    itemField.setText("");
                    quantityField.setText("");
                }
            });
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Quantity must be a valid number.");
        }
    }

    private void reorderItem() {
        String itemName = itemField.getText().trim();
        String reorderQuantityStr = quantityField.getText().trim();
        if (itemName.isEmpty() || reorderQuantityStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in both fields.");
            return;
        }
        try {
            int reorderQuantity = Integer.parseInt(reorderQuantityStr);
            inventoryRef.child(itemName).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    long currentQuantity = snapshot.exists() ? snapshot.getValue(Long.class) : 0;
                    long newQuantity = currentQuantity + reorderQuantity;
                    inventoryRef.child(itemName).setValue(newQuantity, (error, ref) -> {
                        if (error != null) {
                            JOptionPane.showMessageDialog(InventoryManagementScreen.this,
                                    "Failed to reorder item: " + error.getMessage());
                        } else {
                            JOptionPane.showMessageDialog(InventoryManagementScreen.this,
                                    "Reorder successful! New quantity: " + newQuantity);
                            itemField.setText("");
                            quantityField.setText("");
                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    JOptionPane.showMessageDialog(InventoryManagementScreen.this,
                            "Failed to reorder item: " + error.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Reorder quantity must be a valid number.");
        }
    }
}

class BillingScreen extends JPanel {
    private FirebaseDatabase database;
    private DatabaseReference billingRef;
    private JTextArea orderSummary;
    private JComboBox<String> paymentMethodDropdown;
    private JTextField cashReceivedField;
    private JLabel changeLabel;

     // Track the cash received and change
     private double cashReceived = 0.0;
     private double change = 0.0;
     public BillingScreen(CafeteriaManagementSystem mainFrame, int totalCost, String orderDetails) {
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Billing and Payment", SwingConstants.CENTER);

        // Order Summary
        orderSummary = new JTextArea("Order Summary:\n" + orderDetails + "\nTotal: $" + totalCost);
        orderSummary.setEditable(false);

        // Payment Method Dropdown
        paymentMethodDropdown = new JComboBox<>(new String[]{"Cash", "Credit/Debit Card", "Mobile Payment"});
        JLabel paymentMethodLabel = new JLabel("Payment Method:");

        // Cash Received Field and Change Label
        cashReceivedField = new JTextField(10);
        JLabel cashReceivedLabel = new JLabel("Cash Received:");
        changeLabel = new JLabel("Change: $0.00");

        // Buttons
        JButton finalizeOrderButton = new JButton("Finalize Payment");
        JButton receiptButton = new JButton("Generate Receipt");
        JButton backButton = new JButton("Back");

        // Firebase reference for storing billing information
        database = FirebaseDatabase.getInstance();
        billingRef = database.getReference("billing");

        // Layout and Components
        add(title, BorderLayout.NORTH);
        add(new JScrollPane(orderSummary), BorderLayout.CENTER);

        JPanel paymentPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        paymentPanel.add(paymentMethodLabel);
        paymentPanel.add(paymentMethodDropdown);
        paymentPanel.add(cashReceivedLabel);
        paymentPanel.add(cashReceivedField);
        paymentPanel.add(new JLabel());
        paymentPanel.add(changeLabel);

        add(paymentPanel, BorderLayout.EAST);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(finalizeOrderButton);
        buttonPanel.add(receiptButton);
        buttonPanel.add(backButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Action Listeners
        cashReceivedField.addActionListener(e -> calculateChange(totalCost));
        finalizeOrderButton.addActionListener(e -> finalizePayment(totalCost, mainFrame));
        receiptButton.addActionListener(e -> generateReceipt(orderDetails, totalCost));
        backButton.addActionListener(e -> {
            mainFrame.setContentPane(new MainDashboard(mainFrame));
            mainFrame.revalidate();
            mainFrame.repaint();
        });
    }

    private void calculateChange(int totalCost) {
        try {
            // Save cash received
            cashReceived = Double.parseDouble(cashReceivedField.getText());

            // Calculate change
            if (cashReceived >= totalCost) {
                change = cashReceived - totalCost;
                changeLabel.setText("Change: $" + String.format("%.2f", change));
            } else {
                changeLabel.setText("Insufficient cash received.");
            }
        } catch (NumberFormatException e) {
            changeLabel.setText("Enter a valid number.");
        }
    }

    private void finalizePayment(int totalCost, CafeteriaManagementSystem mainFrame) {
        String paymentMethod = (String) paymentMethodDropdown.getSelectedItem();
    
        // Handle "Cash" payment method
        if ("Cash".equals(paymentMethod)) {
            try {
                // Ensure the cashReceived value is updated from the field
                double cashReceived = Double.parseDouble(cashReceivedField.getText());
    
                // Validate if the entered cash is sufficient
                if (cashReceived < totalCost) {
                    JOptionPane.showMessageDialog(this, "Insufficient cash received. Please enter the correct amount.",
                            "Payment Error", JOptionPane.ERROR_MESSAGE);
                    return; // Exit the method if cash is insufficient
                } else {
                    // If cash is sufficient, calculate the change
                    double change = cashReceived - totalCost;
                    changeLabel.setText("Change: $" + String.format("%.2f", change));
    
                    // Proceed to save the data and finalize the payment
                    saveBillingData(paymentMethod, cashReceived, change, totalCost, mainFrame);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid amount for cash received.",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void saveBillingData(String paymentMethod, double cashReceived, double change, int totalCost, CafeteriaManagementSystem mainFrame) {
        String billingId = billingRef.push().getKey();
    
        HashMap<String, Object> billingData = new HashMap<>();
        billingData.put("totalCost", totalCost);
        billingData.put("paymentMethod", paymentMethod);
        billingData.put("cashReceived", cashReceived);
        billingData.put("change", change); // Store the change value
        billingData.put("timestamp", System.currentTimeMillis());
    
        billingRef.child(billingId).setValue(billingData, (error, ref) -> {
            if (error != null) {
                JOptionPane.showMessageDialog(this, "Failed to store billing info: " + error.getMessage());
            } else {
                JOptionPane.showMessageDialog(this, "Payment finalized successfully!");
                mainFrame.setContentPane(new MainDashboard(mainFrame));
                mainFrame.revalidate();
                mainFrame.repaint();
            }
        });
    }
    

    private void generateReceipt(String orderDetails, int totalCost) {
         // Recalculate change in case it's not done when Generate Receipt is pressed
        calculateChange(totalCost);
        // Generate the receipt including cash received and change
        String receipt = "Receipt\n----------\n" + orderDetails + "\nTotal: $" + totalCost
                + "\nCash Received: $" + cashReceived
                + "\nChange: $" + change + "\n\nThank you for your purchase!";
        JOptionPane.showMessageDialog(this, receipt, "Receipt", JOptionPane.INFORMATION_MESSAGE);
    }

    // Overloaded constructor for when order details aren't provided
    public BillingScreen(CafeteriaManagementSystem mainFrame, int totalCost) {
        this(mainFrame, totalCost, "Order details not provided.");
    }
}


class ReportManagementScreen extends JPanel {
    private DatabaseReference ordersRef;
    private DatabaseReference inventoryRef;
    private JList<String> reportList;
    private DefaultListModel<String> listModel = new DefaultListModel<>();
    private JButton dailyReportButton;
    private JButton weeklyReportButton;
    private JButton monthlyReportButton;
    private JButton salesReportButton;
    private JButton inventoryReportButton;
    private JButton profitLossReportButton;

    public ReportManagementScreen(CafeteriaManagementSystem mainFrame) {
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Report Management", SwingConstants.CENTER);
        dailyReportButton = new JButton("Daily Report");
        weeklyReportButton = new JButton("Weekly Report");
        monthlyReportButton = new JButton("Monthly Report");
        salesReportButton = new JButton("Sales Reports");
        inventoryReportButton = new JButton("Inventory Reports");
        profitLossReportButton = new JButton("Profit/Loss Reports");
        JButton backButton = new JButton("Back");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(dailyReportButton);
        buttonPanel.add(weeklyReportButton);
        buttonPanel.add(monthlyReportButton);
        buttonPanel.add(salesReportButton);
        buttonPanel.add(inventoryReportButton);
        buttonPanel.add(profitLossReportButton);
        buttonPanel.add(backButton);

        add(title, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);

        // Firebase Initialization
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        ordersRef = database.getReference("orders");
        inventoryRef = database.getReference("inventory");

        reportList = new JList<>(listModel);
        JScrollPane listScrollPane = new JScrollPane(reportList);
        add(listScrollPane, BorderLayout.SOUTH);

        // Add action listeners for the buttons
        dailyReportButton.addActionListener(e -> generateReport("daily"));
        weeklyReportButton.addActionListener(e -> generateReport("weekly"));
        monthlyReportButton.addActionListener(e -> generateReport("monthly"));
        salesReportButton.addActionListener(e -> generateSalesReport());
        inventoryReportButton.addActionListener(e -> generateInventoryReport());
        profitLossReportButton.addActionListener(e -> generateProfitLossReport());

        backButton.addActionListener(e -> {
            mainFrame.setContentPane(new MainDashboard(mainFrame));
            mainFrame.revalidate();
            mainFrame.repaint();
        });
    }

    // Generate basic reports (Daily, Weekly, Monthly)
    private void generateReport(String reportType) {
        listModel.clear();
        switch (reportType) {
            case "daily":
                listModel.addElement("Daily report generated.");
                break;
            case "weekly":
                listModel.addElement("Weekly report generated.");
                break;
            case "monthly":
                listModel.addElement("Monthly report generated.");
                break;
            default:
                listModel.addElement("Invalid report type.");
                break;
        }
        reportList.setModel(listModel);
    }

    // Generate sales report
    private void generateSalesReport() {
        listModel.clear();
        ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    double totalSales = 0;
                    for (DataSnapshot order : snapshot.getChildren()) {
                        Map<String, Object> orderData = (Map<String, Object>) order.getValue();
                        double orderAmount = Double.parseDouble(orderData.get("total").toString());
                        totalSales += orderAmount;
                    }
                    listModel.addElement("Sales Report:");
                    listModel.addElement("Total Sales: $" + totalSales);
                } else {
                    listModel.addElement("No sales data available.");
                }
                reportList.setModel(listModel);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                listModel.addElement("Error generating sales report: " + error.getMessage());
                reportList.setModel(listModel);
            }
        });
    }

    // Generate inventory report
    private void generateInventoryReport() {
        listModel.clear();
        inventoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    listModel.addElement("Inventory Report:");
                    for (DataSnapshot item : snapshot.getChildren()) {
                        String itemName = item.getKey();
                        long quantity = (long) item.getValue();
                        if (quantity < 10) { // Reorder threshold
                            listModel.addElement(itemName + ": " + quantity + " units (Reorder Required)");
                        } else {
                            listModel.addElement(itemName + ": " + quantity + " units");
                        }
                    }
                } else {
                    listModel.addElement("No inventory data available.");
                }
                reportList.setModel(listModel);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                listModel.addElement("Error generating inventory report: " + error.getMessage());
                reportList.setModel(listModel);
            }
        });
    }

    private void generateProfitLossReport() {
        listModel.clear();
    
        // Use a mutable container for totalRevenue
        final double[] totalRevenue = {0}; // Mutable container
        final Map<String, Double> inventoryCosts = new HashMap<>();
    
        ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot ordersSnapshot) {
                if (ordersSnapshot.exists()) {
                    // Calculate revenue from sales
                    for (DataSnapshot order : ordersSnapshot.getChildren()) {
                        Map<String, Object> orderData = (Map<String, Object>) order.getValue();
                        double orderAmount = Double.parseDouble(orderData.get("total").toString());
                        totalRevenue[0] += orderAmount; // Update value in array
                    }
    
                    // Fetch inventory costs
                    inventoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot inventorySnapshot) {
                            if (inventorySnapshot.exists()) {
                                final double[] totalExpenses = {0}; // Another mutable container
    
                                for (DataSnapshot item : inventorySnapshot.getChildren()) {
                                    String itemName = item.getKey();
                                    Map<String, Object> itemData = (Map<String, Object>) item.getValue();
                                    double cost = Double.parseDouble(itemData.get("cost").toString());
                                    totalExpenses[0] += cost;
                                    inventoryCosts.put(itemName, cost);
                                }
    
                                // Calculate profit/loss
                                double profitOrLoss = totalRevenue[0] - totalExpenses[0];
                                listModel.addElement("Profit/Loss Report:");
                                listModel.addElement("Total Revenue: $" + totalRevenue[0]);
                                listModel.addElement("Total Expenses: $" + totalExpenses[0]);
                                listModel.addElement(profitOrLoss >= 0
                                        ? "Profit: $" + profitOrLoss
                                        : "Loss: $" + Math.abs(profitOrLoss));
                                reportList.setModel(listModel);
                            }
                        }
    
                        @Override
                        public void onCancelled(DatabaseError error) {
                            listModel.addElement("Error generating profit/loss report: " + error.getMessage());
                            reportList.setModel(listModel);
                        }
                    });
                } else {
                    listModel.addElement("No sales data available.");
                    reportList.setModel(listModel);
                }
            }
    
            @Override
            public void onCancelled(DatabaseError error) {
                listModel.addElement("Error fetching sales data: " + error.getMessage());
                reportList.setModel(listModel);
            }
        });
    }
    
}





