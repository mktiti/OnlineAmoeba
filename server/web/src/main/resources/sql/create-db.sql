create table if not exists JoinCode(
    id      bigint identity prime key,
    code    varchar(50) not null unique
);

create table if not exists Game(
    id              bigint identity prime key,
    stored          timestamp(0) default current_timestamp not null,
    hostCodeId      bigint not null references JoinCode(id) on delete cascade,
    clientCodeId    bigint not null references JoinCode(id) on delete cascade,
);

create table if not exists Tile(
    gameId  bigint not null references Game(id) on delete cascade,
    posX    decimal not null,
    posY    decimal not null,
    isX     bit not null,

    unique (gameId, posX, posY)
)

create table if not exists Invite(
    id      bigint identity prime key,
    invite  varchar(50) not null unique,
    gameId  bigint not null references Game(id) on delete cascade
);