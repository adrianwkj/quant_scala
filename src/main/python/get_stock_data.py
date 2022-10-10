# -*- coding: utf-8 -*-
# @Author  : Jason Woo
# @Time    : 2022/9/26 11:32
# @File    : get_stock_data.py
# @Description :

import akshare as ak
import pandas as pd
import datetime
from sqlalchemy import create_engine


def stock_list():
    stock_zh_a_spot_em_df = ak.stock_zh_a_spot_em()
    stock_zh_a_spot_em_df.rename(columns={'代码': 'symbol'}, inplace=True)
    return stock_zh_a_spot_em_df[['symbol']]


def download_and_insert(symbol, latest_trade_date, conn):
    now = datetime.datetime.now()
    today = datetime.date.today()
    if now.hour <= 16:
        today = today - datetime.timedelta(days=1)
    today_str = today.strftime("%Y%m%d")
    start_date = (today - datetime.timedelta(days=365))

    if latest_trade_date is not None:
        if latest_trade_date >= today:
            return 0
        else:
            start_date = (latest_trade_date + datetime.timedelta(days=1))

    start_date_str = start_date.strftime("%Y%m%d")

    stock_zh_a_hist_df = ak.stock_zh_a_hist(symbol=symbol,
                                            period="daily",
                                            start_date=start_date_str,
                                            end_date=today_str,
                                            adjust="hfq")

    if len(stock_zh_a_hist_df.index != 0):
        stock_zh_a_hist_df.rename(columns={'日期': 'trade_date',
                                           '开盘': 'open',
                                           '收盘': 'close',
                                           '最高': 'high',
                                           '最低': 'low',
                                           '成交量': 'volume',
                                           '成交额': 'turnover',
                                           '振幅': 'amplitude',
                                           '涨跌幅': 'change_percent',
                                           '涨跌额': 'change',
                                           '换手率': 'turnover_rate'
                                           }, inplace=True)

        stock_zh_a_hist_df.insert(loc=1, column='symbol', value=symbol)
        stock_zh_a_hist_df.to_sql(name='stock_hist', con=conn, index=False, if_exists='append')
        print("symbol: " + symbol + " " + today_str + " inserted!")
        return 1


if __name__ == '__main__':
    conn = create_engine('postgresql+psycopg2://postgres:postgrespw@localhost:55000/stock')

    sl = stock_list()
    sl.set_index('symbol', inplace=True)

    current_sl = pd.read_sql("select symbol, max(trade_date) as trade_date from stock_hist group by symbol", conn)
    current_sl.set_index('symbol', inplace=True)

    final_sl = sl.join(current_sl, on='symbol', how='left')
    final_sl['symbol'] = final_sl.index
    final_sl = final_sl.where(final_sl.notnull(), None)
    final_sl.apply(lambda x: download_and_insert(x['symbol'], x['trade_date'], conn), axis=1)

    print("Get Data Success!")
