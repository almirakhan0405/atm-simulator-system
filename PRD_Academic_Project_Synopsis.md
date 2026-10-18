# Product Requirements Document (PRD)
## ATM Simulator System

### 1. Project identification
**Title:** ATM Simulator System  
**Category:** Academic desktop application  
**Frontend:** Java Swing  
**Backend access:** Java Database Connectivity (JDBC)  
**Database:** MySQL only

### 2. Abstract
The ATM Simulator System is a desktop application that models the primary customer-facing functions of an automated teller machine. The system authenticates a customer using a card number and four-digit PIN, displays account balance, processes validated withdrawals and deposits, displays a mini statement, and permits PIN changes. The project demonstrates GUI design, relational database design, JDBC programming, validation, and transaction management.

### 3. Problem statement
Manual or paper-based demonstrations of ATM workflows do not adequately show authentication, data persistence, concurrent balance safety, or auditable transaction history. This project provides a compact, persistent, and testable simulation for academic laboratory use.

### 4. Objectives
- Provide a clear Swing interface for common ATM operations.
- Persist customers, accounts, and transactions in MySQL.
- Apply server-side validation and atomic withdrawal logic.
- Demonstrate foreign keys, indexes, constraints, and prepared statements.
- Produce a mini statement in a JTable.

### 5. Scope
**In scope:** card/PIN authentication, balance inquiry, withdrawal, deposit, recent transaction history, PIN change, logout, seeded demo data, JDBC connectivity, and Windows setup scripts.

**Out of scope:** real banking networks, inter-bank transfers, card issuance, ATM hardware, cash cassette sensors, multi-factor authentication, administrator console, interest calculation, and production-grade cryptographic key management.

### 6. Functional requirements
| ID | Requirement | Acceptance criterion |
|---|---|---|
| FR-01 | Authenticate card and PIN | Valid seeded credentials open the dashboard; invalid credentials are rejected. |
| FR-02 | Balance inquiry | Current balance is read from the logged-in account. |
| FR-03 | Cash withdrawal | Positive amount not exceeding balance is deducted atomically and recorded. |
| FR-04 | Cash deposit | Positive amount is added and recorded. |
| FR-05 | Mini statement | Recent transactions appear in a JTable with date, type, amount, balance, and reference. |
| FR-06 | PIN change | Existing PIN is verified, new PIN is exactly four digits, and the hash is updated. |
| FR-07 | Logout | Session data is cleared and login screen is shown. |

### 7. Non-functional requirements
- Java 8 or later.
- MySQL 8 or compatible MySQL server.
- JDBC prepared statements for user input.
- Transactional withdrawal with row locking.
- Useful validation messages and no raw SQL errors shown to users.
- Maintainable separation between UI, service, and database layers.

### 8. Users and assumptions
The primary user is a student or evaluator operating the simulator locally. MySQL is assumed to be available on the local machine. The provided seed account is for demonstration only.

### 9. User workflow
1. Launch the application.
2. Enter a 16-digit card number and four-digit PIN.
3. Select an operation.
4. Review the result and, where applicable, the updated balance or statement.
5. Log out.

### 10. Success criteria
All seven functional requirements pass using the seed data. A withdrawal that exceeds balance leaves both balance and transaction history unchanged. A successful deposit, withdrawal, or PIN change persists after relaunch.

### 11. Risks and mitigations
- **Database unavailable:** display a connection message and document configuration.
- **Concurrent withdrawals:** use `SELECT ... FOR UPDATE` and a database transaction.
- **SQL injection:** use prepared statements exclusively.
- **Demo credential misuse:** clearly label seed credentials as local academic data.
