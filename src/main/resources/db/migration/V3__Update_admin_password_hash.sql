-- Migration V3: Update Admin Password with Valid BCrypt Hash

UPDATE employees
SET password_hash = '$2a$10$Q.ZnnWxCSnsmFFNp6A8MR.HpYQgkO7FJ83q3A6tj/3J505hyJ3Mqm'
WHERE username = 'admin';
