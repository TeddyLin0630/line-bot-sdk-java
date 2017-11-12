package com.example.bot.spring.poker;

/**
 * Created by teddylin on 2017/11/12.
 */
public class Niuniu extends Poker {
    public Niuniu(int num) {
        super(num);
    }

    @Override
    public String getResult () {
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
}
