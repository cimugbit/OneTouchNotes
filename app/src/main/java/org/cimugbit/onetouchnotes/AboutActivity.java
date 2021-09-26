package org.cimugbit.onetouchnotes;

import android.os.Bundle;
import android.webkit.WebView;

public class AboutActivity extends ChildActivity {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_about );
        WebView webView = (WebView) findViewById( R.id.webview );
        webView.loadUrl( "file:///android_res/raw/about.html" );

        setTitle( R.string.title_about );
    }
}
