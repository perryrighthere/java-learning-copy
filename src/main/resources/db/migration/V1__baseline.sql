-- Baseline schema for Kanban backend (Week 1)

create table users (
    id           bigserial primary key,
    email        varchar(255) not null unique,
    display_name varchar(120) not null,
    created_at   timestamp not null default current_timestamp,
    updated_at   timestamp not null default current_timestamp
);

create table boards (
    id          bigserial primary key,
    name        varchar(255) not null,
    owner_id    bigint not null references users(id),
    created_at  timestamp not null default current_timestamp,
    updated_at  timestamp not null default current_timestamp,
    archived_at timestamp
);

create table memberships (
    id         bigserial primary key,
    user_id    bigint not null references users(id),
    board_id   bigint not null references boards(id),
    role       varchar(20) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    unique (user_id, board_id)
);

create table board_columns (
    id         bigserial primary key,
    board_id   bigint not null references boards(id) on delete cascade,
    name       varchar(255) not null,
    position   numeric(12,2) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table cards (
    id          bigserial primary key,
    column_id   bigint not null references board_columns(id) on delete cascade,
    title       varchar(255) not null,
    description text,
    position    numeric(12,2) not null,
    version     integer not null default 0,
    created_at  timestamp not null default current_timestamp,
    updated_at  timestamp not null default current_timestamp,
    deleted_at  timestamp
);

create table comments (
    id         bigserial primary key,
    card_id    bigint not null references cards(id) on delete cascade,
    author_id  bigint not null references users(id),
    body       text not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table attachments (
    id           bigserial primary key,
    card_id      bigint not null references cards(id) on delete cascade,
    filename     varchar(255) not null,
    mime_type    varchar(120) not null,
    size_bytes   bigint not null,
    storage_path varchar(400) not null,
    created_at   timestamp not null default current_timestamp,
    updated_at   timestamp not null default current_timestamp
);

create index idx_columns_board_position on board_columns(board_id, position);
create index idx_cards_column_position on cards(column_id, position);
create index idx_memberships_user on memberships(user_id);
create index idx_comments_card on comments(card_id);
