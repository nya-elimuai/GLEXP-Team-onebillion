package org.onebillion.xprz.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pedroloureiro on 28/06/16.
 */
public class OBWord extends OBSyllable
{

    List<OBSyllable> syllables;
    String imageName;
    Boolean syllablesChecked, phonemesChecked;


    public OBWord (String text)
    {
        this(text, null, null, null, null, null);

    }

    public OBWord (String text, String soundID)
    {
        this(text, soundID, null, null, null, null);
    }


    public OBWord (String text, String soundID, List<OBSyllable> syllables)
    {
        this(text, soundID, null, null, syllables, null);
    }


    public OBWord (String text, String soundID, List<Object> timings, String audio, List<OBSyllable> syllables, String imageName)
    {
        super(text, soundID, timings, audio, null);
        this.syllablesChecked = false;
        this.phonemesChecked = false;
        this.syllables = (syllables == null) ? new ArrayList<OBSyllable>() : new ArrayList<OBSyllable>(syllables);
        this.imageName = (imageName == null) ? null : (imageName.equals("true")) ? soundID : imageName;
    }



    public String ImageFileName()
    {
        return this.imageName;
    }



    public List<OBSyllable> syllables()
    {
        if (!syllablesChecked)
        {
            String partSylWordAudio = new String(soundID).replace("fc_", "fc_syl_");
            List<List<Double>> sylTiming = OBUtils.ComponentTimingsForWord(partSylWordAudio + ".etpa");
            //
            if (sylTiming.size() > 0)
            {
                List timingSyllables = new ArrayList();
                int index = 0;
                for (OBSyllable syllable : syllables)
                {
                    OBSyllable sylCopy = syllable.copy();
                    sylCopy.audio = partSylWordAudio;
                    sylCopy.timings = (List<Object>) (Object) sylTiming.get(index);
                    timingSyllables.add(sylCopy);
                    index++;
                }
                syllables = timingSyllables;
            }
            syllablesChecked = true;
        }
        return syllables;
    }


    public List<OBPhoneme> phonemes()
    {
        if (!phonemesChecked)
        {
            if (phonemes.size() == 0)
            {
                for (OBSyllable syllable : syllables())
                {
                    phonemes.addAll(syllable.phonemes);
                }
            }
            String partPhoWordAudio = new String(soundID).replace("fc_", "fc_let_");
            List<List<Double>> phoTiming = OBUtils.ComponentTimingsForWord(partPhoWordAudio + ".etpa");
            //
            if (phoTiming.size() > 0)
            {
                List timingPhonemes = new ArrayList();
                int index = 0;
                for (OBPhoneme phoneme : phonemes)
                {
                    OBPhoneme phoCopy = phoneme.copy();
                    phoCopy.audio = partPhoWordAudio;
                    phoCopy.timings = (List<Object>) (Object) phoTiming.get(index);
                    timingPhonemes.add(phoCopy);
                    index++;
                }
                phonemes = timingPhonemes;
            }
            phonemesChecked = true;
        }
        return phonemes;
    }


    public OBWord copy()
    {
        List<OBSyllable> syllablesClone = new ArrayList<OBSyllable>();
        //
        for (OBSyllable syllable : syllables)
        {
            syllablesClone.add(syllable.copy());
        }
        return new OBWord(text, soundID, timings, audio, syllablesClone, imageName);
    }

}
