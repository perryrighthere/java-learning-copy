-- Week 2: auth fields and seed-ready defaults

alter table users
    add column password_hash varchar(255) not null default '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi1Qebs2J8sGQ6r5GeUXrXe3G5UpiS6';
