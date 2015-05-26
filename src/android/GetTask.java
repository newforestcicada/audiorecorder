package info.newforestcicada.audiorecorder.plugin;

import java.io.IOException;
import java.net.ConnectException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class GetTask extends AsyncTask<Void, Integer, Void> {

	private static final String HTTP_CALIBRATION = "http://api.newforestcicada.info/emissions/%s/?format=json";
	private static final String LOG_TAG = "GET_CALIBR";
	private Context context;
	
	public GetTask(Context context) {
		super();
		this.context = context;
	}

	protected Void doInBackground(Void... voids) {
		HttpClient httpclient = new DefaultHttpClient();
		httpclient.getParams().setParameter(
				CoreProtocolPNames.PROTOCOL_VERSION,
				HttpVersion.HTTP_1_1);

		HttpGet httpget = new HttpGet(String.format(HTTP_CALIBRATION, AudioAnalyser.phoneModel).
				replace(" ", "%20"));

		System.out.println("executing request "
				+ httpget.getRequestLine());
		HttpResponse response = null;
		HttpEntity resEntity = null;
		try {
			response = httpclient.execute(httpget);
			resEntity = response.getEntity();
			System.out.println(response.getStatusLine());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (ConnectException e) {
			Log.e(LOG_TAG, e.getStackTrace().toString());
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (resEntity != null) {
			try {
				String response_text = EntityUtils.toString(resEntity);
				JSONObject json = new JSONObject(response_text);
				String mean = json.getString("mean");
				String var  = json.getString("variance");
				Emission.updateEmissions(mean, var, context);
				Log.d(LOG_TAG, "New emissions are:\n"
						+ "MEAN: "+mean+"\n"
						+ " VAR: "+var);
				
			} catch (ParseException e) {
				e.printStackTrace();
				Log.e(LOG_TAG, "Could not parse the response");
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(LOG_TAG, "Could not make the http call");
			} catch (JSONException e) {
				Log.e(LOG_TAG, "Could not parse the JSON object in the response");
				e.printStackTrace();
			}
		}

		httpclient.getConnectionManager().shutdown();
		return null;
	}
}