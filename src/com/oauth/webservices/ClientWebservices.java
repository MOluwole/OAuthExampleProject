package com.oauth.webservices;

import java.io.ByteArrayOutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

public class ClientWebservices {
	public static final String METAT_API_KEY = "your_api_key";
	public static final String METAT_API_KEY_SECRET = "your_api_key_secret";
	
	public static final String MEETUP_REQUEST_TOKEN_URL = "https://api.meetup.com/oauth/request/";
	public static final String MEETUP_ACCESS_TOKEN_URL = "https://api.meetup.com/oauth/access/";
	public static final String MEETUP_AUTHORIZE_URL = "http://www.meetup.com/authenticate";

	public static final Uri CALLBACK_URI = Uri.parse("oauth://authorized");
	
	private static final String MEMBER_ID = "id";

	private static final String GET_SELF = "https://api.meetup.com/2/member/self?access_token={key}";

	public static Uri startAuthorization(Context context, OAuthConsumer consumer, OAuthProvider provider)
	{
		provider.setOAuth10a(true);

		SharedPreferences settings = context.getSharedPreferences("oauth_prefs", Context.MODE_PRIVATE);

		try {
			String authUrl = provider.retrieveRequestToken(consumer, CALLBACK_URI.toString());

			SharedPreferences.Editor editor = settings.edit();
		
			editor.putString("request_token", consumer.getToken());
			editor.putString("request_secret", consumer.getTokenSecret());
			
			editor.commit();
			
			return Uri.parse(authUrl);
		} 
		catch (OAuthMessageSignerException ex) {
			Log.e("startMeetupAuthorization", Log.getStackTraceString(ex));
		} 
		catch (OAuthNotAuthorizedException ex) {
			Log.e("startMeetupAuthorization", Log.getStackTraceString(ex));
		} 
		catch (OAuthExpectationFailedException ex) {
			Log.e("startMeetupAuthorization", Log.getStackTraceString(ex));
		} 
		catch (OAuthCommunicationException ex) {
			Log.e("startMeetupAuthorization", Log.getStackTraceString(ex));
		}
		
		return null;
	}
	
	public static void completeAuthorization(Context context, OAuthConsumer consumer, OAuthProvider provider, Uri callbackUri)
	{
		provider.setOAuth10a(true);
		SharedPreferences settings = context.getSharedPreferences("oauth_prefs", Context.MODE_PRIVATE);
		
		String token = settings.getString("request_token", null);
		String secret = settings.getString("request_secret", null);

		try {
			if(!(token == null || secret == null)) {
				consumer.setTokenWithSecret(token, secret);
			}
			
			String verifier = callbackUri.getQueryParameter(OAuth.OAUTH_VERIFIER);

			provider.retrieveAccessToken(consumer, verifier);

			token = consumer.getToken();
			secret = consumer.getTokenSecret();

			SharedPreferences.Editor editor = settings.edit();
			
			editor.putString("user_token", token);
			editor.putString("user_secret", secret);
			
			editor.remove("request_token");
			editor.remove("request_secret");
				
			editor.commit();
		}
		catch (OAuthMessageSignerException ex) {
			Log.e("completeMeetupAuthorization", Log.getStackTraceString(ex));
		}
		catch (OAuthNotAuthorizedException ex) {
			Log.e("completeMeetupAuthorization", Log.getStackTraceString(ex));
		}
		catch (OAuthExpectationFailedException ex) {
			Log.e("completeMeetupAuthorization", Log.getStackTraceString(ex));
		} 
		catch (OAuthCommunicationException ex) {
			Log.e("completeMeetupAuthorization", Log.getStackTraceString(ex));
		}
		catch (Exception ex) {
			Log.e("completeMeetupAuthorization", Log.getStackTraceString(ex));
		}
	}
	  
	public static long getCurrentUser(Context context, String meetupKey)
	{
		HttpResponse response = null;
		StatusLine statusLine = null;
		
		try
		{
			HttpClient httpclient = new DefaultHttpClient();
		    response = httpclient.execute(new HttpGet(GET_SELF.replace("{key}", meetupKey)));

		    statusLine = response.getStatusLine();
		}
		catch (Exception ex)
		{
			Log.e("getCurrentUser()", Log.getStackTraceString(ex));
			return -1;
		}

	    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        
	        try {
	        	response.getEntity().writeTo(out);
		        out.close();
	        }
	        catch (Exception ex) {
				Log.e("getCurrentUser()", Log.getStackTraceString(ex));
				
				return -1;
	        }
	        
	        String responseString = out.toString();
	        
	        try {
				JSONObject contact = new JSONObject(responseString);

		        return contact.getLong(MEMBER_ID);
			}
	        catch (JSONException ex) {
				Log.e("getCurrentUser()", Log.getStackTraceString(ex));
				
				return -1;
			}
	    }
	    else if(statusLine.getStatusCode() == HttpStatus.SC_UNAUTHORIZED){
			return -2;
	    }
	    else {
	        try {
	        	response.getEntity().getContent().close();
	        }
	        catch (Exception ex) {
				Log.e("getCurrentUser()", Log.getStackTraceString(ex));
	        }

			return -1;
	    }
	}
}
