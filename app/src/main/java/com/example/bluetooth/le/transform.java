package com.example.bluetooth.le;


import java.util.Scanner;

/**
 * Created by 03519 on 2017/2/16.
 */

public class transform {

    public static String reverse(String originalStr){

        String resultStr = "";

        for(int i = originalStr.length() - 1 ; i >= 0 ; i--){

            resultStr = resultStr + originalStr.charAt(i);

        }

        return resultStr;

    }



    // hex to byte
    public static byte[] hexToBytes(String hexString) {

        char[] hex = hexString.toCharArray();
        //轉rawData長度減半
        int length = hex.length / 2;
        byte[] rawData = new byte[length];
        for (int i = 0; i < length; i++) {
            //先將hex資料轉10進位數值
            int high = Character.digit(hex[i * 2], 16);
            int low = Character.digit(hex[i * 2 + 1], 16);
            //將第一個值的二進位值左平移4位,ex: 00001000 => 10000000 (8=>128)
            //然後與第二個值的二進位值作聯集ex: 10000000 | 00001100 => 10001100 (137)
            int value = (high << 4) | low;
            //與FFFFFFFF作補集
            if (value > 127)
                value -= 256;
            //最後轉回byte就OK
            rawData [i] = (byte) value;
        }
        return rawData ;
    }

    public static String getStringToHex(String strValue) {
        byte byteData[] = null;
        int intHex = 0;
        String strHex = "";
        String strReturn = "";
        try {
            byteData = strValue.getBytes("ISO8859-1");
            for (int intI=0;intI<byteData.length;intI++)
            {
                intHex = (int)byteData[intI];
                if (intHex<0)
                    intHex += 256;
                if (intHex<16)
                    strHex += "0" + Integer.toHexString(intHex).toUpperCase();
                else
                    strHex += Integer.toHexString(intHex).toUpperCase();
            }
            strReturn = strHex;

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return strReturn;
    }

    public static String getHexToString(String strValue) {
        int intCounts = strValue.length() / 2;
        String strReturn = "";
        String strHex = "";
        int intHex = 0;
        byte byteData[] = new byte[intCounts];
        try {
            for (int intI = 0; intI < intCounts; intI++) {
                strHex = strValue.substring(0, 2);
                strValue = strValue.substring(2);
                intHex = Integer.parseInt(strHex, 16);
                if (intHex > 128)
                    intHex = intHex - 256;
                byteData[intI] = (byte) intHex;
            }
            strReturn = new String(byteData,"ISO8859-1");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return strReturn;
    }



}
