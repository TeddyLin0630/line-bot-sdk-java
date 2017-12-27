package com.example.bot.spring.poker;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by teddylin on 2017/11/12.
 */
public abstract class Poker {
    ArrayList<Integer> pokerPointSet = new ArrayList<>();
    ArrayList<Integer> imagePathSet = new ArrayList<>();

    public abstract String getResult();

    public abstract int getPokerPoint(int num);

    public abstract void deal(int num);

    public Poker(int num) {
        deal(num);
    }

    public String getPath(int number) {
        return String.valueOf(imagePathSet.get(number));
    }

    public int getPoint(int number) {
        return pokerPointSet.get(number);
    }

    protected int random() {
        Random rand = new Random();
        return rand.nextInt(52) + 1;
    }

}
