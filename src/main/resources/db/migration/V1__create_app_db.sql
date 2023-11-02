CREATE SCHEMA myschema;

CREATE TABLE myschema.user
(
    user_id    VARCHAR(30) PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT current_timestamp
);

CREATE TABLE myschema.user_session
(
    session_id    VARCHAR(1000) PRIMARY KEY NOT NULL,
    refresh_token VARCHAR(1000)             NOT NULL,
    user_id       VARCHAR(30)               NOT NULL,
    created_at    TIMESTAMP                 NOT NULL DEFAULT current_timestamp,

    CONSTRAINT userID
        FOREIGN KEY (user_id)
            REFERENCES myschema.user (user_id) ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sessions (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    session_id VARCHAR NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
