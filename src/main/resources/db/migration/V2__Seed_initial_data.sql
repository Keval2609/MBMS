-- Migration V2: Seed Initial Branch and System Admin Employee

INSERT INTO branches (branch_code, name, address)
VALUES ('MAIN001', 'Main Headquarter Branch', '127 Financial Avenue, Central District');

INSERT INTO employees (branch_id, username, password_hash, role)
VALUES (
    (SELECT id FROM branches WHERE branch_code = 'MAIN001'),
    'admin',
    '$2a$10$e7q9V/3gM5hX/uXQxQ/1EO7uW9cQ1fK7uM1uA5gX1u5u5u5u5u5u5', -- Sample BCrypt hash
    'ADMIN'
);
