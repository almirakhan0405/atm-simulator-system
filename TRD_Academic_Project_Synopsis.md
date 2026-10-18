# Technical Requirements Document (TRD)
## ATM Simulator System

### 1. Architecture
The system uses a three-layer desktop architecture:

```text
Swing UI (LoginFrame, DashboardFrame)
          |
Service layer (AuthService, AtmService)
          |
JDBC data access / transaction boundary (Database)
          |
MySQL relational database
```

### 2. Technology stack
- Java SE 8+.
- Java Swing/AWT for the GUI.
- JDBC with MySQL Connector/J 9.0.0.
- MySQL 8.x.
- Windows Batch and PowerShell for setup and execution.

### 3. Package and component design
- `Main`: sets the look and feel and starts the login frame.
- `Database`: loads properties/environment overrides and creates JDBC connections.
- `Session`: stores the authenticated account for the active UI session.
- `AuthService`: validates credentials and changes PINs.
- `AtmService`: performs balance, deposit, withdrawal, and statement operations.
- `LoginFrame`: card/PIN login form.
- `DashboardFrame`: operation buttons, dialogs, and JTable statement.

### 4. Database design
- `customers`: customer identity.
- `accounts`: card number, PIN hash, status, and current balance.
- `transactions`: immutable operation log with balance after operation.
- Foreign keys use `ON DELETE CASCADE` for educational cleanup consistency.
- `accounts.card_number` is unique; transaction account/type/time fields are indexed.
- Monetary values use `DECIMAL(15,2)` rather than floating point.

### 5. Transaction and consistency design
For withdrawal, the service starts a transaction, locks the account row with `FOR UPDATE`, verifies status and balance, updates the balance, inserts the transaction record, and commits. Any failure rolls back both changes. Deposit uses the same transaction pattern. This prevents a partial balance update or an orphaned transaction record.

### 6. Security design
- PINs are stored as SHA-256 digests rather than clear text.
- Prepared statements are used for all SQL containing user input.
- Password/PIN fields use `JPasswordField`.
- Errors shown in the GUI are user-safe; stack traces are printed only for local diagnostics.
- This is an academic simulation, not a production banking implementation.

### 7. Validation rules
- Card number: exactly 16 digits.
- PIN: exactly four digits.
- Amount: numeric, positive, and at most two decimal places through `BigDecimal` parsing.
- Withdrawal: amount must not exceed current balance.
- PIN change: current PIN must match; new PIN must be four digits and must differ from current PIN.

### 8. Build and deployment
`setup_db.bat` creates the database, imports the schema, and downloads Connector/J. `run.bat` compiles every Java source under `src` into `out` and launches `com.atm.Main` with the connector on the classpath. `config/atm.properties` is the default configuration file.

### 9. Testing plan
- Valid and invalid authentication.
- Boundary PIN values and malformed card numbers.
- Balance inquiry after seeded data import.
- Successful and insufficient-funds withdrawal.
- Successful deposit.
- Statement ordering and transaction persistence.
- Correct and incorrect current PIN change attempts.
- Database interruption and rollback behavior.
