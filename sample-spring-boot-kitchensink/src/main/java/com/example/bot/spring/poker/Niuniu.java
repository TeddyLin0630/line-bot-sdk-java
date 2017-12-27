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

        for (int num : pokerPointSet) {
            total += num;
        }

        for (int i = 0; i < pokerPointSet.size(); i++) {
            int num1 = pokerPointSet.get(i);
            for (int j = i + 1; j < pokerPointSet.size(); j++) {
                int num2 = pokerPointSet.get(j);
                for (int k = j + 1; k < pokerPointSet.size(); k++) {
                    int num3 = pokerPointSet.get(k);
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

    @Override
    public void deal(int num) {
        for (int i = 0; i < num; i++) {
            int randomNum;
            while (true) {
                randomNum = random();
                if (pokerPointSet.contains(randomNum)) {
                    continue;
                } else {
                    break;
                }
            }
            imagePathSet.add(i, randomNum);
            pokerPointSet.add(i, getPokerPoint(randomNum));
        }
    }

    @Override
    public int getPokerPoint(int number) {
        int modNum = (number % 13);
        int result = (modNum > 10 || modNum < 1) ? 10 : modNum;
        return result;
    }
}
