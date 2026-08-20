ALTER TABLE addresses
    ADD COLUMN city_name VARCHAR(200),
    ADD COLUMN province VARCHAR(100);

UPDATE addresses AS a
SET city_name = c.name,
    province = c.province
FROM cities AS c
WHERE c.id = a.city_id;

ALTER TABLE addresses
    ALTER COLUMN city_name SET NOT NULL;

ALTER TABLE addresses
    DROP CONSTRAINT fk_addresses_city;

DROP INDEX IF EXISTS idx_addresses_city_id;

ALTER TABLE addresses
    DROP COLUMN city_id;

DROP TABLE cities;
