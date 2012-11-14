package com.oauth.exampleproject;

import com.oauth.webservices.ClientWebservices;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class MainActivity extends Activity {
	private OAuthConsumer _consumer;
	private OAuthProvider _provider;

	private String _userToken = "";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    	_consumer = new CommonsHttpOAuthConsumer(ClientWebservices.METAT_API_KEY, ClientWebservices.METAT_API_KEY_SECRET);
    	_provider = new CommonsHttpOAuthProvider(ClientWebservices.MEETUP_REQUEST_TOKEN_URL, ClientWebservices.MEETUP_ACCESS_TOKEN_URL, ClientWebservices.MEETUP_AUTHORIZE_URL);

        SharedPreferences settings = getSharedPreferences("oauth_prefs", Context.MODE_PRIVATE);
		
        if (settings.getString("user_token", null) != null)
        {
        	_userToken = settings.getString("user_token", "");
        	
        	ClientWebservices.getCurrentUser(this, _userToken);
    	}
        else
        {
        	StartAuthenticateMeetupTask startAuthenticateMeetupTask = new StartAuthenticateMeetupTask(this);
        	startAuthenticateMeetupTask.execute();
        }
    }

	@Override
	protected void onResume()
	{
		super.onResume();
		
		Uri uri = getIntent().getData();
		
		if ((uri != null) && (ClientWebservices.CALLBACK_URI.getScheme().equals(uri.getScheme())))
		{
			FinishAuthenticateMeetupTask finishAuthenticateMeetupTask = new FinishAuthenticateMeetupTask(this, uri);
			finishAuthenticateMeetupTask.execute();
		}
	}
	
	private class StartAuthenticateMeetupTask extends AsyncTask<String, String, String>
	{
		private Activity _parentActivity;
		
		public StartAuthenticateMeetupTask(Activity activity)
		{
			_parentActivity = activity;
		}
		
		@Override
		protected String doInBackground(String... strings) {
			Intent i = _parentActivity.getIntent();
			
			if (i.getData() == null) {
				Uri authenticationUri = ClientWebservices.startAuthorization(_parentActivity.getBaseContext(), _consumer, _provider);
				
				if (authenticationUri != null)
					return authenticationUri.toString();
				else
				{
					return "";
				}
			}
			else
				return "";
		}
		
		@Override
        protected void onPostExecute(String result) {
			if (result.trim().length() > 0)
			{
				_parentActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(result)));
			}
		}
	}
	
	private class FinishAuthenticateMeetupTask extends AsyncTask<String, String, String>
	{
		private Activity _parentActivity;
		private Uri _meetupUri;
		
		public FinishAuthenticateMeetupTask(Activity activity, Uri uri)
		{
			_parentActivity = activity;
			_meetupUri = uri;
		}
		
		@Override
		protected String doInBackground(String... strings) {
			ClientWebservices.completeAuthorization(_parentActivity, _consumer, _provider, _meetupUri);
	        
			return "";
		}
		
		@Override
        protected void onPostExecute(String result) {
			SharedPreferences settings = getSharedPreferences("oauth_prefs" , Context.MODE_PRIVATE);
        	_userToken = settings.getString("user_token", "");
        	getIntent().setData(null);
        	
	        if (settings.getString("user_token", null) != null)
	        {
	        	_userToken = settings.getString("user_token", "");
	        }
		}
	}
}
