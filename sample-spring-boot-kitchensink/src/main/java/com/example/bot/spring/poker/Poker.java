package com.example.bot.spring.poker;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by teddylin on 2017/11/12.
 */
public abstract class Poker {
    ArrayList<Integer> pokerpointSet = new ArrayList<>();
    ArrayList<Integer> imagePathSet = new ArrayList<>();

    public abstract String getResult();

    public Poker(int num) {
        deal(num);
    }

    public void deal(int num) {
        for (int i = 0; i < num; i++) {
            int randomNum;
            while (true) {
                randomNum = random();
                if (pokerpointSet.contains(randomNum)) {
                    continue;
                } else {
                    break;
                }
            }
            imagePathSet.add(i, randomNum);
            pokerpointSet.add(i, getPockerPoint(randomNum));
        }
    }

    public String getPath(int number) {
        return String.valueOf(imagePathSet.get(number));
    }

    public int getPoint(int number) {
        return pokerpointSet.get(number);
    }

    private int getPockerPoint(int number) {
        int modNum = (number % 13);
        int result = (modNum > 10 || modNum < 1) ? 10 : modNum;
        return result;
    }

    private int random() {
        Random rand = new Random();
        return rand.nextInt(52) + 1;
    }

}
