SELECT 'CREATE DATABASE corda_party_d' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname='corda_party_d')\gexec
SELECT 'CREATE DATABASE corda_party_b' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname='corda_party_b')\gexec

SELECT 'CREATE USER user_b WITH PASSWORD ''123456''' WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname='user_b')\gexec
SELECT 'CREATE USER user_d WITH PASSWORD ''123456''' WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname='user_d')\gexec

GRANT ALL PRIVILEGES ON DATABASE corda_party_b TO user_b;
GRANT ALL PRIVILEGES ON DATABASE corda_party_d TO user_d;
\c corda_party_a
DO $$ BEGIN IF NOT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name='public' AND schema_owner='user_a') THEN
    
END IF; END $$;
\c corda_party_b
DO $$ BEGIN IF NOT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name='public' AND schema_owner='user_b') THEN
    GRANT CREATE ON SCHEMA public TO user_b; ALTER SCHEMA public OWNER TO user_b;
END IF; END $$;
\c corda_party_d
DO $$ BEGIN IF NOT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name='public' AND schema_owner='user_d') THEN
    GRANT CREATE ON SCHEMA public TO user_d; ALTER SCHEMA public OWNER TO user_d;
END IF; END $$;

