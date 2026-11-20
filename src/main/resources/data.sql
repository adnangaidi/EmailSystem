INSERT OR IGNORE INTO users(username, email, password_hash, role, enabled, locked, created_at)
VALUES ('user', 'user@example.com', '$2a$12$8Q1KXPGQnNh9mC8gks8CL.drUyptWHocesLizF4oNPnAOsR4Okd4W', 'USER', 1, 0, CURRENT_TIMESTAMP);
INSERT OR IGNORE INTO users(username, email, password_hash, role, enabled, locked, created_at)
VALUES ('admin', 'admin@example.com', '$2a$12$8Q1KXPGQnNh9mC8gks8CL.drUyptWHocesLizF4oNPnAOsR4Okd4W', 'ADMIN', 1, 0, CURRENT_TIMESTAMP);