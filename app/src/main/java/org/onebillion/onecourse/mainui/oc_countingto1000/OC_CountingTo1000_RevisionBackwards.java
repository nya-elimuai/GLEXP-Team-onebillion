package org.onebillion.onecourse.mainui.oc_countingto1000;

import android.graphics.PointF;

import org.onebillion.onecourse.controls.OBControl;
import org.onebillion.onecourse.mainui.generic.OC_Generic;

import java.util.ArrayList;
import java.util.List;

public class OC_CountingTo1000_RevisionBackwards extends OC_CountingTo1000_Revision
{
    public void buildNumbers()
    {
        numbers = new ArrayList();
        for (int i = 0; i < 10; i++)
        {
            String number = String.format("%d", (10 - i) * 100);
            numbers.add(number);
        }
    }

    public void demointro1() throws Exception
    {
        setStatus(STATUS_BUSY);
        presenter.walk((PointF) presenter.control.settings.get("restpos"));
        presenter.faceFront();
        waitForSecs(0.2f);
        //
        List<String> audioFiles = getAudioForScene(currentEvent(), "DEMO");
        String presenterAudio = audioFiles.get(0);                                  // Let’s practise counting to one thousand, : hundreds!;
        presenter.speak((List<Object>) (Object) presenterAudio, 0.3f, this);
        waitForSecs(0.7f);
        //
        presenterAudio = audioFiles.get(1);                                         // This time we’ll count backwards!;
        presenter.speak((List<Object>) (Object) presenterAudio, 0.3f, this);
        waitForSecs(0.3f);
        //
        PointF currPos = OC_Generic.copyPoint(presenter.control.position());
        OBControl front = presenter.control.objectDict.get("front");
        PointF destPos = new PointF(bounds().width() - front.width(),  currPos.y);
        presenter.walk(destPos);
        presenter.faceFront();
        presenterAudio = audioFiles.get(2);                               // Are you ready?;
        presenter.speak((List<Object>) (Object) presenterAudio, 0.3f, this);
        waitForSecs(0.3f);
        //
        currPos = presenter.control.position();
        destPos = new PointF(bounds().width() + front.width(),  currPos.y);
        presenter.walk(destPos);
        nextScene();
    }

}
