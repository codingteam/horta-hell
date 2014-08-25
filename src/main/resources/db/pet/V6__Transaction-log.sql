create table PetTransaction (
  room varchar(255) primary key,
  nickname varchar(255),
  time timestamp,
  change integer,
  reason varchar(255)
);

create index PetTransaction_room_nickname on PetTransaction(room, nickname);
create index PetTransaction_time on PetTransaction(time);