package com.example.bot.spring.poker;

import java.util.ArrayList;

/**
 * Created by teddylin on 2017/11/12.
 */
public abstract class Poker {
    ArrayList<Integer> pokerpointSet = new ArrayList<>();
    ArrayList<Integer> imagePathSet = new ArrayList<>();

    public abstract void deal(int num);
}
