package com.example.bot.spring.poker;

import java.util.Random;

/**
 * Created by teddylin on 2017/11/12.
 */
public class Niuniu extends Poker {
    public Niuniu(int num) {
        deal(num);
    }

    @Override
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

    private int getPockerPoint(int number) {
        int modNum = (number % 13);
        int result = (modNum > 10 || modNum < 1) ? 10 : modNum;
        return result;
    }

    private int random() {
        Random rand = new Random();
        return rand.nextInt(52) + 1;
    }

    public String caculate () {
        String resultText = "沒妞";
        int result;
        int total = 0;

        for (int num : pokerpointSet) {
            total += num;
        }

        for (int i = 0; i < pokerpointSet.size(); i++) {
            int num1 = pokerpointSet.get(i);
            for (int j = i + 1; j < pokerpointSet.size(); j++) {
                int num2 = pokerpointSet.get(j);
                for (int k = j + 1; k < pokerpointSet.size(); k++) {
                    int num3 = pokerpointSet.get(k);
                    int target = num1 + num2 + num3;
                    if (target % 10 == 0) {
//                        System.out.println("hit !! " + num1 +"/"+num2+"/"+num3 );
                        result = ((total - target) % 10);
                        resultText = (result == 0) ? "妞妞" : result + "妞";
                        break;
                    }
                }
            }
        }
        return resultText;
    }

    public String getPath(int number) {
        return String.valueOf(imagePathSet.get(number));
    }

    public int getPoint(int number) {
        return pokerpointSet.get(number);
    }
}
