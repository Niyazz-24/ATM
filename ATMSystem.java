import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/*
 * ATM Simulation System - Java Console Project
 * Demo only: data is stored in memory and resets when the program closes.
 * Default demo accounts:
 * 1001 / PIN 1234 / Niyash / Rs. 10000
 * 1002 / PIN 5678 / Rahul  / Rs. 15000
 * 1003 / PIN 4321 / Arun   / Rs. 8000
 *
 * Admin demo credentials: username admin, password 2468
 */
public class ATMSystem {
    private static final Scanner sc = new Scanner(System.in);
    private static final Map<String, Account> accounts = new LinkedHashMap<>();
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "2468";
    private static final int MAX_PIN_ATTEMPTS = 3;
    private static Account loggedIn;

    static class Account {
        String number;
        String name;
        String pin;
        double balance;
        int wrongPinAttempts;
        boolean locked;
        List<String> history = new ArrayList<>();

        Account(String number, String name, String pin, double balance) {
            this.number = number;
            this.name = name;
            this.pin = pin;
            this.balance = balance;
            history.add(timestamp() + " | Account opened | Balance: " + money(balance));
        }

        void addHistory(String message) {
            history.add(0, timestamp() + " | " + message + " | Balance: " + money(balance));
        }
    }

    public static void main(String[] args) {
        seedAccounts();
        while (true) {
            System.out.println("\n====================================");
            System.out.println("       NATIONAL BANK - ATM");
            System.out.println("====================================");
            System.out.println("1. Customer Login");
            System.out.println("2. Create Account");
            System.out.println("3. Administrator Login");
            System.out.println("4. Exit");
            System.out.print("Choose option: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1": customerLogin(); break;
                case "2": createAccount(); break;
                case "3": adminLogin(); break;
                case "4":
                    System.out.println("Thank you for using National Bank ATM.");
                    return;
                default: System.out.println("Invalid option. Try again.");
            }
        }
    }

    private static void seedAccounts() {
        accounts.put("1001", new Account("1001", "Niyash", "1234", 10000));
        accounts.put("1002", new Account("1002", "Rahul", "5678", 15000));
        accounts.put("1003", new Account("1003", "Arun", "4321", 8000));
    }

    private static void customerLogin() {
        System.out.print("Enter account number: ");
        String number = sc.nextLine().trim();
        Account account = accounts.get(number);

        if (account == null) {
            System.out.println("Account not found.");
            return;
        }
        if (account.locked) {
            System.out.println("This account is BLOCKED after 3 incorrect PIN attempts.");
            System.out.println("Please contact the demo administrator to unlock it.");
            return;
        }

        System.out.print("Enter 4-digit PIN: ");
        String pin = sc.nextLine().trim();
        if (!verifyPin(account, pin)) return;

        loggedIn = account;
        System.out.println("\nWelcome, " + loggedIn.name + "!");
        customerMenu();
    }

    // Every wrong PIN counts toward the same 3-attempt account lock.
    private static boolean verifyPin(Account account, String enteredPin) {
        if (account.locked) {
            System.out.println("Account is blocked. Please contact the administrator.");
            return false;
        }
        if (account.pin.equals(enteredPin)) {
            account.wrongPinAttempts = 0;
            return true;
        }

        account.wrongPinAttempts++;
        int remaining = MAX_PIN_ATTEMPTS - account.wrongPinAttempts;
        if (remaining <= 0) {
            account.locked = true;
            if (loggedIn == account) loggedIn = null;
            System.out.println("Incorrect PIN. Your account is now BLOCKED after 3 failed attempts.");
        } else {
            System.out.println("Incorrect PIN. " + remaining + " attempt(s) remaining.");
        }
        return false;
    }

    private static void customerMenu() {
        while (loggedIn != null && !loggedIn.locked) {
            System.out.println("\n----------- CUSTOMER MENU -----------");
            System.out.println("1. Balance Enquiry");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Money Transfer");
            System.out.println("5. Transaction History");
            System.out.println("6. Change PIN");
            System.out.println("7. Logout");
            System.out.print("Choose option: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1": balanceEnquiry(); break;
                case "2": deposit(); break;
                case "3": withdraw(); break;
                case "4": transfer(); break;
                case "5": transactionHistory(); break;
                case "6": changePin(); break;
                case "7":
                    System.out.println("Logged out successfully.");
                    loggedIn = null;
                    return;
                default: System.out.println("Invalid option. Try again.");
            }
        }
    }

    private static boolean authenticateCurrentAccount() {
        if (loggedIn == null || loggedIn.locked) return false;
        System.out.print("Enter your 4-digit PIN to confirm this operation: ");
        String pin = sc.nextLine().trim();
        boolean ok = verifyPin(loggedIn, pin);
        if (!ok && loggedIn == null) {
            System.out.println("Session ended because the account was blocked.");
        }
        return ok;
    }

    private static void balanceEnquiry() {
        if (!authenticateCurrentAccount()) return;
        System.out.println("Available balance: " + money(loggedIn.balance));
    }

    private static void deposit() {
        System.out.print("Enter deposit amount: Rs. ");
        double amount = readAmount();
        if (amount <= 0) { System.out.println("Enter a valid positive amount."); return; }
        if (!authenticateCurrentAccount()) return;

        loggedIn.balance = round2(loggedIn.balance + amount);
        loggedIn.addHistory("Deposit +" + money(amount));
        System.out.println("Deposit successful. New balance: " + money(loggedIn.balance));
    }

    private static void withdraw() {
        System.out.print("Enter withdrawal amount: Rs. ");
        double amount = readAmount();
        if (amount <= 0) { System.out.println("Enter a valid positive amount."); return; }
        if (amount > loggedIn.balance) { System.out.println("Insufficient balance."); return; }
        if (!authenticateCurrentAccount()) return;

        loggedIn.balance = round2(loggedIn.balance - amount);
        loggedIn.addHistory("Withdrawal -" + money(amount));
        System.out.println("Withdrawal successful. Remaining balance: " + money(loggedIn.balance));
    }

    private static void transfer() {
        System.out.print("Enter recipient account number: ");
        String recipientNumber = sc.nextLine().trim();
        Account recipient = accounts.get(recipientNumber);

        if (recipient == null) { System.out.println("Recipient account not found."); return; }
        if (recipient.number.equals(loggedIn.number)) {
            System.out.println("You cannot transfer money to your own account."); return;
        }
        if (recipient.locked) { System.out.println("Recipient account is blocked. Transfer cancelled."); return; }

        System.out.print("Enter transfer amount: Rs. ");
        double amount = readAmount();
        if (amount <= 0) { System.out.println("Enter a valid positive amount."); return; }
        if (amount > loggedIn.balance) { System.out.println("Insufficient balance."); return; }
        if (!authenticateCurrentAccount()) return;

        loggedIn.balance = round2(loggedIn.balance - amount);
        recipient.balance = round2(recipient.balance + amount);
        loggedIn.addHistory("Transfer to " + recipient.name + " (" + recipient.number + ") -" + money(amount));
        recipient.addHistory("Transfer received from " + loggedIn.name + " (" + loggedIn.number + ") +" + money(amount));
        System.out.println("Transfer successful to " + recipient.name + ".");
        System.out.println("Remaining balance: " + money(loggedIn.balance));
    }

    private static void transactionHistory() {
        if (!authenticateCurrentAccount()) return;
        System.out.println("\n-------- TRANSACTION HISTORY --------");
        if (loggedIn.history.isEmpty()) {
            System.out.println("No transactions available.");
        } else {
            for (String item : loggedIn.history) System.out.println(item);
        }
    }

    private static void changePin() {
        System.out.print("Enter new 4-digit PIN: ");
        String newPin = sc.nextLine().trim();
        if (!newPin.matches("\\d{4}")) {
            System.out.println("PIN must contain exactly 4 digits."); return;
        }
        System.out.print("Confirm new PIN: ");
        String confirm = sc.nextLine().trim();
        if (!newPin.equals(confirm)) {
            System.out.println("PIN confirmation does not match."); return;
        }
        if (newPin.equals(loggedIn.pin)) {
            System.out.println("Choose a PIN different from your current PIN."); return;
        }
        if (!authenticateCurrentAccount()) return;

        loggedIn.pin = newPin;
        loggedIn.addHistory("PIN changed");
        System.out.println("PIN changed successfully.");
    }

    private static void createAccount() {
        System.out.println("\n----------- CREATE ACCOUNT -----------");
        System.out.print("Enter customer name: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) { System.out.println("Name cannot be empty."); return; }

        System.out.print("Choose account number (4-16 letters/numbers/hyphens): ");
        String number = sc.nextLine().trim();
        if (!number.matches("[A-Za-z0-9-]{4,16}")) {
            System.out.println("Invalid account number format."); return;
        }
        if (accounts.containsKey(number)) {
            System.out.println("That account number already exists."); return;
        }

        System.out.print("Create a 4-digit PIN: ");
        String pin = sc.nextLine().trim();
        if (!pin.matches("\\d{4}")) { System.out.println("PIN must be exactly 4 digits."); return; }
        System.out.print("Confirm PIN: ");
        String confirm = sc.nextLine().trim();
        if (!pin.equals(confirm)) { System.out.println("PINs do not match."); return; }

        Account account = new Account(number, name, pin, 0);
        accounts.put(number, account);
        System.out.println("Account created successfully!");
        System.out.println("Account number: " + number + " | Initial balance: Rs. 0.00");
    }

    private static void adminLogin() {
        System.out.print("Admin username: ");
        String username = sc.nextLine().trim();
        System.out.print("Admin password: ");
        String password = sc.nextLine().trim();

        if (!ADMIN_USERNAME.equals(username) || !ADMIN_PASSWORD.equals(password)) {
            System.out.println("Invalid administrator credentials.");
            return;
        }
        adminMenu();
    }

    private static void adminMenu() {
        while (true) {
            System.out.println("\n----------- ADMIN PANEL -----------");
            System.out.println("1. View all accounts");
            System.out.println("2. Unlock blocked account");
            System.out.println("3. Logout from admin");
            System.out.print("Choose option: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.println("\nAccount No. | Name | Balance | Status");
                    for (Account a : accounts.values()) {
                        System.out.printf("%s | %s | %s | %s%n", a.number, a.name,
                                money(a.balance), a.locked ? "BLOCKED" : "ACTIVE");
                    }
                    break;
                case "2":
                    System.out.print("Enter account number to unlock: ");
                    String number = sc.nextLine().trim();
                    Account account = accounts.get(number);
                    if (account == null) {
                        System.out.println("Account not found.");
                    } else if (!account.locked) {
                        System.out.println("This account is already active.");
                    } else {
                        account.locked = false;
                        account.wrongPinAttempts = 0;
                        account.addHistory("Account unlocked by demo administrator");
                        System.out.println("Account " + number + " unlocked successfully.");
                        System.out.println("Wrong PIN attempts reset. Balance and history preserved.");
                    }
                    break;
                case "3":
                    System.out.println("Admin logged out.");
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static double readAmount() {
        try {
            return round2(Double.parseDouble(sc.nextLine().trim()));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String money(double amount) {
        return String.format(Locale.US, "Rs. %.2f", amount);
    }

    private static String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
    }
}
