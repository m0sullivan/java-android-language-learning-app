package com.example.duolingo2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.widget.TextView;
import android.widget.Button;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
import java.util.Arrays;
import java.util.HashMap;
import java.io.InputStream;
import java.io.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.*;
import java.util.Random;
import org.json.JSONObject;

class TranslateSpan extends ClickableSpan {
    @Override
    public void onClick(View widget) {
            TextView tv = (TextView) widget;
            Spanned s = (Spanned) tv.getText();
            int start = s.getSpanStart(this);
            int end = s.getSpanEnd(this);
            Log.d("myApp", "[" + s.subSequence(start, end) + "]");

    }
    @Override
    public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(true);
    }
};
public class MainActivity extends AppCompatActivity {

    Random rand = new Random();

    ArrayList<String> lines = new ArrayList<String>();
    ArrayList<String> sourceLines = new ArrayList<String>();
    ArrayList<String> translatedLines = new ArrayList<String>();
    HashMap<String, String> translationLines = new HashMap<String, String>();
    HashMap<String, String> audioMap = new HashMap<String, String>();

    int selectedAnswer;

    public void startSound(String filename) {
        AssetFileDescriptor afd = null;
        try {
            afd = getResources().getAssets().openFd(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaPlayer player = new MediaPlayer();
        try {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        player.start();
    }

    public void setQuestion(
            ArrayList<String> sourceLines,
            HashMap<String, String> translationLines,
            ArrayList<String> translatedLines,
            HashMap<String, String> audioMap
    ) {
        int selectedSentence = rand.nextInt(sourceLines.size());

        Button a = (Button)findViewById(R.id.buttonA);
        Button b = (Button)findViewById(R.id.buttonB);
        Button c = (Button)findViewById(R.id.buttonC);
        Button d = (Button)findViewById(R.id.buttonD);

        ArrayList<Button> buttons = new ArrayList<>(Arrays.asList(a,b,c,d));

        TextView source = (TextView)findViewById(R.id.source);
        String sourceText = sourceLines.get(selectedSentence);

        SpannableString ss = new SpannableString(sourceText);

        ArrayList<Integer> spaceList = new ArrayList<Integer>();

        for(int i = 0; i < sourceText.length(); i++) {
            if(sourceText.charAt(i) == ' ') {
                spaceList.add(i);
                Log.i("myApp", Integer.toString(i));
            }
        }

        for(int i = 0; i < spaceList.size(); i++) {
            if(i == spaceList.size() - 1) {
                if(i == 0) {
                    ss.setSpan(new TranslateSpan(), 0, spaceList.get(i), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ss.setSpan(new TranslateSpan(), spaceList.get(i) + 1, sourceText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    ss.setSpan(new TranslateSpan(), (spaceList.get(i - 1) + 1), spaceList.get(i), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ss.setSpan(new TranslateSpan(), spaceList.get(i) + 1, sourceText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else if(i == 0) {
                ss.setSpan(new TranslateSpan(), 0, spaceList.get(i), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                ss.setSpan(new TranslateSpan(), (spaceList.get(i - 1) + 1), spaceList.get(i), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        source.setText(ss);
        source.setMovementMethod(LinkMovementMethod.getInstance());
        source.setHighlightColor(Color.TRANSPARENT);

        //source.setText(sourceText);

        selectedAnswer = rand.nextInt(buttons.size());

        for(int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setBackgroundColor(Color.parseColor("#ff6750a4"));
            if(i != selectedAnswer) {
                int selectedIncorrect = rand.nextInt(sourceLines.size());
                while(selectedIncorrect == selectedSentence) {
                    selectedIncorrect = rand.nextInt(sourceLines.size());
                }
                buttons.get(i).setText(translatedLines.get(selectedIncorrect));
            } else {
                buttons.get(i).setText(translatedLines.get(selectedSentence));
            }


        }
        Button nextButton = (Button)findViewById(R.id.nextButton);
        nextButton.setEnabled(false);

        startSound(audioMap.get(sourceText)+".mp3");

    }

    public void checkAnswer() {
        Button a = (Button)findViewById(R.id.buttonA);
        Button b = (Button)findViewById(R.id.buttonB);
        Button c = (Button)findViewById(R.id.buttonC);
        Button d = (Button)findViewById(R.id.buttonD);

        ArrayList<Button> buttons = new ArrayList<>(Arrays.asList(a,b,c,d));

        TextView source = (TextView)findViewById(R.id.source);
        String sourceText = source.getText().toString();

        for(int i = 0; i < buttons.size(); i++) {
            if(i == selectedAnswer) {
                buttons.get(i).setBackgroundColor(Color.parseColor("#33cc33"));
            } else {
                buttons.get(i).setBackgroundColor(Color.parseColor("#e80000"));
            }

        }
        Button nextButton = (Button)findViewById(R.id.nextButton);
        nextButton.setEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(getAssets().open("source.csv"), "UTF-8"));

            String mLine;
            while ((mLine = reader.readLine()) != null) {
                lines.add(mLine);
            }
        } catch (IOException e) {
            Log.i("myApp", "error");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.i("myApp", "error");
                }
            }
        }

        Pattern patternSource = Pattern.compile(".+(?=\\[sound:\\d+\\.mp3\\]\\t)");
        Pattern patternTranslation = Pattern.compile("(?<=\\t).+");
        Pattern patternAudio = Pattern.compile("(?<=\\:).+(?=\\.mp3\\])");
        for(int i = 0; i < lines.size(); i++) {
            Matcher m = patternSource.matcher(lines.get(i));

            Matcher mAudio = patternAudio.matcher(lines.get(i));
            String tmpLine = "";


            while(m.find()) {
                tmpLine = lines.get(i).substring(m.start(), m.end());
                sourceLines.add(tmpLine);
            }
            while(mAudio.find()) {
                audioMap.put(tmpLine, lines.get(i).substring(mAudio.start(), mAudio.end()));
            }


            Matcher matcherTranslation = patternTranslation.matcher(lines.get(i));

            while (matcherTranslation.find()) {
                if (tmpLine != null) {
                    translationLines.put(lines.get(i).substring(matcherTranslation.start(), matcherTranslation.end()), tmpLine);
                    translatedLines.add(lines.get(i).substring(matcherTranslation.start(), matcherTranslation.end()));
                }
            }
        }


        setQuestion(sourceLines, translationLines, translatedLines, audioMap);

        findViewById(R.id.buttonA).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkAnswer();
            }
        });
        findViewById(R.id.buttonB).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkAnswer();
            }
        });
        findViewById(R.id.buttonC).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkAnswer();
            }
        });
        findViewById(R.id.buttonD).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkAnswer();
            }
        });
        findViewById(R.id.nextButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setQuestion(sourceLines, translationLines, translatedLines, audioMap);
            }
        });
        findViewById(R.id.audioNormal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView source = (TextView)findViewById(R.id.source);
                String sourceText = source.getText().toString();
                startSound(audioMap.get(sourceText)+".mp3");
            }
        });
        findViewById(R.id.audioSlow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView source = (TextView)findViewById(R.id.source);
                String sourceText = source.getText().toString();
                startSound(audioMap.get(sourceText)+"-slowed.mp3");
            }
        });



    }
}