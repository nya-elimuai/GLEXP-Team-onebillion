package org.onebillion.xprz.mainui.x_reading;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.AsyncTask;

import org.onebillion.xprz.controls.OBControl;
import org.onebillion.xprz.controls.OBEmitter;
import org.onebillion.xprz.controls.OBEmitterCell;
import org.onebillion.xprz.controls.OBGroup;
import org.onebillion.xprz.controls.OBPath;
import org.onebillion.xprz.controls.XPRZ_Presenter;
import org.onebillion.xprz.mainui.MainActivity;
import org.onebillion.xprz.utils.OBAnim;
import org.onebillion.xprz.utils.OBAnimBlock;
import org.onebillion.xprz.utils.OBAnimationGroup;
import org.onebillion.xprz.utils.OBReadingPara;
import org.onebillion.xprz.utils.OBReadingWord;
import org.onebillion.xprz.utils.OBUtils;
import org.onebillion.xprz.utils.OB_Maths;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by alan on 07/06/16.
 */
public class X_ReadingReadToMeNTx extends X_ReadingReadToMe
{
    XPRZ_Presenter presenter;
    int cqType;
    boolean questionsAsked;
    String pageName;
    OBEmitter starEmitter;

    public void start()
    {
        setStatus(0);
        new AsyncTask<Void, Void,Void>()
        {
            protected Void doInBackground(Void... params) {
                try
                {
                    if (pageNo == 0)
                    {
                        setStatus(STATUS_DOING_DEMO);
                        demoa();
                        waitForSecs(0.7);
                        readTitle();
                        waitForSecs(0.5);
                        democ();
                        setStatus(STATUS_AWAITING_CLICK);
                    }
                    else
                    {
                        waitForSecs(0.5);
                        replayAudio();
                    }
                }
                catch (Exception exception)
                {
                }
                return null;
            }}.execute();
    }

    public void loadTemplate()
    {
        eventsDict = loadXML(getConfigPath("booktemplate.xml"));
        if (pageNo == 0)
        {
            loadEvent("title");
            String s;
            if ((s = eventAttributes.get("largefontsize"))!=null)
                fontSize = applyGraphicScale(Float.parseFloat(s));
            if ((s = eventAttributes.get("largelineheight"))!=null)
                lineHeightMultiplier = Float.parseFloat(s);
            if ((s = eventAttributes.get("largespacing"))!=null)
                letterSpacing = Float.parseFloat(s);
            textJustification = TEXT_JUSTIFY_CENTRE;
        }
        else
        {
            if (picJustify == PIC_JUSTIFY_RIGHT|| picJustify == PIC_JUSTIFY_LEFT)
                loadEvent("piconlyport");
            else
                loadEvent("piconly");
        }
    }

    public float layOutText()
    {
        if (pageNo == 0)
            return super.layOutText();
        return 0;
    }

    public void readParagraph(int pidx,double token,boolean canInterrupt) throws Exception
    {
        List<Object> l = (List<Object>)(Object)Collections.singletonList(String.format("p%d_%d",pageNo,pidx+1,true));
        playAudioQueued(l,true);
    }

    public void readTitle() throws Exception
    {
        OBReadingPara para = paragraphs.get(0);
        lockScreen();
        for (OBReadingWord w : para.words)
        {
            if (w.label != null && ((w.flags & OBReadingWord.WORD_SPEAKABLE) != 0))
            {
                highlightWord(w,true,false);
            }
        }
        textBox.setNeedsRetexture();
        unlockScreen();
        readParagraph(0,0,false);
        lockScreen();
        for (OBReadingWord w : para.words)
        {
            if (w.label != null && ((w.flags & OBReadingWord.WORD_SPEAKABLE) != 0))
            {
                highlightWord(w,false,false);
            }
        }
        textBox.setNeedsRetexture();
        unlockScreen();
    }

    public void readingFinished()
    {
        try
    {
        waitForSecs(0.8);
        if (status() != STATUS_FINISHING && !_aborting)
        {
            if (!considerComprehensionQuestions())
                bringUpNextButton();
        }
    }
        catch (Exception exception)
        {
        }

    }
    public boolean readPage()
    {
        if (super.readPage())
        {
            readingFinished();
            return true;
        }
        return false;
    }
    public boolean showNextButton()
    {
        return false;
    }

    public void replayAudio()
    {
        if (pageNo == 0)
        {
            setStatus(status());
            new AsyncTask<Void, Void, Void>()
            {
                protected Void doInBackground(Void... params)
                {
                    try
                    {
                        readTitle();
                    }
                    catch(Exception e)
                    {

                    }
                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
        }
        else
            super.replayAudio();
    }

    public void demoa() throws Exception
    {
        lockScreen();
        loadEvent("anna");
        presenter = XPRZ_Presenter.characterWithGroup((OBGroup)objectDict.get("presenter"));
        presenter.control.setZPosition(200);
        presenter.control.setProperty("restpos",new PointF(presenter.control.position().x,presenter.control.position().y));
        presenter.control.setRight(0);
        presenter.control.show();
        unlockScreen();

        presenter.walk((PointF) presenter.control.propertyValue("restpos"));
        presenter.faceFront();
        waitForSecs(0.2f);
        Map<String,List> eventd = (Map<String, List>) audioScenes.get("a");

        List<Object> aud = eventd.get("DEMO");
        presenter.speak(aud,this);
        waitForSecs(0.4f);
        PointF currPos = presenter.control.position();
        PointF destpos = new PointF(-presenter.control.width()/2, currPos.y);
        presenter.walk(destpos);
    }

    public void democ() throws Exception
    {
        waitForSecs(0.3f);
        showNextArrowAndRA(true);
        PointF destPoint = OB_Maths.locationForRect(-0.1f, 0.3f, MainActivity.mainViewController.bottomRightButton.frame());
        loadPointerStartPoint(OB_Maths.locationForRect(0.5f, 1.1f,new RectF(bounds())),destPoint);
        movePointerToPoint(destPoint,-1,true);
        playAudioQueuedScene("c","DEMO",true);
        waitForSecs(0.5f);
        thePointer.hide();
    }



    public void showQuestionElements() throws Exception
    {
        if (!objectDict.get("cameo").hidden)
            return;
        playSfxAudio("anna_on",false);
        lockScreen();
        showControls("cameo");
        unlockScreen();
    }

    public void hideQuestionElements() throws Exception
    {
        playSfxAudio("anna_off",false);
        lockScreen();
        hideControls("cameo");
        if (cqType == 2)
        {
            hideControls("answer.*");
        }
        unlockScreen();
    }

    public void workOutQuestionType(Map<String,Object> eventAudio)
    {
        if (eventAudio.get("ANSWER") != null)
            cqType = 2;
        else if (eventAudio.get("CORRECT") != null)
            cqType = 1;
        else
            cqType = 3;
    }

    public boolean correctFirst()
    {
        Map<String,List<String>> asp = (Map<String, List<String>>) audioScenes.get(pageName);
        List<String> keys = asp.get("__keys");
        for (String key : keys)
        {
            if (key.equals("CORRECT"))
                return true;
            else if (key.equals("INCORRECT"))
                return false;
        }
        return true;
    }

    public void loadCQAudioXMLs()
    {
        String path = getConfigPath("cqaudio.xml");
        loadAudioXML(path);
        Map<String,Object> d = audioScenes;
        if (pageNo == maxPageNo)
        {
            if (d.get("final") != null)
                d.put(pageName,d.get("final"));
        }
        path = getConfigPath("cqsfx.xml");
        loadAudioXML(path);
        audioScenes.putAll(d);
    }

    public String pageName()
    {
        return String.format("p%d",pageNo);
    }

    public boolean considerComprehensionQuestions() throws Exception
    {
        String usecq = parameters.get("cq");
        if (usecq == null || !usecq.equals("true"))
            return false;
        if (questionsAsked)
            return false;
        loadCQAudioXMLs();
        pageName = pageName();
        if (audioScenes.get(pageName) != null)
        {
            workOutQuestionType((Map<String, Object>) audioScenes.get(pageName));
            loadCQPage();
            questionsAsked = true;
            if (cqType == 1)
            {
                targets = Arrays.asList(objectDict.get("shape"));
                demoCqType1a();
            }
            else if (cqType == 2)
            {
                if (correctFirst())
                    targets = Arrays.asList(objectDict.get("answer1"),objectDict.get("answer2"));
                else
                    targets = Arrays.asList(objectDict.get("answer2"),objectDict.get("answer1"));
                demoCqType2a();
            }
            else if (cqType == 3)
            {
                targets = Collections.emptyList();
                //demoCqType3a();
            }
            return true;
        }
        return false;
    }

    public void demoCqType2a() throws Exception
    {
        setStatus(STATUS_DOING_DEMO);
        waitForSecs(0.4f);
        showQuestionElements();
        waitForSecs(0.4f);
        demoCqType2b(true);
    }

    public void demoCqType2b(boolean firstTime)
    {
        long token = -1;
        try
        {
            token = takeSequenceLockInterrupt(true);
            if (token == sequenceToken)
            {
                List<Object>audl = (List<Object>) ((Map<String,Object>)audioScenes.get(pageName)).get("PROMPT");
                audl = OBUtils.insertAudioInterval(audl,300);
                presenter.speak(audl,this);
                waitForSecs(0.4f);
                OBPath answer1 = (OBPath) objectDict.get("answer1");
                answer1.show();
                deployFlashAnim(answer1);
                audl = (List<Object>) ((Map<String,Object>)audioScenes.get(pageName)).get("ANSWER");
                presenter.speak(audl,this);
                checkSequenceToken(token);
                waitForSecs(0.2f);
                killAnimations();
                setStatus(STATUS_WAITING_FOR_ANSWER);
                checkSequenceToken(token);
                waitForSecs(0.4f);
                OBPath answer2 = (OBPath) objectDict.get("answer2");
                answer2.show();
                deployFlashAnim(answer2);
                audl = (List<Object>) ((Map<String,Object>)audioScenes.get(pageName)).get("ANSWER2");
                presenter.speak(audl,this);
                checkSequenceToken(token);
                waitForSecs(0.2f);
                checkSequenceToken(token);
                killAnimations();
                waitForSecs(0.3f);
                checkSequenceToken(token);
                audl = (List<Object>) ((Map<String,Object>)audioScenes.get(pageName)).get("PROMPT2");
                presenter.speak(audl,this);
                checkSequenceToken(token);
                waitForSecs(0.3f);
                checkSequenceToken(token);
                if (firstTime)
                    reprompt(statusTime, null, 5, new OBUtils.RunLambda() {
                        @Override
                        public void run() throws Exception
                        {
                            demoCqType2b(true);
                        }
                    });
                demoCqType2b(true);
            }
        }
        catch (Exception exception)
        {
        }
        killAnimations();
        sequenceLock.unlock();
    }

    public void demoCqType3a() throws Exception {
        setStatus(STATUS_DOING_DEMO);
        waitForSecs(0.4f);
        showQuestionElements();
        waitForSecs(0.4f);
        demoCqType3b(true);
    }

    public void demoCqType3b(boolean firstTime)
    {
        long token = -1;
        try
        {
            token = takeSequenceLockInterrupt(true);
            if (token == sequenceToken)
            {
                checkSequenceToken(token);
                for (int i = 1;i < 10;i++)
                {
                    String nm = StrAndNo("PROMPT", i);
                    List<Object>audl = (List<Object>) ((Map<String,Object>)audioScenes.get(pageName)).get(nm);

                    if (audl == null)
                        break;
                    presenter.speak(audl,this);
                    checkSequenceToken(token);
                    waitForSecs(1.2f);
                }
            }
        }
        catch (Exception exception)
        {
        }
        killAnimations();
        sequenceLock.unlock();
        try
        {
            finishQuestion();
        }
        catch (Exception e)
        {

        }
    }

    public void finishQuestion() throws Exception {
        waitForSecs(0.6f);
        if (cqType == 1)
        {
        }
        hideQuestionElements();
        waitForSecs(0.8f);
        setStatus(STATUS_IDLE);
        bringUpNextButton();
    }

    public void deployFlashAnim(final OBPath c)
    {
        OBAnim blockAnim = new OBAnimBlock()
        {
            @Override
            public void runAnimBlock(float frac)
            {
                if (frac < 0.5)
                {
                    setAnswerButtonSelected(c);
                }
                else
                {
                    setAnswerButtonActive(c);
                }
            }
        };
        OBAnimationGroup ag = new OBAnimationGroup();
        registerAnimationGroup(ag,"flash");
        ag.applyAnimations(Collections.singletonList(blockAnim),0.25f,true,OBAnim.ANIM_LINEAR,this);
        setAnswerButtonActive(c);
    }
    void setAnswerButtonActive(OBPath c)
    {
        lockScreen();
        c.setFillColor((Integer)c.propertyValue("fillcolour"));
        c.setStrokeColor((Integer)c.propertyValue("strokecolour"));
        unlockScreen();
    }

    void setAnswerButtonInActive(OBPath c)
    {
        lockScreen();
        c.setFillColor((Integer)c.propertyValue("desatfillcolour"));
        c.setStrokeColor((Integer)c.propertyValue("desatstrokecolour"));
        unlockScreen();
    }

    void setAnswerButtonSelected(OBPath c)
    {
        lockScreen();
        c.setFillColor((Integer)c.propertyValue("fillcolour"));
        c.setStrokeColor((Integer)c.propertyValue("strokecolour"));
        unlockScreen();
    }

    public void loadCQPage()
    {
        Map<String, Object> evd = loadXML(getConfigPath("cq.xml"));
        eventsDict.putAll(evd);
        evd = loadXML(getConfigPath("eventcq.xml"));
        eventsDict.putAll(evd);
        lockScreen();
        doVisual("cqmain");
        presenter = XPRZ_Presenter.characterWithGroup((OBGroup)objectDict.get("annahead"));

        //anna = (OBGroup) objectDict.get("annahead");
        if (cqType == 1 || cqType == 3)
        {
            loadEvent(pageName());
        }
        else if (cqType == 2)
        {
            for (OBControl p : filterControls("answer.*"))
            {
                OBPath c = (OBPath) p;
                int col = c.fillColor();
                c.setProperty("fillcolour", col);
                c.setProperty("desatfillcolour", OBUtils.DesaturatedColour(col, 0.2f));
                col = c.strokeColor();
                c.setProperty("strokecolour", col);
                c.setProperty("desatstrokecolour", OBUtils.DesaturatedColour(col, 0.2f));
                setAnswerButtonInActive(c);
            }
        }
        else
        {

        }
        hideControls("answer.*");
        hideControls("cameo");
        unlockScreen();
    }

    public void demoCqType1a() throws Exception
    {
        setStatus(STATUS_DOING_DEMO);
        waitForSecs(0.4f);
        showQuestionElements();
        waitForSecs(0.4f);
        demoCqType1b(true);
    }

    public void demoCqType1b(boolean firstTime)
    {
        long token = -1;
        try
        {
            token = takeSequenceLockInterrupt(true);
            if (token == sequenceToken)
            {
                waitForSecs(0.4f);
                showQuestionElements();
                waitForSecs(0.4f);
                checkSequenceToken(token);
                List<Object>audl = (List<Object>) ((Map<String,Object>)audioScenes.get(pageName)).get("PROMPT");
                audl = OBUtils.insertAudioInterval(audl,500);
                presenter.speak(audl,this);
                checkSequenceToken(token);
                setStatus(STATUS_WAITING_FOR_ANSWER);
                waitForSecs(0.4f);
                checkSequenceToken(token);
                if (firstTime)
                    reprompt(statusTime, null, 5, new OBUtils.RunLambda() {
                        @Override
                        public void run() throws Exception {
                            demoCqType1b(true);
                        }
                    });
            }
        }
        catch (Exception exception)
        {
        }
        sequenceLock.unlock();
    }

    public OBEmitterCell starEmitterCell(int i, Bitmap im, float rs,float gs,float bs)
    {
        OBEmitterCell ec = new OBEmitterCell();
        ec.birthRate = 10;
        ec.lifeTime = 2;
        ec.lifeTimeRange = 0.5f;
        ec.red = 1.0f;
        ec.green = 216.0f/255.0f;
        ec.blue = 0.0f;
        ec.contents = im;
        ec.name = String.format("star%d",i);
        ec.velocity = 0;
        ec.velocityRange = 2;
        ec.emissionRange = (float)(Math.PI * 2.0);
        ec.blueSpeed = bs;
        ec.greenSpeed = gs;
        ec.redSpeed = rs;
        ec.alphaSpeed = -0.2f;
        ec.spin = 0.0f;
        ec.spinRange = 3.0f;
        ec.scale = 1.0f ;
        ec.scaleRange = -(ec.scale / 2.0f);
        ec.scaleSpeed = ec.scaleRange / 2.0f;
        return ec;
    }

    public void stopEmissions() throws Exception {
        OBEmitterCell cell = starEmitter.cells.get(0);
        cell.birthRate = 0;
        cell.lifeTime = 10;
        waitForSecs(2f);
    }

    public void doEmitter()
    {
        OBPath smallStar = StarWithScale(applyGraphicScale(8), false);
        smallStar.setFillColor(Color.WHITE);
        smallStar.enCache();
        OBPath shape = (OBPath) objectDict.get("shape");
        OBEmitterCell cell = starEmitterCell(0, smallStar.cache, 0, 0, 0);

        starEmitter = new OBEmitter();
        starEmitter.setFrame(0, 0, bounds().width(), bounds().height());
        starEmitter.cells.add(cell);

        starEmitter.setZPosition(100);
        objectDict.put("starEmitter", starEmitter);
        attachControl(starEmitter);
        starEmitter.run();
        OBUtils.runOnOtherThreadDelayed(2, new OBUtils.RunLambda() {
            @Override
            public void run() throws Exception {
                stopEmissions();
            }
        });
    }
}