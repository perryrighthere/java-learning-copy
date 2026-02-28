-- Week 3: core CRUD hardening (soft delete + query indexes)

alter table board_columns
    add column deleted_at timestamp;

alter table comments
    add column deleted_at timestamp;

create index idx_boards_owner_archived on boards(owner_id, archived_at);
create index idx_boards_name_archived on boards(name, archived_at);

create index idx_columns_board_deleted_position on board_columns(board_id, deleted_at, position);
create index idx_columns_board_deleted_name on board_columns(board_id, deleted_at, name);

create index idx_cards_column_deleted_position on cards(column_id, deleted_at, position);
create index idx_cards_column_deleted_title on cards(column_id, deleted_at, title);

create index idx_comments_card_deleted_created on comments(card_id, deleted_at, created_at);
