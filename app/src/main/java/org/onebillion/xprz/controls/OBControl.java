package org.onebillion.xprz.controls;


        import java.lang.reflect.Constructor;
        import java.util.ArrayList;
        import java.util.Collections;
        import java.util.Comparator;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Map;

        import android.graphics.*;
        import android.graphics.Matrix;
        import android.opengl.*;
        import android.renderscript.Allocation;
        import android.renderscript.RenderScript;
        import android.renderscript.ScriptIntrinsicBlur;
        import android.util.Log;

        import org.onebillion.xprz.glstuff.ColorShaderProgram;
        import org.onebillion.xprz.glstuff.GradientRect;
        import org.onebillion.xprz.glstuff.OBRenderer;
        import org.onebillion.xprz.glstuff.Texture;
        import org.onebillion.xprz.glstuff.TextureRect;
        import org.onebillion.xprz.glstuff.TextureShaderProgram;
        import org.onebillion.xprz.mainui.MainActivity;
        import org.onebillion.xprz.mainui.OBSectionController;
        import org.onebillion.xprz.mainui.OBViewController;
        import org.onebillion.xprz.utils.OBRunnableSyncUI;
        import org.onebillion.xprz.utils.OB_Maths;
        import org.onebillion.xprz.utils.OBUtils;

public class OBControl
{
    public static int LCC_NORMAL = 0,
            LCC_DISABLED = 1,
            LCC_SELECTED = 2;

    public int state;
    public Map<String,Object> settings;
    public OBLayer layer;
    public PointF anchorPoint;
    public RectF frame,bounds;
    public Boolean hidden,animationsDisabled;
    public long animationKey;
    public Matrix drawMatrix,convertMatrix;
    public Bitmap cache,shadowCache;
    public float scaleX,scaleY,rotation,yRotation,borderWidth;
    public float invalOutdent;
    public OBGroup parent;
    public int backgroundColor,highlightColour,borderColour;
    public int tempSortInt;
    public OBControl maskControl;
    public OBViewController controller;
    public String textureKey;
    public Texture texture;
    public float[] modelMatrix = new float[16];
    public float[] tempMatrix = new float[16];
    public float blendColour[] = {1,1,1,1};
    public float m34,cornerRadius;
    public boolean doubleSided=true;
    public boolean shouldTexturise=true;
    PointF position;
    PointF tempPoint;
    RectF tempRect = new RectF();
    float rasterScale,zPosition;
    OBStroke stroke;
    boolean frameValid,masksToBounds;
    int shadowColour;
    float shadowOffsetX,shadowOffsetY,shadowOpacity,shadowRadius;
    boolean needsRetexture;

    public OBControl()
    {
        settings = new HashMap<String, Object>();
        position = new PointF();
        anchorPoint = new PointF(0.5f,0.5f);
        frame = new RectF();
        frameValid = false;
        bounds = new RectF();
        tempPoint = new PointF();
        animationsDisabled = false;
        hidden = false;
        drawMatrix = new Matrix();
        convertMatrix = new Matrix();
        scaleX = 1;
        scaleY = 1;
        zPosition = 0;
        backgroundColor = 0;
        highlightColour = 0;
        borderWidth = 0;
        borderColour = 0;
        invalOutdent = 0;
        setOpacity(1);
        rasterScale = 1;
        layer = new OBLayer();
    }

    public static List<OBControl> controlsSortedFrontToBack(List<OBControl> controls)
    {
        List<OBControl> arr = new ArrayList<>(controls);
        Collections.sort(arr, new Comparator<OBControl>(){
            public int compare(OBControl c1, OBControl c2) {
                if (c1.zPosition() == c2.zPosition())
                    return 0;
                if (c1.zPosition() < c2.zPosition())
                    return 1;
                return -1;
            }
        });
        return arr;
    }

    public boolean shouldTexturise()
    {
        return shouldTexturise;
    }

    public void setShouldTexturise(boolean shouldTexturise)
    {
        this.shouldTexturise = shouldTexturise;
    }

    public OBControl copy()
    {
        OBControl obj;
        try
        {
            Constructor<?> cons;
            cons = getClass().getConstructor();
            obj = (OBControl)cons.newInstance();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
        obj.state = state;
        obj.settings.putAll(settings);
        obj.layer = layer.copy();
        obj.bounds.set(bounds);
        obj.frame.set(frame);
        obj.position.set(position);
        obj.hidden = hidden;
        obj.animationsDisabled = animationsDisabled;
        obj.rotation = rotation;
        obj.yRotation = yRotation;
        obj.zPosition = zPosition;
        obj.scaleX = scaleX;
        obj.scaleY = scaleY;
        obj.backgroundColor = backgroundColor;
        obj.setOpacity(opacity());
        obj.borderColour = borderColour;
        obj.borderWidth = borderWidth;
        obj.masksToBounds = masksToBounds;
        obj.rasterScale = rasterScale;
        obj.shadowRadius = shadowRadius;
        obj.shadowOpacity = shadowOpacity;
        obj.shadowOffsetX = shadowOffsetX;
        obj.shadowOffsetY = shadowOffsetY;
        obj.shadowColour = shadowColour;
        obj.textureKey = textureKey;
        obj.blendColour = blendColour.clone();
        if (maskControl != null)
            obj.maskControl = maskControl;
        obj.m34 = m34;
        return obj;
    }

    public void enable()
    {
        state = LCC_NORMAL;
    }

    public boolean isEnabled()
    {
        return (state == LCC_NORMAL);
    }

    public void disable()
    {
        state = LCC_DISABLED;
    }

    public void select()
    {
        state = LCC_SELECTED;
    }

    public float opacity()
    {
        if (layer != null)
            return layer.opacity();
        return 1.0f;
    }

    public void setOpacity(float f)
    {
        if (layer != null)
            layer.setOpacity(f);
        invalidate();
    }

    public RectF bounds()
    {
        bounds.set(layer.bounds());
        return bounds;
    }

    public void setBounds(RectF b)
    {
        setBounds(b.left, b.top, b.right, b.bottom);
    }
    public void setBounds(float l,float t,float r,float b)
    {
        bounds.set(l, t, r, b);
        if (layer != null)
            layer.setBounds(bounds);
    }
    public RectF frame()
    {
        frame.set(bounds());
        Matrix m = matrixForBackwardConvert();
        m.mapRect(frame);
        frameValid = true;
        return frame;
    }

    public void setFrame(RectF f)
    {
        if (f != frame)
            frame.set(f);
        position.set(OB_Maths.midPoint(frame));
        setBounds(0, 0, (f.right - f.left), (f.bottom - f.top));
        invalidate();
        setNeedsRetexture();
    }

    public void setFrame(float left,float top,float right,float bottom)
    {
        frame.left = left;
        frame.top = top;
        frame.right = right;
        frame.bottom = bottom;
        setFrame(frame);
    }

    public void setNeedsRetexture()
    {
        needsRetexture = true;
    }

    public PointF position()
    {
        return position;
    }

    public void setPosition(PointF pt)
    {
        setPosition(pt.x, pt.y);
    }
    public void setPosition(final float x,final float y)
    {
        if (position.x != x || position.y != y)
        {
            new OBRunnableSyncUI()
            {
                public void ex()
                {
                    invalidate();
                    position.set(x, y);
                    frameValid = false;
                    invalidate();
                }
            }.run();
         }
    }

    public void setRight(float rt)
    {
        frame();
        float diff = rt - frame.right;
        setPosition(position.x + diff, position.y);
    }

    public float right()
    {
        return frame().right;
    }

    public void setLeft(float lt)
    {
        frame();
        float diff = lt - frame.left;
        setPosition(position.x + diff, position.y);
    }

    public float left()
    {
        frame.set(frame());
        return frame.left;
    }

    public void setTop(float tp)
    {
        frame();
        float diff = tp - frame.top;
        setPosition(position.x, diff + position.y);
    }

    public float top()
    {
        return frame().top;
    }

    public void setBottom(float bt)
    {
        frame();
        float diff = bt - frame.bottom;
        setPosition(position.x, diff + position.y);
    }

    public float bottom()
    {
        return frame().bottom;
    }

    public PointF bottomPoint()
    {
        frame();
        return new PointF((frame.left + frame.right) / 2,frame.bottom);
    }

    public PointF bottomRight()
    {
        frame();
        return new PointF(frame.right,frame.bottom);
    }

    public PointF bottomLeft()
    {
        frame();
        return new PointF(frame.left,frame.bottom);
    }

    public PointF topPoint()
    {
        frame();
        return new PointF((frame.left + frame.right) / 2,frame.top);
    }

    public PointF topRight()
    {
        frame();
        return new PointF(frame.right,frame.top);
    }

    public PointF topLeft()
    {
        frame();
        return new PointF(frame.left,frame.top);
    }

    public PointF rightPoint()
    {
        frame();
        return new PointF(frame.right,frame.centerY());
    }

    public PointF leftPoint()
    {
        frame();
        return new PointF(frame.left,frame.centerY());
    }

    public float zPosition()
    {
        return zPosition;
    }

    public void setZPosition(float f)
    {
        zPosition = f;
        invalidate();
        if (parent != null)
        {
            parent.sortedAttachedControlsValid = false;
        }
        if (controller != null)
        {
            ((OBSectionController)controller).sortedAttachedControlsValid = false;
        }
    }

    public void setBackgroundColor(final int col)
    {
        if (col != backgroundColor)
        {
            new OBRunnableSyncUI()
            {
                public void ex()
                {
                    backgroundColor = col;
                    invalidate();
                }
            }.run();
        }
    }

    public int backgroundColor()
    {
        return backgroundColor;
    }

    public void setScaleX(final float sx)
    {
        if (scaleX != sx)
        {
            new OBRunnableSyncUI()
            {
                public void ex()
                {
                    invalidate();
                    scaleX = sx;
                    frameValid = false;
                    invalidate();
                }
            }.run();
        }
    }

    public void setScaleY(final float sy)
    {
        if (scaleY != sy)
        {
            new OBRunnableSyncUI()
            {
                public void ex()
                {
                    invalidate();
                    scaleY = sy;
                    frameValid = false;
                    invalidate();
                }
            }.run();
        }
    }

    public float scaleX()
    {
        return scaleX;
    }
    public float scaleY()
    {
        return scaleY;
    }
    public float scale()
    {
        return scaleX();
    }
    public void setScale(float sc)
    {
        setScaleX(sc);
        setScaleY(sc);
    }
    public float width()
    {
        frame();
        return frame.width();
    }

    public void setWidth(float w)
    {
        frame();
        float oldwidth = frame.width();
        float ratio = w / oldwidth;
        setScaleX(scaleX * ratio);

    }
    public float height()
    {
        frame();
        return frame.height();
    }

    public void setHeight(float h)
    {
        frame();
        float oldHeight = frame.height();
        float ratio = h / oldHeight;
        setScaleY(scaleY * ratio);

    }

    public void pointAt(PointF pt)
    {
        PointF p = OB_Maths.DiffPoints(pt, position);
        float ang = (float)Math.atan2(p.x, -p.y);
        setRotation(ang);
    }

    public void drawLayer(Canvas canvas)
    {
        if (layer != null)
        {
            if (highlightColour != 0)
            {
                layer.setColourFilter(new PorterDuffColorFilter(highlightColour, PorterDuff.Mode.SRC_ATOP));
            }
            boolean needsRestore = false;
            if (needsRestore = (opacity() != 1.0f))
                canvas.saveLayerAlpha(bounds(), (int) (opacity() * 255));
            layer.draw(canvas);
            if (needsRestore)
                canvas.restore();
        }
    }

    public Matrix matrixForDraw()
    {
        drawMatrix.reset();
        float ax = anchorPoint.x * bounds().width();
        float ay = anchorPoint.y * bounds.height();
        drawMatrix.preTranslate(position.x, position.y);
        if (rotation != 0)
            drawMatrix.preRotate((float) Math.toDegrees(rotation));
        if (scaleX != 1 || scaleY != 1)
            drawMatrix.preScale(scaleX, scaleY);
        drawMatrix.preTranslate(-ax, -ay);
        return drawMatrix;
    }

    public Matrix totalMatrixForDraw()
    {
        List<OBControl> plist = controlsToAncestor(null);
        Collections.reverse(plist);
        plist.add(this);
        convertMatrix.reset();
        for (OBControl c : plist)
        {
            convertMatrix.preConcat(c.matrixForDraw());
        }
        return convertMatrix;
    }
    public Matrix matrixForPointForwardConvert()
    {
        convertMatrix.reset();
        float ax = anchorPoint.x * bounds().width();
        float ay = anchorPoint.y * bounds.height();
        convertMatrix.postTranslate(-position.x, -position.y);
        if (rotation != 0)
            convertMatrix.postRotate((float) Math.toDegrees(-rotation));
        if (scaleX != 1 || scaleY != 1)
            convertMatrix.postScale(1 / scaleX, 1 / scaleY);
        convertMatrix.postTranslate(ax, ay);
        return convertMatrix;
    }

    public Matrix matrixForPointBackwardConvert()
    {
        convertMatrix.reset();
        float ax = anchorPoint.x * bounds().width();
        float ay = anchorPoint.y * bounds.height();
        convertMatrix.postTranslate(-ax, -ay);
        if (rotation != 0)
            convertMatrix.postRotate((float) Math.toDegrees(rotation));
        if (scaleX != 1 || scaleY != 1)
            convertMatrix.postScale(scaleX, scaleY);
        convertMatrix.postTranslate(position.x, position.y);
        return convertMatrix;
    }

    public Matrix matrixForForwardConvert()
    {
        //return matrixForPointBackwardConvert();

        convertMatrix.reset();
        float ax = anchorPoint.x * bounds().width();
        float ay = anchorPoint.y * bounds.height();
        convertMatrix.preTranslate(-position.x, -position.y);
        if (rotation != 0)
            convertMatrix.preRotate((float) Math.toDegrees(-rotation));
        if (scaleX != 1 || scaleY != 1)
            convertMatrix.preScale(1 / scaleX, 1 / scaleY);
        convertMatrix.preTranslate(ax, ay);
        return convertMatrix;
    }
    public Matrix matrixForBackwardConvert()
    {
        //return matrixForPointForwardConvert();

        convertMatrix.reset();
        float ax = anchorPoint.x * bounds().width();
        float ay = anchorPoint.y * bounds.height();
        convertMatrix.preTranslate(position.x, position.y);
        //convertMatrix.preTranslate(-ax,-ay);
        if (rotation != 0)
            convertMatrix.preRotate((float) Math.toDegrees(rotation));
        if (scaleX != 1 || scaleY != 1)
            convertMatrix.preScale(scaleX, scaleY);
        //convertMatrix.preTranslate(position.x, position.y);
        convertMatrix.preTranslate(-ax, -ay);
        return convertMatrix;
    }

    public PointF convertPointFromParent(PointF pt)
    {
        Matrix m = matrixForForwardConvert();
        float[] pts = new float[2];
        pts[0] = pt.x;
        pts[1] = pt.y;
        m.mapPoints(pts);
        return new PointF(pts[0],pts[1]);
    }
    public PointF convertPointToParent(PointF pt)
    {
        Matrix m = matrixForBackwardConvert();
        float[] pts = new float[2];
        pts[0] = pt.x;
        pts[1] = pt.y;
        m.mapPoints(pts);
        return new PointF(pts[0],pts[1]);
    }

    public List<OBControl>controlsToAncestor(OBControl ancestor)
    {
        List<OBControl> alist = new ArrayList<OBControl>();
        OBControl pptr = parent;
        while (pptr != ancestor)
        {
            if (pptr == null)
                return new ArrayList<OBControl>();
            alist.add(pptr);
            pptr = pptr.parent;
        }
        return alist;
    }
    public List<OBControl>controlsToDescendant(OBControl descendant)
    {
        List<OBControl> alist = descendant.controlsToAncestor(this);
        Collections.reverse(alist);
        return alist;
    }

    public OBControl commonParentWith(OBControl l)
    {
        OBControl selfp,p;
        selfp = this;
        p = l;
        List<OBControl> parentSet = new ArrayList<OBControl>();
        while (selfp != null || p != null)
        {
            if (selfp != null)
            {
                if (parentSet.contains(selfp))
                    return selfp;
                parentSet.add(selfp);
                selfp = selfp.parent;
            }
            if (p != null)
            {
                if (parentSet.contains(p))
                    return p;
                parentSet.add(p);
                p = p.parent;
            }
        }
        return null;
    }

    public Matrix matrixForConvertToControl(OBControl c)
    {
        Matrix m = new Matrix();
        if (c == this)
            return m;
        OBControl par = commonParentWith(c);
        if (par != this)
        {
            List<OBControl> alist = controlsToAncestor(par);
            alist.add(0, this);
            for (OBControl ch : alist)
                m.preConcat(ch.matrixForBackwardConvert());
        }
        if (c == null)
            return m;
        List<OBControl> clist = c.controlsToAncestor(par);
        clist.add(0,this);
        Collections.reverse(clist);
        for (OBControl ch : clist)
            m.preConcat(ch.matrixForForwardConvert());
        return m;
    }

    public Matrix matrixToConvertPointToControl(OBControl c)
    {
        Matrix m = new Matrix();
        if (c == this)
            return m;
        OBControl par = commonParentWith(c);
        List<OBControl> alist = controlsToAncestor(par);
        alist.add(0, this);
        for (OBControl ch : alist)
            m.postConcat(ch.matrixForPointBackwardConvert());
        if (par != c)
        {
            List<OBControl> clist = c.controlsToAncestor(par);
            Collections.reverse(clist);
            for (OBControl ch : clist)
                m.postConcat(ch.matrixForPointForwardConvert());
        }
        return m;
    }
    public Matrix matrixToConvertPointFromControl(OBControl c)
    {
        Matrix m = new Matrix();
        if (c == this)
            return m;
        OBControl par = commonParentWith(c);
        if (par != c)
        {
            List<OBControl> clist = c.controlsToAncestor(par);
            clist.add(0,c);
            for (OBControl ch : clist)
                m.postConcat(ch.matrixForPointBackwardConvert());
        }
        if (par != this)
        {
            List<OBControl> alist = controlsToAncestor(par);
            alist.add(0,this);
            Collections.reverse(alist);
            for (OBControl ch : alist)
                m.postConcat(ch.matrixForPointForwardConvert());
        }
        return m;
    }

    public Matrix matrixForConvertFromControl(OBControl c)
    {
        Matrix m = new Matrix();
        if (c == this)
            return m;
        OBControl par = commonParentWith(c);
        if (par != c)
        {
            List<OBControl> clist = c.controlsToAncestor(par);
            clist.add(0,c);
            for (OBControl ch : clist)
                m.preConcat(ch.matrixForBackwardConvert());
        }
        List<OBControl> alist = controlsToAncestor(par);
        alist.add(0,this);
        Collections.reverse(alist);
        for (OBControl ch : alist)
            m.preConcat(ch.matrixForForwardConvert());
        return m;
    }

    public PointF convertPointToControl(PointF pt,OBControl c)
    {
        Matrix m = matrixToConvertPointToControl(c);
        float[] pts = new float[2];
        pts[0] = pt.x;
        pts[1] = pt.y;
        m.mapPoints(pts);
        return new PointF(pts[0],pts[1]);
    }

    public PointF convertPointFromControl(PointF pt,OBControl c)
    {
        Matrix m = matrixToConvertPointFromControl(c);
        float[] pts = new float[2];
        pts[0] = pt.x;
        pts[1] = pt.y;
        m.mapPoints(pts);
        return new PointF(pts[0],pts[1]);
    }

    public RectF convertRectToControl(RectF r,OBControl c)
    {
        Matrix m = matrixToConvertPointToControl(c);
        RectF nr = new RectF();
        m.mapRect(nr, r);
        return nr;
    }

    public RectF convertRectFromControl(RectF r,OBControl c)
    {
        Matrix m = matrixToConvertPointFromControl(c);
        RectF nr = new RectF();
        m.mapRect(nr, r);
        return nr;
    }

    public boolean intersectsWith(OBControl c)
    {
        OBControl par = commonParentWith(c);
        RectF thisFrame = convertRectToControl(bounds(), par);
        RectF thatFrame = c.convertRectToControl(c.bounds(),par);
        if (!thisFrame.intersect(thatFrame))
            return false;
        int width = (int) Math.ceil(thatFrame.right - thatFrame.left);
        int height = (int) Math.ceil(thatFrame.bottom - thatFrame.top);
        Bitmap tinyCache = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        Canvas canvas1 = new Canvas(tinyCache);
        canvas1.save();
        Matrix m = totalMatrixForDraw();
        m.postTranslate(-thisFrame.left, -thisFrame.top);
        canvas1.concat(m);
        simpleDraw(canvas1);
        canvas1.restore();

        Bitmap tinyCache2 = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        Canvas canvas2 = new Canvas(tinyCache2);
        canvas2.save();
        m = c.totalMatrixForDraw();
        m.postTranslate(-thisFrame.left, -thisFrame.top);
        canvas2.concat(m);
        c.simpleDraw(canvas2);
        canvas2.restore();

        Paint p = new Paint();
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas1.drawBitmap(tinyCache2, 0, 0, p);
        int pixels[] = new int[tinyCache.getWidth()];
        for (int i = 0;i < tinyCache.getHeight();i++)
        {
            tinyCache.getPixels(pixels,0,tinyCache.getWidth(),0,i,tinyCache.getWidth(),1);
            for (int j = 0;j < tinyCache.getWidth();j++)
            {
                int col = pixels[j];
                if (Color.alpha(col) > 0)
                    return true;
            }
        }
        return false;
    }

    public Bitmap intersectsWithx(OBControl c)
    {
        OBControl par = commonParentWith(c);
        RectF thisFrame = convertRectToControl(bounds(), par);
        RectF thatFrame = c.convertRectToControl(c.bounds(),par);
        if (!thisFrame.intersect(thatFrame))
            return null;
        //thisFrame = thatFrame;
        int width = (int) Math.ceil(thisFrame.right - thisFrame.left);
        int height = (int) Math.ceil(thisFrame.bottom - thisFrame.top);
        Bitmap tinyCache = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        Canvas canvas1 = new Canvas(tinyCache);
        canvas1.save();
        Matrix m = totalMatrixForDraw();
        m.postTranslate(-thisFrame.left, -thisFrame.top);
        canvas1.concat(m);
        simpleDraw(canvas1);
        canvas1.restore();

        Bitmap tinyCache2 = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        Canvas canvas2 = new Canvas(tinyCache2);
        canvas2.save();
        m = c.totalMatrixForDraw();
        m.postTranslate(-thisFrame.left, -thisFrame.top);
        canvas2.concat(m);
        c.simpleDraw(canvas2);
        canvas2.restore();

        Paint p = new Paint();
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
        canvas1.drawBitmap(tinyCache2, 0, 0, p);

        int pixels[] = new int[tinyCache.getWidth()];
        for (int i = 0;i < tinyCache.getHeight();i++)
        {
            tinyCache.getPixels(pixels,0,tinyCache.getWidth(),0,i,tinyCache.getWidth(),1);
            for (int j = 0;j < tinyCache.getWidth();j++)
            {
                int col = pixels[j];
                if (Color.alpha(col) > 0)
                    return tinyCache;
            }
        }
        return tinyCache;
    }

    public boolean intersectsWithn(OBControl c)
    {
        RectF thisFrame = convertRectToControl(bounds(), null);
        RectF thatFrame = c.convertRectToControl(bounds(), null);
        if (!thisFrame.intersect(thatFrame))
            return false;
        int w = (int)thisFrame.width();
        int h = (int)thisFrame.height();
        Bitmap tinycache1 = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas1 = new Canvas(tinycache1);
        canvas1.clipRect(0, 0, w, h);
        canvas1.save();
        canvas1.translate(-thisFrame.left, -thisFrame.top);
        draw(canvas1);
        canvas1.restore();
        Bitmap tinycache2 = Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888);
        Canvas canvas2 = new Canvas(tinycache2);
        canvas2.clipRect(0, 0, w, h);
        canvas2.translate(-thisFrame.left, -thisFrame.top);
        c.draw(canvas2);
        Paint p = new Paint();
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas1.drawBitmap(tinycache2, 0, 0, p);
        int pixels[] = new int[w];
        for (int i = 0;i < h;i++)
        {
            tinycache1.getPixels(pixels,0,w,0,i,w,1);
            for (int j = 0;j < w;j++)
            {
                int px = pixels[j] & 0xFF000000;
                if (px != 0)
                    return true;
            }
        }

        return false;
    }

    public Matrix matrixForCacheDraw()
    {
        convertMatrix.reset();
        float ax = anchorPoint.x * bounds().width();
        float ay = anchorPoint.y * bounds.height();
        convertMatrix.preTranslate(ax,ay);
        if (rotation != 0)
            convertMatrix.preRotate(-rotation);
        if (scaleX != 1 || scaleY != 1)
            convertMatrix.preScale(scaleX,scaleY);
        convertMatrix.preTranslate(-ax, -ay);
        return convertMatrix;
    }

    public float cornerRadius()
    {
        return cornerRadius;
    }

    public void setCornerRadius(float cornerRadius)
    {
        this.cornerRadius = cornerRadius;
    }

    public void drawBorderAndBackground(Canvas canvas)
    {
        if (backgroundColor != 0)
        {
            Paint fillPaint = new Paint();
            int col = OBUtils.applyColourOpacity(backgroundColor,opacity());
            fillPaint.setColor(col);
            fillPaint.setStyle(Paint.Style.FILL);
            if (cornerRadius == 0)
                canvas.drawRect(bounds(), fillPaint);
            else
                canvas.drawRoundRect(bounds(),cornerRadius,cornerRadius,fillPaint);
        }
        if (borderColour != 0 && borderWidth > 0.0)
        {
            Paint strokePaint = new Paint();
            strokePaint.setStrokeWidth(borderWidth);
            int col = OBUtils.applyColourOpacity(borderColour,opacity());
            strokePaint.setColor(col);
            strokePaint.setStyle(Paint.Style.STROKE);
            if (cornerRadius == 0)
                canvas.drawRect(bounds(), strokePaint);
            else
            {
                canvas.save();
                Path p = new Path();
                p.addRoundRect(bounds(),cornerRadius(),cornerRadius(),Path.Direction.CCW);
                canvas.clipPath(p);
                canvas.drawRoundRect(bounds(), cornerRadius, cornerRadius, strokePaint);
                canvas.restore();
            }
        }
    }

    public void simpleDraw(Canvas canvas)
    {
        canvas.save();
        boolean shadowrequired = (shadowColour != 0 && shadowRadius > 0);
        if (cache != null)
        {
            Matrix m = new Matrix();
            float rs = 1 / rasterScale;
            m.preScale(rs, rs);
            canvas.concat(m);
            //canvas.concat(matrixForCacheDraw());
            Paint p = null;
            if (highlightColour != 0)
            {
                p = new Paint();
                p.setColorFilter(new PorterDuffColorFilter(highlightColour, PorterDuff.Mode.SRC_ATOP));
            }
            else if (shadowrequired)
            {
                 if (shadowCache == null)
                    createShadowCache();
                canvas.drawBitmap(shadowCache, shadowOffsetX, shadowOffsetY, p);

            }
            canvas.drawBitmap(cache, 0, 0, p);
        }
        else
        {
            if (masksToBounds)
                canvas.clipRect(bounds());
            if (shadowrequired)
            {
                Paint p = new Paint();
                p.setShadowLayer(shadowRadius, shadowOffsetX, shadowOffsetY, shadowColour);
                canvas.saveLayer(bounds(), p, Canvas.ALL_SAVE_FLAG);
            }
            drawBorderAndBackground(canvas);
            drawLayer(canvas);
            if (shadowrequired)
                canvas.restore();
        }
        canvas.restore();
    }

    public void getModelViewMatrix(OBViewController vc)
    {
        float mvm[] = vc.modelViewMatrix;
        for (int i = 0; i < modelMatrix.length;i++)
            modelMatrix[i] = mvm[i];
    }

    public float[] matrix3dForDraw()
    {
        float[] wMatrix = new float[16];
        android.opengl.Matrix.setIdentityM(wMatrix,0);
        if (m34 != 0)
            wMatrix[11] = m34;

        android.opengl.Matrix.setIdentityM(tempMatrix,0);
        android.opengl.Matrix.translateM(tempMatrix,0,position.x,position.y,0);
        android.opengl.Matrix.multiplyMM(modelMatrix,0,tempMatrix,0,wMatrix,0);

        float ax = anchorPoint.x * bounds().width();
        float ay = anchorPoint.y * bounds.height();
        if (rotation != 0)
            android.opengl.Matrix.rotateM(modelMatrix,0,(float) Math.toDegrees(rotation),0,0,1);
        if (yRotation != 0)
        {
            android.opengl.Matrix.rotateM(modelMatrix, 0, (float) Math.toDegrees(yRotation), 0, 1, 0);
        }
        if (scaleX != 1 || scaleY != 1)
            android.opengl.Matrix.scaleM(modelMatrix,0,scaleX, scaleY,1);
        android.opengl.Matrix.translateM(modelMatrix,0,-ax,-ay,0);
        return modelMatrix;

    }
    public boolean needsTexture()
    {
        return !(cornerRadius == 0 && (borderWidth == 0 || borderColour == 0));
    }
    public void renderLayer(OBRenderer renderer,OBViewController vc)
    {
        TextureRect tr = renderer.textureRect;
        if (texture == null || needsRetexture)
        {
            texturise(false, vc);
            needsRetexture = false;
        }
        tr.draw(renderer,0,0,bounds.right - bounds.left,bounds.bottom - bounds.top,texture.bitmap());
    }
    public void render(OBRenderer renderer,OBViewController vc,float[] modelViewMatrix)
    {
        if (!hidden  && bounds().width() > 0 && bounds().height() > 0)
        {
            matrix3dForDraw();
            if (doubleSided)
                GLES20.glDisable(GLES20.GL_CULL_FACE);
            else
                GLES20.glEnable(GLES20.GL_CULL_FACE);
            android.opengl.Matrix.multiplyMM(tempMatrix,0,modelViewMatrix,0,modelMatrix,0);
            if (needsTexture())
            {
                TextureShaderProgram textureShader = (TextureShaderProgram) renderer.textureProgram;
                float op = opacity();
                float[]finalCol = new float[4];
                for (int i = 0;i < 3;i++)
                    finalCol[i] = blendColour[i];
                finalCol[3] = blendColour[3] * op;
                textureShader.setUniforms(tempMatrix,renderer.textureObjectIds[0],finalCol);
                renderLayer(renderer,vc);
            }
            else
            {
                ColorShaderProgram colourShader = (ColorShaderProgram) renderer.colourProgram;
                float col[] = {1,1,1,1};
                OBUtils.getFloatColour(backgroundColor,col);
                colourShader.setUniforms(tempMatrix);
                GradientRect gr = renderer.gradientRect;
                gr.draw(renderer,0,0,bounds.right - bounds.left,bounds.bottom - bounds.top,col,col);

            }
        }
    }
    public void draw(Canvas canvas)
    {
        if (!hidden)
        {
            canvas.save();
             boolean shadowrequired = (shadowColour != 0 && shadowRadius > 0);
            if (cache != null)
            {
                Matrix m = matrixForDraw();
                float rs = 1 / rasterScale;
                m.preScale(rs, rs);
                canvas.concat(m);
                //canvas.concat(matrixForCacheDraw());
                Paint p = null;
                if (highlightColour != 0)
                {
                    p = new Paint();
                    p.setColorFilter(new PorterDuffColorFilter(highlightColour, PorterDuff.Mode.SRC_ATOP));
                }
                else if (shadowrequired)
                {
                    //p = new Paint();
                    //p.setShadowLayer(shadowRadius, shadowOffsetX, shadowOffsetY, shadowColour);
                    //p.setAntiAlias(true);
                    if (shadowCache == null)
                        createShadowCache();
                    canvas.drawBitmap(shadowCache, shadowOffsetX, shadowOffsetY, p);

                }
                canvas.drawBitmap(cache, 0, 0, p);
            }
            else
            {
                canvas.concat(matrixForDraw());
                if (masksToBounds)
                    canvas.clipRect(bounds());
                if (shadowrequired)
                {
                    Paint p = new Paint();
                    p.setShadowLayer(shadowRadius,shadowOffsetX,shadowOffsetY,shadowColour);
                    canvas.saveLayer(bounds(),p,Canvas.ALL_SAVE_FLAG);
                }
                drawBorderAndBackground(canvas);
                drawLayer(canvas);
                if (shadowrequired)
                    canvas.restore();
            }
            canvas.restore();
        }
    }

    public Bitmap drawn()
    {
        Bitmap bitmap = null;
        int width = (int) Math.ceil((bounds().right - bounds().left) * Math.abs(rasterScale));
        int height = (int) Math.ceil((bounds().bottom - bounds().top) * Math.abs(rasterScale));
        if (width == 0 || height == 0)
            Log.i("error","drawn");
        try
        {
            bitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        }
        catch(Exception exception)
        {
            exception.printStackTrace();
            Log.i("drawn",String.format("%s %g %g",attributes().get("id"),width,height));
        }
        Canvas canvas = new Canvas(bitmap);
        Matrix m = new Matrix();
        m.preScale(rasterScale, rasterScale);
        canvas.concat(m);
        drawBorderAndBackground(canvas);
        drawLayer(canvas);
        if (maskControl != null)
        {
            Paint p = new Paint();
            p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            canvas.saveLayer(0, 0, width, height, p, Canvas.ALL_SAVE_FLAG);
            maskControl.draw(canvas);
            canvas.restore();
        }
        return bitmap;
    }
    public void enCache()
    {
        cache = drawn();
    }


    public void texturise(boolean shared,OBViewController vc)
    {
        texture=vc.createTexture(this,textureKey,shared);
    }

    private void blur(Bitmap bt,float radius)
    {

        RenderScript rs = RenderScript.create(MainActivity.mainActivity);

        Allocation overlayAlloc = Allocation.createFromBitmap(
                rs, bt);

        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(
                rs, overlayAlloc.getElement());

        blur.setInput(overlayAlloc);

        blur.setRadius(radius);

        blur.forEach(overlayAlloc);

        overlayAlloc.copyTo(bt);

        rs.destroy();
    }

    public void setMasksToBounds(boolean m)
    {
        masksToBounds = true;
    }

    public boolean masksToBounds()
    {
        return masksToBounds;
    }

    public void createShadowCache()
    {
        int width = (int) Math.ceil((bounds().right - bounds().left) * rasterScale);
        int height = (int) Math.ceil((bounds().bottom - bounds().top) * rasterScale);
        shadowCache = Bitmap.createBitmap(width,height,Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(shadowCache);
        Matrix m = new Matrix();
        m.preScale(rasterScale,rasterScale);
        //canvas.concat(m);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        /*BlurMaskFilter bmf = new BlurMaskFilter(3,BlurMaskFilter.Blur.NORMAL);
        paint.setMaskFilter(bmf);
        paint.setColor(Color.BLACK);
        paint.setFilterBitmap(true);
        canvas.drawBitmap(cache,0,0,paint);*/
        Bitmap alpha = cache.extractAlpha();
        paint.setColor(shadowColour);
        paint.setAlpha((int)(shadowOpacity * 255));

        canvas.drawBitmap(alpha, 0, 0, paint);
        // Create outer blur
       /* final BlurMaskFilter filter = new BlurMaskFilter(9, BlurMaskFilter.Blur.OUTER);
        paint.setMaskFilter(filter);
        canvas.drawBitmap(alpha, 0, 0, paint);*/
        Log.i("shadow",String.format("%g",shadowRadius));
        blur(shadowCache, shadowRadius);
        //Runnable r2 = () -> System.out.println("Hello world two!");
    }

    public float rasterScale()
    {
        return rasterScale;
    }

    public void setRasterScale(float rs)
    {
        rasterScale = rs;
    }
    public Bitmap renderedImage()
    {
        Bitmap bm = Bitmap.createBitmap((int)(frame.right - frame.left),(int)(frame.bottom - frame.top), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        drawLayer(canvas);
        return bm;
    }

    public OBImage renderedImageControl()
    {
        //BitmapDrawable bmd = new BitmapDrawable(MainActivity.mainActivity.getResources(),renderedImage());
        OBImage im = new OBImage(renderedImage());
        return im;
    }

    public Bitmap renderedImageOverlay(int col)
    {
        Bitmap bm = Bitmap.createBitmap((int)(frame.right - frame.left),(int)(frame.bottom - frame.top), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        drawLayer(canvas);
        Paint paint = new Paint();
        paint.setColor(col);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        canvas.drawRect(0,0,(int)(frame.right - frame.left),(int)(frame.bottom - frame.top),paint);
        return bm;
    }


    public void unCache()
    {
        cache = null;
    }
    public void setProperty(String prop,Object val)
    {
        if (val == null)
            settings.remove(prop);
        else
            settings.put(prop, val);
    }

    public Object propertyValue(String prop)
    {
        return settings.get(prop);
    }

    public void show()
    {
        if (hidden)
        {
            new OBRunnableSyncUI()
            {
                public void ex()
                {
                    hidden = false;
                    invalidate();
                }
            }.run();
        }
    }

    public void hide()
    {
        if (!hidden)
        {
            new OBRunnableSyncUI()
            {
                public void ex()
                {
                    invalidate();
                    hidden = true;
                }
            }.run();
        }
    }

    public void setDoubleSided(boolean t)
    {
        doubleSided = t;
    }

    public boolean doubleSided()
    {
        return doubleSided;
    }

    public void setHidden(boolean h)
    {
        hidden = h;
        invalidate();
    }
    public void setMaskControl(OBControl m)
    {
        maskControl = m;
    }

    public float rotation()
    {
        return rotation;
    }
    public void setRotation(final float rt)
    {
        new OBRunnableSyncUI()
        {
            public void ex()
            {
                invalidate();
                rotation = rt;
                frameValid = false;
                invalidate();
            }
        }.run();
    }
    public float yRotation()
    {
        return yRotation;
    }
    public void setYRotation(final float rt)
    {
        new OBRunnableSyncUI()
        {
            public void ex()
            {
                invalidate();
                yRotation = rt;
                frameValid = false;
                invalidate();
            }
        }.run();
    }

    /*
    void move(PointF frompt,PointF topt,double secs,OBSectionController cont)
    {
        long startms = SystemClock.uptimeMillis();
        double duration = secs*1000f;
        double frac = 0;
        RectF r1 = new RectF();
        RectF r2 = new RectF();
        PointF pout = new PointF();
        final OBSectionController fcont = cont;
        while (frac <= 1.0)
        {
            long currtime = SystemClock.uptimeMillis();
            frac = (currtime - startms) / duration;
            double t = OB_Maths.clamp01(frac);
            r1.set(frame());
            OB_Maths.tPointAlongLine((float)t,frompt,topt,pout);
            setPosition(pout);
            r2.set(frame());
            final int rl1 = (int)r1.left,rt1 = (int)r1.top,rr1 = (int)r1.right,rb1 = (int)r1.bottom;
            final int rl2 = (int)r2.left,rt2 = (int)r2.top,rr2 = (int)r2.right,rb2 = (int)r2.bottom;
            fcont.invalidateView(rl1,rt1,rr1,rb1);
            fcont.invalidateView(rl2,rt2,rr2,rb2);
            try
            {
                Thread.sleep(20);
            }
            catch (InterruptedException e)
            {
            }
        }
    }

    public void moveAlongPath(Path path,double secs,boolean wait,OBViewController cont)
    {
        long startms = SystemClock.uptimeMillis();
        double duration = secs*1000f;
        double frac = 0;
        RectF r1 = new RectF();
        RectF r2 = new RectF();
        PointF pout = new PointF();
        final OBViewController fcont = cont;
        while (frac <= 1.0)
        {
            long currtime = SystemClock.uptimeMillis();
            frac = (currtime - startms) / duration;
            double t = OB_Maths.clamp01(frac);
            r1.set(frame());
            setPosition(pout);
            r2.set(frame());
            final int rl1 = (int)r1.left,rt1 = (int)r1.top,rr1 = (int)r1.right,rb1 = (int)r1.bottom;
            final int rl2 = (int)r2.left,rt2 = (int)r2.top,rr2 = (int)r2.right,rb2 = (int)r2.bottom;
            new OBRunnableSyncUI(){public void ex()
            {
                fcont.invalidateView(rl1,rt1,rr1,rb1);
                fcont.invalidateView(rl2,rt2,rr2,rb2);
            }}.run();
            try
            {
                Thread.sleep(20);
            }
            catch (InterruptedException e)
            {
            }
        }
    }
*/
    public boolean containsPoint(PointF pt)
    {
        pt = convertPointFromControl(pt,null);
        return bounds().contains(pt.x, pt.y);
    }

    public void setAnchorPoint(PointF pt)
    {
        anchorPoint.set(pt);
    }

    public void setAnchorPoint(float x,float y)
    {
        anchorPoint.set(x,y);
    }

    public void setShadow(float sradius,float sopacity,float soffsetx,float soffsety,int scolour)
    {
        shadowRadius = sradius;
        shadowOffsetX = soffsetx;
        shadowOffsetY = soffsety;
        shadowOpacity = sopacity;
        shadowColour = scolour;
    }

    public OBStroke stroke()
    {
        return stroke;
    }

    public void setStroke(OBStroke s)
    {
        stroke = s;
    }
    public void invalidate()
    {
        if (controller == null)
        {
            if (parent != null)
                parent.invalidate();
        }
        else
        {
            final RectF f = frame();
            new OBRunnableSyncUI()
            {
                public void ex()
                {
                    tempRect.set(f);
                    if (shadowColour != 0 && shadowRadius > 0 && shadowOpacity > 0)
                    {
                        tempRect.offset(shadowOffsetX,shadowOffsetY);
                        tempRect.inset(-shadowRadius, -shadowRadius);
                        tempRect.union(f);
                    }
                    controller.invalidateView((int) (tempRect.left - invalOutdent), (int) (tempRect.top - invalOutdent), (int) (tempRect.right + invalOutdent), (int) (tempRect.bottom+invalOutdent));
                }
            }.run();
        }
    }
    public void highlight()
    {
        new OBRunnableSyncUI()
        {
            public void ex()
            {
                highlightColour = Color.argb(127, 0, 0, 0);
                OBUtils.setFloatColour(.8f,.8f,.8f,1,blendColour);
                invalidate();
            }
        }.run();
    }

    public void lowlight()
    {
        new OBRunnableSyncUI()
        {
            public void ex()
            {
                highlightColour = 0;
                OBUtils.setFloatColour(1,1,1,1,blendColour);
                invalidate();
            }
        }.run();
    }
    public void setBorderColor(final int i)
    {
        new OBRunnableSyncUI()
        {
            public void ex()
            {
                borderColour = i;
                if (texture != null)
                    needsRetexture = true;
                invalidate();
            }
        }.run();
    }
    public void setBorderWidth(final float f)
    {
        new OBRunnableSyncUI()
        {
            public void ex()
            {
                borderWidth = f;
                if (texture != null)
                    needsRetexture = true;
                invalidate();
            }
        }.run();
    }

    public int colourAtPoint(float x,float y)
    {
        Bitmap tinycache = Bitmap.createBitmap(1,1, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(tinycache);
        canvas.clipRect(0, 0, 1, 1);
        canvas.translate(-x, -y);
        drawBorderAndBackground(canvas);
        drawLayer(canvas);
        return tinycache.getPixel(0,0);
    }
    public float alphaAtPoint(float x,float y)
    {
        return (Color.alpha(colourAtPoint(x,y))/255f);
    }

    public Map<String,Object>attributes()
    {
    return (Map<String,Object>)settings.get("attrs");
    }

    public OBGroup primogenitor()
    {
        OBGroup dad = this.parent;
        if (dad == null)
            return null;
        while (dad.parent != null)
        {
            dad = (OBGroup)dad.parent;
        }
        return dad;
    }

    public void reflectInAncestor(OBControl dad)
    {
        PointF pos = dad.convertPointFromControl(position(),parent);
        float f = dad.bounds().width();
        pos.x = f - pos.x;
        position = parent.convertPointFromControl(pos,dad);
        setScaleX(-scaleX);
    }

    public void lockScreen()
    {
        if (controller != null)
            controller.lockScreen();
    }

    public void unlockScreen()
    {
        if (controller != null)
            controller.unlockScreen();
    }

}