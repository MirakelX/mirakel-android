package de.azapps.mirakel.main_activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;


public class SurveyFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        WebView webView=new WebView(getActivity());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.loadUrl("https://mirakel1.typeform.com/to/Vr5TU4");
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        return webView;
    }
}
