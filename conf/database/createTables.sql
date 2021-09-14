-- CREATE EXTENSION citext;
-- CREATE DOMAIN domain_email AS citext
--     CHECK ( value ~ '(?:[a-z0-9!#$%&''*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&''*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])' );

-- Create database tables
CREATE TABLE IF NOT EXISTS scaladb.public.users
(
    id SERIAL PRIMARY KEY,
    username varchar(20) UNIQUE NOT NULL,
    password varchar(200) NOT NULL,
    email text UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS scaladb.public.costs (
    id SERIAL PRIMARY KEY,
    user_id int4 NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    purchase_date TIMESTAMP NOT NULL,
    category TEXT NOT NULL,
    cost_price NUMERIC NOT NULL
);

CREATE TABLE IF NOT EXISTS scaladb.public.password_requests (
  id SERIAL PRIMARY KEY,
  user_id int4 NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  password_reset_token TEXT UNIQUE NOT NULL,
  password_reset_expiration TIMESTAMP NOT NULL
);

-- Grant all privileges to user scala
GRANT ALL ON SCHEMA public TO scala;
GRANT ALL ON SCHEMA public TO public;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO scala;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO scala;
