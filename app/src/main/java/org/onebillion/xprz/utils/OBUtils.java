package org.onebillion.xprz.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageButton;
import android.widget.ImageView;

import org.onebillion.xprz.controls.OBControl;
import org.onebillion.xprz.controls.OBGroup;
import org.onebillion.xprz.controls.OBImage;
import org.onebillion.xprz.mainui.MainActivity;

public class OBUtils
{
    public static String lastPathComponent(String path)
    {
        int idx = path.lastIndexOf("/");
        if (idx == -1)
            return path;
        if (idx == path.length() - 1)
            return lastPathComponent(path.substring(0, path.length() - 1));
        return path.substring(idx + 1, path.length());
    }

    public static String stringByDeletingLastPathComponent(String path)
    {
        int idx = path.lastIndexOf("/");
        if (idx == -1)
            return path;
        if (idx == path.length() - 1)
            return stringByDeletingLastPathComponent(path.substring(0, path.length() - 1));
        return path.substring(0, idx);
    }

    public static String stringByAppendingPathComponent(String path, String component)
    {
        int idx = path.lastIndexOf("/");
        if (idx == path.length() - 1)
            return path + component;
        return path + "/" + component;
    }

    public static String pathExtension(String path)
    {
        String lastcomp = lastPathComponent(path);
        int idx = lastcomp.lastIndexOf(".");
        if (idx == -1)
            return "";
        if (idx == lastcomp.length() - 1)
            return "";
        return lastcomp.substring(idx + 1, lastcomp.length());
    }

    public static Boolean assetsDirectoryExists(String path)
    {
        AssetManager am = MainActivity.mainActivity.getAssets();
        try
        {
            String files[] = am.list(path);
        }
        catch (IOException e)
        {
            return false;
        }
        return true;
    }

    public static List<String> filesAtPath(String path)
    {
        AssetManager am = MainActivity.mainActivity.getAssets();
        try
        {
            String lst[] = am.list(path);
            if (lst != null)
            {
                return Arrays.asList(lst);
            }
        }
        catch (IOException e)
        {
        }
        return Collections.emptyList();
    }

    public static Boolean fileExistsAtPath(String path)
    {
        AssetManager am = MainActivity.mainActivity.getAssets();
        try
        {
            InputStream pis = am.open(path);
            return (pis != null);
        }
        catch (IOException e)
        {
            //e.printStackTrace();
        }
        return false;
    }

    public static void getFloatColour(int col,float outcol[])
    {
        outcol[0] = Color.red(col)/255f;
        outcol[1] = Color.green(col)/255f;
        outcol[2] = Color.blue(col)/255f;
        outcol[3] = Color.alpha(col)/255f;
    }
    public static void setFloatColour(float r,float g,float b,float a,float outcol[])
    {
        outcol[0] = r;
        outcol[1] = g;
        outcol[2] = b;
        outcol[3] = a;
    }
    public static int parseColourComponent(String comp)
    {
        int i = comp.indexOf("%");
        if (i == 0)
            return 0;
        if (i > -1)
        {
            float f = Float.parseFloat(comp.substring(0, i - 1));
            return (int) Math.round(f / 100f * 255f);
        }
        i = comp.indexOf(".");
        if (i > -1)
        {
            float f = Float.parseFloat(comp);
            return (int) Math.round(f * 255f);
        }
        return Integer.parseInt(comp);
    }

    public static int colorFromRGBString(String colstr)
    {
        String strings[] = colstr.split(",");
        return Color.argb(255, parseColourComponent(strings[0]), parseColourComponent(strings[1]), parseColourComponent(strings[2]));
    }

    public static int svgColorFromRGBString(String str)
    {
        if (str.equals("none"))
            return 0;
        if (str.length() < 4)
            return 0;
        if (str.startsWith("#"))
        {
            str = str.substring(1).toLowerCase();
            if (str.length() == 3)
            {
                int rgb[] = {0, 0, 0};
                for (int i = 0; i < 3; i++)
                {
                    int ch = str.codePointAt(i);
                    int val = 0;
                    if (Character.isDigit(ch))
                        val = ch - '0';
                    else if (ch >= 'a' && ch <= 'f')
                        val = ch - 'a' + 10;
                    rgb[i] = val;
                }
                return Color.argb(255, Math.round(rgb[0] / 15f * 255f), Math.round(rgb[1] / 15f * 255f), Math.round(rgb[2] / 15f * 255f));
            }
            else if (str.length() == 6)
            {
                int rrggbb[] = {0, 0, 0, 0, 0, 0};
                for (int i = 0; i < 6; i++)
                {
                    int ch = str.codePointAt(i);
                    int val = 0;
                    if (Character.isDigit(ch))
                        val = ch - '0';
                    else if (ch >= 'a' && ch <= 'f')
                        val = ch - 'a' + 10;
                    rrggbb[i] = val;
                }
                return Color.argb(255, (rrggbb[0] * 16 + rrggbb[1]), (rrggbb[2] * 16 + rrggbb[3]), (rrggbb[4] * 16 + rrggbb[5]));
            }
        }
        else if (str.startsWith("rgb(") && str.substring(str.length() - 1).equals(")"))
        {
            str = str.substring(4, str.length() - 1);
            String components[] = str.split(",");
            if (components.length == 3)
            {
                int rgb[] = {0, 0, 0};
                for (int i = 0; i < 3; i++)
                {
                    String comp = components[i];
                    float f;
                    if (comp.endsWith("%"))
                        f = Float.parseFloat(comp.substring(0, comp.length() - 1)) / 100f * 255f;
                    else
                        f = Float.parseFloat(comp);
                    rgb[i] = Math.round(f);
                }
                return Color.argb(255, rgb[0], rgb[1], rgb[2]);
            }
        }
        return 0;
    }

    public static PointF pointFromString(String str)
    {
        String strings[] = str.split(",");
        return new PointF(Float.parseFloat(strings[0]), Float.parseFloat(strings[1]));
    }

    public static Point roundPoint(PointF ptf)
    {
        return new Point((int) ptf.x, (int) ptf.y);
    }


    public static InputStream getConfigStream(String cfgName)
    {
        Map<String, Object> config = MainActivity.mainActivity.config;
        @SuppressWarnings("unchecked")
        List<String> searchPaths = (List<String>) config.get(MainActivity.CONFIG_CONFIG_SEARCH_PATH);
        AssetManager am = MainActivity.mainActivity.getAssets();
        for (String path : searchPaths)
        {
            String fullpath = path + "/" + cfgName;
            try
            {
                InputStream is = am.open(fullpath);
                return is;
            }
            catch (IOException e)
            {
            }
        }
        return null;
    }


    public static OBImage buttonFromImageName(String imageName)
    {
        OBImage im  = OBImageManager.sharedImageManager().imageForName(imageName);
        float imageScale = MainActivity.mainActivity.configFloatForKey(MainActivity.CONFIG_GRAPHIC_SCALE);
        im.setScale(imageScale);
        return im;
    }

    public static OBControl buttonFromSVGName(String imageName)
    {
        OBGroup im  = OBImageManager.sharedImageManager().vectorForName(imageName);
        float imageScale = MainActivity.mainActivity.configFloatForKey(MainActivity.CONFIG_GRAPHIC_SCALE);
        im.setScale(imageScale);
        im.setRasterScale(imageScale);
        return im;
    }

    public static int PresenterColourIndex()
    {
        return (Integer)MainActivity.Config().get(MainActivity.CONFIG_SKINCOLOUR);
    }

    public static int SkinColour(int offset)
    {
        @SuppressWarnings("unchecked")
        List<Integer> colList = (List<Integer>) MainActivity.mainActivity.config.get(MainActivity.CONFIG_SKINCOLOURS);
        return colList.get(Math.abs(9 - (((PresenterColourIndex() + offset) + 8) % 18)));
    }

    public static int SkinColourIndex()
    {
        return ((Integer) MainActivity.mainActivity.config.get(MainActivity.CONFIG_SKINCOLOUR)).intValue();
    }

    public static List<String> stringSplitByCharType(String str)
    {
        List<String> arr = new ArrayList<String>();
        if (str.length() > 0)
        {
            int idx = 1, startindex = 0;
            while (idx < str.length())
            {
                while (idx < str.length() && Character.isDigit(str.charAt(idx)) == Character.isDigit(str.charAt(idx - 1)))
                    idx++;
                if (idx > startindex)
                    arr.add(str.substring(startindex, idx));
                startindex = idx;
                idx++;
            }
            if (startindex < str.length())
                arr.add(str.substring(startindex));
        }
        return arr;
    }

    static boolean isInteger(String s)
    {
        try
        {
            int v1 = Integer.parseInt(s);
            return true;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    public static int orderStringArray(List<String> a1, List<String> a2)
    {
        for (int idx = 0; true; idx++)
        {
            if (idx >= a1.size())
            {
                if (idx >= a2.size())
                    return 0;
                else
                    return -1;
            }
            if (idx >= a2.size())
            {
                return 1;
            }
            String s1 = a1.get(idx), s2 = a2.get(idx);
            int res;
            if (isInteger(s1) && isInteger(s2))
            {
                int v1 = Integer.parseInt(s1);
                int v2 = Integer.parseInt(s2);
                if (v1 < v2)
                    res = -1;
                else if (v1 > v2)
                    res = 1;
                else
                    res = 0;
            }
            else
                res = s1.compareToIgnoreCase(s2);
            if (res != 0)
                return res;
        }
    }

    public static int caseInsensitiveCompareWithNumbers(String s1, String s2)
    {
        return orderStringArray(stringSplitByCharType(s1), stringSplitByCharType(s2));
    }

    public static String StrAndNo(String s, int n)
    {
        if (n == 1)
            return s;
        return s + n;
    }

    public static float floatOrPercentage(String str)
    {
        str = str.trim();
        if (str.length() == 0)
            return 0;
        boolean ispc = false;
        if (str.substring(str.length() - 1).equals("%"))
        {
            ispc = true;
            str = str.substring(0, str.length() - 1);
        }
        float f = Float.parseFloat(str);
        if (ispc)
            f = f / 100;
        return f;
    }

    public static Typeface standardTypeFace()
    {
        if (MainActivity.standardTypeFace == null)
            MainActivity.standardTypeFace = Typeface.createFromAsset(MainActivity.mainActivity.getAssets(), "fonts/Heinemann Collection - HeinemannSpecial-Roman.otf");
        return MainActivity.standardTypeFace;
    }

    public static int setColourOpacity(int colour, float opacity)
    {
        int intop = Math.round(opacity * 255f);
        colour = colour | (intop << 24);
        return colour;
    }
    public static int applyColourOpacity(int colour, float opacity)
    {
        if (opacity == 1)
            return colour;
        int opac = Color.alpha(colour);
        float fopac = opac / 255f;
        fopac = fopac * opacity;
        int intop = Math.round(fopac * 255f);
        //colour = colour | (intop << 24);
        colour = Color.argb(intop,Color.red(colour),Color.green(colour),Color.blue(colour));
        return colour;
    }

    public static float durationForPointDist(PointF p0,PointF p1,float speed)
    {
        return OB_Maths.PointDistance(p0,p1)/speed;
    }

    public static <T> List<T> randomlySortedArray(List<T> sofar,List<T> inarray)
    {
        if (inarray.size() == 0)
            return sofar;
        if (inarray.size() == 1)
        {
            sofar.add(inarray.get(0));
            return sofar;
        }
        int idx = OB_Maths.randomInt(0, (int) inarray.size() - 1);
        T obj = inarray.get(idx);
        inarray.remove(idx);
        sofar.add(obj);
        return randomlySortedArray(sofar,inarray);
    }

    public static <T> List<T> randomlySortedArray(List<T> inarray)
    {
        return randomlySortedArray(new ArrayList<T>(), new ArrayList<T>(inarray));
    }

    public static String readTextFileFromResource(int resourceId)
    {
        Context context = MainActivity.mainActivity;
        StringBuilder body = new StringBuilder();

        try
        {
            InputStream inputStream = context.getResources().openRawResource(resourceId);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String nextLine;

            while ((nextLine = bufferedReader.readLine()) != null)
            {
                body.append(nextLine);
                body.append('\n');
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(
                    "Could not open resource: " + resourceId, e);
        }
        catch (Resources.NotFoundException nfe)
        {
            throw new RuntimeException("Resource not found: " + resourceId, nfe);
        }

        return body.toString();
    }

    public static float scaleFromTransform(Matrix t)
    {
        float values[] = new float[9];
        t.getValues(values);
        float a = values[0];
        float b = values[3];
        float c = values[1];
        float d = values[4];
        float sx = (float)Math.sqrt(a*a + c * c);
        float sy = (float)Math.sqrt(b*b + d * d);
        return Math.max(sx,sy);
    }

    public static List<Object> insertAudioInterval(Object audios, float interval)
    {
        List<Object> arr = new ArrayList<>();
        //
        if (audios instanceof String)
        {
            String audioFile = (String ) audios;
            arr.add(audioFile);
        }
        else
        {
            List<String> ls = (List<String>) audios;
            for(String audio : ls)
            {
                arr.add(audio);
                if(ls.get(ls.size()-1) != audio)
                {
                    arr.add(interval);
                }
            }
        }
        return arr;
    }

    public static void runOnMainThread(final RunLambda lamb)
    {
         new OBRunnableSyncUI() {
            @Override
            public void ex() {
                try {
                    lamb.run();
                }
                catch (Exception exception)
                {
                }
            }
        }.run();
    }

    public static void runOnOtherThread(final RunLambda lamb)
    {
        new AsyncTask<Void, Void,Void>()
        {
            protected Void doInBackground(Void... params) {
                try
                {
                    lamb.run();
                }
                catch (Exception exception)
                {
                }
                return null;
            }}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
    }

    public static void runOnOtherThreadDelayed(final float delay, final RunLambda lamb)
    {
        new AsyncTask<Void, Void,Void>()
        {
            protected Void doInBackground(Void... params) {
                try
                {
                    Thread.sleep(Math.round(delay*1000));
                    lamb.run();
                }
                catch (Exception exception)
                {
                }
                return null;
            }}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
    }

    public interface RunLambda {
         public void run() throws Exception;
    }

    public static Path SimplePath(PointF from,PointF to,float offset)
    {
        Path path = new Path();
        path.moveTo(from.x,from.y);
        PointF c1 = OB_Maths.tPointAlongLine(0.33f, from, to);
        PointF c2 = OB_Maths.tPointAlongLine(0.66f, from, to);
        PointF lp = OB_Maths.ScalarTimesPoint(offset,OB_Maths.NormalisedVector(OB_Maths.lperp(OB_Maths.DiffPoints(to,from))));
        PointF cp1 = OB_Maths.AddPoints(c1, lp);
        PointF cp2 = OB_Maths.AddPoints(c2, lp);
        path.cubicTo(cp1.x,cp1.y,cp2.x,cp2.y,to.x,to.y);
        return path;
    }

}