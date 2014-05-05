create table PetCoins (
  room varchar(255) not null,
  nick varchar(255) not null,
  amount integer not null
);

alter table PetCoins add primary key (room, nick);