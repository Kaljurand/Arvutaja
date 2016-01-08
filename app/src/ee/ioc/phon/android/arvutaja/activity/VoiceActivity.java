package ee.ioc.phon.android.arvutaja.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.VoiceInteractor;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.speech.RecognizerIntent;

import ee.ioc.phon.android.arvutaja.ArvutajaActivity;
import ee.ioc.phon.android.arvutaja.Log;
import ee.ioc.phon.android.speechutils.Extras;

public class VoiceActivity extends Activity {

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (false && isVoiceInteractionRoot()) {
                // TODO: disabled for the time being
                Log.i("isVoiceInteractionRoot");
                launchArvutaja("et-EE");
            } else if (isVoiceInteraction()) {
                Log.i("isVoiceInteraction");
                VoiceInteractor.PickOptionRequest.Option option1 = new VoiceInteractor.PickOptionRequest.Option("et-EE", 0);
                option1.addSynonym("in Estonian");
                option1.addSynonym("Estonian");

                VoiceInteractor.PickOptionRequest.Option option2 = new VoiceInteractor.PickOptionRequest.Option("en-US", 0);
                option2.addSynonym("in English");
                option2.addSynonym("English");

                VoiceInteractor.Prompt prompt = new VoiceInteractor.Prompt("In which language?");

                getVoiceInteractor()
                        .submitRequest(new VoiceInteractor.PickOptionRequest(prompt, new VoiceInteractor.PickOptionRequest.Option[]{option1, option2}, null) {
                            @Override
                            public void onPickOptionResult(boolean finished, Option[] selections, Bundle result) {
                                if (finished && selections.length == 1) {
                                    Message message = Message.obtain();
                                    message.obj = result;
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        launchArvutaja(selections[0].getLabel().toString());
                                    }
                                } else {
                                    Log.i("onPickOptionResult = failed");
                                }
                            }

                            @Override
                            public void onCancel() {
                                Log.i("getVoiceInteractor(): onCancel");
                            }
                        });
            } else {
                Log.i("isVoiceInteraction = false");
            }
        }
    }


    private void launchArvutaja(String lang) {
        Intent intent = new Intent(this, ArvutajaActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Extras.EXTRA_LAUNCH_RECOGNIZER, true);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
        startActivity(intent);
        finish();
    }
}
