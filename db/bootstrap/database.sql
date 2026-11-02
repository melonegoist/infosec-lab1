SELECT NOT EXISTS (SELECT FROM pg_database WHERE datname = :'db') AS create_db \gset
\if :create_db
CREATE DATABASE :"db" OWNER :"owner_user" ENCODING 'UTF8' TEMPLATE template0;
\endif
ALTER DATABASE :"db" OWNER TO :"owner_user";

REVOKE ALL ON DATABASE :"db" FROM PUBLIC;
GRANT CONNECT ON DATABASE :"db" TO lab1_rw;

\connect :"db"
REVOKE ALL ON SCHEMA public FROM PUBLIC;
\connect postgres
