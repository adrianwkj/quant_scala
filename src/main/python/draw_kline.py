import numpy as np
import pandas as pd
import matplotlib as mpl
import matplotlib.pyplot as plt
import mplfinance as mpf
from sqlalchemy import create_engine

if __name__ == '__main__':
    conn = create_engine('postgresql+psycopg2://postgres:postgrespw@localhost:55000/stock')

    symbol = '601668'

    circle = 'Day'

    data = pd.read_sql(
        "select symbol, trade_date, open, close, high, low, volume from stock_hist where symbol = '%s'" % symbol, conn)
    data.set_index('trade_date', inplace=True)
    data.index = pd.DatetimeIndex(data.index)

    points_data = pd.read_sql(
        "select trade_date, price from stroke where symbol = '%s' and circle = '%s'" % (symbol, circle), conn
    )

    points_trade_date = points_data['trade_date'].values
    points_price = points_data['price'].values
    # points = list(map(lambda x: list(zip(points_trade_date, x)), points_price))
    points = list(zip(map(str, map(np.datetime_as_string, points_trade_date)), points_price))

    mc_data = pd.read_sql(
        "select start_time, end_time from middlecenter where symbol = '%s' and circle = '%s'" % (symbol, circle), conn
    )
    start_times = mc_data['start_time'].values
    end_times = mc_data['end_time'].values
    start_times_str = list(map(str, map(np.datetime_as_string, start_times)))
    end_times_str = list(map(str, map(np.datetime_as_string, end_times)))
    start_times_str.extend(end_times_str)
    print(start_times_str)
    # mcs = list(zip(map(str, map(np.datetime_as_string, start_times)), map(str, map(np.datetime_as_string, end_times))))

    # print(data.head)

    # 设置基本参数
    # type:绘制图形的类型，有candle, renko, ohlc, line等
    # 此处选择candle,即K线图
    # mav(moving average):均线类型,此处设置7,30,60日线
    # volume:布尔类型，设置是否显示成交量，默认False
    # title:设置标题
    # y_label:设置纵轴主标题
    # y_label_lower:设置成交量图一栏的标题
    # figratio:设置图形纵横比
    # figscale:设置图形尺寸(数值越大图像质量越高)
    kwargs = dict(
        type='candle',
        mav=(7, 30, 60),
        volume=True,
        title='candle_line',
        ylabel='OHLC Candles',
        ylabel_lower='Shares\nTraded Volume',
        figratio=(15, 10),
        figscale=5)

    # 设置marketcolors
    # up:设置K线线柱颜色，up意为收盘价大于等于开盘价
    # down:与up相反，这样设置与国内K线颜色标准相符
    # edge:K线线柱边缘颜色(i代表继承自up和down的颜色)，下同。详见官方文档)
    # wick:灯芯(上下影线)颜色
    # volume:成交量直方图的颜色
    # inherit:是否继承，选填
    mc = mpf.make_marketcolors(
        up='red',
        down='green',
        edge='i',
        wick='i',
        volume='in',
        inherit=True)

    s = mpf.make_mpf_style(
        gridaxis='both',
        gridstyle='-.',
        y_on_right=False,
        marketcolors=mc)
    # 设置图形风格
    # gridaxis:设置网格线位置
    # gridstyle:设置网格线线型
    # y_on_right:设置y轴位置是否在右

    # 设置均线颜色，配色表可见下图
    # 建议设置较深的颜色且与红色、绿色形成对比
    # 此处设置七条均线的颜色，也可应用默认设置
    mpl.rcParams['axes.prop_cycle'] = mpl.cycler(
        color=['dodgerblue', 'deeppink',
               'navy', 'teal', 'maroon', 'darkorange',
               'indigo'])

    # 设置线宽
    mpl.rcParams['lines.linewidth'] = .5

    # 图形绘制
    # show_nontrading:是否显示非交易日，默认False
    # savefig:导出图片，填写文件名及后缀
    mpf.plot(data,
             **kwargs,
             alines=dict(
                 alines=[points],
                 colors=['r', 'blue'],
                 linestyle=['-', '--'],
                 linewidths=[1, 2],
             ),
             vlines=dict(
                 vlines=start_times_str,
                 alpha=0.4
             ),
             style=s,
             show_nontrading=False,
             savefig='../../../dra.png')



    # mpf.show()
