CREATE TABLE IF NOT EXISTS public.users
(
    id SERIAL PRIMARY KEY,
    username varchar(20) NOT NULL,
    password varchar(200) NOT NULL,
    email text NOT NULL
);

CREATE TABLE IF NOT EXISTS public.costs (
    id SERIAL PRIMARY KEY,
    user_id int4 REFERENCES users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    purchase_date DATE NOT NULL,
    category TEXT NOT NULL,
    cost_price NUMERIC NOT NULL
);

