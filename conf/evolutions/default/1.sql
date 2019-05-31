-- Initial tables

-- !Ups

CREATE TABLE users (
    id bigserial,
    email text NOT NULL,
    name text NOT NULL,
    phone text DEFAULT NULL,
    hashed_password text NOT NULL,
    is_admin boolean NOT NULL default FALSE,
    created_at timestamptz NOT NULL DEFAULT NOW(),
PRIMARY KEY(id),
UNIQUE(email)
);

CREATE EXTENSION IF NOT EXISTS postgis;



CREATE TABLE mediatypes (
    id serial,
    mime_type text,
    name text NOT NULL,
PRIMARY KEY(id)
);



CREATE TABLE media (
    id bigserial,
    mediatype_id integer not null references mediatypes(id) on delete no action,
location geography(POINT,4269),
media_path text NOT NULL,
owner_id bigint NOT NULL references users(id) on delete cascade,
created_at timestamptz NOT NULL DEFAULT NOW(),
PRIMARY KEY(id)
);



CREATE TABLE subjecttypes (
    id serial,
    name text NOT NULL,
primary key(id)
);



CREATE TABLE media_subjects (
    id bigserial,
    subjecttype_id integer not null references subjecttypes(id) on delete no action,
name text NOT NULL,
description text,
user_id bigint references users(id) on delete cascade,
primary key(id)
);



CREATE TABLE media_media_subjects (
    id bigserial,
    media_id bigint not null references media(id) on delete cascade,
subject_id bigint not null references media_subjects on delete cascade,
created_at timestamptz NOT NULL default NOW(),
created_by bigint references users(id) on delete no action,
primary key(id)
);



CREATE TABLE groups (
    id bigserial,
    name text NOT NULL,
    description text,
    created_at timestamptz NOT NULL default now(),
created_by bigint NOT NULL references users(id) on delete cascade,
primary key(id)
);



CREATE TABLE group_users (
    id bigserial,
    group_id bigint references groups(id) on delete cascade,
user_id bigint references users(id) on delete no action,
invited_by bigint references users(id) on delete no action,
invitation_key text,
invited_at timestamptz NOT NULL DEFAULT NOW(),
joined_at timestamptz,
left_at timestamptz,
primary key(id)
);



CREATE TABLE shares (
    id bigserial,
    media_id bigint NOT NULL references media(id) on delete cascade,
user_id bigint references users(id) on delete cascade,
group_id bigint references groups(id) on delete cascade,
created_at timestamptz not null default now(),
created_by bigint not null references users(id),
primary key(id)
);


-- !Downs

DROP TABLE IF EXISTS users CASCADE;

DROP TABLE IF EXISTS mediatypes CASCADE;

DROP TABLE IF EXISTS media CASCADE;

DROP TABLE IF EXISTS subjecttypes CASCADE;

DROP TABLE IF EXISTS media_subjects CASCADE;

DROP TABLE IF EXISTS media_media_subjects CASCADE;

DROP TABLE IF EXISTS groups CASCADE;

DROP TABLE IF EXISTS group_users CASCADE;

DROP TABLE IF EXISTS shares CASCADE;