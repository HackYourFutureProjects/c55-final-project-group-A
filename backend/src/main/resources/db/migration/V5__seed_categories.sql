INSERT INTO categories (name)
VALUES ('Music'),
       ('Arts & Culture'),
       ('Sports & Fitness'),
       ('Theatre & Performance'),
       ('Food & Drink'),
       ('Nightlife'),
       ('Community & Social'),
       ('Education & Workshops'),
       ('Business & Networking'),
       ('Family & Kids'),
       ('Technology'),
       ('Other')
ON CONFLICT (name) DO NOTHING;