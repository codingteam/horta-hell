create table Karma (
  id integer auto_increment primary key,
  room varchar(255),
  member varchar(255),
  karma integer
);
create index Karma_room_member on Karma(room, member);
create index Karma_room_karma on Karma(room, karma);