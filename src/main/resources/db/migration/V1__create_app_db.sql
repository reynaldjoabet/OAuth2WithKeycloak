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

