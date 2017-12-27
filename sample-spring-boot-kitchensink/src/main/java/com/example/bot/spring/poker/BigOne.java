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
        for (int pokerPoint : pokerPointSet) {
            result = pokerPoint;
        }
        return String.valueOf(result) + "點";
    }

    @Override
    public void deal(int num) {
        int randomNum = random();
        imagePathSet.add(randomNum);
        pokerPointSet.add(getPokerPoint(randomNum));
    }

    @Override
    public int getPokerPoint(int number) {
        int modNum = (number % 13);
        int result = (modNum < 1) ? 13 : modNum;
        return result;
    }
}
