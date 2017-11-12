package com.example.bot.spring.poker;

/**
 * Created by teddylin on 2017/11/12.
 */
public class BigOne extends Poker {

    public BigOne(int num) {
        super(num);
    }

    @Override
    public String getResult() {
        int result = 0;
        for (int pokerPoint : pokerpointSet) {
            result = pokerPoint;
        }
        return String.valueOf(result) + "點";
    }
}
