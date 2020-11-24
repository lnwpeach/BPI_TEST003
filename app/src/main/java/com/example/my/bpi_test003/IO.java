package com.example.my.bpi_test003;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class IO{
    static String[] port2 = {"PE19", "PE4","PG12","PG11","PG10"};
    public static void init() {
        Process p= null;
        try {
            p = Runtime.getRuntime().exec("su");
            DataOutputStream cmd = new DataOutputStream(p.getOutputStream());
            cmd.writeBytes("esettings put global captive_portal_detection_enabled 0\n");
            //input
            cmd.writeBytes("echo 0 > /sys/class/gpio_sw/PB2/cfg\n"); //16
            cmd.writeBytes("echo 0 > /sys/class/gpio_sw/PL8/cfg\n"); //18
            cmd.writeBytes("echo 0 > /sys/class/gpio_sw/PL9/cfg\n"); //22
            cmd.writeBytes("echo 0 > /sys/class/gpio_sw/PC3/cfg\n"); //24
            cmd.writeBytes("echo 0 > /sys/class/gpio_sw/PH10/cfg\n"); //26
            cmd.writeBytes("echo 0 > /sys/class/gpio_sw/PE4/cfg\n"); //37

            //output
            cmd.writeBytes("echo 1 > /sys/class/gpio_sw/PE19/cfg\n");   //40
            cmd.writeBytes("echo 1 > /sys/class/gpio_sw/PE18/cfg\n");   //38
            cmd.writeBytes("echo 1 > /sys/class/gpio_sw/PE5/cfg\n");   //36

            //init output
            cmd.writeBytes("echo 1 > /sys/class/gpio_sw/PE19/data\n");
            cmd.writeBytes("echo 0 > /sys/class/gpio_sw/PE18/data\n");
            cmd.writeBytes("echo 0 > /sys/class/gpio_sw/PE5/data\n");

            cmd.flush();
            cmd.close();
            //p.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public static int getLevel(String port) {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("cat " + "/sys/class/gpio_sw/"+ port +"/data");
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while (null != (line = br.readLine())) {
                return Integer.parseInt(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.w("NGT", e.getMessage());
        }
        return -1;
    }
    public static void setlevel(final int s1, final int s2, final int s3, final int s4) {
        final Process[] p = {null};
        new Thread() {
            public void run() {
                try {
                    p[0] = Runtime.getRuntime().exec("su");
                    DataOutputStream cmd = new DataOutputStream(p[0].getOutputStream());
                    cmd.writeBytes("echo "+s1+" > /sys/class/gpio_sw/"+IO.port2[0]+"/data\n");
                    cmd.writeBytes("echo "+s2+" > /sys/class/gpio_sw/"+IO.port2[1]+"/data\n");
                    cmd.writeBytes("echo "+s3+" > /sys/class/gpio_sw/"+IO.port2[2]+"/data\n");
                    cmd.writeBytes("echo "+s4+" > /sys/class/gpio_sw/"+IO.port2[3]+"/data\n");
                    cmd.flush();
                    cmd.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public static void setlevel(final String port, final int lv) {
        final Process[] p = new Process[1];
        new Thread() {
            public void run() {
                try {
                    p[0] = Runtime.getRuntime().exec("su");
                    DataOutputStream cmd = new DataOutputStream(p[0].getOutputStream());
                    cmd.writeBytes("echo "+lv+" > /sys/class/gpio_sw/"+port+"/data\n");
                    cmd.flush();
                    cmd.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
