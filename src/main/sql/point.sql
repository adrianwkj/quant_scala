create table point(
    id serial primary key,
    symbol text,
    circle text,
    trade_time timestamp,
    price float,
    bid_ask text
)
