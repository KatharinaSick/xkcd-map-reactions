CREATE SEQUENCE seq_places;

CREATE TABLE places(
    id BIGINT default nextval('seq_places') PRIMARY KEY,
    name VARCHAR,
    latitude DECIMAL(9,6),
    longitude DECIMAL(9,6)
);

CREATE SEQUENCE seq_beider_morse_encoded_places;
CREATE TABLE beider_morse_encoded_places (
                id BIGINT default nextval('seq_beider_morse_encoded_places') PRIMARY KEY,
                code VARCHAR,
                place_id BIGINT);

CREATE SEQUENCE seq_nysiis_encoded_places;
CREATE TABLE nysiis_encoded_places (
                id BIGINT default nextval('seq_nysiis_encoded_places') PRIMARY KEY,
                code VARCHAR,
                place_id BIGINT);

CREATE SEQUENCE seq_soundex_encoded_places;
CREATE TABLE soundex_encoded_places (
                id BIGINT default nextval('seq_soundex_encoded_places') PRIMARY KEY,
                code VARCHAR,
                place_id BIGINT);