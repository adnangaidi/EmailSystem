# 📧 EmailSystem

A full-featured, self-hosted web email client built with **Spring Boot**. Users can register, link their personal mail accounts (POP3 / SMTP), and manage their inbox, sent items, and trash — all from a clean, responsive interface. Email bodies and attachments are stored **encrypted at rest** using AES-256-GCM.

---

## ✨ Features

- **User authentication** — secure registration and login powered by Spring Security
- **Mail account management** — link any POP3/SMTP-compatible email address
- **Inbox / Sent / Trash** — paginated folder views with unread counters
- **Compose & send** — rich compose form with file attachment support
- **Automatic mail sync** — scheduled POP3 polling fetches new messages in the background
- **End-to-end encryption at rest** — email bodies and attachments are AES-256-GCM encrypted before being saved to the database
- **Admin panel** — manage users and view usage statistics
- **XSS protection** — all HTML input sanitised with the OWASP Java HTML Sanitizer
- **Embedded SQLite database** — zero-configuration, single-file storage (no separate DB server needed)
- **Responsive UI** — built with Bootstrap 5 and Bootstrap Icons via Thymeleaf templates

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 4.1.2 |
| Language | Java 17 |
| Persistence | Spring Data JPA + Hibernate + SQLite |
| Security | Spring Security 7 |
| Templating | Thymeleaf + Thymeleaf-Extras-SpringSecurity |
| Mail | Jakarta Mail (SMTP send / POP3 receive) |
| Encryption | AES-256-GCM (custom `EncryptionService`) |
| UI | Bootstrap 5.3 + Bootstrap Icons 1.11 |
| Build tool | Apache Maven |

---

## 🚀 Local Installation

### Prerequisites

Make sure you have the following installed:

- **Java 17+** — [Download](https://adoptium.net/)
- **Apache Maven 3.9+** — [Download](https://maven.apache.org/download.cgi)
- **Git** — [Download](https://git-scm.com/)

Verify your setup:
```bash
java -version   # should print 17 or higher
mvn -version
git --version
```

---

### 1 — Clone the repository

```bash
git clone https://github.com/adnangaidi/EmailSystem.git
cd EmailSystem
```

---

### 2 — Configure the application

Open `src/main/resources/application.properties` and review the settings:

```properties
# SQLite database file (created automatically in the project root)
spring.datasource.url=jdbc:sqlite:emailsystem.db

# AES-256 encryption key (Base64-encoded 32 bytes)
# Override with an environment variable in production:
#   export APP_CRYPTO_KEY=<your-base64-32-byte-key>
app.crypto-key=${APP_CRYPTO_KEY:MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=}
```

> [!WARNING]
> The default `APP_CRYPTO_KEY` is for development only. **Always** set a strong, secret key via the `APP_CRYPTO_KEY` environment variable before running in any shared or production environment.

To generate a secure key:
```bash
# Linux / macOS
openssl rand -base64 32

# Windows (PowerShell)
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Max 256 }))
```

---

### 3 — Build the project

```bash
mvn clean package -DskipTests
```

The compiled JAR will be at `target/EmailProject-0.0.1-SNAPSHOT.jar`.

---

### 4 — Run the application

**Option A — Maven (development, with hot-reload):**
```bash
mvn spring-boot:run
```

**Option B — JAR (production-style):**
```bash
# Linux / macOS
APP_CRYPTO_KEY=<your-key> java -jar target/EmailProject-0.0.1-SNAPSHOT.jar

# Windows (PowerShell)
$env:APP_CRYPTO_KEY="<your-key>"
java -jar target/EmailProject-0.0.1-SNAPSHOT.jar
```

---

### 5 — Open the app

Navigate to **[http://localhost:8080](http://localhost:8080)** in your browser.

1. **Register** a new account
2. Go to **Settings → Mail Accounts** and add your POP3 / SMTP credentials
3. Start sending and receiving emails!

---

## 📁 Project Structure

```
src/
└── main/
    ├── java/com/projects/adnan/EmailProject/
    │   ├── config/        # Security, dialect, mail properties
    │   ├── controller/    # MVC controllers (Inbox, Compose, Auth, Admin)
    │   ├── dto/           # Form & view data transfer objects
    │   ├── entity/        # JPA entities (User, EmailMessage, Attachment, MailAccount)
    │   ├── mail/          # POP3 sync scheduler & SMTP send service
    │   ├── repository/    # Spring Data JPA repositories
    │   ├── security/      # UserDetailsService & AES-256 EncryptionService
    │   ├── service/       # Business logic (EmailService, UserService, AdminService)
    │   └── util/          # Input sanitizer (XSS protection)
    └── resources/
        ├── templates/     # Thymeleaf HTML templates
        ├── static/        # CSS, JS, images
        └── application.properties
```

---

## 🔒 Security Notes

- Passwords are hashed with **BCrypt**
- Email bodies and attachments are **AES-256-GCM encrypted** before being stored
- Each encrypted value stores its own random **IV (nonce)**
- HTML user input is sanitised with the **OWASP Java HTML Sanitizer** to prevent XSS

---

## 📄 License

This project is for educational purposes.
