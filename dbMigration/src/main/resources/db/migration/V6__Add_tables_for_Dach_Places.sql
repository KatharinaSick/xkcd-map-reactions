CREATE SEQUENCE seq_dach_places;

CREATE TABLE dach_places(
    id BIGINT default nextval('seq_dach_places') PRIMARY KEY,
    name VARCHAR,
    latitude DECIMAL(9,6),
    longitude DECIMAL(9,6)
);

CREATE SEQUENCE seq_beider_morse_encoded_dach_places;
CREATE TABLE beider_morse_encoded_dach_places (
                id BIGINT default nextval('seq_beider_morse_encoded_dach_places') PRIMARY KEY,
                code VARCHAR,
                place_id BIGINT);

CREATE SEQUENCE seq_nysiis_encoded_dach_places;
CREATE TABLE nysiis_encoded_dach_places (
                id BIGINT default nextval('seq_nysiis_encoded_dach_places') PRIMARY KEY,
                code VARCHAR,
                place_id BIGINT);

CREATE SEQUENCE seq_soundex_encoded_dach_places;
CREATE TABLE soundex_encoded_dach_places (
                id BIGINT default nextval('seq_soundex_encoded_dach_places') PRIMARY KEY,
                code VARCHAR,
                place_id BIGINT);

CREATE INDEX ON beider_morse_encoded_dach_places (code);
CREATE INDEX ON nysiis_encoded_dach_places (code);
CREATE INDEX ON soundex_encoded_dach_places (code);
CREATE INDEX ON dach_places (lower(name));