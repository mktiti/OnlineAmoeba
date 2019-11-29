create table if not exists Game(
    id          bigint identity primary key,
    lastStored  timestamp(0) not null,
    hostCode    varchar(50) not null,
    clientCode  varchar(50) not null,
    toWin       int not null,

    check (toWin > 2)
);

create table if not exists Tile(
    gameId  bigint not null references Game(id) on delete cascade,
    posX    decimal not null,
    posY    decimal not null,
    isX     bit not null,

    primary key (gameId, posX, posY)
);

create table if not exists Invite(
    id      bigint identity primary key,
    invite  varchar(50) not null unique,
    gameId  bigint not null references Game(id) on delete cascade
);
