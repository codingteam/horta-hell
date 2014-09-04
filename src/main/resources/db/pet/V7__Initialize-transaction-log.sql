insert into PetTransaction (room, nickname, time, change, reason)
select room, nick, now(), amount, 'initial transaction' from PetCoins;