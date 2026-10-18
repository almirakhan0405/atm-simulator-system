# ATM Simulator System

A desktop ATM simulator implemented with **Java Swing**, **JDBC**, and **MySQL**. It is structured for NetBeans, Eclipse, or command-line compilation.

## Demo credentials
- **Primary Demo Account** (Easy to remember):
  - Card number: `1234123412341234`
  - PIN: `1234`
  - Balance: `₹50,000.00`
- **Secondary Demo Account**:
  - Card number: `4321432143214321`
  - PIN: `4321`
  - Balance: `₹25,000.00`
- **Legacy Demo Account**:
  - Card number: `5000123412341234`
  - PIN: `1234`
  - Balance: `₹50,000.00`

## Security Features & Mitigations
1. **Per-Account Cryptographic Salting & Constant-Time Hashing**:
   - PINs are hashed using per-account cryptographic random salts (`SecureRandom`) + SHA-256, protecting against precomputed rainbow table attacks.
   - Validation uses constant-time byte comparisons (`MessageDigest.isEqual`) to eliminate timing side-channel attacks.
2. **Brute-Force & PIN Guessing Protection (3-Attempt Lockout)**:
   - Tracks consecutive failed attempts in the database.
   - Automatically blocks the card (`status = 'BLOCKED'`) upon 3 consecutive incorrect PIN entries.
   - Blocked cards cannot be authenticated until unlocked.
3. **Financial Logic & Balance Exploit Protections**:
   - Enforces strictly positive values for deposits and withdrawals, eliminating negative withdrawal balance-inflation exploits.
   - Enforces maximum per-transaction caps and bounds checking.
   - Uses row-level locks (`SELECT ... FOR UPDATE`) inside atomic database transactions to prevent race conditions and double-spending.
4. **Memory Security**:
   - Password fields are cleared and zeroed out (`Arrays.fill(..., '\0')`) immediately after processing to prevent lingering plaintext PINs in heap memory.
5. **Session Enforcement**:
   - Thread-safe session management verifying that every financial operation belongs to an active, authenticated session.

## Quick start on Windows
1. Make sure JDK (JDK 8+) and MySQL 8+ are installed.
2. Verify credentials in `config/atm.properties` (defaults to user `root`, password `2805`).
3. Run `setup_db.bat` to import schema and download connector library if needed.
4. Run `run.bat` (or execute `java -cp "out;lib\mysql-connector-j-9.0.0.jar" com.atm.Main`).

## CLI Compilation
```bat
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -cp "lib\mysql-connector-j-9.0.0.jar" -d out @sources.txt
del sources.txt
java -cp "out;lib\mysql-connector-j-9.0.0.jar" com.atm.Main
```
