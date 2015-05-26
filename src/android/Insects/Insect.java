package info.newforestcicada.plugin.insects;

import info.newforestcicada.audiorecorder.plugin.LongTermResult;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class Insect {
	
	private int id;
	private String name;
	protected ArrayList<Float> longTermValues = new ArrayList<Float>();
	
	public Insect(int id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public JSONObject toJSON() throws JSONException{
		JSONObject json = new JSONObject();
		json.put("insect", this.id);
		json.put("name"  , this.name);
		return json;
	}
	
	@Override
	public String toString(){
		//return this.name;
		
		try {
			return toJSON().toString();
		} catch (JSONException e) {
			return this.name;
		}
		
	}
	
	public abstract float getValue(float[] freqs);
	public abstract LongTermResult getLongTermResult();

	public int getId() {
		return this.id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void updateLongTermValue(float[] freqs) {
		longTermValues.add(getValue(freqs));
	}
	
}
