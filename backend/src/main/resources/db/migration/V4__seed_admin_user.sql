INSERT INTO users (id, role, name, email, password_hash, created_at)
VALUES (
           gen_random_uuid(),
           'admin',
           'Admin',
           'admin@example.com',
           '$2a$12$rotHXnx5NNurqtF5/gWpi.01lQpJnJrWZsVdOSAHAJNS6ZipMaVjG',
           now()
       );