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

	private static String UserToken = "";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the provider and consumer for authenticating the app against Meetup using oauth
    	_consumer = new CommonsHttpOAuthConsumer(ClientWebservices.METAT_API_KEY, ClientWebservices.METAT_API_KEY_SECRET);
    	_provider = new CommonsHttpOAuthProvider(ClientWebservices.MEETUP_REQUEST_TOKEN_URL, ClientWebservices.MEETUP_ACCESS_TOKEN_URL, ClientWebservices.MEETUP_AUTHORIZE_URL);

        SharedPreferences settings = getSharedPreferences("oauth_prefs", Context.MODE_PRIVATE);
		
        // Check if a user token has been specified in shared preferences. If so, it is assumed that they have previously authenticated
        if (settings.getString("user_token", null) != null)
        {
        	UserToken = settings.getString("user_token", "");
        	
        	// This actually won't work as it's not on a thread and webservices cannot be ran on the main thread. Just an example of how to use the token when retrieved
        	ClientWebservices.getCurrentUser(this, UserToken);
    	}
        else
        {
        	// It is assumed that if the user token is not specified then the app needs to authenticate. This is done in an asynch task as webservices cannot be ran on the main thread
        	StartAuthenticateMeetupTask startAuthenticateMeetupTask = new StartAuthenticateMeetupTask(this);
        	startAuthenticateMeetupTask.execute();
        }
    }

	@Override
	protected void onResume()
	{
		super.onResume();
		
		Uri uri = getIntent().getData();
		
		// Check on the onResume if the callback URL was specified and if so finish off the oauth authentication
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
			// Store the parent activity so that it can be used in the asynch task
			_parentActivity = activity;
		}
		
		@Override
		protected String doInBackground(String... strings) {
			Intent i = _parentActivity.getIntent();
			
			if (i.getData() == null) {
				// Retrieve the URL for authenticating against Meetup
				Uri authenticationUri = ClientWebservices.startAuthorization(_parentActivity.getBaseContext(), _consumer, _provider);
				
				// If the URL was correctly generated then pass if back for use on the post execute. This allows it to be performed back on the main thread.
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
				// On the main thread call Meetup with the request URL (including callback)
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
			// Finish authentication with the returned token. This will also store the user token in shared preferences
			ClientWebservices.completeAuthorization(_parentActivity, _consumer, _provider, _meetupUri);
	        
			return "";
		}
		
		@Override
        protected void onPostExecute(String result) {
			SharedPreferences settings = getSharedPreferences("oauth_prefs" , Context.MODE_PRIVATE);
			UserToken = settings.getString("user_token", "");
        	getIntent().setData(null);
        	
        	// Check that the user token was successfully set and put the token onto the activity variable for use in methods
	        if (settings.getString("user_token", null) != null)
	        {
	        	UserToken = settings.getString("user_token", "");
	        }
		}
	}
}
