package hwid;

import hwid.util.StringUtil;
import hwid.util.Webhook;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Hwid {
    //巨大的 auth 事情 fr
    public static boolean validateHwid(){
        String hwid = getHwid();
        System.out.println(hwid);
        try {
            // 用您自己的网址替换示例
            URL url = new URL("https://raw.iqiq.io/buiawpkgew1e/HWID/main/hwid.json?hwid=" + hwid);
            URLConnection conn = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line=reader.readLine())!=null){
                if (line.contains(hwid))return true;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    // 有人登录时发送不和谐的 webhook
    public static void sendWebhook()throws IOException{
        try {
            // 你的 webhook url，如果你甚至想使用 Discord webhook.
            Webhook webhook=new Webhook("https://discord.com/api/webhooks/1007984093935435796/jXS4udZfPma9ygo8hHPyAduhy73wd0M1Wy1vFFtkJANa_4eEP5ZgJSdKQthBif7IAQSz");
            Webhook.EmbedObject embed =new Webhook.EmbedObject();

            embed.setTitle("hwid");

            embed.setDescription("New login"+getHwid());
            embed.setFooter(getTime(), null);
            webhook.addEmbed(embed);

            if (validateHwid())webhook.execute();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public static String getHwid(){
        StringBuilder returnhwid = new StringBuilder();
        // 您可以使其更加安全，但我现在将使用它.
        String hwid = System.getProperty("user.name")+System.getProperty("user.home") + System.getProperty("os.version") + System.getProperty("os.name");
        for (String s: StringUtil.getSubstrings(hwid)){
            returnhwid.append(StringUtil.convertToString(s));
        }
        return returnhwid.toString();
    }
    public static String getTime(){
        SimpleDateFormat formatter=new SimpleDateFormat("yyyy-dd-MM HH:mm:ss");
        Date date=new Date();
        return (formatter.format(date));
    }
}
