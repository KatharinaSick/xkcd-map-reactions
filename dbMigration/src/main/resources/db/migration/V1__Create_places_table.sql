CREATE SEQUENCE seq_places;

CREATE TABLE places(
    id BIGINT default nextval('seq_places') PRIMARY KEY,
    name VARCHAR,
    latitude DECIMAL(9,6),
    longitude DECIMAL(9,6)
);
