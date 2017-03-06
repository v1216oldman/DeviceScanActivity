package com.example.bluetooth.le;


/**
 * Created by 03519 on 2017/2/16.
 */

public class CRC {

    public static short calculate_crc(byte[] buf, int size) {

        long ckSum = 0;
        for (int i = 0; i < size - 1; i += 2) {
            ckSum += (((buf[i + 1] << 8) & 0xff00) ^ (buf[i] & 0xff));
        }

        if (size % 2 == 1) {
            ckSum += buf[size - 1] & 0xff;
        }

        ckSum = (ckSum >> 16) + (ckSum & 0xffff);
        ckSum += (ckSum >> 16);
        ckSum=~ckSum;

        return (short) (ckSum);
    }

    //CRC Reverse
    public static long calculate_crc_reverse(byte[] buf, int length) {
        //length = buf.length;
        int i = 0;

        long sum = 0;
        long data;

        while (length > 1) {

            data = (((buf[i] << 8) & 0xFF00) | ((buf[i + 1]) & 0xFF));
            sum += data;
            if ((sum & 0xFFFF0000) > 0) {
                sum = sum & 0xFFFF;
                sum += 1;
            }

            i += 2;
            length -= 2;
        }

        if (length > 0) {
            sum += (buf[i] << 8 & 0xFF00);
            if ((sum & 0xFFFF0000) > 0) {
                sum = sum & 0xFFFF;
                sum += 1;
            }
        }

        sum = ~sum;
        sum = sum & 0xFFFF;
        return sum;

    }


}
