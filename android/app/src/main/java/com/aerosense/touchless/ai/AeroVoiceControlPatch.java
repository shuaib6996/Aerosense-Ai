package com.aerosense.touchless.ai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.webkit.WebView;
import java.util.ArrayList;

/**
 * Optional Android Native SpeechRecognizer Helper.
 * Keeps Voice Commands ("Next", "Back", "Home", "Comment...") working reliably
 * even when AeroSense AI is in Floating Overlay mode over Instagram or YouTube.
 */
public class AeroVoiceControlPatch {

    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private boolean isListening = false;
    private String currentLang = "en-US";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void startListening(Context context, WebView webView, String languageTag) {
        if (languageTag != null && !languageTag.isEmpty()) {
            currentLang = languageTag;
        }
        mainHandler.post(() -> {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) return;
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
                    speechRecognizer.setRecognitionListener(new RecognitionListener() {
                        @Override public void onReadyForSpeech(Bundle params) {}
                        @Override public void onBeginningOfSpeech() {}
                        @Override public void onRmsChanged(float rmsdB) {}
                        @Override public void onBufferReceived(byte[] buffer) {}
                        @Override public void onEndOfSpeech() {}

                        @Override
                        public void onError(int error) {
                            if (isListening) restartWithDelay();
                        }

                        @Override
                        public void onResults(Bundle results) {
                            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                            if (matches != null && !matches.isEmpty() && webView != null) {
                                String topPhrase = matches.get(0).replace("'", "\\'");
                                webView.evaluateJavascript(
                                    "if(window.onNativeVoiceTranscript) window.onNativeVoiceTranscript('" + topPhrase + "');",
                                    null
                                );
                            }
                            if (isListening) restartWithDelay();
                        }

                        @Override public void onPartialResults(Bundle partialResults) {}
                        @Override public void onEvent(int eventType, Bundle params) {}
                    });
                }

                recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLang);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

                isListening = true;
                speechRecognizer.startListening(recognizerIntent);
            } catch (Exception ignored) {}
        });
    }

    private void restartWithDelay() {
        mainHandler.postDelayed(() -> {
            if (isListening && speechRecognizer != null && recognizerIntent != null) {
                try { speechRecognizer.startListening(recognizerIntent); } catch (Exception ignored) {}
            }
        }, 350);
    }

    public void stopListening() {
        isListening = false;
        mainHandler.post(() -> {
            if (speechRecognizer != null) {
                try {
                    speechRecognizer.stopListening();
                    speechRecognizer.destroy();
                } catch (Exception ignored) {}
                speechRecognizer = null;
            }
        });
    }
}
