package com.waid.utils;

/**
 * Created by kodjobaah on 10/08/2015.
 */
public class Round {

    public static void main(String[] args) {

        double num = 96.0;

        int val = 0;

        if (num != 0.0) {
            int mod =(int) (num % 90);
            System.out.println("1-"+mod);
            if (mod == 0) {
                //Multiple of 90
                val =(int)((num / 90 ) * 90);
                System.out.println("2-"+val);
            } else {
                int start = (int) (num / 90) * 90;
                int end = (int) ((num / 90) + 1) * 90;

                int lowestDiff = (int) (start - num);
                int highDiff = (int) (end - num);

                if (start == 0) {
                    val = 90;
                } else {
                    if (lowestDiff > highDiff) {
                        val = end;
                    } else {
                        val = start;
                    }
                }
            }

        }
        System.out.println("val["+val+"]");
    }


}
