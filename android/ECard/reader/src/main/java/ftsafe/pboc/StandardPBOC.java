package ftsafe.pboc;

import ftsafe.reader.Reader;

/**
 * Created by qingyuan on 16/7/5.
 */
public abstract class StandardPBOC {
    private static Class<?>[][] applets = { };

    public static void readerCard(Reader reader, Class<?> applet) throws Exception {
        if (reader == null || applet == null)
            return;
        // 卡片上电
        reader.powerOn();

        final StandardPBOC app = (StandardPBOC) applet.newInstance();

        // 卡片下电
        reader.powerOff();
    }



}
